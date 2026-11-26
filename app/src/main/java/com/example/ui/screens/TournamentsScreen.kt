package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Registration
import com.example.data.model.Tournament
import com.example.data.model.UiState
import com.example.i18n.LocalClutchStrings
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TournamentCard
import com.example.ui.theme.BorderCarbon
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueContainer
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.ElectricOrangeContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DiscoveryFilterState
import com.example.ui.viewmodel.FeeFilter
import com.example.ui.viewmodel.GameFilter
import com.example.ui.viewmodel.ModeFilter
import com.example.ui.viewmodel.StatusFilter
import com.example.ui.viewmodel.TournamentSort

@Composable
fun TournamentsScreen(
    tournamentsState: UiState<List<Tournament>>,
    registrationsState: UiState<List<Registration>>,
    filterState: DiscoveryFilterState,
    onSearchChange: (String) -> Unit,
    onGameFilterChange: (GameFilter) -> Unit,
    onModeFilterChange: (ModeFilter) -> Unit,
    onFeeFilterChange: (FeeFilter) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onSortChange: (TournamentSort) -> Unit,
    onResetFilters: () -> Unit,
    onOpenTournament: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalClutchStrings.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val registeredIds = (registrationsState as? UiState.Success)?.data
        ?.filter { it.status != "CANCELLED" && it.status != "REJECTED" }
        ?.map { it.tournamentId }
        ?.toSet() ?: emptySet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Sticky Discovery Filter Header
        Surface(
            color = CharcoalSurface,
            border = BorderStroke(1.dp, BorderCarbon)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.navTournaments,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Box {
                        Surface(
                            color = CharcoalElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderCarbon),
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("tournament_sort_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (filterState.sortBy) {
                                        TournamentSort.START_TIME -> strings.sortByStart
                                        TournamentSort.PRIZE_POOL -> strings.sortByPrize
                                        TournamentSort.ENTRY_FEE -> strings.sortByFee
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.sortByStart) },
                                onClick = {
                                    onSortChange(TournamentSort.START_TIME)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.sortByPrize) },
                                onClick = {
                                    onSortChange(TournamentSort.PRIZE_POOL)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.sortByFee) },
                                onClick = {
                                    onSortChange(TournamentSort.ENTRY_FEE)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(strings.searchPlaceholder, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricOrange,
                        unfocusedBorderColor = BorderCarbon,
                        focusedContainerColor = CharcoalElevated,
                        unfocusedContainerColor = CharcoalElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tournament_search_input")
                )

                // Row 1: Game & Entry Fee filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameFilter.entries.forEach { g ->
                        val label = when (g) {
                            GameFilter.ALL -> strings.all
                            GameFilter.FREE_FIRE -> "Free Fire"
                            GameFilter.PUBG_MOBILE -> "PUBG Mobile"
                        }
                        DiscoveryChip(
                            label = label,
                            selected = filterState.gameFilter == g,
                            accent = ElectricOrange,
                            container = ElectricOrangeContainer,
                            onClick = { onGameFilterChange(g) },
                            tag = "filter_game_${g.name}"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    FeeFilter.entries.forEach { f ->
                        val label = when (f) {
                            FeeFilter.ALL -> "Any Fee"
                            FeeFilter.FREE -> strings.free
                            FeeFilter.PAID -> strings.paid
                        }
                        DiscoveryChip(
                            label = label,
                            selected = filterState.feeFilter == f,
                            accent = ElectricBlue,
                            container = ElectricBlueContainer,
                            onClick = { onFeeFilterChange(f) },
                            tag = "filter_fee_${f.name}"
                        )
                    }
                }

                // Row 2: Mode & Status filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeFilter.entries.forEach { m ->
                        val label = when (m) {
                            ModeFilter.ALL -> "All Modes"
                            ModeFilter.SOLO -> "Solo"
                            ModeFilter.DUO -> "Duo"
                            ModeFilter.SQUAD -> "Squad"
                        }
                        DiscoveryChip(
                            label = label,
                            selected = filterState.modeFilter == m,
                            accent = ElectricBlue,
                            container = ElectricBlueContainer,
                            onClick = { onModeFilterChange(m) },
                            tag = "filter_mode_${m.name}"
                        )
                    }
                    StatusFilter.entries.forEach { s ->
                        val label = when (s) {
                            StatusFilter.ALL -> "Active"
                            StatusFilter.UPCOMING -> strings.upcoming
                            StatusFilter.LIVE -> strings.live
                            StatusFilter.COMPLETED -> strings.completed
                            StatusFilter.FULL -> strings.full
                        }
                        DiscoveryChip(
                            label = label,
                            selected = filterState.statusFilter == s,
                            accent = ElectricOrange,
                            container = ElectricOrangeContainer,
                            onClick = { onStatusFilterChange(s) },
                            tag = "filter_status_${s.name}"
                        )
                    }
                }
            }
        }

        // Responsive Grid of Tournament Cards
        when (tournamentsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricOrange)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.FilterList,
                        title = "Error loading tournaments",
                        subtitle = tournamentsState.message
                    )
                }
            }
            is UiState.Success -> {
                val list = tournamentsState.data
                if (list.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyStateCard(
                            icon = Icons.Default.EmojiEvents,
                            title = strings.noDataFound,
                            subtitle = "Try clearing your search or game/fee filters."
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = onResetFilters) {
                            Text("Reset Filters", color = ElectricBlue)
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= 680.dp) 2 else 1
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(list, key = { it.id }) { tournament ->
                                TournamentCard(
                                    tournament = tournament,
                                    isRegistered = tournament.id in registeredIds,
                                    onClick = { onOpenTournament(tournament.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryChip(
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    tag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = container,
            selectedLabelColor = accent,
            containerColor = CharcoalElevated,
            labelColor = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = accent.copy(alpha = 0.7f),
            borderColor = BorderCarbon
        ),
        modifier = Modifier.testTag(tag)
    )
}
