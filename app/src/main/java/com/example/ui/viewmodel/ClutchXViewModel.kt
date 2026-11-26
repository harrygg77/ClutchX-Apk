package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.ClutchXRepository
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
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameFilter { ALL, FREE_FIRE, PUBG_MOBILE }
enum class ModeFilter { ALL, SOLO, DUO, SQUAD }
enum class FeeFilter { ALL, FREE, PAID }
enum class StatusFilter { ALL, UPCOMING, LIVE, COMPLETED, FULL }
enum class TournamentSort { START_TIME, PRIZE_POOL, ENTRY_FEE }

data class DiscoveryFilterState(
    val searchQuery: String = "",
    val gameFilter: GameFilter = GameFilter.ALL,
    val modeFilter: ModeFilter = ModeFilter.ALL,
    val feeFilter: FeeFilter = FeeFilter.ALL,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sortBy: TournamentSort = TournamentSort.START_TIME
)

@OptIn(ExperimentalCoroutinesApi::class)
class ClutchXViewModel(
    private val repository: ClutchXRepository,
    val currentUserId: String,
    val currentUserEmail: String
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _localLanguageOverride = MutableStateFlow<AppLanguage?>(null)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _filters = MutableStateFlow(DiscoveryFilterState())
    val filters: StateFlow<DiscoveryFilterState> = _filters.asStateFlow()

    private val _revealedCredentials = MutableStateFlow<Map<String, MatchCredentials>>(emptyMap())
    val revealedCredentials: StateFlow<Map<String, MatchCredentials>> = _revealedCredentials.asStateFlow()

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null && user.uid == currentUserId) {
                repository.bootstrapUserIfNeeded(user)
                    .onSuccess { admin -> _isAdmin.value = admin }
                    .onFailure { err ->
                        Log.w(TAG, "Profile bootstrap failed: ${err.message}")
                    }
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

    val currentLanguage: StateFlow<AppLanguage> =
        combine(userProfile, _localLanguageOverride) { state, override ->
            override ?: (state as? UiState.Success)?.data?.gameLanguage ?: AppLanguage.EN
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.EN)

    val publicTournaments: StateFlow<UiState<List<Tournament>>> =
        repository.observePublishedTournaments(currentUserId).toUiStateFlow("tournaments")

    val filteredTournaments: StateFlow<UiState<List<Tournament>>> =
        combine(publicTournaments, _filters) { state, f ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> {
                    val q = f.searchQuery.trim().lowercase()
                    val list = state.data
                        .filter { t ->
                            val matchesSearch = q.isEmpty() ||
                                t.title.lowercase().contains(q) ||
                                t.description.lowercase().contains(q)
                            val matchesGame = when (f.gameFilter) {
                                GameFilter.ALL -> true
                                GameFilter.FREE_FIRE -> t.gameType == GameType.FREE_FIRE
                                GameFilter.PUBG_MOBILE -> t.gameType == GameType.PUBG_MOBILE
                            }
                            val matchesMode = when (f.modeFilter) {
                                ModeFilter.ALL -> true
                                ModeFilter.SOLO -> t.gameMode == GameMode.SOLO
                                ModeFilter.DUO -> t.gameMode == GameMode.DUO
                                ModeFilter.SQUAD -> t.gameMode == GameMode.SQUAD
                            }
                            val matchesFee = when (f.feeFilter) {
                                FeeFilter.ALL -> true
                                FeeFilter.FREE -> t.entryFeeNpr == 0L
                                FeeFilter.PAID -> t.entryFeeNpr > 0L
                            }
                            val matchesStatus = when (f.statusFilter) {
                                StatusFilter.ALL -> t.tournamentStatus != TournamentStatus.CANCELLED
                                StatusFilter.UPCOMING -> t.tournamentStatus == TournamentStatus.UPCOMING
                                StatusFilter.LIVE -> t.tournamentStatus == TournamentStatus.LIVE
                                StatusFilter.COMPLETED -> t.tournamentStatus == TournamentStatus.COMPLETED
                                StatusFilter.FULL -> t.tournamentStatus == TournamentStatus.FULL
                            }
                            matchesSearch && matchesGame && matchesMode && matchesFee && matchesStatus
                        }
                        .let { filtered ->
                            when (f.sortBy) {
                                TournamentSort.START_TIME -> filtered.sortedBy { it.startTimeMs }
                                TournamentSort.PRIZE_POOL -> filtered.sortedByDescending { it.prizePoolNpr }
                                TournamentSort.ENTRY_FEE -> filtered.sortedBy { it.entryFeeNpr }
                            }
                        }
                    UiState.Success(list)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myRegistrations: StateFlow<UiState<List<Registration>>> =
        repository.observeMyRegistrations(currentUserId).toUiStateFlow("registrations")

    val myTeams: StateFlow<UiState<List<Team>>> =
        repository.observeMyTeams(currentUserId).toUiStateFlow("my teams")

    val publicTeams: StateFlow<UiState<List<Team>>> =
        repository.observePublicTeams(currentUserId).toUiStateFlow("teams")

    val myInvitations: StateFlow<UiState<List<TeamInvitation>>> =
        repository.observeMyInvitations(currentUserId, currentUserEmail).toUiStateFlow("invitations")

    val publicMatches: StateFlow<UiState<List<MatchItem>>> =
        repository.observePublicMatches(currentUserId).toUiStateFlow("matches")

    val matchResults: StateFlow<UiState<List<MatchResult>>> =
        repository.observeMatchResults(currentUserId).toUiStateFlow("match results")

    val leaderboardPlayers: StateFlow<UiState<List<LeaderboardPlayer>>> =
        repository.observeLeaderboardPlayers(currentUserId).toUiStateFlow("leaderboard")

    val notifications: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId).toUiStateFlow("notifications")

    val unreadNotificationCount: StateFlow<Int> =
        notifications.map { state ->
            (state as? UiState.Success)?.data?.count { !it.read } ?: 0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val myRewards: StateFlow<UiState<List<Reward>>> =
        repository.observeMyRewards(currentUserId).toUiStateFlow("rewards")

    val announcements: StateFlow<UiState<List<Announcement>>> =
        repository.observeAnnouncements(currentUserId).toUiStateFlow("announcements")

    val myReports: StateFlow<UiState<List<ReportItem>>> =
        repository.observeMyReports(currentUserId).toUiStateFlow("reports")

    // --- ADMIN-ONLY OBSERVABLES (GATED BY isAdmin) ---

    val adminTournaments: StateFlow<UiState<List<Tournament>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAdminTournaments(currentUserId) else flowOf(emptyList())
        }.toUiStateFlow("admin tournaments")

    val adminMatches: StateFlow<UiState<List<MatchItem>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAdminMatches(currentUserId) else flowOf(emptyList())
        }.toUiStateFlow("admin matches")

    val adminRegistrations: StateFlow<UiState<List<Registration>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAdminRegistrations(currentUserId) else flowOf(emptyList())
        }.toUiStateFlow("admin registrations")

    val adminRewards: StateFlow<UiState<List<Reward>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAdminRewards(currentUserId) else flowOf(emptyList())
        }.toUiStateFlow("admin rewards")

    val adminReports: StateFlow<UiState<List<ReportItem>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAdminReports(currentUserId) else flowOf(emptyList())
        }.toUiStateFlow("admin reports")

    // --- FILTER MUTATIONS ---

    fun updateSearch(query: String) {
        _filters.update { it.copy(searchQuery = query) }
    }

    fun updateGameFilter(game: GameFilter) {
        _filters.update { it.copy(gameFilter = game) }
    }

    fun updateModeFilter(mode: ModeFilter) {
        _filters.update { it.copy(modeFilter = mode) }
    }

    fun updateFeeFilter(fee: FeeFilter) {
        _filters.update { it.copy(feeFilter = fee) }
    }

    fun updateStatusFilter(status: StatusFilter) {
        _filters.update { it.copy(statusFilter = status) }
    }

    fun updateSort(sort: TournamentSort) {
        _filters.update { it.copy(sortBy = sort) }
    }

    fun resetFilters() {
        _filters.value = DiscoveryFilterState()
    }

    fun toggleLanguage() {
        val next = if (currentLanguage.value == AppLanguage.EN) AppLanguage.NE else AppLanguage.EN
        _localLanguageOverride.value = next
        val profile = (userProfile.value as? UiState.Success)?.data
        if (profile != null) {
            viewModelScope.launch {
                repository.updateProfile(
                    username = profile.username,
                    avatarKey = profile.avatarKey,
                    freeFireUid = profile.freeFireUid,
                    pubgUid = profile.pubgUid,
                    language = next
                )
            }
        }
    }

    // --- PLAYER ACTIONS ---

    private fun runAction(successMessage: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit(successMessage)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "Action failed")
            }
        }
    }

    fun saveProfile(
        username: String,
        avatarKey: String,
        freeFireUid: String,
        pubgUid: String,
        language: AppLanguage
    ) {
        _localLanguageOverride.value = language
        runAction("Profile updated") {
            repository.updateProfile(username, avatarKey, freeFireUid, pubgUid, language)
        }
    }

    fun registerForTournament(tournament: Tournament, selectedTeam: Team?) {
        val profile = (userProfile.value as? UiState.Success)?.data
        if (profile == null) {
            _snackbarMessages.tryEmit("Profile still loading, please wait.")
            return
        }
        runAction("Successfully registered for ${tournament.title}!") {
            repository.registerForTournament(tournament, profile, selectedTeam)
        }
    }

    fun createTeam(name: String, tag: String, game: GameType, logoKey: String) {
        val profile = (userProfile.value as? UiState.Success)?.data
        val captainName = profile?.username ?: currentUserEmail.substringBefore("@")
        runAction("Team created!") {
            repository.createTeam(name, tag, game, logoKey, captainName)
        }
    }

    fun updateTeam(teamId: String, name: String, tag: String, logoKey: String) {
        runAction("Team updated!") {
            repository.updateTeamInfo(teamId, name, tag, logoKey)
        }
    }

    fun inviteTeammate(team: Team, email: String) {
        runAction("Invitation sent to $email") {
            repository.inviteToTeam(team, email)
        }
    }

    fun respondToInvitation(invitation: TeamInvitation, accept: Boolean) {
        val profile = (userProfile.value as? UiState.Success)?.data
        val name = profile?.username ?: currentUserEmail.substringBefore("@")
        runAction(if (accept) "Joined ${invitation.teamName}!" else "Invitation declined") {
            repository.respondToInvitation(invitation, accept, name)
        }
    }

    fun removeTeamMember(team: Team, memberUid: String) {
        runAction("Team roster updated") {
            repository.removeTeamMember(team, memberUid)
        }
    }

    fun revealRoomCredentials(matchId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.fetchMatchCredentials(matchId)
            _isBusy.value = false
            result.onSuccess { creds ->
                _revealedCredentials.update { it + (matchId to creds) }
                _snackbarMessages.tryEmit("Room credentials unlocked")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "Credentials not available yet")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun markAllNotificationsRead() {
        val currentList = (notifications.value as? UiState.Success)?.data ?: emptyList()
        if (currentList.none { !it.read }) return
        runAction("All notifications marked as read") {
            repository.markAllNotificationsRead(currentList)
        }
    }

    fun submitSupportOrReport(category: String, targetId: String, subject: String, details: String) {
        val profile = (userProfile.value as? UiState.Success)?.data
        val name = profile?.username ?: currentUserEmail.substringBefore("@")
        runAction("Submitted to ClutchX Moderation & Support") {
            repository.submitReport(name, category, targetId, subject, details)
        }
    }

    // --- ADMIN ACTIONS ---

    fun createTournamentAdmin(
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
    ) {
        runAction("Tournament created") {
            repository.createTournament(
                title = title,
                description = description,
                game = game,
                mode = mode,
                entryFeeNpr = entryFeeNpr,
                prizePoolNpr = prizePoolNpr,
                maxParticipants = maxParticipants,
                status = status,
                featured = featured,
                demo = false,
                registrationDeadlineMs = registrationDeadlineMs,
                startTimeMs = startTimeMs,
                killPoints = killPoints,
                booyahPoints = booyahPoints,
                placementRule = placementRule,
                tieBreakerRule = tieBreakerRule,
                rulesSummary = rulesSummary
            )
        }
    }

    fun updateTournamentAdmin(
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
    ) {
        runAction("Tournament updated") {
            repository.updateTournament(
                tournamentId = tournamentId,
                title = title,
                description = description,
                game = game,
                mode = mode,
                entryFeeNpr = entryFeeNpr,
                prizePoolNpr = prizePoolNpr,
                maxParticipants = maxParticipants,
                status = status,
                featured = featured,
                registrationDeadlineMs = registrationDeadlineMs,
                startTimeMs = startTimeMs,
                killPoints = killPoints,
                booyahPoints = booyahPoints,
                placementRule = placementRule,
                tieBreakerRule = tieBreakerRule,
                rulesSummary = rulesSummary
            )
        }
    }

    fun updateTournamentStatusAdmin(tournamentId: String, status: TournamentStatus) {
        runAction("Status updated to ${status.displayName}") {
            repository.updateTournamentStatus(tournamentId, status)
        }
    }

    fun deleteTournamentAdmin(tournamentId: String) {
        runAction("Tournament deleted") {
            repository.deleteTournament(tournamentId)
        }
    }

    fun seedDemoTournamentsAdmin() {
        runAction("Demo tournaments seeded") {
            repository.seedDemoTournaments()
        }
    }

    fun createMatchAdmin(
        tournament: Tournament,
        roundName: String,
        matchNumber: Long,
        mapName: String,
        scheduledTimeMs: Long,
        roomId: String,
        roomPassword: String,
        notes: String,
        releaseImmediately: Boolean
    ) {
        runAction("Match scheduled") {
            repository.createMatchAndCredentials(
                tournament = tournament,
                roundName = roundName,
                matchNumber = matchNumber,
                mapName = mapName,
                scheduledTimeMs = scheduledTimeMs,
                roomId = roomId,
                roomPassword = roomPassword,
                notes = notes,
                releaseImmediately = releaseImmediately
            )
        }
    }

    fun releaseCredentialsAdmin(
        match: MatchItem,
        roomId: String,
        roomPassword: String,
        notes: String,
        release: Boolean
    ) {
        runAction(if (release) "Room credentials released & players notified!" else "Room credentials saved privately") {
            repository.releaseOrUpdateCredentials(match, roomId, roomPassword, notes, release)
        }
    }

    fun sendMatchReminderAdmin(match: MatchItem) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.sendMatchReminderAdmin(match)
            _isBusy.value = false
            result.onSuccess { count ->
                val msg = if (count > 0) "Match reminder sent to $count approved players" else "No approved participants registered yet"
                _snackbarMessages.tryEmit(msg)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "Failed to send match reminder")
            }
        }
    }

    fun publishMatchResultAdmin(
        match: MatchItem,
        tournament: Tournament,
        participantUserId: String,
        participantName: String,
        teamId: String,
        placement: Long,
        kills: Long
    ) {
        runAction("Match result published & standings updated") {
            repository.publishMatchResult(
                match = match,
                tournament = tournament,
                participantUserId = participantUserId,
                participantName = participantName,
                teamId = teamId,
                placement = placement,
                kills = kills
            )
        }
    }

    fun updateRegistrationStatusAdmin(reg: Registration, status: String) {
        runAction("Registration marked $status") {
            repository.updateRegistrationStatusAdmin(reg, status)
        }
    }

    fun issueRewardAdmin(
        tournament: Tournament,
        userId: String,
        recipientName: String,
        placement: Long,
        amountNpr: Long
    ) {
        runAction("Reward issued in NPR") {
            repository.issueRewardAdmin(tournament, userId, recipientName, placement, amountNpr)
        }
    }

    fun updateRewardStatusAdmin(rewardId: String, status: String) {
        runAction("Reward status updated to $status") {
            repository.updateRewardStatusAdmin(rewardId, status)
        }
    }

    fun publishAnnouncementAdmin(title: String, body: String, category: String) {
        runAction("Announcement published") {
            repository.publishAnnouncementAdmin(title, body, category)
        }
    }

    fun updateReportStatusAdmin(reportId: String, status: String) {
        runAction("Report marked $status") {
            repository.updateReportStatusAdmin(reportId, status)
        }
    }

    private companion object {
        const val TAG = "ClutchXVM"
    }
}
