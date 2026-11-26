package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GameType(val displayName: String, val shortName: String) {
    FREE_FIRE("Free Fire", "FF"),
    PUBG_MOBILE("PUBG Mobile", "PUBGM");

    companion object {
        fun fromRaw(raw: String): GameType =
            entries.firstOrNull { it.name == raw } ?: FREE_FIRE
    }
}

enum class GameMode(val displayName: String, val teamSize: Int) {
    SOLO("Solo", 1),
    DUO("Duo", 2),
    SQUAD("Squad", 4);

    companion object {
        fun fromRaw(raw: String): GameMode =
            entries.firstOrNull { it.name == raw } ?: SQUAD
    }
}

enum class TournamentStatus(val displayName: String) {
    DRAFT("Draft"),
    UPCOMING("Upcoming"),
    LIVE("Live"),
    FULL("Full"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String): TournamentStatus =
            entries.firstOrNull { it.name == raw } ?: UPCOMING
    }
}

enum class TieBreakerRule(val displayName: String) {
    MOST_KILLS("Most Kills"),
    HIGHEST_PLACEMENT("Highest Placement"),
    MOST_WINS("Most Booyah / WWCD");

    companion object {
        fun fromRaw(raw: String): TieBreakerRule =
            entries.firstOrNull { it.name == raw } ?: MOST_KILLS
    }
}

enum class AppLanguage {
    EN, NE;

    companion object {
        fun fromRaw(raw: String): AppLanguage =
            entries.firstOrNull { it.name == raw } ?: EN
    }
}

enum class UserRole {
    PLAYER, ADMIN;

    companion object {
        fun fromRaw(raw: String): UserRole =
            entries.firstOrNull { it.name == raw } ?: PLAYER
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val avatarKey: String = "phoenix",
    val freeFireUid: String = "",
    val pubgUid: String = "",
    val language: String = AppLanguage.EN.name,
    val role: String = UserRole.PLAYER.name,
    val matchesPlayed: Long = 0L,
    val totalKills: Long = 0L,
    val tournamentWins: Long = 0L,
    val totalPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gameLanguage: AppLanguage get() = AppLanguage.fromRaw(language)
    val userRole: UserRole get() = UserRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val game: String = GameType.FREE_FIRE.name,
    val mode: String = GameMode.SQUAD.name,
    val entryFeeNpr: Long = 0L,
    val prizePoolNpr: Long = 0L,
    val maxParticipants: Long = 48L,
    val registeredCount: Long = 0L,
    val status: String = TournamentStatus.UPCOMING.name,
    val published: Boolean = true,
    val featured: Boolean = false,
    val demo: Boolean = false,
    val registrationDeadlineMs: Long = 0L,
    val startTimeMs: Long = 0L,
    val killPoints: Long = 2L,
    val booyahPoints: Long = 12L,
    val placementRule: String = "#1: 12, #2: 9, #3: 8, #4: 7, #5: 6, #6-8: 4",
    val tieBreakerRule: String = TieBreakerRule.MOST_KILLS.name,
    val rulesSummary: String = "Mobile devices only. No emulators or third-party scripts. Join room 10 minutes before match time.",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gameType: GameType get() = GameType.fromRaw(game)
    val gameMode: GameMode get() = GameMode.fromRaw(mode)
    val tournamentStatus: TournamentStatus get() = TournamentStatus.fromRaw(status)
    val tieBreaker: TieBreakerRule get() = TieBreakerRule.fromRaw(tieBreakerRule)
    val paidTournament: Boolean get() = entryFeeNpr > 0L
}

@Keep
@IgnoreExtraProperties
data class Team(
    @DocumentId val id: String = "",
    val name: String = "",
    val tag: String = "",
    val game: String = GameType.FREE_FIRE.name,
    val captainId: String = "",
    val captainName: String = "",
    val logoKey: String = "shield",
    val memberUids: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val matchesPlayed: Long = 0L,
    val wins: Long = 0L,
    val totalKills: Long = 0L,
    val totalPoints: Long = 0L,
    val publicRoster: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gameType: GameType get() = GameType.fromRaw(game)
}

@Keep
@IgnoreExtraProperties
data class TeamInvitation(
    @DocumentId val id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val game: String = GameType.FREE_FIRE.name,
    val captainId: String = "",
    val captainName: String = "",
    val invitedEmail: String = "",
    val invitedByName: String = "",
    val status: String = "PENDING",
    val answeredByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gameType: GameType get() = GameType.fromRaw(game)
}

@Keep
@IgnoreExtraProperties
data class Registration(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val game: String = GameType.FREE_FIRE.name,
    val mode: String = GameMode.SQUAD.name,
    val userId: String = "",
    val username: String = "",
    val inGameUid: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val memberUids: List<String> = emptyList(),
    val status: String = "APPROVED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gameType: GameType get() = GameType.fromRaw(game)
    val gameMode: GameMode get() = GameMode.fromRaw(mode)
}

@Keep
@IgnoreExtraProperties
data class MatchItem(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val roundName: String = "",
    val matchNumber: Long = 1L,
    val mapName: String = "",
    val game: String = GameType.FREE_FIRE.name,
    val scheduledTimeMs: Long = 0L,
    val status: String = "SCHEDULED",
    val credentialsReleased: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gameType: GameType get() = GameType.fromRaw(game)
}

@Keep
@IgnoreExtraProperties
data class MatchCredentials(
    @DocumentId val id: String = "",
    val matchId: String = "",
    val tournamentId: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    val notes: String = "",
    val released: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchResult(
    @DocumentId val id: String = "",
    val matchId: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val teamId: String = "",
    val participantName: String = "",
    val placement: Long = 1L,
    val kills: Long = 0L,
    val placementPoints: Long = 0L,
    val killPoints: Long = 0L,
    val totalPoints: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardPlayer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarKey: String = "phoenix",
    val primaryGame: String = GameType.FREE_FIRE.name,
    val matchesPlayed: Long = 0L,
    val totalKills: Long = 0L,
    val tournamentWins: Long = 0L,
    val totalPoints: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val gameType: GameType get() = GameType.fromRaw(primaryGame)
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "INFO",
    val relatedId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Reward(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val userId: String = "",
    val recipientName: String = "",
    val placement: Long = 1L,
    val amountNpr: Long = 0L,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "TOURNAMENT",
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReportItem(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val category: String = "SUPPORT",
    val targetId: String = "",
    val subject: String = "",
    val details: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
