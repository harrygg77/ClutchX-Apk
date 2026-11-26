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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.i18n.LocalClutchStrings
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueContainer
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.ElectricOrangeContainer
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VictoryGold

@Composable
fun ProfileScreen(
    userProfileState: UiState<UserProfile?>,
    currentLanguage: AppLanguage,
    isAdmin: Boolean,
    unreadNotifications: Int,
    onSaveProfile: (String, String, String, String, AppLanguage) -> Unit,
    onOpenTeams: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupportLegal: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalClutchStrings.current
    val profile = (userProfileState as? UiState.Success)?.data

    var username by remember(profile?.username) { mutableStateOf(profile?.username ?: "") }
    var avatarKey by remember(profile?.avatarKey) { mutableStateOf(profile?.avatarKey ?: "phoenix") }
    var freeFireUid by remember(profile?.freeFireUid) { mutableStateOf(profile?.freeFireUid ?: "") }
    var pubgUid by remember(profile?.pubgUid) { mutableStateOf(profile?.pubgUid ?: "") }
    var selectedLang by remember(currentLanguage) { mutableStateOf(currentLanguage) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Player Header
        item {
            Surface(
                color = CharcoalElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ElectricOrangeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricOrange, modifier = Modifier.size(30.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.username?.ifBlank { "ClutchX Player" } ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = profile?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = ElectricOrangeContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ADMIN VERIFIED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Admin Dashboard Access (ONLY visible for verified Admins)
        if (isAdmin) {
            item {
                Surface(
                    color = ElectricOrangeContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ElectricOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_admin_dashboard_button")
                        .clickable(onClick = onOpenAdminDashboard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = ElectricOrange, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.adminDashboard, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Manage tournaments, rooms, results & rewards", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ElectricOrange)
                    }
                }
            }
        }

        // Quick Links: Teams, Notifications, Support & Legal
        item {
            Surface(
                color = CharcoalSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileNavRow(
                        icon = Icons.Default.Groups,
                        title = strings.teams,
                        accent = ElectricBlue,
                        badge = null,
                        onClick = onOpenTeams,
                        tag = "profile_teams_link"
                    )
                    ProfileNavRow(
                        icon = Icons.Default.Notifications,
                        title = strings.notifications,
                        accent = VictoryGold,
                        badge = if (unreadNotifications > 0) "$unreadNotifications" else null,
                        onClick = onOpenNotifications,
                        tag = "profile_notifications_link"
                    )
                    ProfileNavRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = strings.supportLegal,
                        accent = LiveEmerald,
                        badge = null,
                        onClick = onOpenSupportLegal,
                        tag = "profile_support_link"
                    )
                }
            }
        }

        // Edit Gaming Profile & UIDs
        item {
            Surface(
                color = CharcoalElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderCarbon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Player Details & Game UIDs", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Display Handle / IGN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_username_input")
                    )
                    OutlinedTextField(
                        value = freeFireUid,
                        onValueChange = { freeFireUid = it },
                        label = { Text("Free Fire UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_ff_uid_input")
                    )
                    OutlinedTextField(
                        value = pubgUid,
                        onValueChange = { pubgUid = it },
                        label = { Text("PUBG Mobile UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_pubg_uid_input")
                    )

                    Text("Avatar Crest", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("phoenix", "gurkha", "yeti", "sniper").forEach { key ->
                            FilterChip(
                                selected = avatarKey == key,
                                onClick = { avatarKey = key },
                                label = { Text(key.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Text(strings.languageLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedLang == AppLanguage.EN,
                            onClick = { selectedLang = AppLanguage.EN },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = selectedLang == AppLanguage.NE,
                            onClick = { selectedLang = AppLanguage.NE },
                            label = { Text("नेपाली") }
                        )
                    }

                    Button(
                        onClick = {
                            onSaveProfile(username, avatarKey, freeFireUid, pubgUid, selectedLang)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.saveProfile)
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CrimsonError)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.signOut, color = CrimsonError)
            }
        }
    }
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    title: String,
    accent: Color,
    badge: String?,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ElectricBlueContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge != null) {
                Surface(color = accent, shape = CircleShape) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBg,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
