package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.MatchItem
import com.example.data.model.Registration
import com.example.i18n.NstTimeFormatter
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Collections

/**
 * Application-scoped background Firestore listener for real-time alerts:
 * - Registration approvals & status updates (`REGISTRATION`, `REGISTRATION_APPROVAL`)
 * - Match reminders & room credential unlocks (`MATCH_REMINDER`, `ROOM_CREDENTIALS`)
 *
 * Keeps listening whenever an authenticated session is active, fires OS notifications via
 * [ClutchXNotificationManager], and streams live alerts to the Compose UI banner.
 */
class NotificationBackgroundMonitor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val db: FirebaseFirestore by lazy {
        val dbId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private val _realtimeAlerts = MutableSharedFlow<AppNotification>(extraBufferCapacity = 16)
    val realtimeAlerts: SharedFlow<AppNotification> = _realtimeAlerts.asSharedFlow()

    private val _activeBannerAlert = MutableStateFlow<AppNotification?>(null)
    val activeBannerAlert: StateFlow<AppNotification?> = _activeBannerAlert.asStateFlow()

    private var activeUid: String? = null
    private var notificationListener: ListenerRegistration? = null
    private var registrationListener: ListenerRegistration? = null
    private var matchListener: ListenerRegistration? = null
    private var reminderTickerJob: Job? = null

    private var initialSnapshotProcessed = false
    private val seenNotificationIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val createdReminderIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    @Volatile
    private var cachedApprovedTournamentIds: Set<String> = emptySet()

    @Volatile
    private var cachedPublishedMatches: List<MatchItem> = emptyList()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != activeUid) {
            if (uid == null) {
                stopListening()
            } else {
                startListening(uid)
            }
        }
    }

    fun start() {
        ClutchXNotificationManager.ensureChannels(appContext)
        runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            Firebase.auth.addAuthStateListener(authStateListener)
            Firebase.auth.currentUser?.uid?.let { startListening(it) }
        }.onFailure { err ->
            Log.w(TAG, "Deferred Firebase listener init: ${err.message}")
        }
    }

    fun dismissBanner() {
        _activeBannerAlert.value = null
    }

    @Synchronized
    private fun startListening(uid: String) {
        stopListening()
        activeUid = uid
        initialSnapshotProcessed = false
        seenNotificationIds.clear()
        createdReminderIds.clear()

        // 1. Real-time Firestore listener on user's /notifications collection
        notificationListener = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Notification listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val nowSec = System.currentTimeMillis() / 1000L
                if (!initialSnapshotProcessed) {
                    initialSnapshotProcessed = true
                    var latestRecentUnread: AppNotification? = null
                    snapshot.documents.forEach { doc ->
                        seenNotificationIds.add(doc.id)
                        val item = doc.toObject(
                            AppNotification::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (item != null && !item.read) {
                            val ageSec = nowSec - (item.createdAt?.seconds ?: nowSec)
                            if (ageSec in -30L..45L) {
                                latestRecentUnread = item
                            }
                        }
                    }
                    // Surface a freshly arrived unread notification on cold attach
                    latestRecentUnread?.let { dispatchAlert(it) }
                } else {
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            if (seenNotificationIds.add(doc.id)) {
                                val item = doc.toObject(
                                    AppNotification::class.java,
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )
                                if (item != null && !item.read) {
                                    dispatchAlert(item)
                                }
                            }
                        }
                    }
                }
            }

        // 2. Listen to approved registrations for automatic upcoming match reminders
        registrationListener = db.collection("registrations")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val approvedIds = snapshot.toObjects(
                    Registration::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                    .filter { it.status == "APPROVED" }
                    .map { it.tournamentId }
                    .toSet()
                cachedApprovedTournamentIds = approvedIds
                evaluateMatchReminders(uid)
            }

        // 3. Listen to published matches to detect upcoming match windows in NST
        matchListener = db.collection("matches")
            .whereEqualTo("published", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                cachedPublishedMatches = snapshot.toObjects(
                    MatchItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                evaluateMatchReminders(uid)
            }

        // 4. Periodic background check every 60s so matches entering the 60-min NST reminder window notify on time
        reminderTickerJob = scope.launch {
            while (isActive) {
                delay(60_000L)
                evaluateMatchReminders(uid)
            }
        }
    }

    @Synchronized
    private fun stopListening() {
        activeUid = null
        notificationListener?.remove()
        notificationListener = null
        registrationListener?.remove()
        registrationListener = null
        matchListener?.remove()
        matchListener = null
        reminderTickerJob?.cancel()
        reminderTickerJob = null
        initialSnapshotProcessed = false
        seenNotificationIds.clear()
        createdReminderIds.clear()
        cachedApprovedTournamentIds = emptySet()
        cachedPublishedMatches = emptyList()
        _activeBannerAlert.value = null
    }

    private fun dispatchAlert(notification: AppNotification) {
        _activeBannerAlert.value = notification
        _realtimeAlerts.tryEmit(notification)
        ClutchXNotificationManager.showNotification(appContext, notification)
    }

    private fun evaluateMatchReminders(uid: String) {
        val approvedIds = cachedApprovedTournamentIds
        val matches = cachedPublishedMatches
        if (approvedIds.isEmpty() || matches.isEmpty()) return

        val nowMs = System.currentTimeMillis()
        val windowMs = 60L * 60L * 1000L // 60 minutes before match start in NST

        val dueMatches = matches.filter { match ->
            match.tournamentId in approvedIds &&
                match.status != "COMPLETED" &&
                (
                    match.status == "LIVE" ||
                        match.credentialsReleased ||
                        (match.scheduledTimeMs in (nowMs - 15 * 60_000L)..(nowMs + windowMs))
                    )
        }

        if (dueMatches.isEmpty()) return

        scope.launch {
            dueMatches.forEach { match ->
                val rawDocId = "rem_${match.id}_$uid"
                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    .take(120)
                if (!createdReminderIds.add(rawDocId) || seenNotificationIds.contains(rawDocId)) {
                    return@forEach
                }
                try {
                    val docRef = db.collection("notifications").document(rawDocId)
                    val existing = docRef.get().await()
                    if (!existing.exists()) {
                        val title = if (match.credentialsReleased) {
                            "Room Open: ${match.roundName}"
                        } else {
                            "Match Reminder: ${match.roundName}"
                        }.take(120)
                        val body = if (match.credentialsReleased) {
                            "${match.tournamentTitle} (${match.mapName}) room is live! Open My Matches to view Room ID & Password."
                        } else {
                            "${match.tournamentTitle} (${match.mapName}) starts at ${NstTimeFormatter.formatDateTimeNst(match.scheduledTimeMs)}. Get ready!"
                        }.take(500)

                        docRef.set(
                            mapOf(
                                "userId" to uid,
                                "title" to title,
                                "body" to body,
                                "type" to "MATCH_REMINDER",
                                "relatedId" to match.tournamentId,
                                "read" to false,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    createdReminderIds.remove(rawDocId)
                    Log.w(TAG, "Auto match reminder write skipped: ${e.message}")
                }
            }
        }
    }

    private companion object {
        const val TAG = "NotificationBgMonitor"
    }
}
