package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MatchCredentials
import com.example.data.model.MatchItem
import com.example.data.model.MatchResult
import com.example.data.model.Registration
import com.example.data.model.Reward
import com.example.data.model.UiState
import com.example.i18n.LocalClutchStrings
import com.example.i18n.NstTimeFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GameBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueContainer
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.LiveEmeraldBg
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VictoryGold

@Composable
fun MyMatchesScreen(
    currentUserId: String,
    registrationsState: UiState<List<Registration>>,
    matchesState: UiState<List<MatchItem>>,
    resultsState: UiState<List<MatchResult>>,
    rewardsState: UiState<List<Reward>>,
    revealedCredentials: Map<String, MatchCredentials>,
    onRevealCredentials: (String) -> Unit,
    onOpenTournament: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalClutchStrings.current
    val clipboard = LocalClipboardManager.current

    val registrations = (registrationsState as? UiState.Success)?.data ?: emptyList()
    val activeRegs = registrations.filter { it.status != "CANCELLED" && it.status != "REJECTED" }
    val approvedTournamentIds = activeRegs.filter { it.status == "APPROVED" }.map { it.tournamentId }.toSet()
    val registeredTournamentIds = activeRegs.map { it.tournamentId }.toSet()

    val myMatches = ((matchesState as? UiState.Success)?.data ?: emptyList())
        .filter { it.tournamentId in registeredTournamentIds }
    val myResults = ((resultsState as? UiState.Success)?.data ?: emptyList())
        .filter { it.userId == currentUserId || it.tournamentId in registeredTournamentIds }
    val myRewards = (rewardsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.navMyMatches,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Registered Tournaments
        item { SectionHeader(title = "Registered Tournaments (${activeRegs.size})") }
        if (activeRegs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.SportsEsports,
                    title = "No Active Registrations",
                    subtitle = "Join a Free Fire or PUBG Mobile tournament from Discovery to see your match schedule and room credentials."
                )
            }
        } else {
            items(activeRegs, key = { it.id }) { reg ->
                Surface(
                    color = CharcoalElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTournament(reg.tournamentId) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GameBadge(reg.gameType)
                            Text(
                                text = reg.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (reg.status == "APPROVED") LiveEmerald else ElectricOrange
                            )
                        }
                        Text(reg.tournamentTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = if (reg.teamName.isNotBlank()) {
                                "${reg.mode} • Team: ${reg.teamName}"
                            } else {
                                "${reg.mode} • UID: ${reg.inGameUid}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Upcoming & Live Match Rooms
        if (myMatches.isNotEmpty()) {
            item { SectionHeader(title = "Match Rooms & Credentials") }
            items(myMatches, key = { it.id }) { match ->
                val isApproved = match.tournamentId in approvedTournamentIds
                val creds = revealedCredentials[match.id]

                Surface(
                    color = CharcoalSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (match.credentialsReleased && isApproved) LiveEmerald.copy(alpha = 0.5f) else BorderCarbon
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(match.tournamentTitle, style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                            Text(match.status, style = MaterialTheme.typography.labelSmall, color = if (match.status == "LIVE") LiveEmerald else TextSecondary)
                        }
                        Text("${match.roundName} • Map: ${match.mapName}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(NstTimeFormatter.formatDateTimeNst(match.scheduledTimeMs), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                        if (creds != null) {
                            Surface(
                                color = LiveEmeraldBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Room ID: ${creds.roomId}",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        IconButton(onClick = {
                                            clipboard.setText(AnnotatedString(creds.roomId))
                                            onShowMessage("Room ID copied")
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room ID", tint = LiveEmerald, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Pass: ${creds.roomPassword}",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        IconButton(onClick = {
                                            clipboard.setText(AnnotatedString(creds.roomPassword))
                                            onShowMessage("Password copied")
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = LiveEmerald, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    if (creds.notes.isNotBlank()) {
                                        Text(creds.notes, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                }
                            }
                        } else if (match.credentialsReleased && isApproved) {
                            Button(
                                onClick = { onRevealCredentials(match.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("reveal_creds_${match.id}")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.revealCredentials)
                            }
                        } else {
                            Surface(
                                color = ElectricBlueContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.credentialsLocked, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Completed Match Results
        if (myResults.isNotEmpty()) {
            item { SectionHeader(title = "Match Results") }
            items(myResults, key = { it.id }) { res ->
                Surface(
                    color = CharcoalElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(res.participantName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Rank #${res.placement} • ${res.kills} Kills", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text(
                            text = "${res.totalPoints} PTS",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = LiveEmerald
                        )
                    }
                }
            }
        }

        // Reward History
        item { SectionHeader(title = "NPR Reward History") }
        if (myRewards.isEmpty()) {
            item {
                Surface(
                    color = CharcoalSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = VictoryGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("No rewards issued yet. Win placements in tournaments to earn NPR prizes.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            items(myRewards, key = { it.id }) { reward ->
                Surface(
                    color = CharcoalElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VictoryGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(reward.tournamentTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Placement #${reward.placement} • ${reward.status}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text(
                            text = NstTimeFormatter.formatPrizeNpr(reward.amountNpr),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = VictoryGold
                        )
                    }
                }
            }
        }
    }
}
