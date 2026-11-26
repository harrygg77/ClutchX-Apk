package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.data.model.GameType
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.i18n.LocalClutchStrings
import com.example.i18n.NstTimeFormatter
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalHighest
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonErrorBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueContainer
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.ElectricOrangeContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.LiveEmeraldBg
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VictoryGold

@Composable
fun TournamentBannerRes(game: GameType): Int = when (game) {
    GameType.FREE_FIRE -> R.drawable.img_banner_freefire
    GameType.PUBG_MOBILE -> R.drawable.img_banner_pubg
}

@Composable
fun StatusPill(status: TournamentStatus, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        TournamentStatus.LIVE -> Triple(LiveEmeraldBg, LiveEmerald, "● LIVE")
        TournamentStatus.UPCOMING -> Triple(ElectricBlueContainer, ElectricBlue, "UPCOMING")
        TournamentStatus.FULL -> Triple(Color(0xFF3A2806), AmberWarning, "FULL")
        TournamentStatus.COMPLETED -> Triple(CharcoalHighest, TextSecondary, "COMPLETED")
        TournamentStatus.DRAFT -> Triple(CharcoalHighest, TextMuted, "DRAFT")
        TournamentStatus.CANCELLED -> Triple(CrimsonErrorBg, CrimsonError, "CANCELLED")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GameBadge(game: GameType, modifier: Modifier = Modifier) {
    val accent = if (game == GameType.FREE_FIRE) ElectricOrange else ElectricBlue
    val bg = if (game == GameType.FREE_FIRE) ElectricOrangeContainer else ElectricBlueContainer
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = game.displayName.uppercase(),
                color = accent,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    isRegistered: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalClutchStrings.current
    val fillRatio = if (tournament.maxParticipants > 0L) {
        (tournament.registeredCount.toFloat() / tournament.maxParticipants.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val gameAccent = if (tournament.gameType == GameType.FREE_FIRE) ElectricOrange else ElectricBlue

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tournament_card_${tournament.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
        border = BorderStroke(
            width = 1.dp,
            color = if (tournament.featured) gameAccent.copy(alpha = 0.55f) else BorderCarbon
        )
    ) {
        Column {
            // Banner with gradient overlay & status badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                Image(
                    painter = painterResource(id = TournamentBannerRes(tournament.gameType)),
                    contentDescription = "${tournament.title} banner",
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
                                    ObsidianBg.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GameBadge(tournament.gameType)
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, BorderCarbon)
                        ) {
                            Text(
                                text = tournament.gameMode.displayName.uppercase(),
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (tournament.demo) {
                            Surface(
                                color = VictoryGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, VictoryGold.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "DEMO",
                                    color = VictoryGold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    StatusPill(tournament.tournamentStatus)
                }

                // Bottom title inside banner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Metrics grid
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock(
                        label = strings.prizePool,
                        value = NstTimeFormatter.formatPrizeNpr(tournament.prizePoolNpr),
                        valueColor = VictoryGold,
                        icon = Icons.Default.EmojiEvents
                    )
                    MetricBlock(
                        label = strings.entryFee,
                        value = NstTimeFormatter.formatNpr(tournament.entryFeeNpr, strings.free),
                        valueColor = if (tournament.entryFeeNpr == 0L) LiveEmerald else ElectricOrange,
                        icon = null
                    )
                    MetricBlock(
                        label = strings.slots,
                        value = "${tournament.registeredCount}/${tournament.maxParticipants}",
                        valueColor = TextPrimary,
                        icon = Icons.Default.Groups
                    )
                }

                LinearProgressIndicator(
                    progress = { fillRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = gameAccent,
                    trackColor = ObsidianBg
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = NstTimeFormatter.formatShortTimeNst(tournament.startTimeMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    if (isRegistered) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(LiveEmeraldBg, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = LiveEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.registered,
                                style = MaterialTheme.typography.labelSmall,
                                color = LiveEmerald
                            )
                        }
                    } else {
                        Text(
                            text = "${strings.deadline}: ${NstTimeFormatter.formatShortTimeNst(tournament.registrationDeadlineMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector?
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .background(
                        Brush.verticalGradient(listOf(ElectricOrange, ElectricBlue)),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = ElectricBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CharcoalElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderCarbon),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(ElectricBlueContainer, CircleShape)
                    .border(1.dp, ElectricBlue.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
