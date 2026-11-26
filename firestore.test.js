const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PLAYER_A_UID = "player_a_101";
const PLAYER_B_UID = "player_b_202";
const ADMIN_UID = "admin_clutchx_001";
const ADMIN_EMAIL = "gangame424@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_999", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function playerContext(uid, email = `${uid}@clutchx.np`) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
}

function validTournamentPayload(overrides = {}) {
  const now = Date.now();
  const ts = new Date(now - 2000);
  return {
    title: "Himalayan Clash S1",
    description: "Nepali Free Fire Squad Championship",
    game: "FREE_FIRE",
    mode: "SQUAD",
    entryFeeNpr: 0,
    prizePoolNpr: 25000,
    maxParticipants: 48,
    registeredCount: 0,
    status: "UPCOMING",
    published: true,
    featured: true,
    demo: true,
    registrationDeadlineMs: now + 3600000,
    startTimeMs: now + 7200000,
    killPoints: 2,
    booyahPoints: 12,
    placementRule: "#1: 12, #2: 9, #3: 8",
    tieBreakerRule: "MOST_KILLS",
    rulesSummary: "No emulators, fair play enforced.",
    createdBy: ADMIN_UID,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied on all protected collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("tournaments").where("published", "==", true).get());
  await assertFails(db.collection("users").doc(PLAYER_A_UID).get());
  await assertFails(db.collection("registrations").where("memberUids", "array-contains", PLAYER_A_UID).get());
  await assertFails(db.collection("notifications").where("userId", "==", PLAYER_A_UID).get());
  await assertFails(db.collection("matches").doc("m1").collection("private").doc("credentials").get());
});

