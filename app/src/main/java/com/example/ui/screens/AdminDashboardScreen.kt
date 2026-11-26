package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.GameMode
import com.example.data.model.GameType
import com.example.data.model.MatchItem
import com.example.data.model.Registration
import com.example.data.model.ReportItem
import com.example.data.model.Reward
import com.example.data.model.TieBreakerRule
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.model.UiState
import com.example.i18n.LocalClutchStrings
import com.example.i18n.NstTimeFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GameBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VictoryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    tournamentsState: UiState<List<Tournament>>,
    matchesState: UiState<List<MatchItem>>,
    registrationsState: UiState<List<Registration>>,
    rewardsState: UiState<List<Reward>>,
    reportsState: UiState<List<ReportItem>>,
    onBack: () -> Unit,
    onCreateTournament: (
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
    ) -> Unit,
    onUpdateTournament: (
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
    ) -> Unit,
    onUpdateTournamentStatus: (String, TournamentStatus) -> Unit,
    onDeleteTournament: (String) -> Unit,
    onSeedDemo: () -> Unit,
    onCreateMatch: (
        tournament: Tournament,
        roundName: String,
        matchNumber: Long,
        mapName: String,
        scheduledTimeMs: Long,
        roomId: String,
        roomPassword: String,
        notes: String,
        releaseImmediately: Boolean
    ) -> Unit,
    onReleaseCredentials: (MatchItem, String, String, String, Boolean) -> Unit,
    onSendMatchReminder: (MatchItem) -> Unit,
    onPublishMatchResult: (MatchItem, Tournament, String, String, String, Long, Long) -> Unit,
    onUpdateRegistrationStatus: (Registration, String) -> Unit,
    onIssueReward: (Tournament, String, String, Long, Long) -> Unit,
    onUpdateRewardStatus: (String, String) -> Unit,
    onPublishAnnouncement: (String, String, String) -> Unit,
    onUpdateReportStatus: (String, String) -> Unit
) {
    val strings = LocalClutchStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var editingTournament by remember { mutableStateOf<Tournament?>(null) }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = ElectricOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.adminDashboard)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSurface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            if (isAdmin && selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingTournament = null
                        showCreateEditDialog = true
                    },
                    containerColor = ElectricOrange,
                    contentColor = TextPrimary,
                    modifier = Modifier.testTag("admin_add_tournament_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.createTournament)
                }
            }
        }
    ) { padding ->
        if (!isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Default.Lock,
                    title = "Administrator Access Required",
                    subtitle = "Your account does not hold administrator privileges."
                )
            }
            return@Scaffold
        }

        val tournaments = (tournamentsState as? UiState.Success)?.data ?: emptyList()
        val matches = (matchesState as? UiState.Success)?.data ?: emptyList()
        val registrations = (registrationsState as? UiState.Success)?.data ?: emptyList()
        val rewards = (rewardsState as? UiState.Success)?.data ?: emptyList()
        val reports = (reportsState as? UiState.Success)?.data ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val tabs = listOf("Tournaments", "Matches & Rooms", "Registrations", "Rewards & News")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CharcoalSurface,
                contentColor = ElectricOrange,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminTournamentsTab(
                    tournaments = tournaments,
                    onSeedDemo = onSeedDemo,
                    onCreateNew = {
                        editingTournament = null
                        showCreateEditDialog = true
                    },
                    onEdit = { t ->
                        editingTournament = t
                        showCreateEditDialog = true
                    },
                    onUpdateStatus = onUpdateTournamentStatus,
                    onDelete = onDeleteTournament
                )
                1 -> AdminMatchesTab(
                    tournaments = tournaments,
                    matches = matches,
                    registrations = registrations,
                    onCreateMatch = onCreateMatch,
                    onReleaseCredentials = onReleaseCredentials,
                    onSendMatchReminder = onSendMatchReminder,
                    onPublishMatchResult = onPublishMatchResult
                )
                2 -> AdminRegistrationsTab(
                    registrations = registrations,
                    reports = reports,
                    onUpdateRegStatus = onUpdateRegistrationStatus,
                    onUpdateReportStatus = onUpdateReportStatus
                )
                3 -> AdminRewardsContentTab(
                    tournaments = tournaments,
                    registrations = registrations,
                    rewards = rewards,
                    onIssueReward = onIssueReward,
                    onUpdateRewardStatus = onUpdateRewardStatus,
                    onPublishAnnouncement = onPublishAnnouncement
                )
            }
        }
    }

    if (showCreateEditDialog) {
        TournamentCrudDialog(
            initial = editingTournament,
            onDismiss = { showCreateEditDialog = false },
            onSave = { title, desc, game, mode, fee, prize, maxSlots, status, featured, deadlineMs, startMs, killPts, booyahPts, placementRule, tieBreaker, rules ->
                if (editingTournament == null) {
                    onCreateTournament(
                        title, desc, game, mode, fee, prize, maxSlots, status,
                        featured, deadlineMs, startMs, killPts, booyahPts, placementRule, tieBreaker, rules
                    )
                } else {
                    onUpdateTournament(
                        editingTournament!!.id, title, desc, game, mode, fee, prize, maxSlots,
                        status, featured, deadlineMs, startMs, killPts, booyahPts, placementRule, tieBreaker, rules
                    )
                }
                showCreateEditDialog = false
            }
        )
    }
}

