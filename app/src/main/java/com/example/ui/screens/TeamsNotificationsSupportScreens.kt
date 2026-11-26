package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.GameType
import com.example.data.model.ReportItem
import com.example.data.model.Team
import com.example.data.model.TeamInvitation
import com.example.data.model.UiState
import com.example.i18n.LocalClutchStrings
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GameBadge
import com.example.ui.components.SectionHeader
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
fun TeamsScreen(
    currentUserId: String,
    myTeamsState: UiState<List<Team>>,
    invitationsState: UiState<List<TeamInvitation>>,
    onBack: () -> Unit,
    onCreateTeam: (String, String, GameType, String) -> Unit,
    onInviteTeammate: (Team, String) -> Unit,
    onRespondInvitation: (TeamInvitation, Boolean) -> Unit,
    onRemoveMember: (Team, String) -> Unit
) {
    val strings = LocalClutchStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var inviteTargetTeam by remember { mutableStateOf<Team?>(null) }

    val teams = (myTeamsState as? UiState.Success)?.data ?: emptyList()
    val pendingInvites = ((invitationsState as? UiState.Success)?.data ?: emptyList())
        .filter { it.status == "PENDING" }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = { Text(strings.teams) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_team_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Squad / Duo Team")
                }
            }

            if (pendingInvites.isNotEmpty()) {
                item { SectionHeader(title = "Pending Invitations (${pendingInvites.size})") }
                items(pendingInvites, key = { it.id }) { inv ->
                    Surface(
                        color = CharcoalElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mail, contentDescription = null, tint = ElectricBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${inv.teamName} (${inv.gameType.displayName})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            Text("Invited by Captain ${inv.captainName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onRespondInvitation(inv, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LiveEmerald),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Accept", color = ObsidianBg)
                                }
                                OutlinedButton(
                                    onClick = { onRespondInvitation(inv, false) },
                                    border = BorderStroke(1.dp, CrimsonError),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject", color = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "My Teams (${teams.size})") }
            if (teams.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Groups,
                        title = "No Teams Yet",
                        subtitle = "Create a Free Fire or PUBG Mobile team and invite teammates to join Duo & Squad tournaments."
                    )
                }
            } else {
                items(teams, key = { it.id }) { team ->
                    val isCaptain = team.captainId == currentUserId
                    Surface(
                        color = CharcoalElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderCarbon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GameBadge(team.gameType)
                                Text(if (isCaptain) "CAPTAIN" else "MEMBER", style = MaterialTheme.typography.labelSmall, color = VictoryGold)
                            }
                            Text("[${team.tag}] ${team.name}", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text("Roster (${team.memberUids.size}/5) • Wins: ${team.wins} • Points: ${team.totalPoints}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                            team.memberUids.forEachIndexed { idx, mUid ->
                                val mName = team.memberNames.getOrNull(idx) ?: "Player ${idx + 1}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CharcoalSurface, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (mUid == team.captainId) "$mName (Captain)" else mName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    if (isCaptain && mUid != team.captainId) {
                                        IconButton(onClick = { onRemoveMember(team, mUid) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CrimsonError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            if (isCaptain && team.memberUids.size < 5) {
                                OutlinedButton(
                                    onClick = { inviteTargetTeam = team },
                                    border = BorderStroke(1.dp, ElectricBlue),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Invite Teammate by Email", color = ElectricBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var tag by remember { mutableStateOf("") }
        var game by remember { mutableStateOf(GameType.FREE_FIRE) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Create Esports Team", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Team Name") })
                    OutlinedTextField(value = tag, onValueChange = { tag = it.take(8) }, label = { Text("Clan Tag (e.g. GRK)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameType.entries.forEach { g ->
                            FilterChip(selected = game == g, onClick = { game = g }, label = { Text(g.displayName) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.trim().length >= 2 && tag.trim().length >= 2) {
                        onCreateTeam(name, tag, game, "shield")
                        showCreateDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    if (inviteTargetTeam != null) {
        var email by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { inviteTargetTeam = null },
            containerColor = CharcoalSurface,
            title = { Text("Invite to ${inviteTargetTeam!!.name}", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Teammate Google Email") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (email.contains("@")) {
                        onInviteTeammate(inviteTargetTeam!!, email)
                        inviteTargetTeam = null
                    }
                }) { Text("Send Invite") }
            },
            dismissButton = { TextButton(onClick = { inviteTargetTeam = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<AppNotification>>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenAlertTarget: (AppNotification) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalClutchStrings.current
    val list = (notificationsState as? UiState.Success)?.data ?: emptyList()
    var filter by remember { mutableStateOf("ALL") }

    val filtered = remember(list, filter) {
        when (filter) {
            "APPROVALS" -> list.filter {
                it.type.uppercase() in setOf("REGISTRATION", "REGISTRATION_APPROVAL")
            }
            "REMINDERS" -> list.filter {
                it.type.uppercase() in setOf("MATCH_REMINDER", "ROOM_CREDENTIALS")
            }
            else -> list
        }
    }
    val hasUnread = list.any { !it.read }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = { Text(strings.notifications) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasUnread) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("notifications_mark_all_read")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Read", color = ElectricBlue)
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == "ALL",
                    onClick = { filter = "ALL" },
                    label = { Text("All (${list.size})") }
                )
                FilterChip(
                    selected = filter == "APPROVALS",
                    onClick = { filter = "APPROVALS" },
                    label = { Text("Approvals") }
                )
                FilterChip(
                    selected = filter == "REMINDERS",
                    onClick = { filter = "REMINDERS" },
                    label = { Text("Match Reminders") }
                )
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        icon = Icons.Default.Notifications,
                        title = "All Caught Up",
                        subtitle = "Real-time registration approvals, upcoming NST match reminders, room unlocks, and rewards appear here."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { n ->
                        val typeUpper = n.type.uppercase()
                        val isApproval = typeUpper in setOf("REGISTRATION", "REGISTRATION_APPROVAL")
                        val isReminder = typeUpper in setOf("MATCH_REMINDER", "ROOM_CREDENTIALS")
                        val accentColor = when {
                            isReminder -> ElectricBlue
                            isApproval -> LiveEmerald
                            else -> ElectricOrange
                        }
                        val icon = when {
                            isReminder -> Icons.Default.Alarm
                            isApproval -> Icons.Default.Verified
                            else -> Icons.Default.EmojiEvents
                        }

                        Surface(
                            color = if (n.read) CharcoalSurface else CharcoalElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (n.read) BorderCarbon else accentColor.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!n.read) onMarkRead(n.id)
                                    onOpenAlertTarget(n)
                                }
                                .testTag("notification_card_${n.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = n.type,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = n.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!n.read) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(ElectricOrange)
                                            )
                                        }
                                    }
                                    Text(n.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = n.type.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor
                                        )
                                        val tsMs = n.createdAt?.toDate()?.time
                                        if (tsMs != null) {
                                            Text(
                                                text = com.example.i18n.NstTimeFormatter.formatDateTimeNst(tsMs),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportLegalScreen(
    myReportsState: UiState<List<ReportItem>>,
    onSubmitReport: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalClutchStrings.current
    var category by remember { mutableStateOf("SUPPORT") }
    var targetId by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    val reports = (myReportsState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = { Text(strings.supportLegal) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(title = "Frequently Asked Questions")
            }
            item {
                FaqItem("How do I get Room ID & Password?", "Register in a free tournament and wait for the Admin to release credentials before match start (NST). Open My Matches to reveal them.")
            }
            item {
                FaqItem("Why are paid tournaments disabled?", "Paid entry processing, deposits, and wallets remain disabled until a verified payment provider is integrated.")
            }
            item {
                FaqItem("What timezone does ClutchX use?", "All tournament deadlines and match times are displayed in Nepal Standard Time (Asia/Kathmandu, UTC+5:45).")
            }

            item { SectionHeader(title = "Contact Support / Fair-Play Report") }
            item {
                Surface(
                    color = CharcoalElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SUPPORT", "CHEATING", "DISPUTE").forEach { cat ->
                                FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                            }
                        }
                        OutlinedTextField(value = targetId, onValueChange = { targetId = it }, label = { Text("Tournament / Match ID (Optional)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Describe issue or fair-play evidence") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (subject.trim().length >= 3 && details.trim().length >= 5) {
                                    onSubmitReport(category, targetId, subject, details)
                                    targetId = ""
                                    subject = ""
                                    details = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Ticket")
                        }
                    }
                }
            }

            if (reports.isNotEmpty()) {
                item { SectionHeader(title = "My Submitted Tickets") }
                items(reports, key = { it.id }) { rep ->
                    Surface(
                        color = CharcoalSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderCarbon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rep.subject, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(rep.status, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                            }
                            Text("${rep.category} • ${rep.details}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Terms, Privacy & Fair-Play Policy") }
            item {
                Surface(
                    color = CharcoalSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderCarbon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fair-Play Policy", style = MaterialTheme.typography.titleMedium, color = ElectricOrange)
                        Text("Mobile devices only unless explicitly stated. Any third-party scripts, GFX hacks, teaming, or account sharing results in immediate disqualification.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Privacy Policy", style = MaterialTheme.typography.titleMedium, color = ElectricBlue)
                        Text("Your Google email and private profile details are isolated to your account and administrators. Only your chosen gaming handle and match stats appear on leaderboards.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Terms of Service", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("ClutchX operates skill-based Nepali esports tournaments. Initial release supports free-entry tournaments only; paid entry processing and wallets are disabled.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Surface(
        color = CharcoalElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderCarbon),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(question, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(answer, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