test("User profile isolation and privilege escalation prevention", async () => {
  const dbA = playerContext(PLAYER_A_UID).firestore();
  const dbB = playerContext(PLAYER_B_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  // Player A creates own profile as PLAYER
  await assertSucceeds(
    dbA.collection("users").doc(PLAYER_A_UID).set({
      userId: PLAYER_A_UID,
      username: "KathmanduSniper",
      email: "player_a_101@clutchx.np",
      avatarKey: "phoenix",
      freeFireUid: "2233445566",
      pubgUid: "5123456789",
      language: "EN",
      role: "PLAYER",
      matchesPlayed: 0,
      totalKills: 0,
      tournamentWins: 0,
      totalPoints: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Player A cannot self-escalate role to ADMIN
  await assertFails(
    dbA.collection("users").doc(PLAYER_A_UID).update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Player A can update allowed profile fields
  await assertSucceeds(
    dbA.collection("users").doc(PLAYER_A_UID).update({
      username: "EverestClutch",
      language: "NE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Player B cannot read or list Player A's profile (PII protection)
  await assertFails(dbB.collection("users").doc(PLAYER_A_UID).get());
  await assertFails(dbB.collection("users").get());
});

test("Admin CRUD on tournaments & email spoofing rejection", async () => {
  const adminDb = adminContext().firestore();
  const spoofDb = spoofedAdminContext().firestore();
  const playerDb = playerContext(PLAYER_A_UID).firestore();

  // Spoofed email (email_verified: false) cannot create tournament
  await assertFails(
    spoofDb.collection("tournaments").doc("t_spoof").set(validTournamentPayload({ createdBy: "spoof_999" }))
  );

  // Non-admin player cannot create tournament
  await assertFails(
    playerDb.collection("tournaments").doc("t_player").set(validTournamentPayload({ createdBy: PLAYER_A_UID }))
  );

  // Verified admin creates DRAFT and UPCOMING tournaments
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_draft").set(
      validTournamentPayload({ status: "DRAFT", published: false })
    )
  );
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_live").set(validTournamentPayload())
  );

  // Admin updates prizePoolNpr and status
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_live").update({
      prizePoolNpr: 50000,
      status: "UPCOMING",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Player filtered query (published == true) succeeds; unfiltered query fails; DRAFT get fails
  await assertSucceeds(
    playerDb.collection("tournaments").where("published", "==", true).get()
  );
  await assertFails(playerDb.collection("tournaments").get());
  await assertFails(playerDb.collection("tournaments").doc("t_draft").get());

  // Admin can list all tournaments including DRAFT and delete
  await assertSucceeds(adminDb.collection("tournaments").get());
  await assertSucceeds(adminDb.collection("tournaments").doc("t_draft").delete());
});

test("Free tournament registration succeeds; paid & duplicate/tampered writes fail", async () => {
  const adminDb = adminContext().firestore();
  const playerDb = playerContext(PLAYER_A_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_free").set(
      validTournamentPayload({ mode: "SOLO", entryFeeNpr: 0, maxParticipants: 16 })
    )
  );
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_paid").set(
      validTournamentPayload({ mode: "SOLO", entryFeeNpr: 200, maxParticipants: 16 })
    )
  );

  // Player registers for free tournament
  await assertSucceeds(
    playerDb.collection("registrations").doc(`t_free_${PLAYER_A_UID}`).set({
      tournamentId: "t_free",
      tournamentTitle: "Himalayan Clash S1",
      game: "FREE_FIRE",
      mode: "SOLO",
      userId: PLAYER_A_UID,
      username: "KathmanduSniper",
      inGameUid: "2233445566",
      teamId: "",
      teamName: "",
      memberUids: [PLAYER_A_UID],
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    playerDb.collection("tournaments").doc("t_free").collection("participants").doc(PLAYER_A_UID).set({
      tournamentId: "t_free",
      userId: PLAYER_A_UID,
      registrationId: `t_free_${PLAYER_A_UID}`,
      status: "APPROVED",
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    playerDb.collection("tournaments").doc("t_free").update({
      registeredCount: 1,
      status: "UPCOMING",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Player CANNOT register for paid tournament while payment processing is disabled
  await assertFails(
    playerDb.collection("registrations").doc(`t_paid_${PLAYER_A_UID}`).set({
      tournamentId: "t_paid",
      tournamentTitle: "Himalayan Clash S1",
      game: "FREE_FIRE",
      mode: "SOLO",
      userId: PLAYER_A_UID,
      username: "KathmanduSniper",
      inGameUid: "2233445566",
      teamId: "",
      teamName: "",
      memberUids: [PLAYER_A_UID],
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Player CANNOT tamper with prizePoolNpr during slot increment
  await assertFails(
    playerDb.collection("tournaments").doc("t_free").update({
      registeredCount: 2,
      prizePoolNpr: 999999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Private room credentials stay hidden until released and approved", async () => {
  const adminDb = adminContext().firestore();
  const dbA = playerContext(PLAYER_A_UID).firestore();
  const dbB = playerContext(PLAYER_B_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_room").set(validTournamentPayload({ mode: "SOLO" }))
  );
  await assertSucceeds(
    adminDb.collection("matches").doc("m_1").set({
      tournamentId: "t_room",
      tournamentTitle: "Himalayan Clash S1",
      roundName: "Finals Match 1",
      matchNumber: 1,
      mapName: "Bermuda",
      game: "FREE_FIRE",
      scheduledTimeMs: Date.now() + 3600000,
      status: "SCHEDULED",
      credentialsReleased: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("matches").doc("m_1").collection("private").doc("credentials").set({
      matchId: "m_1",
      tournamentId: "t_room",
      roomId: "88442211",
      roomPassword: "CLUTCHNP",
      notes: "Join 10 mins early",
      released: false,
      updatedAt: ts,
    })
  );

  // Approved participant A added
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_room").collection("participants").doc(PLAYER_A_UID).set({
      tournamentId: "t_room",
      userId: PLAYER_A_UID,
      registrationId: `t_room_${PLAYER_A_UID}`,
      status: "APPROVED",
      updatedAt: ts,
    })
  );

  // Before release: even approved participant A is denied
  await assertFails(dbA.collection("matches").doc("m_1").collection("private").doc("credentials").get());

  // Admin releases credentials
  await assertSucceeds(
    adminDb.collection("matches").doc("m_1").update({
      credentialsReleased: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("matches").doc("m_1").collection("private").doc("credentials").update({
      released: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Approved participant A can now read; non-participant B is still denied
  await assertSucceeds(dbA.collection("matches").doc("m_1").collection("private").doc("credentials").get());
  await assertFails(dbB.collection("matches").doc("m_1").collection("private").doc("credentials").get());
});

test("Team invitations can only be answered once", async () => {
  const dbA = playerContext(PLAYER_A_UID, "captain@clutchx.np").firestore();
  const dbB = playerContext(PLAYER_B_UID, "teammate@clutchx.np").firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    dbA.collection("teams").doc("team_1").set({
      name: "Gurkha Esports",
      tag: "GRK",
      game: "FREE_FIRE",
      captainId: PLAYER_A_UID,
      captainName: "CaptainA",
      logoKey: "shield",
      memberUids: [PLAYER_A_UID],
      memberNames: ["CaptainA"],
      matchesPlayed: 0,
      wins: 0,
      totalKills: 0,
      totalPoints: 0,
      publicRoster: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    dbA.collection("team_invitations").doc("inv_1").set({
      teamId: "team_1",
      teamName: "Gurkha Esports",
      game: "FREE_FIRE",
      captainId: PLAYER_A_UID,
      captainName: "CaptainA",
      invitedEmail: "teammate@clutchx.np",
      invitedByName: "CaptainA",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // First answer succeeds
  await assertSucceeds(
    dbB.collection("team_invitations").doc("inv_1").update({
      status: "ACCEPTED",
      answeredByUid: PLAYER_B_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Second answer fails (cannot re-answer)
  await assertFails(
    dbB.collection("team_invitations").doc("inv_1").update({
      status: "REJECTED",
      answeredByUid: PLAYER_B_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Notification collection enforces isolation, admin approvals, and match reminders", async () => {
  const adminDb = adminContext().firestore();
  const dbA = playerContext(PLAYER_A_UID).firestore();
  const dbB = playerContext(PLAYER_B_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  // Admin creates a registration approval notification for Player A
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_approval_a").set({
      userId: PLAYER_A_UID,
      title: "Registration Approved!",
      body: "Your slot for Himalayan Clash S1 is approved.",
      type: "REGISTRATION_APPROVAL",
      relatedId: "t_1",
      read: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin dispatches a match reminder notification for Player A
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_reminder_a").set({
      userId: PLAYER_A_UID,
      title: "Match Reminder: Match 1",
      body: "Himalayan Clash S1 starts soon in NST.",
      type: "MATCH_REMINDER",
      relatedId: "t_1",
      read: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Player A can self-create a deterministic upcoming match reminder doc
  await assertSucceeds(
    dbA.collection("notifications").doc(`rem_m1_${PLAYER_A_UID}`).set({
      userId: PLAYER_A_UID,
      title: "Match Reminder: Finals",
      body: "Starts in 30 mins (NST).",
      type: "MATCH_REMINDER",
      relatedId: "t_1",
      read: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Player A CANNOT spoof/send a notification to Player B
  await assertFails(
    dbA.collection("notifications").doc("notif_spoof_b").set({
      userId: PLAYER_B_UID,
      title: "Fake Admin Alert",
      body: "Spoofed notification",
      type: "MATCH_REMINDER",
      relatedId: "t_1",
      read: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Player A CANNOT create notification with invalid type
  await assertFails(
    dbA.collection("notifications").doc("notif_bad_type").set({
      userId: PLAYER_A_UID,
      title: "Test",
      body: "Body",
      type: "ARBITRARY_TYPE",
      relatedId: "t_1",
      read: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Player A can list own notifications and mark as read
  await assertSucceeds(dbA.collection("notifications").where("userId", "==", PLAYER_A_UID).get());
  await assertSucceeds(
    dbA.collection("notifications").doc("notif_approval_a").update({
      read: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Player A cannot alter notification title/body when marking read
  await assertFails(
    dbA.collection("notifications").doc("notif_reminder_a").update({
      read: true,
      title: "Tampered Title",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Player B cannot read or list Player A's notifications or run unfiltered list
  await assertFails(dbB.collection("notifications").doc("notif_approval_a").get());
  await assertFails(dbB.collection("notifications").where("userId", "==", PLAYER_A_UID).get());
  await assertFails(dbB.collection("notifications").get());
});