@Composable
private fun AdminTournamentsTab(
    tournaments: List<Tournament>,
    onSeedDemo: () -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Tournament) -> Unit,
    onUpdateStatus: (String, TournamentStatus) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCreateNew,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                    modifier = Modifier.weight(1f).testTag("admin_create_tournament_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Tournament")
                }
                OutlinedButton(
                    onClick = onSeedDemo,
                    border = BorderStroke(1.dp, ElectricBlue),
                    modifier = Modifier.weight(1f).testTag("admin_seed_demo_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Seed Demo", color = ElectricBlue)
                }
            }
        }

        if (tournaments.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "No Tournaments Created",
                    subtitle = "Tap New Tournament or Seed Demo to launch your first Nepali esports cup."
                )
            }
        }

        items(tournaments, key = { it.id }) { t ->
            Surface(
                color = CharcoalElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GameBadge(t.gameType)
                            StatusPill(t.tournamentStatus)
                        }
                        Row {
                            IconButton(
                                onClick = { onEdit(t) },
                                modifier = Modifier.testTag("admin_edit_${t.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ElectricBlue)
                            }
                            IconButton(
                                onClick = { onDelete(t.id) },
                                modifier = Modifier.testTag("admin_delete_${t.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonError)
                            }
                        }
                    }

                    Text(t.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        text = "${t.gameMode.displayName} • Entry: ${NstTimeFormatter.formatNpr(t.entryFeeNpr)} • Prize: ${NstTimeFormatter.formatPrizeNpr(t.prizePoolNpr)} • Slots: ${t.registeredCount}/${t.maxParticipants}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "NST Start: ${NstTimeFormatter.formatDateTimeNst(t.startTimeMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TournamentStatus.entries.forEach { st ->
                            FilterChip(
                                selected = t.tournamentStatus == st,
                                onClick = { onUpdateStatus(t.id, st) },
                                label = { Text(st.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCrudDialog(
    initial: Tournament?,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var game by remember { mutableStateOf(initial?.gameType ?: GameType.FREE_FIRE) }
    var mode by remember { mutableStateOf(initial?.gameMode ?: GameMode.SQUAD) }
    var entryFeeText by remember { mutableStateOf((initial?.entryFeeNpr ?: 0L).toString()) }
    var prizePoolText by remember { mutableStateOf((initial?.prizePoolNpr ?: 10000L).toString()) }
    var maxSlotsText by remember { mutableStateOf((initial?.maxParticipants ?: 48L).toString()) }
    var status by remember { mutableStateOf(initial?.tournamentStatus ?: TournamentStatus.UPCOMING) }
    var featured by remember { mutableStateOf(initial?.featured ?: true) }
    var hoursUntilStartText by remember { mutableStateOf("24") }
    var killPointsText by remember { mutableStateOf((initial?.killPoints ?: 2L).toString()) }
    var booyahPointsText by remember { mutableStateOf((initial?.booyahPoints ?: 12L).toString()) }
    var placementRule by remember { mutableStateOf(initial?.placementRule ?: "#1: 12, #2: 9, #3: 8, #4: 7, #5: 6") }
    var tieBreaker by remember { mutableStateOf(initial?.tieBreaker ?: TieBreakerRule.MOST_KILLS) }
    var rulesSummary by remember { mutableStateOf(initial?.rulesSummary ?: "Mobile only. Fair-play enforced.") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = { Text(if (initial == null) "Create Tournament" else "Edit Tournament", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tournament Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("crud_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Game Title", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameType.entries.forEach { g ->
                        FilterChip(
                            selected = game == g,
                            onClick = { game = g },
                            label = { Text(g.displayName) }
                        )
                    }
                }
                Text("Game Mode", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m.displayName) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryFeeText,
                        onValueChange = { entryFeeText = it },
                        label = { Text("Entry Fee (NPR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("crud_fee_input")
                    )
                    OutlinedTextField(
                        value = prizePoolText,
                        onValueChange = { prizePoolText = it },
                        label = { Text("Prize Pool (NPR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("crud_prize_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxSlotsText,
                        onValueChange = { maxSlotsText = it },
                        label = { Text("Max Slots") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hoursUntilStartText,
                        onValueChange = { hoursUntilStartText = it },
                        label = { Text("Starts in (hrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Status", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TournamentStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.displayName) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured on Home", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = killPointsText,
                        onValueChange = { killPointsText = it },
                        label = { Text("Kill Pts") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = booyahPointsText,
                        onValueChange = { booyahPointsText = it },
                        label = { Text("Win Pts") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = placementRule,
                    onValueChange = { placementRule = it },
                    label = { Text("Placement Points Rule") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TieBreakerRule.entries.forEach { tb ->
                        FilterChip(
                            selected = tieBreaker == tb,
                            onClick = { tieBreaker = tb },
                            label = { Text(tb.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = rulesSummary,
                    onValueChange = { rulesSummary = it },
                    label = { Text("Rules & Fair-Play Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(validationError!!, color = CrimsonError, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val fee = entryFeeText.toLongOrNull()
                    val prize = prizePoolText.toLongOrNull()
                    val slots = maxSlotsText.toLongOrNull()
                    val hours = (hoursUntilStartText.toLongOrNull() ?: 24L).coerceAtLeast(1L)
                    if (cleanTitle.length < 3) {
                        validationError = "Title must be at least 3 characters."
                        return@Button
                    }
                    if (fee == null || fee < 0L || prize == null || prize < 0L) {
                        validationError = "Enter valid NPR numbers for Entry Fee and Prize Pool."
                        return@Button
                    }
                    if (slots == null || slots !in 2L..1000L) {
                        validationError = "Max slots must be between 2 and 1000."
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val startMs = initial?.startTimeMs?.takeIf { hoursUntilStartText == "24" && initial != null }
                        ?: (now + hours * 3_600_000L)
                    val deadlineMs = (startMs - 1_800_000L).coerceAtLeast(now + 600_000L)
                    onSave(
                        cleanTitle,
                        description,
                        game,
                        mode,
                        fee,
                        prize,
                        slots,
                        status,
                        featured,
                        deadlineMs,
                        startMs,
                        killPointsText.toLongOrNull() ?: 2L,
                        booyahPointsText.toLongOrNull() ?: 12L,
                        placementRule,
                        tieBreaker,
                        rulesSummary
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                modifier = Modifier.testTag("crud_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AdminMatchesTab(
    tournaments: List<Tournament>,
    matches: List<MatchItem>,
    registrations: List<Registration>,
    onCreateMatch: (Tournament, String, Long, String, Long, String, String, String, Boolean) -> Unit,
    onReleaseCredentials: (MatchItem, String, String, String, Boolean) -> Unit,
    onSendMatchReminder: (MatchItem) -> Unit,
    onPublishMatchResult: (MatchItem, Tournament, String, String, String, Long, Long) -> Unit
) {
    var selectedTournament by remember(tournaments) { mutableStateOf(tournaments.firstOrNull()) }
    var roundName by remember { mutableStateOf("Match 1 - Finals") }
    var mapName by remember { mutableStateOf("Bermuda") }
    var roomId by remember { mutableStateOf("") }
    var roomPass by remember { mutableStateOf("") }
    var releaseNow by remember { mutableStateOf(false) }

    var resultMatch by remember { mutableStateOf<MatchItem?>(null) }
    var credMatch by remember { mutableStateOf<MatchItem?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                color = CharcoalElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Schedule Match & Private Room", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (tournaments.isEmpty()) {
                        Text("Create a tournament first.", color = TextMuted)
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tournaments.forEach { t ->
                                FilterChip(
                                    selected = selectedTournament?.id == t.id,
                                    onClick = { selectedTournament = t },
                                    label = { Text(t.title.take(22)) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = roundName,
                            onValueChange = { roundName = it },
                            label = { Text("Round Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mapName,
                            onValueChange = { mapName = it },
                            label = { Text("Map (Bermuda / Erangel / Miramar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = roomId,
                                onValueChange = { roomId = it },
                                label = { Text("Private Room ID") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = roomPass,
                                onValueChange = { roomPass = it },
                                label = { Text("Password") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Release credentials immediately", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Switch(checked = releaseNow, onCheckedChange = { releaseNow = it })
                        }
                        Button(
                            onClick = {
                                val t = selectedTournament ?: return@Button
                                onCreateMatch(
                                    t,
                                    roundName,
                                    (matches.count { it.tournamentId == t.id } + 1).toLong(),
                                    mapName,
                                    t.startTimeMs,
                                    roomId,
                                    roomPass,
                                    "Join 10 minutes before match time (NST).",
                                    releaseNow
                                )
                                roomId = ""
                                roomPass = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Match")
                        }
                    }
                }
            }
        }

        items(matches, key = { it.id }) { m ->
            Surface(
                color = CharcoalSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderCarbon),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.tournamentTitle, style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                            Text("${m.roundName} • ${m.mapName}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(NstTimeFormatter.formatDateTimeNst(m.scheduledTimeMs), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text(
                            text = if (m.credentialsReleased) "ROOM LIVE" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (m.credentialsReleased) LiveEmerald else TextMuted
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { credMatch = m },
                            border = BorderStroke(1.dp, ElectricBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Room Creds", color = ElectricBlue)
                        }
                        Button(
                            onClick = { resultMatch = m },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Post Result")
                        }
                    }
                    OutlinedButton(
                        onClick = { onSendMatchReminder(m) },
                        border = BorderStroke(1.dp, VictoryGold),
                        modifier = Modifier.fillMaxWidth().testTag("admin_send_reminder_${m.id}")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = VictoryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Match Reminder Alert", color = VictoryGold)
                    }
                }
            }
        }
    }

    if (credMatch != null) {
        var rId by remember { mutableStateOf("") }
        var rPass by remember { mutableStateOf("") }
        var rNotes by remember { mutableStateOf("Join 10 mins early in NST.") }
        var rel by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { credMatch = null },
            containerColor = CharcoalSurface,
            title = { Text("Manage Room Credentials", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = rId, onValueChange = { rId = it }, label = { Text("Room ID") })
                    OutlinedTextField(value = rPass, onValueChange = { rPass = it }, label = { Text("Room Password") })
                    OutlinedTextField(value = rNotes, onValueChange = { rNotes = it }, label = { Text("Lobby Notes") })
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Release to approved players", color = TextPrimary)
                        Switch(checked = rel, onCheckedChange = { rel = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (rId.isNotBlank() && rPass.isNotBlank()) {
                        onReleaseCredentials(credMatch!!, rId, rPass, rNotes, rel)
                        credMatch = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { credMatch = null }) { Text("Close") } }
        )
    }

    if (resultMatch != null) {
        val m = resultMatch!!
        val t = tournaments.firstOrNull { it.id == m.tournamentId }
        val regsForTournament = registrations.filter { it.tournamentId == m.tournamentId && it.status == "APPROVED" }
        var selectedReg by remember { mutableStateOf(regsForTournament.firstOrNull()) }
        var manualUid by remember { mutableStateOf("") }
        var manualName by remember { mutableStateOf("") }
        var placementText by remember { mutableStateOf("1") }
        var killsText by remember { mutableStateOf("6") }

        AlertDialog(
            onDismissRequest = { resultMatch = null },
            containerColor = CharcoalSurface,
            title = { Text("Verify & Publish Result", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (regsForTournament.isNotEmpty()) {
                        Text("Select Approved Participant:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            regsForTournament.forEach { r ->
                                FilterChip(
                                    selected = selectedReg?.id == r.id,
                                    onClick = { selectedReg = r },
                                    label = { Text(r.teamName.ifBlank { r.username }) }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(value = manualUid, onValueChange = { manualUid = it }, label = { Text("Player User ID") })
                        OutlinedTextField(value = manualName, onValueChange = { manualName = it }, label = { Text("Player/Team Name") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = placementText,
                            onValueChange = { placementText = it },
                            label = { Text("Rank #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = killsText,
                            onValueChange = { killsText = it },
                            label = { Text("Kills") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (t != null) {
                        val uid = selectedReg?.userId ?: manualUid.trim()
                        val name = selectedReg?.let { it.teamName.ifBlank { it.username } } ?: manualName.trim()
                        val teamId = selectedReg?.teamId ?: ""
                        if (uid.isNotBlank() && name.isNotBlank()) {
                            onPublishMatchResult(
                                m, t, uid, name, teamId,
                                placementText.toLongOrNull() ?: 1L,
                                killsText.toLongOrNull() ?: 0L
                            )
                            resultMatch = null
                        }
                    }
                }) { Text("Publish") }
            },
            dismissButton = { TextButton(onClick = { resultMatch = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminRegistrationsTab(
    registrations: List<Registration>,
    reports: List<ReportItem>,
    onUpdateRegStatus: (Registration, String) -> Unit,
    onUpdateReportStatus: (String, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Registrations (${registrations.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        items(registrations, key = { it.id }) { reg ->
            Surface(
                color = CharcoalElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(reg.tournamentTitle, style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                        Text(reg.status, style = MaterialTheme.typography.labelSmall, color = if (reg.status == "APPROVED") LiveEmerald else ElectricOrange)
                    }
                    Text("${reg.username} ${if (reg.teamName.isNotBlank()) "• Team ${reg.teamName}" else ""}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Game UID: ${reg.inGameUid} • Mode: ${reg.mode}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onUpdateRegStatus(reg, "APPROVED") }, border = BorderStroke(1.dp, LiveEmerald)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = LiveEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve", color = LiveEmerald)
                        }
                        OutlinedButton(onClick = { onUpdateRegStatus(reg, "REJECTED") }, border = BorderStroke(1.dp, CrimsonError)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject", color = CrimsonError)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Player Reports & Support (${reports.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        items(reports, key = { it.id }) { rep ->
            Surface(
                color = CharcoalSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${rep.category} • ${rep.reporterName}", style = MaterialTheme.typography.labelMedium, color = ElectricOrange)
                        Text(rep.status, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                    }
                    Text(rep.subject, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(rep.details, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = rep.status == "REVIEWED", onClick = { onUpdateReportStatus(rep.id, "REVIEWED") }, label = { Text("Reviewed") })
                        FilterChip(selected = rep.status == "RESOLVED", onClick = { onUpdateReportStatus(rep.id, "RESOLVED") }, label = { Text("Resolved") })
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRewardsContentTab(
    tournaments: List<Tournament>,
    registrations: List<Registration>,
    rewards: List<Reward>,
    onIssueReward: (Tournament, String, String, Long, Long) -> Unit,
    onUpdateRewardStatus: (String, String) -> Unit,
    onPublishAnnouncement: (String, String, String) -> Unit
) {
    var annTitle by remember { mutableStateOf("") }
    var annBody by remember { mutableStateOf("") }
    var selectedTournament by remember(tournaments) { mutableStateOf(tournaments.firstOrNull()) }
    var selectedReg by remember(registrations) { mutableStateOf(registrations.firstOrNull()) }
    var placementText by remember { mutableStateOf("1") }
    var amountText by remember { mutableStateOf("5000") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Analytics card
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsTile("Tournaments", "${tournaments.size}", ElectricOrange, Modifier.weight(1f))
                AnalyticsTile("Registrations", "${registrations.size}", ElectricBlue, Modifier.weight(1f))
                AnalyticsTile("Total Prize", NstTimeFormatter.formatPrizeNpr(tournaments.sumOf { it.prizePoolNpr }), VictoryGold, Modifier.weight(1f))
            }
        }

        // Publish Announcement
        item {
            Surface(
                color = CharcoalElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Publish Announcement", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    OutlinedTextField(value = annTitle, onValueChange = { annTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = annBody, onValueChange = { annBody = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (annTitle.trim().length >= 2 && annBody.trim().length >= 2) {
                                onPublishAnnouncement(annTitle, annBody, "TOURNAMENT")
                                annTitle = ""
                                annBody = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Publish Announcement", color = ObsidianBg) }
                }
            }
        }

        // Issue NPR Reward
        item {
            Surface(
                color = CharcoalElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Record Tournament Reward (NPR)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (tournaments.isNotEmpty() && registrations.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tournaments.forEach { t ->
                                FilterChip(selected = selectedTournament?.id == t.id, onClick = { selectedTournament = t }, label = { Text(t.title.take(18)) })
                            }
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            registrations.forEach { r ->
                                FilterChip(selected = selectedReg?.id == r.id, onClick = { selectedReg = r }, label = { Text(r.teamName.ifBlank { r.username }) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = placementText, onValueChange = { placementText = it }, label = { Text("Rank #") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (NPR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                val t = selectedTournament ?: return@Button
                                val r = selectedReg ?: return@Button
                                onIssueReward(t, r.userId, r.teamName.ifBlank { r.username }, placementText.toLongOrNull() ?: 1L, amountText.toLongOrNull() ?: 0L)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Issue Reward Record") }
                    } else {
                        Text("Need at least one tournament and approved registration to issue a reward.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }
            }
        }

        items(rewards, key = { it.id }) { rw ->
            Surface(
                color = CharcoalSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${rw.recipientName} (#${rw.placement})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(NstTimeFormatter.formatPrizeNpr(rw.amountNpr), style = MaterialTheme.typography.titleMedium, color = VictoryGold)
                    }
                    Text("${rw.tournamentTitle} • Status: ${rw.status}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PENDING", "APPROVED", "DISTRIBUTED").forEach { st ->
                            FilterChip(selected = rw.status == st, onClick = { onUpdateRewardStatus(rw.id, st) }, label = { Text(st) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        color = CharcoalSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderCarbon),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
        }
    }
}
