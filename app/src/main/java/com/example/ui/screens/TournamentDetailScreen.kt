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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GameMode
import com.example.data.model.GameType
import com.example.data.model.MatchItem
import com.example.data.model.MatchResult
import com.example.data.model.Registration
import com.example.data.model.Team
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.model.UserProfile
import com.example.i18n.LocalClutchStrings
import com.example.i18n.NstTimeFormatter
import com.example.ui.components.GameBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.TournamentBannerRes
import com.example.ui.theme.AmberWarning
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    tournament: Tournament?,
    userProfile: UserProfile?,
    myRegistrations: List<Registration>,
    myTeams: List<Team>,
    matches: List<MatchItem>,
    results: List<MatchResult>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onRegister: (Tournament, Team?) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToTeams: () -> Unit
) {
    val strings = LocalClutchStrings.current

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = { Text(tournament?.title ?: "Tournament Details", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSurface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        if (tournament == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tournament not found", color = TextSecondary)
            }
            return@Scaffold
        }

        val myReg = myRegistrations.firstOrNull {
            it.tournamentId == tournament.id && it.status != "CANCELLED" && it.status != "REJECTED"
        }
        val eligibleTeams = myTeams.filter {
            it.gameType == tournament.gameType &&
                it.captainId == userProfile?.userId &&
                it.memberUids.size >= tournament.gameMode.teamSize
        }
        var selectedTeamId by remember(eligibleTeams) {
            mutableStateOf(eligibleTeams.firstOrNull()?.id ?: "")
        }

        val requiredUid = when (tournament.gameType) {
            GameType.FREE_FIRE -> userProfile?.freeFireUid.orEmpty()
            GameType.PUBG_MOBILE -> userProfile?.pubgUid.orEmpty()
        }
        val deadlinePassed = System.currentTimeMillis() >= tournament.registrationDeadlineMs
        val isFull = tournament.registeredCount >= tournament.maxParticipants ||
            tournament.tournamentStatus == TournamentStatus.FULL
        val tournamentMatches = matches.filter { it.tournamentId == tournament.id }
        val tournamentStandings = results
            .filter { it.tournamentId == tournament.id }
            .groupBy { it.participantName }
            .map { (name, list) ->
                Triple(name, list.sumOf { it.kills }, list.sumOf { it.totalPoints })
            }
            .sortedWith(compareByDescending<Triple<String, Long, Long>> { it.third }.thenByDescending { it.second })

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    Image(
                        painter = painterResource(id = TournamentBannerRes(tournament.gameType)),
                        contentDescription = tournament.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(ObsidianBg.copy(alpha = 0.2f), ObsidianBg)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GameBadge(tournament.gameType)
                            Surface(
                                color = CharcoalElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, BorderCarbon)
                            ) {
                                Text(
                                    text = tournament.gameMode.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            StatusPill(tournament.tournamentStatus)
                        }
                        Text(
                            text = tournament.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Key Financials & Schedule Card
            item {
                Surface(
                    color = CharcoalElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(strings.prizePool.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = NstTimeFormatter.formatPrizeNpr(tournament.prizePoolNpr),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = VictoryGold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(strings.entryFee.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = NstTimeFormatter.formatNpr(tournament.entryFeeNpr, strings.free),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (tournament.entryFeeNpr == 0L) LiveEmerald else ElectricOrange
                                )
                            }
                        }

                        val ratio = if (tournament.maxParticipants > 0L) {
                            (tournament.registeredCount.toFloat() / tournament.maxParticipants.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${strings.slots}: ${tournament.registeredCount}/${tournament.maxParticipants}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${(ratio * 100).toInt()}% filled", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricOrange,
                            trackColor = ObsidianBg
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("${strings.matchStart}: ${NstTimeFormatter.formatDateTimeNst(tournament.startTimeMs)}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text("${strings.deadline}: ${NstTimeFormatter.formatDateTimeNst(tournament.registrationDeadlineMs)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Registration Card
            item {
                Surface(
                    color = CharcoalSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(title = "Tournament Registration")

                        if (myReg != null) {
                            Surface(
                                color = LiveEmeraldBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LiveEmerald)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Registered (${myReg.status})", style = MaterialTheme.typography.titleMedium, color = LiveEmerald)
                                        val rosterInfo = if (myReg.teamName.isNotBlank()) "Team: ${myReg.teamName}" else "UID: ${myReg.inGameUid}"
                                        Text(rosterInfo, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    }
                                }
                            }
                        } else if (tournament.paidTournament) {
                            Surface(
                                color = ElectricBlueContainer,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricBlue)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(strings.paidDisabledNote, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }
                        } else if (tournament.tournamentStatus != TournamentStatus.UPCOMING || deadlinePassed || isFull) {
                            Text(
                                text = when {
                                    isFull -> "This tournament has reached maximum participant capacity."
                                    deadlinePassed -> "The NST registration deadline has passed."
                                    else -> "Registration is currently closed (${tournament.tournamentStatus.displayName})."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberWarning
                            )
                        } else if (requiredUid.length < 3) {
                            Text(
                                text = "Add your ${tournament.gameType.displayName} UID in Profile before registering.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberWarning
                            )
                            OutlinedButton(
                                onClick = onGoToProfile,
                                border = BorderStroke(1.dp, ElectricOrange),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Set Game UID in Profile", color = ElectricOrange)
                            }
                        } else {
                            var selectedTeam: Team? = null
                            if (tournament.gameMode != GameMode.SOLO) {
                                Text(
                                    text = "Select Eligible ${tournament.gameMode.displayName} Team (${tournament.gameType.displayName}, min ${tournament.gameMode.teamSize} members):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                if (eligibleTeams.isEmpty()) {
                                    Text(
                                        text = "No eligible ${tournament.gameType.displayName} team with ${tournament.gameMode.teamSize}+ members found where you are captain.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AmberWarning
                                    )
                                    OutlinedButton(
                                        onClick = onGoToTeams,
                                        border = BorderStroke(1.dp, ElectricBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = ElectricBlue)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Manage Teams & Invite Players", color = ElectricBlue)
                                    }
                                } else {
                                    eligibleTeams.forEach { team ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(CharcoalElevated)
                                                .clickable { selectedTeamId = team.id }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            RadioButton(
                                                selected = selectedTeamId == team.id,
                                                onClick = { selectedTeamId = team.id },
                                                colors = RadioButtonDefaults.colors(selectedColor = ElectricOrange)
                                            )
                                            Column {
                                                Text("[${team.tag}] ${team.name}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                                Text("${team.memberUids.size} members • Captain: ${team.captainName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                            }
                                        }
                                    }
                                    selectedTeam = eligibleTeams.firstOrNull { it.id == selectedTeamId }
                                }
                            }

                            val canSubmit = !isBusy && (tournament.gameMode == GameMode.SOLO || selectedTeam != null)
                            Button(
                                onClick = { onRegister(tournament, selectedTeam) },
                                enabled = canSubmit,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("tournament_register_button")
                            ) {
                                Text(strings.registerNow, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // Description, Scoring & Rules
            item {
                Surface(
                    color = CharcoalElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(title = "Rules & Scoring System")
                        if (tournament.description.isNotBlank()) {
                            Text(tournament.description, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = CharcoalSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("KILL POINTS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("+${tournament.killPoints} pts / kill", style = MaterialTheme.typography.titleMedium, color = ElectricOrange)
                                }
                            }
                            Surface(
                                color = CharcoalSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("BOOYAH / WWCD", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("+${tournament.booyahPoints} pts", style = MaterialTheme.typography.titleMedium, color = VictoryGold)
                                }
                            }
                        }
                        Text("Placement: ${tournament.placementRule}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Tie-breaker: ${tournament.tieBreaker.displayName}", style = MaterialTheme.typography.bodyMedium, color = ElectricBlue)
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = ElectricOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tournament.rulesSummary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }

            // Match Schedule (No Room Credentials shown publicly!)
            if (tournamentMatches.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionHeader(title = "Match Schedule")
                        tournamentMatches.forEach { m ->
                            Surface(
                                color = CharcoalSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderCarbon),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${m.roundName} • Map: ${m.mapName}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(NstTimeFormatter.formatDateTimeNst(m.scheduledTimeMs), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Text(m.status, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                }
                            }
                        }
                    }
                }
            }

            // Tournament Standings
            if (tournamentStandings.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionHeader(title = "Official Standings")
                        tournamentStandings.forEachIndexed { idx, (name, kills, pts) ->
                            Surface(
                                color = CharcoalElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderCarbon),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = if (idx == 0) VictoryGold else TextMuted, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("#${idx + 1} $name", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    }
                                    Text("$kills kills • $pts PTS", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelLarge, color = LiveEmerald)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
