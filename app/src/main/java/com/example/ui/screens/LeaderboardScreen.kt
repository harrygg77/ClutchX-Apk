package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LeaderboardPlayer
import com.example.data.model.Team
import com.example.data.model.UiState
import com.example.i18n.LocalClutchStrings
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.BronzeRank
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricBlue
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
fun LeaderboardScreen(
    playersState: UiState<List<LeaderboardPlayer>>,
    teamsState: UiState<List<Team>>,
    modifier: Modifier = Modifier
) {
    val strings = LocalClutchStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Header with Trophy illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_victory_trophy),
                contentDescription = "ClutchX Championship Trophy",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(ObsidianBg.copy(alpha = 0.25f), ObsidianBg)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text("NEPAL RANKINGS", style = MaterialTheme.typography.labelSmall, color = VictoryGold)
                Text(strings.navLeaderboard, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CharcoalSurface,
            contentColor = ElectricOrange
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Players") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Teams") })
        }

        when (selectedTab) {
            0 -> {
                when (playersState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricOrange)
                    }
                    is UiState.Error -> Box(Modifier.padding(16.dp)) {
                        EmptyStateCard(Icons.Default.EmojiEvents, "Leaderboard Error", playersState.message)
                    }
                    is UiState.Success -> {
                        if (playersState.data.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(Icons.Default.EmojiEvents, "No Rankings Yet", "Play matches and verify results to climb the Nepali leaderboard.")
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(playersState.data, key = { _, p -> p.id }) { idx, p ->
                                    RankRowCard(
                                        rank = idx + 1,
                                        title = p.username,
                                        subtitle = "${p.gameType.displayName} • ${p.matchesPlayed} matches • ${p.totalKills} kills • ${p.tournamentWins} wins",
                                        points = p.totalPoints
                                    )
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                when (teamsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricBlue)
                    }
                    is UiState.Error -> Box(Modifier.padding(16.dp)) {
                        EmptyStateCard(Icons.Default.Groups, "Team Rankings Error", teamsState.message)
                    }
                    is UiState.Success -> {
                        if (teamsState.data.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(Icons.Default.Groups, "No Teams Yet", "Create a squad or duo team in Profile > My Teams to compete.")
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(teamsState.data, key = { _, t -> t.id }) { idx, t ->
                                    RankRowCard(
                                        rank = idx + 1,
                                        title = "[${t.tag}] ${t.name}",
                                        subtitle = "${t.gameType.displayName} • ${t.memberUids.size} members • ${t.wins} wins",
                                        points = t.totalPoints
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
private fun RankRowCard(
    rank: Int,
    title: String,
    subtitle: String,
    points: Long
) {
    val rankColor = when (rank) {
        1 -> VictoryGold
        2 -> SilverRank
        3 -> BronzeRank
        else -> TextMuted
    }
    Surface(
        color = CharcoalElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (rank <= 3) rankColor.copy(alpha = 0.45f) else BorderCarbon),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = rankColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Text(
                text = "$points PTS",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = LiveEmerald,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
