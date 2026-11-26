package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.GameType
import com.example.data.model.LeaderboardPlayer
import com.example.data.model.Registration
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.model.UiState
import com.example.i18n.LocalClutchStrings
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TournamentCard
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.BronzeRank
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueContainer
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SilverRank
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VictoryGold

@Composable
fun HomeScreen(
    tournamentsState: UiState<List<Tournament>>,
    registrationsState: UiState<List<Registration>>,
    announcementsState: UiState<List<Announcement>>,
    leaderboardState: UiState<List<LeaderboardPlayer>>,
    unreadNotifications: Int,
    onOpenTournament: (String) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToMyMatches: () -> Unit,
    onNavigateToTeams: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalClutchStrings.current
    val registeredIds = (registrationsState as? UiState.Success)?.data
        ?.filter { it.status != "CANCELLED" && it.status != "REJECTED" }
        ?.map { it.tournamentId }
        ?.toSet() ?: emptySet()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Hero Arena Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_arena),
                    contentDescription = "ClutchX Nepal Arena",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.2f),
                                    ObsidianBg.copy(alpha = 0.72f),
                                    ObsidianBg
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "NEPAL ESPORTS ARENA • NST",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue
                    )
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onNavigateToDiscover,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("home_explore_button")
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.navTournaments, style = MaterialTheme.typography.labelLarge)
                        }
                        Surface(
                            color = CharcoalElevated.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderCarbon),
                            modifier = Modifier
                                .clickable(onClick = onNavigateToMyMatches)
                                .testTag("home_quick_my_matches")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.quickMyMatches, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Access Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(
                    icon = Icons.Default.SportsEsports,
                    label = strings.quickMyMatches,
                    badge = if (registeredIds.isNotEmpty()) "${registeredIds.size}" else null,
                    accent = ElectricOrange,
                    onClick = onNavigateToMyMatches,
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Default.Groups,
                    label = "Teams",
                    badge = null,
                    accent = ElectricBlue,
                    onClick = onNavigateToTeams,
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Default.Notifications,
                    label = "Inbox",
                    badge = if (unreadNotifications > 0) "$unreadNotifications" else null,
                    accent = VictoryGold,
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Tournaments Feed Sections from Firestore
        when (tournamentsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricOrange)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.EmojiEvents,
                        title = "Unable to load tournaments",
                        subtitle = tournamentsState.message,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            is UiState.Success -> {
                val all = tournamentsState.data
                val featured = all.filter { it.featured && it.tournamentStatus != TournamentStatus.CANCELLED }
                val live = all.filter { it.tournamentStatus == TournamentStatus.LIVE }
                val upcomingFf = all.filter { it.gameType == GameType.FREE_FIRE && it.tournamentStatus == TournamentStatus.UPCOMING }
                val upcomingPubg = all.filter { it.gameType == GameType.PUBG_MOBILE && it.tournamentStatus == TournamentStatus.UPCOMING }
                val completed = all.filter { it.tournamentStatus == TournamentStatus.COMPLETED }

                if (all.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.EmojiEvents,
                            title = "Arena Open Soon",
                            subtitle = "Admins can create or seed [DEMO] tournaments from the Admin Dashboard.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (featured.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(
                                title = strings.featuredTournaments,
                                actionLabel = strings.all,
                                onActionClick = onNavigateToDiscover,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(featured, key = { "feat_${it.id}" }) { t ->
                                    TournamentCard(
                                        tournament = t,
                                        isRegistered = t.id in registeredIds,
                                        onClick = { onOpenTournament(t.id) },
                                        modifier = Modifier.width(305.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (live.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionHeader(title = strings.liveNow)
                            live.forEach { t ->
                                TournamentCard(
                                    tournament = t,
                                    isRegistered = t.id in registeredIds,
                                    onClick = { onOpenTournament(t.id) }
                                )
                            }
                        }
                    }
                }

                if (upcomingFf.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionHeader(
                                title = strings.upcomingFreeFire,
                                actionLabel = strings.all,
                                onActionClick = onNavigateToDiscover
                            )
                            upcomingFf.take(3).forEach { t ->
                                TournamentCard(
                                    tournament = t,
                                    isRegistered = t.id in registeredIds,
                                    onClick = { onOpenTournament(t.id) }
                                )
                            }
                        }
                    }
                }

                if (upcomingPubg.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionHeader(
                                title = strings.upcomingPubg,
                                actionLabel = strings.all,
                                onActionClick = onNavigateToDiscover
                            )
                            upcomingPubg.take(3).forEach { t ->
                                TournamentCard(
                                    tournament = t,
                                    isRegistered = t.id in registeredIds,
                                    onClick = { onOpenTournament(t.id) }
                                )
                            }
                        }
                    }
                }

                if (completed.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionHeader(title = strings.recentCompleted)
                            completed.take(2).forEach { t ->
                                TournamentCard(
                                    tournament = t,
                                    isRegistered = t.id in registeredIds,
                                    onClick = { onOpenTournament(t.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Announcements from Firestore
        val announcements = (announcementsState as? UiState.Success)?.data ?: emptyList()
        if (announcements.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(title = strings.announcements)
                    announcements.take(3).forEach { ann ->
                        Surface(
                            color = CharcoalSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderCarbon),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElectricBlueContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(ann.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(ann.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Global Leaderboard Preview
        val topPlayers = (leaderboardState as? UiState.Success)?.data?.take(3) ?: emptyList()
        if (topPlayers.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = strings.leaderboardPreview,
                        actionLabel = strings.navLeaderboard,
                        onActionClick = onNavigateToLeaderboard
                    )
                    Surface(
                        color = CharcoalElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderCarbon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            topPlayers.forEachIndexed { idx, p ->
                                val rankColor = when (idx) {
                                    0 -> VictoryGold
                                    1 -> SilverRank
                                    else -> BronzeRank
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(rankColor.copy(alpha = 0.18f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${idx + 1}",
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = rankColor,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(p.username, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${p.totalKills} kills • ${p.tournamentWins} wins", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                        }
                                    }
                                    Text(
                                        text = "${p.totalPoints} PTS",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = LiveEmerald,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    badge: String?,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CharcoalElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderCarbon),
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (badge != null) {
                Surface(
                    color = accent,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
