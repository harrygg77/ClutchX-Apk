package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AppLanguage
import com.example.data.model.AppNotification
import com.example.data.model.GameMode
import com.example.data.model.GameType
import com.example.data.model.LeaderboardPlayer
import com.example.data.model.MatchCredentials
import com.example.data.model.MatchItem
import com.example.data.model.MatchResult
import com.example.data.model.Registration
import com.example.data.model.ReportItem
import com.example.data.model.Reward
import com.example.data.model.Team
import com.example.data.model.TeamInvitation
import com.example.data.model.TieBreakerRule
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.model.UserProfile
import com.example.i18n.NstTimeFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ClutchXRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logAndRethrow(op: OperationType, path: String, expectedUid: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == expectedUid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- USER PROFILE & ADMIN CHECK ---

    suspend fun bootstrapUserIfNeeded(user: FirebaseUser): Result<Boolean> = try {
        val uid = user.uid
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        val displayName = (user.displayName ?: user.email?.substringBefore("@") ?: "Player")
            .trim()
            .take(36)
            .ifBlank { "Player" }
        val email = user.email ?: "${uid}@clutchx.np"

        val isAdmin = (user.isEmailVerified && email == BOOTSTRAP_ADMIN_EMAIL) ||
            try {
                db.collection("admins").document(uid).get().await().exists()
            } catch (_: Exception) {
                false
            }

        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "username" to displayName,
                "email" to email,
                "avatarKey" to "phoenix",
                "freeFireUid" to "",
                "pubgUid" to "",
                "language" to AppLanguage.EN.name,
                "role" to "PLAYER",
                "matchesPlayed" to 0L,
                "totalKills" to 0L,
                "tournamentWins" to 0L,
                "totalPoints" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.set(payload).await()
        }

        val lbRef = db.collection("leaderboard_players").document(uid)
        if (!lbRef.get().await().exists()) {
            lbRef.set(
                mapOf(
                    "userId" to uid,
                    "username" to displayName,
                    "avatarKey" to "phoenix",
                    "primaryGame" to GameType.FREE_FIRE.name,
                    "matchesPlayed" to 0L,
                    "totalKills" to 0L,
                    "tournamentWins" to 0L,
                    "totalPoints" to 0L,
                    "published" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(isAdmin)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}")
        Result.failure(e)
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        require(uid.isNotBlank()) { "UID must not be blank" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    suspend fun updateProfile(
        username: String,
        avatarKey: String,
        freeFireUid: String,
        pubgUid: String,
        language: AppLanguage
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanName = username.trim()
            require(cleanName.length in 2..40) { "Username must be 2-40 characters." }
            require(freeFireUid.length <= 32 && pubgUid.length <= 32) { "UIDs must be under 32 characters." }
            path = "users/$uid"
            db.collection("users").document(uid).update(
                mapOf(
                    "username" to cleanName,
                    "avatarKey" to avatarKey,
                    "freeFireUid" to freeFireUid.trim(),
                    "pubgUid" to pubgUid.trim(),
                    "language" to language.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            // Sync public leaderboard display name & avatar
            db.collection("leaderboard_players").document(uid).update(
                mapOf(
                    "username" to cleanName,
                    "avatarKey" to avatarKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- TOURNAMENTS (PUBLIC DISCOVERY & ADMIN CRUD) ---

    fun observePublishedTournaments(uid: String): Flow<List<Tournament>> = flow {
        emitAll(
            db.collection("tournaments")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.startTimeMs }
                }
                .logAndRethrow(OperationType.LIST, "tournaments", uid)
        )
    }

    fun observeAdminTournaments(uid: String): Flow<List<Tournament>> = flow {
        emitAll(
            db.collection("tournaments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "tournaments", uid)
        )
    }

    suspend fun createTournament(
        title: String,
        description: String,
        game: GameType,
        mode: GameMode,
        entryFeeNpr: Long,
        prizePoolNpr: Long,
        maxParticipants: Long,
        status: TournamentStatus,
        featured: Boolean,
        demo: Boolean,
        registrationDeadlineMs: Long,
        startTimeMs: Long,
        killPoints: Long,
        booyahPoints: Long,
        placementRule: String,
        tieBreakerRule: TieBreakerRule,
        rulesSummary: String
    ): Result<String> {
        var docPath = "tournaments"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            require(cleanTitle.length in 3..100) { "Tournament title must be 3-100 characters." }
            require(entryFeeNpr in 0L..100000L) { "Entry fee must be 0 to 100,000 NPR." }
            require(prizePoolNpr in 0L..10000000L) { "Prize pool must be 0 to 10,000,000 NPR." }
            require(maxParticipants in 2L..1000L) { "Max participants must be 2-1000." }

            val docRef = db.collection("tournaments").document()
            docPath = docRef.path
            val isPublished = status != TournamentStatus.DRAFT
            val payload = mapOf(
                "title" to cleanTitle,
                "description" to description.trim().take(1500),
                "game" to game.name,
                "mode" to mode.name,
                "entryFeeNpr" to entryFeeNpr,
                "prizePoolNpr" to prizePoolNpr,
                "maxParticipants" to maxParticipants,
                "registeredCount" to 0L,
                "status" to status.name,
                "published" to isPublished,
                "featured" to featured,
                "demo" to demo,
                "registrationDeadlineMs" to registrationDeadlineMs,
                "startTimeMs" to startTimeMs,
                "killPoints" to killPoints.coerceIn(0L, 100L),
                "booyahPoints" to booyahPoints.coerceIn(0L, 200L),
                "placementRule" to placementRule.trim().take(300),
                "tieBreakerRule" to tieBreakerRule.name,
                "rulesSummary" to rulesSummary.trim().take(2000),
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTournament(
        tournamentId: String,
        title: String,
        description: String,
        game: GameType,
        mode: GameMode,
        entryFeeNpr: Long,
        prizePoolNpr: Long,
        maxParticipants: Long,
        status: TournamentStatus,
        featured: Boolean,
        registrationDeadlineMs: Long,
        startTimeMs: Long,
        killPoints: Long,
        booyahPoints: Long,
        placementRule: String,
        tieBreakerRule: TieBreakerRule,
        rulesSummary: String
    ): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            requireUserId()
            require(tournamentId.isNotBlank()) { "Tournament ID required." }
            val cleanTitle = title.trim()
            require(cleanTitle.length in 3..100) { "Tournament title must be 3-100 characters." }
            require(entryFeeNpr in 0L..100000L) { "Entry fee must be 0 to 100,000 NPR." }
            require(prizePoolNpr in 0L..10000000L) { "Prize pool must be 0 to 10,000,000 NPR." }
            require(maxParticipants in 2L..1000L) { "Max participants must be 2-1000." }

            val isPublished = status != TournamentStatus.DRAFT
            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "title" to cleanTitle,
                    "description" to description.trim().take(1500),
                    "game" to game.name,
                    "mode" to mode.name,
                    "entryFeeNpr" to entryFeeNpr,
                    "prizePoolNpr" to prizePoolNpr,
                    "maxParticipants" to maxParticipants,
                    "status" to status.name,
                    "published" to isPublished,
                    "featured" to featured,
                    "registrationDeadlineMs" to registrationDeadlineMs,
                    "startTimeMs" to startTimeMs,
                    "killPoints" to killPoints.coerceIn(0L, 100L),
                    "booyahPoints" to booyahPoints.coerceIn(0L, 200L),
                    "placementRule" to placementRule.trim().take(300),
                    "tieBreakerRule" to tieBreakerRule.name,
                    "rulesSummary" to rulesSummary.trim().take(2000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTournamentStatus(tournamentId: String, status: TournamentStatus): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            requireUserId()
            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "status" to status.name,
                    "published" to (status != TournamentStatus.DRAFT),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTournament(tournamentId: String): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            requireUserId()
            db.collection("tournaments").document(tournamentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDemoTournaments(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            val demoTournaments = listOf(
                Triple(
                    "demo_ff_himalayan_s1",
                    mapOf(
                        "title" to "[DEMO] Himalayan Free Fire Squad Cup",
                        "description" to "Official Nepali Free Fire Squad battleground. Top 3 squads earn NPR prizes and leaderboard ranking points.",
                        "game" to GameType.FREE_FIRE.name,
                        "mode" to GameMode.SQUAD.name,
                        "entryFeeNpr" to 0L,
                        "prizePoolNpr" to 25000L,
                        "maxParticipants" to 48L,
                        "registeredCount" to 0L,
                        "status" to TournamentStatus.UPCOMING.name,
                        "published" to true,
                        "featured" to true,
                        "demo" to true,
                        "registrationDeadlineMs" to now + 18 * hour,
                        "startTimeMs" to now + 24 * hour,
                        "killPoints" to 2L,
                        "booyahPoints" to 12L,
                        "placementRule" to "#1: 12, #2: 9, #3: 8, #4: 7, #5: 6, #6-8: 4",
                        "tieBreakerRule" to TieBreakerRule.MOST_KILLS.name,
                        "rulesSummary" to "Mobile only. Nepal servers. No emulators, hacks, or teaming.",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    mapOf(
                        "roundName" to "Qualifier Lobby A",
                        "matchNumber" to 1L,
                        "mapName" to "Bermuda",
                        "game" to GameType.FREE_FIRE.name,
                        "scheduledTimeMs" to now + 24 * hour
                    )
                ),
                Triple(
                    "demo_pubg_kathmandu_solo",
                    mapOf(
                        "title" to "[DEMO] Kathmandu Solo Erangel Clash",
                        "description" to "Fast-paced solo PUBG Mobile showdown for Nepali contenders. Free entry opening series.",
                        "game" to GameType.PUBG_MOBILE.name,
                        "mode" to GameMode.SOLO.name,
                        "entryFeeNpr" to 0L,
                        "prizePoolNpr" to 10000L,
                        "maxParticipants" to 100L,
                        "registeredCount" to 0L,
                        "status" to TournamentStatus.UPCOMING.name,
                        "published" to true,
                        "featured" to true,
                        "demo" to true,
                        "registrationDeadlineMs" to now + 8 * hour,
                        "startTimeMs" to now + 12 * hour,
                        "killPoints" to 1L,
                        "booyahPoints" to 15L,
                        "placementRule" to "#1: 15, #2: 12, #3: 10, #4: 8, #5: 6",
                        "tieBreakerRule" to TieBreakerRule.MOST_WINS.name,
                        "rulesSummary" to "TPP Solo Erangel. Anti-cheat verification active.",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    mapOf(
                        "roundName" to "Finals Match 1",
                        "matchNumber" to 1L,
                        "mapName" to "Erangel",
                        "game" to GameType.PUBG_MOBILE.name,
                        "scheduledTimeMs" to now + 12 * hour
                    )
                ),
                Triple(
                    "demo_ff_pokhara_solo",
                    mapOf(
                        "title" to "[DEMO] Pokhara Free Fire Solo Rush",
                        "description" to "Solo Purgatory drop rush. Register your Free Fire UID to lock your slot before deadline.",
                        "game" to GameType.FREE_FIRE.name,
                        "mode" to GameMode.SOLO.name,
                        "entryFeeNpr" to 0L,
                        "prizePoolNpr" to 7500L,
                        "maxParticipants" to 48L,
                        "registeredCount" to 0L,
                        "status" to TournamentStatus.UPCOMING.name,
                        "published" to true,
                        "featured" to false,
                        "demo" to true,
                        "registrationDeadlineMs" to now + 5 * hour,
                        "startTimeMs" to now + 6 * hour,
                        "killPoints" to 2L,
                        "booyahPoints" to 12L,
                        "placementRule" to "#1: 12, #2: 9, #3: 8, #4: 7",
                        "tieBreakerRule" to TieBreakerRule.HIGHEST_PLACEMENT.name,
                        "rulesSummary" to "Standard ClutchX Solo rules apply.",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    null
                ),
                Triple(
                    "demo_pubg_everest_pro",
                    mapOf(
                        "title" to "[DEMO] Everest Pro League (Paid Preview)",
                        "description" to "High-stakes PUBG Mobile Squad Invitational. Paid entry processing activates once payment gateway is connected.",
                        "game" to GameType.PUBG_MOBILE.name,
                        "mode" to GameMode.SQUAD.name,
                        "entryFeeNpr" to 500L,
                        "prizePoolNpr" to 100000L,
                        "maxParticipants" to 25L,
                        "registeredCount" to 0L,
                        "status" to TournamentStatus.UPCOMING.name,
                        "published" to true,
                        "featured" to false,
                        "demo" to true,
                        "registrationDeadlineMs" to now + 48 * hour,
                        "startTimeMs" to now + 72 * hour,
                        "killPoints" to 1L,
                        "booyahPoints" to 10L,
                        "placementRule" to "PMGC Official Points",
                        "tieBreakerRule" to TieBreakerRule.MOST_KILLS.name,
                        "rulesSummary" to "Paid entry disabled in initial release.",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    null
                )
            )

            val batch = db.batch()
            demoTournaments.forEach { (id, data, matchData) ->
                val tRef = db.collection("tournaments").document(id)
                batch.set(tRef, data)
                if (matchData != null) {
                    val mId = "${id}_m1"
                    val mRef = db.collection("matches").document(mId)
                    batch.set(
                        mRef,
                        mapOf(
                            "tournamentId" to id,
                            "tournamentTitle" to (data["title"] as String),
                            "roundName" to (matchData["roundName"] as String),
                            "matchNumber" to (matchData["matchNumber"] as Long),
                            "mapName" to (matchData["mapName"] as String),
                            "game" to (matchData["game"] as String),
                            "scheduledTimeMs" to (matchData["scheduledTimeMs"] as Long),
                            "status" to "SCHEDULED",
                            "credentialsReleased" to false,
                            "published" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val credRef = mRef.collection("private").document("credentials")
                    batch.set(
                        credRef,
                        mapOf(
                            "matchId" to mId,
                            "tournamentId" to id,
                            "roomId" to "90441122",
                            "roomPassword" to "CLUTCHNP",
                            "notes" to "Join lobby 10 mins prior in Nepal Standard Time.",
                            "released" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            val annRef = db.collection("announcements").document("demo_welcome_np")
            batch.set(
                annRef,
                mapOf(
                    "title" to "ClutchX Nepal Launch Cup Open",
                    "body" to "Free-entry demo tournaments for Free Fire & PUBG Mobile are live in Nepal Standard Time (NST). Set your In-Game UID in Profile before registering.",
                    "category" to "TOURNAMENT",
                    "published" to true,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/seed")
            Result.failure(e)
        }
    }

    // --- REGISTRATION (SOLO & TEAM WITH VALIDATION) ---

    fun observeMyRegistrations(uid: String): Flow<List<Registration>> = flow {
        emitAll(
            db.collection("registrations")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap -> snap.toObjects(Registration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "registrations", uid)
        )
    }

    fun observeAdminRegistrations(uid: String): Flow<List<Registration>> = flow {
        emitAll(
            db.collection("registrations")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Registration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "registrations", uid)
        )
    }

    suspend fun registerForTournament(
        tournament: Tournament,
        userProfile: UserProfile,
        selectedTeam: Team?
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "registrations"
        return try {
            require(tournament.published && tournament.tournamentStatus == TournamentStatus.UPCOMING) {
                "Registration is closed for this tournament."
            }
            require(tournament.entryFeeNpr == 0L) {
                "Paid entry processing is disabled until a secure payment provider is configured."
            }
            require(System.currentTimeMillis() < tournament.registrationDeadlineMs) {
                "Registration deadline (NST) has passed."
            }
            require(tournament.registeredCount < tournament.maxParticipants) {
                "Tournament is already full."
            }

            val inGameUid = when (tournament.gameType) {
                GameType.FREE_FIRE -> userProfile.freeFireUid.trim()
                GameType.PUBG_MOBILE -> userProfile.pubgUid.trim()
            }
            require(inGameUid.length >= 3) {
                "Please add your ${tournament.gameType.displayName} UID in Profile before registering."
            }

            val memberUids: List<String>
            val regId: String
            val teamId: String
            val teamName: String

            if (tournament.gameMode == GameMode.SOLO) {
                regId = "${tournament.id}_$uid"
                memberUids = listOf(uid)
                teamId = ""
                teamName = ""
            } else {
                val team = requireNotNull(selectedTeam) {
                    "Select an eligible ${tournament.gameMode.displayName} team for ${tournament.gameType.displayName}."
                }
                require(team.gameType == tournament.gameType) {
                    "Selected team is for ${team.gameType.displayName}, not ${tournament.gameType.displayName}."
                }
                require(team.captainId == uid) {
                    "Only the team captain can register the team."
                }
                val requiredMembers = tournament.gameMode.teamSize
                require(team.memberUids.size >= requiredMembers) {
                    "${tournament.gameMode.displayName} requires at least $requiredMembers team members."
                }
                regId = "${tournament.id}_${team.id}"
                memberUids = team.memberUids.take(5)
                teamId = team.id
                teamName = team.name
            }

            val tournamentRef = db.collection("tournaments").document(tournament.id)
            val regRef = db.collection("registrations").document(regId)
            val notifRef = db.collection("notifications").document()

            db.runTransaction { tx ->
                val freshTournament = tx.get(tournamentRef)
                val existingReg = tx.get(regRef)
                if (existingReg.exists()) {
                    throw IllegalStateException("Already registered for this tournament.")
                }
                val currentCount = freshTournament.getLong("registeredCount") ?: 0L
                val maxCount = freshTournament.getLong("maxParticipants") ?: 0L
                val freshStatus = freshTournament.getString("status") ?: ""
                if (freshStatus != TournamentStatus.UPCOMING.name || currentCount >= maxCount) {
                    throw IllegalStateException("Tournament is no longer accepting registrations.")
                }

                val newCount = currentCount + 1L
                val nextStatus = if (newCount >= maxCount) TournamentStatus.FULL.name else TournamentStatus.UPCOMING.name

                tx.set(
                    regRef,
                    mapOf(
                        "tournamentId" to tournament.id,
                        "tournamentTitle" to tournament.title,
                        "game" to tournament.game,
                        "mode" to tournament.mode,
                        "userId" to uid,
                        "username" to userProfile.username.ifBlank { "Player" },
                        "inGameUid" to inGameUid,
                        "teamId" to teamId,
                        "teamName" to teamName,
                        "memberUids" to memberUids,
                        "status" to "APPROVED",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                memberUids.forEach { memberUid ->
                    val partRef = tournamentRef.collection("participants").document(memberUid)
                    tx.set(
                        partRef,
                        mapOf(
                            "tournamentId" to tournament.id,
                            "userId" to memberUid,
                            "registrationId" to regId,
                            "status" to "APPROVED",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.update(
                    tournamentRef,
                    mapOf(
                        "registeredCount" to newCount,
                        "status" to nextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Registration Approved",
                        "body" to "You are confirmed for ${tournament.title} (${tournament.mode}). Room credentials unlock when released by Admin.",
                        "type" to "REGISTRATION_APPROVAL",
                        "relatedId" to tournament.id,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRegistrationStatusAdmin(reg: Registration, newStatus: String): Result<Unit> {
        val path = "registrations/${reg.id}"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("registrations").document(reg.id),
                mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
            )
            reg.memberUids.forEach { memberUid ->
                val partRef = db.collection("tournaments").document(reg.tournamentId)
                    .collection("participants").document(memberUid)
                batch.set(
                    partRef,
                    mapOf(
                        "tournamentId" to reg.tournamentId,
                        "userId" to memberUid,
                        "registrationId" to reg.id,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val notifType = if (newStatus == "APPROVED") "REGISTRATION_APPROVAL" else "REGISTRATION"
            val notifTitle = when (newStatus) {
                "APPROVED" -> "Registration Approved!"
                "REJECTED" -> "Registration Rejected"
                else -> "Registration $newStatus"
            }.take(120)
            val notifBody = when (newStatus) {
                "APPROVED" -> "Your slot for ${reg.tournamentTitle} (${reg.mode}) is approved. Watch My Matches for NST room alerts."
                else -> "Your registration for ${reg.tournamentTitle} is now $newStatus."
            }.take(500)
            val recipients = (reg.memberUids + reg.userId).filter { it.isNotBlank() }.distinct()
            recipients.forEach { recipientUid ->
                val notifRef = db.collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to recipientUid,
                        "title" to notifTitle,
                        "body" to notifBody,
                        "type" to notifType,
                        "relatedId" to reg.tournamentId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- TEAMS & SINGLE-USE INVITATIONS ---

    fun observeMyTeams(uid: String): Flow<List<Team>> = flow {
        emitAll(
            db.collection("teams")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap -> snap.toObjects(Team::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "teams", uid)
        )
    }

    fun observePublicTeams(uid: String): Flow<List<Team>> = flow {
        emitAll(
            db.collection("teams")
                .whereEqualTo("publicRoster", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Team::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalPoints }
                }
                .logAndRethrow(OperationType.LIST, "teams", uid)
        )
    }

    fun observeMyInvitations(uid: String, email: String): Flow<List<TeamInvitation>> = flow {
        if (email.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("team_invitations")
                .whereEqualTo("invitedEmail", email.lowercase().trim())
                .snapshots()
                .map { snap -> snap.toObjects(TeamInvitation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "team_invitations", uid)
        )
    }

    suspend fun createTeam(
        name: String,
        tag: String,
        game: GameType,
        logoKey: String,
        captainName: String
    ): Result<Unit> {
        val uid = requireUserId()
        return try {
            val cleanName = name.trim()
            val cleanTag = tag.trim().uppercase()
            require(cleanName.length in 2..40) { "Team name must be 2-40 characters." }
            require(cleanTag.length in 2..8) { "Team tag must be 2-8 characters." }

            val docRef = db.collection("teams").document()
            docRef.set(
                mapOf(
                    "name" to cleanName,
                    "tag" to cleanTag,
                    "game" to game.name,
                    "captainId" to uid,
                    "captainName" to captainName.ifBlank { "Captain" }.take(50),
                    "logoKey" to logoKey,
                    "memberUids" to listOf(uid),
                    "memberNames" to listOf(captainName.ifBlank { "Captain" }.take(40)),
                    "matchesPlayed" to 0L,
                    "wins" to 0L,
                    "totalKills" to 0L,
                    "totalPoints" to 0L,
                    "publicRoster" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "teams")
            Result.failure(e)
        }
    }

    suspend fun updateTeamInfo(teamId: String, name: String, tag: String, logoKey: String): Result<Unit> {
        val path = "teams/$teamId"
        return try {
            requireUserId()
            db.collection("teams").document(teamId).update(
                mapOf(
                    "name" to name.trim(),
                    "tag" to tag.trim().uppercase(),
                    "logoKey" to logoKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun inviteToTeam(team: Team, invitedEmail: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            require(team.captainId == uid) { "Only the team captain can send invitations." }
            val cleanEmail = invitedEmail.trim().lowercase()
            require(cleanEmail.contains("@") && cleanEmail.length in 5..160) { "Enter a valid player email." }
            require(team.memberUids.size < 5) { "Team roster already at maximum capacity (5)." }

            val safeKey = cleanEmail.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val invId = "${team.id}_$safeKey".take(120)
            val invRef = db.collection("team_invitations").document(invId)
            if (invRef.get().await().exists()) {
                throw IllegalStateException("An invitation has already been sent to $cleanEmail for this team.")
            }

            invRef.set(
                mapOf(
                    "teamId" to team.id,
                    "teamName" to team.name,
                    "game" to team.game,
                    "captainId" to uid,
                    "captainName" to team.captainName,
                    "invitedEmail" to cleanEmail,
                    "invitedByName" to team.captainName,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "team_invitations")
            Result.failure(e)
        }
    }

    suspend fun respondToInvitation(invitation: TeamInvitation, accept: Boolean, username: String): Result<Unit> {
        val uid = requireUserId()
        val invRef = db.collection("team_invitations").document(invitation.id)
        return try {
            require(invitation.status == "PENDING") { "Invitation has already been answered." }
            if (accept) {
                val teamRef = db.collection("teams").document(invitation.teamId)
                val teamSnap = teamRef.get().await()
                val existingMembers = (teamSnap.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val existingNames = (teamSnap.get("memberNames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                require(!existingMembers.contains(uid)) { "You are already on this team." }
                require(existingMembers.size < 5) { "Team is already full." }

                val batch = db.batch()
                batch.update(
                    invRef,
                    mapOf(
                        "status" to "ACCEPTED",
                        "answeredByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    teamRef,
                    mapOf(
                        "memberUids" to (existingMembers + uid),
                        "memberNames" to (existingNames + username.ifBlank { "Player" }.take(40)),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            } else {
                invRef.update(
                    mapOf(
                        "status" to "REJECTED",
                        "answeredByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, invRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeTeamMember(team: Team, memberUidToRemove: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            require(team.captainId == uid || memberUidToRemove == uid) { "Not authorized to remove member." }
            require(memberUidToRemove != team.captainId) { "Captain cannot leave without transferring or deleting team." }
            val idx = team.memberUids.indexOf(memberUidToRemove)
            val updatedUids = team.memberUids.filterNot { it == memberUidToRemove }
            val updatedNames = if (idx in team.memberNames.indices) {
                team.memberNames.filterIndexed { i, _ -> i != idx }
            } else {
                team.memberNames
            }
            db.collection("teams").document(team.id).update(
                mapOf(
                    "memberUids" to updatedUids,
                    "memberNames" to updatedNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "teams/${team.id}")
            Result.failure(e)
        }
    }

    // --- MATCHES, PROTECTED ROOM CREDENTIALS & MATCH RESULTS ---

    fun observePublicMatches(uid: String): Flow<List<MatchItem>> = flow {
        emitAll(
            db.collection("matches")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MatchItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.scheduledTimeMs }
                }
                .logAndRethrow(OperationType.LIST, "matches", uid)
        )
    }

    fun observeAdminMatches(uid: String): Flow<List<MatchItem>> = flow {
        emitAll(
            db.collection("matches")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MatchItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.scheduledTimeMs }
                }
                .logAndRethrow(OperationType.LIST, "matches", uid)
        )
    }

    suspend fun createMatchAndCredentials(
        tournament: Tournament,
        roundName: String,
        matchNumber: Long,
        mapName: String,
        scheduledTimeMs: Long,
        roomId: String,
        roomPassword: String,
        notes: String,
        releaseImmediately: Boolean
    ): Result<Unit> {
        return try {
            requireUserId()
            val matchRef = db.collection("matches").document()
            val batch = db.batch()
            batch.set(
                matchRef,
                mapOf(
                    "tournamentId" to tournament.id,
                    "tournamentTitle" to tournament.title,
                    "roundName" to roundName.trim().ifBlank { "Match $matchNumber" },
                    "matchNumber" to matchNumber.coerceAtLeast(1L),
                    "mapName" to mapName.trim().ifBlank { "Bermuda" },
                    "game" to tournament.game,
                    "scheduledTimeMs" to scheduledTimeMs,
                    "status" to if (releaseImmediately) "LIVE" else "SCHEDULED",
                    "credentialsReleased" to releaseImmediately,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (roomId.isNotBlank() && roomPassword.isNotBlank()) {
                val credRef = matchRef.collection("private").document("credentials")
                batch.set(
                    credRef,
                    mapOf(
                        "matchId" to matchRef.id,
                        "tournamentId" to tournament.id,
                        "roomId" to roomId.trim(),
                        "roomPassword" to roomPassword.trim(),
                        "notes" to notes.trim(),
                        "released" to releaseImmediately,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            if (releaseImmediately) {
                dispatchMatchReminderNotifications(
                    tournamentId = tournament.id,
                    tournamentTitle = tournament.title,
                    roundName = roundName.trim().ifBlank { "Match $matchNumber" },
                    mapName = mapName.trim().ifBlank { "Bermuda" },
                    scheduledTimeMs = scheduledTimeMs,
                    isRoomRelease = true
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "matches")
            Result.failure(e)
        }
    }

    suspend fun releaseOrUpdateCredentials(
        match: MatchItem,
        roomId: String,
        roomPassword: String,
        notes: String,
        release: Boolean
    ): Result<Unit> {
        val path = "matches/${match.id}/private/credentials"
        return try {
            requireUserId()
            require(roomId.isNotBlank() && roomPassword.isNotBlank()) { "Room ID and Password are required." }
            val matchRef = db.collection("matches").document(match.id)
            val credRef = matchRef.collection("private").document("credentials")
            val batch = db.batch()
            batch.update(
                matchRef,
                mapOf(
                    "credentialsReleased" to release,
                    "status" to if (release) "LIVE" else match.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                credRef,
                mapOf(
                    "matchId" to match.id,
                    "tournamentId" to match.tournamentId,
                    "roomId" to roomId.trim(),
                    "roomPassword" to roomPassword.trim(),
                    "notes" to notes.trim(),
                    "released" to release,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            if (release) {
                dispatchMatchReminderNotifications(
                    tournamentId = match.tournamentId,
                    tournamentTitle = match.tournamentTitle,
                    roundName = match.roundName,
                    mapName = match.mapName,
                    scheduledTimeMs = match.scheduledTimeMs,
                    isRoomRelease = true
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMatchReminderAdmin(match: MatchItem): Result<Int> {
        return try {
            requireUserId()
            val count = dispatchMatchReminderNotifications(
                tournamentId = match.tournamentId,
                tournamentTitle = match.tournamentTitle,
                roundName = match.roundName,
                mapName = match.mapName,
                scheduledTimeMs = match.scheduledTimeMs,
                isRoomRelease = match.credentialsReleased
            )
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "notifications")
            Result.failure(e)
        }
    }

    private suspend fun dispatchMatchReminderNotifications(
        tournamentId: String,
        tournamentTitle: String,
        roundName: String,
        mapName: String,
        scheduledTimeMs: Long,
        isRoomRelease: Boolean
    ): Int {
        val regSnap = db.collection("registrations")
            .whereEqualTo("tournamentId", tournamentId)
            .whereEqualTo("status", "APPROVED")
            .get()
            .await()

        val recipients = regSnap.toObjects(Registration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .flatMap { it.memberUids + it.userId }
            .filter { it.isNotBlank() }
            .distinct()

        if (recipients.isEmpty()) return 0

        val title = if (isRoomRelease) {
            "Room Open: $roundName"
        } else {
            "Match Reminder: $roundName"
        }.take(120)

        val timeText = NstTimeFormatter.formatDateTimeNst(scheduledTimeMs)
        val body = if (isRoomRelease) {
            "$tournamentTitle ($mapName) lobby is open! Open My Matches now to view Room ID & Password."
        } else {
            "$tournamentTitle ($mapName) starts at $timeText. Check My Matches and join lobby 10 mins early."
        }.take(500)

        recipients.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { recipientUid ->
                val notifRef = db.collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to recipientUid,
                        "title" to title,
                        "body" to body,
                        "type" to "MATCH_REMINDER",
                        "relatedId" to tournamentId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        }
        return recipients.size
    }

    suspend fun fetchMatchCredentials(matchId: String): Result<MatchCredentials> {
        val path = "matches/$matchId/private/credentials"
        return try {
            requireUserId()
            val snap = db.collection("matches").document(matchId)
                .collection("private").document("credentials")
                .get()
                .await()
            val creds = snap.toObject(MatchCredentials::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Room credentials have not been configured yet.")
            Result.success(creds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeMatchResults(uid: String): Flow<List<MatchResult>> = flow {
        emitAll(
            db.collection("match_results")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MatchResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalPoints }
                }
                .logAndRethrow(OperationType.LIST, "match_results", uid)
        )
    }

    suspend fun publishMatchResult(
        match: MatchItem,
        tournament: Tournament,
        participantUserId: String,
        participantName: String,
        teamId: String,
        placement: Long,
        kills: Long
    ): Result<Unit> {
        return try {
            requireUserId()
            val placementPts = when (placement) {
                1L -> tournament.booyahPoints
                2L -> (tournament.booyahPoints * 3) / 4
                3L -> (tournament.booyahPoints * 2) / 3
                4L -> tournament.booyahPoints / 2
                5L -> (tournament.booyahPoints / 3).coerceAtLeast(2L)
                in 6L..8L -> 2L
                else -> 0L
            }
            val killPts = kills * tournament.killPoints
            val totalPts = placementPts + killPts

            val resRef = db.collection("match_results").document()
            val batch = db.batch()
            batch.set(
                resRef,
                mapOf(
                    "matchId" to match.id,
                    "tournamentId" to match.tournamentId,
                    "userId" to participantUserId,
                    "teamId" to teamId,
                    "participantName" to participantName.trim(),
                    "placement" to placement.coerceIn(1L, 100L),
                    "kills" to kills.coerceIn(0L, 100L),
                    "placementPoints" to placementPts,
                    "killPoints" to killPts,
                    "totalPoints" to totalPts,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // Update public player leaderboard entry
            val lbRef = db.collection("leaderboard_players").document(participantUserId)
            val existingLb = lbRef.get().await()
            if (existingLb.exists()) {
                batch.update(
                    lbRef,
                    mapOf(
                        "matchesPlayed" to FieldValue.increment(1L),
                        "totalKills" to FieldValue.increment(kills),
                        "tournamentWins" to FieldValue.increment(if (placement == 1L) 1L else 0L),
                        "totalPoints" to FieldValue.increment(totalPts),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    lbRef,
                    mapOf(
                        "userId" to participantUserId,
                        "username" to participantName.trim(),
                        "avatarKey" to "phoenix",
                        "primaryGame" to tournament.game,
                        "matchesPlayed" to 1L,
                        "totalKills" to kills,
                        "tournamentWins" to if (placement == 1L) 1L else 0L,
                        "totalPoints" to totalPts,
                        "published" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                db.collection("matches").document(match.id),
                mapOf("status" to "COMPLETED", "updatedAt" to FieldValue.serverTimestamp())
            )
            val notifRef = db.collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "userId" to participantUserId,
                    "title" to "Match Results Published",
                    "body" to "${match.tournamentTitle} (${match.roundName}): #$placement • $kills kills • $totalPts pts.",
                    "type" to "RESULT",
                    "relatedId" to match.tournamentId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "match_results")
            Result.failure(e)
        }
    }

    // --- LEADERBOARD, NOTIFICATIONS, REWARDS, ANNOUNCEMENTS, REPORTS ---

    fun observeLeaderboardPlayers(uid: String): Flow<List<LeaderboardPlayer>> = flow {
        emitAll(
            db.collection("leaderboard_players")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<LeaderboardPlayer> { it.totalPoints }
                                .thenByDescending { it.totalKills }
                                .thenByDescending { it.tournamentWins }
                        )
                }
                .logAndRethrow(OperationType.LIST, "leaderboard_players", uid)
        )
    }

    fun observeNotifications(uid: String): Flow<List<AppNotification>> = flow {
        emitAll(
            db.collection("notifications")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "notifications", uid)
        )
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = try {
        requireUserId()
        db.collection("notifications").document(notificationId).update(
            mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
        Result.failure(e)
    }

    suspend fun markAllNotificationsRead(unreadNotifications: List<AppNotification>): Result<Unit> = try {
        requireUserId()
        val unread = unreadNotifications.filter { !it.read && it.id.isNotBlank() }
        if (unread.isNotEmpty()) {
            unread.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { n ->
                    batch.update(
                        db.collection("notifications").document(n.id),
                        mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())
                    )
                }
                batch.commit().await()
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "notifications/markAllRead")
        Result.failure(e)
    }

    fun observeMyRewards(uid: String): Flow<List<Reward>> = flow {
        emitAll(
            db.collection("rewards")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Reward::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "rewards", uid)
        )
    }

    fun observeAdminRewards(uid: String): Flow<List<Reward>> = flow {
        emitAll(
            db.collection("rewards")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Reward::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "rewards", uid)
        )
    }

    suspend fun issueRewardAdmin(
        tournament: Tournament,
        userId: String,
        recipientName: String,
        placement: Long,
        amountNpr: Long
    ): Result<Unit> = try {
        requireUserId()
        val ref = db.collection("rewards").document()
        val batch = db.batch()
        batch.set(
            ref,
            mapOf(
                "tournamentId" to tournament.id,
                "tournamentTitle" to tournament.title,
                "userId" to userId.trim(),
                "recipientName" to recipientName.trim(),
                "placement" to placement.coerceIn(1L, 100L),
                "amountNpr" to amountNpr.coerceIn(0L, 10_000_000L),
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val notifRef = db.collection("notifications").document()
        batch.set(
            notifRef,
            mapOf(
                "userId" to userId.trim(),
                "title" to "Reward Issued: रु $amountNpr",
                "body" to "Placement #$placement reward recorded for ${tournament.title}.",
                "type" to "REWARD",
                "relatedId" to tournament.id,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "rewards")
        Result.failure(e)
    }

    suspend fun updateRewardStatusAdmin(rewardId: String, status: String): Result<Unit> = try {
        requireUserId()
        db.collection("rewards").document(rewardId).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "rewards/$rewardId")
        Result.failure(e)
    }

    fun observeAnnouncements(uid: String): Flow<List<Announcement>> = flow {
        emitAll(
            db.collection("announcements")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "announcements", uid)
        )
    }

    suspend fun publishAnnouncementAdmin(title: String, body: String, category: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("announcements").document().set(
            mapOf(
                "title" to title.trim().take(120),
                "body" to body.trim().take(1000),
                "category" to category,
                "published" to true,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "announcements")
        Result.failure(e)
    }

    fun observeMyReports(uid: String): Flow<List<ReportItem>> = flow {
        emitAll(
            db.collection("reports")
                .whereEqualTo("reporterId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReportItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "reports", uid)
        )
    }

    fun observeAdminReports(uid: String): Flow<List<ReportItem>> = flow {
        emitAll(
            db.collection("reports")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReportItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "reports", uid)
        )
    }

    suspend fun submitReport(
        reporterName: String,
        category: String,
        targetId: String,
        subject: String,
        details: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        require(subject.trim().length in 3..120) { "Subject must be 3-120 characters." }
        require(details.trim().length in 5..1500) { "Details must be 5-1500 characters." }
        val id = "rep_${UUID.randomUUID().toString().take(12)}"
        db.collection("reports").document(id).set(
            mapOf(
                "reporterId" to uid,
                "reporterName" to reporterName.ifBlank { "Player" }.take(60),
                "category" to category,
                "targetId" to targetId.trim().take(128),
                "subject" to subject.trim(),
                "details" to details.trim(),
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "reports")
        Result.failure(e)
    }

    suspend fun updateReportStatusAdmin(reportId: String, status: String): Result<Unit> = try {
        requireUserId()
        db.collection("reports").document(reportId).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
        Result.failure(e)
    }

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "gangame424@gmail.com"
    }
}
