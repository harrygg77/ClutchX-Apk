package com.example.ui

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ClutchXApplication
import com.example.R
import com.example.auth.signOut
import com.example.data.firebase.ClutchXRepository
import com.example.data.model.AppLanguage
import com.example.data.model.AppNotification
import com.example.data.model.UiState
import com.example.i18n.LocalClutchStrings
import com.example.i18n.stringsFor
import com.example.notifications.ClutchXNotificationManager
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MyMatchesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SupportLegalScreen
import com.example.ui.screens.TeamsScreen
import com.example.ui.screens.TournamentDetailScreen
import com.example.ui.screens.TournamentsScreen
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.ElectricOrangeContainer
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ClutchXViewModel
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_TOURNAMENTS = "tournaments"
const val ROUTE_MY_MATCHES = "my_matches"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_PROFILE = "profile"
const val ROUTE_TOURNAMENT_DETAIL = "tournament_detail/{tournamentId}"
const val ROUTE_ADMIN = "admin_dashboard"
const val ROUTE_TEAMS = "teams"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_SUPPORT = "support_legal"

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClutchXApp(
    currentUserId: String,
    currentUserEmail: String,
    pendingDeepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val notificationMonitor = remember(context) {
        (context.applicationContext as? ClutchXApplication)?.notificationMonitor
            ?: com.example.notifications.NotificationBackgroundMonitor(context.applicationContext).apply { start() }
    }

    val viewModel: ClutchXViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                ClutchXViewModel(
                    repository = ClutchXRepository(db),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail
                )
            }
        }
    )

    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val strings = stringsFor(language)
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val publicTournamentsState by viewModel.publicTournaments.collectAsStateWithLifecycle()
    val filteredTournamentsState by viewModel.filteredTournaments.collectAsStateWithLifecycle()
    val filterState by viewModel.filters.collectAsStateWithLifecycle()
    val registrationsState by viewModel.myRegistrations.collectAsStateWithLifecycle()
    val myTeamsState by viewModel.myTeams.collectAsStateWithLifecycle()
    val publicTeamsState by viewModel.publicTeams.collectAsStateWithLifecycle()
    val invitationsState by viewModel.myInvitations.collectAsStateWithLifecycle()
    val matchesState by viewModel.publicMatches.collectAsStateWithLifecycle()
    val matchResultsState by viewModel.matchResults.collectAsStateWithLifecycle()
    val leaderboardPlayersState by viewModel.leaderboardPlayers.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val rewardsState by viewModel.myRewards.collectAsStateWithLifecycle()
    val announcementsState by viewModel.announcements.collectAsStateWithLifecycle()
    val reportsState by viewModel.myReports.collectAsStateWithLifecycle()
    val revealedCredentials by viewModel.revealedCredentials.collectAsStateWithLifecycle()

    val adminTournamentsState by viewModel.adminTournaments.collectAsStateWithLifecycle()
    val adminMatchesState by viewModel.adminMatches.collectAsStateWithLifecycle()
    val adminRegistrationsState by viewModel.adminRegistrations.collectAsStateWithLifecycle()
    val adminRewardsState by viewModel.adminRewards.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReports.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val activeBannerAlert by notificationMonitor.activeBannerAlert.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember {
        mutableStateOf(ClutchXNotificationManager.hasNotificationPermission(context))
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Enable notifications in Settings for lock-screen match alerts. In-app live alerts remain active.")
            }
        }
    }

    LaunchedEffect(currentUserId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            !permissionRequestedOnce
        ) {
            permissionRequestedOnce = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabs = listOf(
        BottomTab(ROUTE_HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
        BottomTab(ROUTE_TOURNAMENTS, strings.navTournaments, Icons.Filled.Explore, Icons.Outlined.Explore),
        BottomTab(ROUTE_MY_MATCHES, strings.navMyMatches, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
        BottomTab(ROUTE_LEADERBOARD, strings.navLeaderboard, Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard),
        BottomTab(ROUTE_PROFILE, strings.navProfile, Icons.Filled.Person, Icons.Outlined.Person)
    )
    val showMainChrome = currentRoute in tabs.map { it.route }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openNotificationTarget(alert: AppNotification) {
        if (!alert.read && alert.id.isNotBlank()) {
            viewModel.markNotificationRead(alert.id)
        }
        val target = when (alert.type.uppercase()) {
            "MATCH_REMINDER", "ROOM_CREDENTIALS" -> ROUTE_MY_MATCHES
            else -> ROUTE_NOTIFICATIONS
        }
        if (target == ROUTE_MY_MATCHES) {
            navigateTab(ROUTE_MY_MATCHES)
        } else if (currentRoute != ROUTE_NOTIFICATIONS) {
            navController.navigate(ROUTE_NOTIFICATIONS)
        }
    }

    LaunchedEffect(pendingDeepLinkRoute) {
        val route = pendingDeepLinkRoute ?: return@LaunchedEffect
        if (route == ROUTE_MY_MATCHES) {
            navigateTab(ROUTE_MY_MATCHES)
        } else if (route == ROUTE_NOTIFICATIONS) {
            navController.navigate(ROUTE_NOTIFICATIONS)
        }
        onDeepLinkConsumed()
    }

    CompositionLocalProvider(LocalClutchStrings provides strings) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
            val useRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && showMainChrome) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.size(12.dp))
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricOrange,
                                    selectedTextColor = ElectricOrange,
                                    indicatorColor = ElectricOrangeContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = ObsidianBg,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (showMainChrome) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = ElectricOrange,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CLUTCHX",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }
                                },
                                actions = {
                                    Surface(
                                        color = CharcoalElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BorderCarbon),
                                        modifier = Modifier
                                            .clickable { viewModel.toggleLanguage() }
                                            .testTag("topbar_language_toggle")
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.EN) "EN" else "ने",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (isAdmin) {
                                        IconButton(
                                            onClick = { navController.navigate(ROUTE_ADMIN) },
                                            modifier = Modifier.testTag("topbar_admin_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = strings.adminDashboard,
                                                tint = ElectricOrange
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                        modifier = Modifier.testTag("topbar_notifications_button")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadCount > 0) {
                                                    Badge(containerColor = ElectricOrange) {
                                                        Text("$unreadCount", color = TextPrimary)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = strings.notifications,
                                                tint = TextPrimary
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = CharcoalSurface,
                                    titleContentColor = TextPrimary
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (!useRail && showMainChrome) {
                            NavigationBar(
                                containerColor = CharcoalSurface,
                                tonalElevation = 0.dp
                            ) {
                                tabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricOrange,
                                            selectedTextColor = ElectricOrange,
                                            indicatorColor = ElectricOrangeContainer,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        ),
                                        modifier = Modifier.testTag("nav_${tab.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                tournamentsState = publicTournamentsState,
                                registrationsState = registrationsState,
                                announcementsState = announcementsState,
                                leaderboardState = leaderboardPlayersState,
                                unreadNotifications = unreadCount,
                                onOpenTournament = { id -> navController.navigate("tournament_detail/$id") },
                                onNavigateToDiscover = { navigateTab(ROUTE_TOURNAMENTS) },
                                onNavigateToMyMatches = { navigateTab(ROUTE_MY_MATCHES) },
                                onNavigateToTeams = { navController.navigate(ROUTE_TEAMS) },
                                onNavigateToNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                onNavigateToLeaderboard = { navigateTab(ROUTE_LEADERBOARD) }
                            )
                        }

                        composable(ROUTE_TOURNAMENTS) {
                            TournamentsScreen(
                                tournamentsState = filteredTournamentsState,
                                registrationsState = registrationsState,
                                filterState = filterState,
                                onSearchChange = viewModel::updateSearch,
                                onGameFilterChange = viewModel::updateGameFilter,
                                onModeFilterChange = viewModel::updateModeFilter,
                                onFeeFilterChange = viewModel::updateFeeFilter,
                                onStatusFilterChange = viewModel::updateStatusFilter,
                                onSortChange = viewModel::updateSort,
                                onResetFilters = viewModel::resetFilters,
                                onOpenTournament = { id -> navController.navigate("tournament_detail/$id") }
                            )
                        }

                        composable(ROUTE_MY_MATCHES) {
                            MyMatchesScreen(
                                currentUserId = currentUserId,
                                registrationsState = registrationsState,
                                matchesState = matchesState,
                                resultsState = matchResultsState,
                                rewardsState = rewardsState,
                                revealedCredentials = revealedCredentials,
                                onRevealCredentials = viewModel::revealRoomCredentials,
                                onOpenTournament = { id -> navController.navigate("tournament_detail/$id") },
                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }

                        composable(ROUTE_LEADERBOARD) {
                            LeaderboardScreen(
                                playersState = leaderboardPlayersState,
                                teamsState = publicTeamsState
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                userProfileState = userProfileState,
                                currentLanguage = language,
                                isAdmin = isAdmin,
                                unreadNotifications = unreadCount,
                                onSaveProfile = viewModel::saveProfile,
                                onOpenTeams = { navController.navigate(ROUTE_TEAMS) },
                                onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                onOpenSupportLegal = { navController.navigate(ROUTE_SUPPORT) },
                                onOpenAdminDashboard = { navController.navigate(ROUTE_ADMIN) },
                                onSignOut = {
                                    signOut(credentialManager, scope)
                                }
                            )
                        }

                        composable(
                            route = ROUTE_TOURNAMENT_DETAIL,
                            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                        ) { entry ->
                            val tId = entry.arguments?.getString("tournamentId").orEmpty()
                            val tournament = (publicTournamentsState as? UiState.Success)?.data?.firstOrNull { it.id == tId }
                                ?: (adminTournamentsState as? UiState.Success)?.data?.firstOrNull { it.id == tId }
                            TournamentDetailScreen(
                                tournament = tournament,
                                userProfile = (userProfileState as? UiState.Success)?.data,
                                myRegistrations = (registrationsState as? UiState.Success)?.data ?: emptyList(),
                                myTeams = (myTeamsState as? UiState.Success)?.data ?: emptyList(),
                                matches = (matchesState as? UiState.Success)?.data ?: emptyList(),
                                results = (matchResultsState as? UiState.Success)?.data ?: emptyList(),
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onRegister = viewModel::registerForTournament,
                                onGoToProfile = { navigateTab(ROUTE_PROFILE) },
                                onGoToTeams = { navController.navigate(ROUTE_TEAMS) }
                            )
                        }

                        composable(ROUTE_ADMIN) {
                            AdminDashboardScreen(
                                isAdmin = isAdmin,
                                tournamentsState = adminTournamentsState,
                                matchesState = adminMatchesState,
                                registrationsState = adminRegistrationsState,
                                rewardsState = adminRewardsState,
                                reportsState = adminReportsState,
                                onBack = { navController.popBackStack() },
                                onCreateTournament = viewModel::createTournamentAdmin,
                                onUpdateTournament = viewModel::updateTournamentAdmin,
                                onUpdateTournamentStatus = viewModel::updateTournamentStatusAdmin,
                                onDeleteTournament = viewModel::deleteTournamentAdmin,
                                onSeedDemo = viewModel::seedDemoTournamentsAdmin,
                                onCreateMatch = viewModel::createMatchAdmin,
                                onReleaseCredentials = viewModel::releaseCredentialsAdmin,
                                onSendMatchReminder = viewModel::sendMatchReminderAdmin,
                                onPublishMatchResult = viewModel::publishMatchResultAdmin,
                                onUpdateRegistrationStatus = viewModel::updateRegistrationStatusAdmin,
                                onIssueReward = viewModel::issueRewardAdmin,
                                onUpdateRewardStatus = viewModel::updateRewardStatusAdmin,
                                onPublishAnnouncement = viewModel::publishAnnouncementAdmin,
                                onUpdateReportStatus = viewModel::updateReportStatusAdmin
                            )
                        }

                        composable(ROUTE_TEAMS) {
                            TeamsScreen(
                                currentUserId = currentUserId,
                                myTeamsState = myTeamsState,
                                invitationsState = invitationsState,
                                onBack = { navController.popBackStack() },
                                onCreateTeam = viewModel::createTeam,
                                onInviteTeammate = viewModel::inviteTeammate,
                                onRespondInvitation = viewModel::respondToInvitation,
                                onRemoveMember = viewModel::removeTeamMember
                            )
                        }

                        composable(ROUTE_NOTIFICATIONS) {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                onMarkRead = viewModel::markNotificationRead,
                                onMarkAllRead = viewModel::markAllNotificationsRead,
                                onOpenAlertTarget = { n ->
                                    val tUpper = n.type.uppercase()
                                    if (tUpper in setOf("MATCH_REMINDER", "ROOM_CREDENTIALS")) {
                                        navigateTab(ROUTE_MY_MATCHES)
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_SUPPORT) {
                            SupportLegalScreen(
                                myReportsState = reportsState,
                                onSubmitReport = viewModel::submitSupportOrReport,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }

            // Real-time floating alert banner for Registration Approvals & Match Reminders
            AnimatedVisibility(
                visible = activeBannerAlert != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val alert = activeBannerAlert
                if (alert != null) {
                    val typeUpper = alert.type.uppercase()
                    val isReminder = typeUpper in setOf("MATCH_REMINDER", "ROOM_CREDENTIALS")
                    val isApproval = typeUpper in setOf("REGISTRATION", "REGISTRATION_APPROVAL")
                    val accent = when {
                        isReminder -> ElectricBlue
                        isApproval -> LiveEmerald
                        else -> ElectricOrange
                    }
                    val icon = when {
                        isReminder -> Icons.Default.Alarm
                        isApproval -> Icons.Default.Verified
                        else -> Icons.Default.NotificationsActive
                    }
                    Surface(
                        color = CharcoalElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, accent),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                notificationMonitor.dismissBanner()
                                openNotificationTarget(alert)
                            }
                            .testTag("realtime_alert_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = alert.type,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = alert.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                            }
                            TextButton(
                                onClick = {
                                    notificationMonitor.dismissBanner()
                                    openNotificationTarget(alert)
                                },
                                modifier = Modifier.testTag("realtime_alert_view_btn")
                            ) {
                                Text("View", color = accent)
                            }
                            IconButton(
                                onClick = { notificationMonitor.dismissBanner() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("realtime_alert_dismiss_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss alert",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
