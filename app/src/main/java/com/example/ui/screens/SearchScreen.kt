package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.ui.components.TivionsBottomNavBar
import com.example.ui.components.TivionsChip
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundAppBrush
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SearchScreen(
    allItems: List<PlaylistItem> = emptyList(),
    searchResults: List<PlaylistItem> = emptyList(),
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onPlayItem: (PlaylistItem) -> Unit,
    onOpenDetail: (PlaylistItem) -> Unit,
    onNavigate: (String) -> Unit
) {
    var selectedTypeFilter by remember { mutableStateOf<ItemType?>(null) }

    val pool = if (searchResults.isNotEmpty()) searchResults else allItems
    val filteredResults = remember(pool, searchQuery, selectedTypeFilter) {
        val query = searchQuery.trim().lowercase()
        pool.filter { item ->
            val matchesType = selectedTypeFilter == null || item.type == selectedTypeFilter
            val matchesQuery = if (query.isEmpty()) true else {
                item.name.lowercase().contains(query) ||
                        item.category.lowercase().contains(query) ||
                        item.cast.lowercase().contains(query) ||
                        item.description.lowercase().contains(query)
            }
            matchesType && matchesQuery
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppBrush)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input Field
            Box(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Kanal, film, dizi veya oyuncu ara...", color = TextTertiary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AccentCyan
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Temizle",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color(0xFF171B30),
                        unfocusedContainerColor = Color(0xFF171B30),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("search_text_field")
                )
            }

            // Quick Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TivionsChip(
                    text = "Tümü",
                    isSelected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null }
                )
                TivionsChip(
                    text = "Canlı TV",
                    isSelected = selectedTypeFilter == ItemType.LIVE_TV,
                    icon = Icons.Default.Tv,
                    onClick = { selectedTypeFilter = ItemType.LIVE_TV }
                )
                TivionsChip(
                    text = "Filmler",
                    isSelected = selectedTypeFilter == ItemType.VOD_MOVIE,
                    icon = Icons.Default.Movie,
                    onClick = { selectedTypeFilter = ItemType.VOD_MOVIE }
                )
                TivionsChip(
                    text = "Diziler",
                    isSelected = selectedTypeFilter == ItemType.VOD_SERIES,
                    icon = Icons.Default.LiveTv,
                    onClick = { selectedTypeFilter = ItemType.VOD_SERIES }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Results count
            Text(
                text = "${filteredResults.size} sonuç bulundu",
                color = TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Results List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredResults) { item ->
                    when (item.type) {
                        ItemType.LIVE_TV -> {
                            ChannelListItemCard(
                                channel = item,
                                onPlay = { onPlayItem(item) },
                                onToggleFav = {}
                            )
                        }
                        ItemType.VOD_MOVIE -> {
                            MovieBrowseCard(
                                movie = item,
                                onClick = { onOpenDetail(item) }
                            )
                        }
                        ItemType.VOD_SERIES -> {
                            SeriesBrowseCard(
                                series = item,
                                onClick = { onOpenDetail(item) }
                            )
                        }
                        ItemType.EPISODE -> {
                            MovieBrowseCard(
                                movie = item,
                                onClick = { onPlayItem(item) }
                            )
                        }
                    }
                }
            }

            TivionsBottomNavBar(
                selectedRoute = "search",
                onNavigate = onNavigate
            )
        }
    }
}
