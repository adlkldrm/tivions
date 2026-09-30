package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.HomeViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.WatchRecord
import com.example.ui.components.TivionsBottomNavBar
import com.example.ui.components.TivionsCard
import com.example.ui.components.TivionsChip
import com.example.ui.components.TivionsPrimaryButton
import com.example.ui.components.TivionsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BackgroundAppBrush
import com.example.ui.theme.BgCardDark
import com.example.ui.theme.BgCardPurple
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GradientPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.GenreHelper
import com.example.util.TitleNormalizer

/**
 * BÖLÜM 6, 7, 8, 9, 10: Tivions Home Screen (Anasayfa)
 * Sections in exact order:
 * 1. Popüler (Popular Movies & Series)
 * 2. İzlemeye Devam Et (Continue Watching)
 * 3. Son Eklenen Filmler (Recently Added Movies)
 * 4. Son Eklenen Diziler (Recently Added Series)
 * 5. Önerilen (Personalized Recommendations / Genre Filtering)
 */
@Composable
fun HomeScreen(
    popularMovies: List<PlaylistItem> = emptyList(),
    popularSeries: List<PlaylistItem> = emptyList(),
    continueWatching: List<WatchRecord> = emptyList(),
    recentMovies: List<PlaylistItem> = emptyList(),
    recentSeries: List<PlaylistItem> = emptyList(),
    recommendations: List<PlaylistItem> = emptyList(),
    liveChannels: List<PlaylistItem> = emptyList(),
    allMovies: List<PlaylistItem> = emptyList(),
    allSeries: List<PlaylistItem> = emptyList(),
    watchHistory: List<PlaylistItem> = emptyList(),
    homeViewModel: HomeViewModel = viewModel(),
    initialGenreId: Int? = null,
    onGenreSelected: ((Int?) -> Unit)? = null,
    onNavigate: (String) -> Unit,
    onOpenDetail: (PlaylistItem) -> Unit,
    onPlayItem: (PlaylistItem) -> Unit,
    onResumeWatchRecord: (WatchRecord) -> Unit = {},
    onDeleteWatchRecord: (String) -> Unit = {},
    onOpenSettings: () -> Unit
) {
    var recordToDelete by remember { mutableStateOf<WatchRecord?>(null) }
    var popularTab by rememberSaveable(key = "home_popular_tab") { mutableStateOf(0) } // 0: Filmler, 1: Diziler

    // Sync source data to HomeViewModel for genre filtering logic (optimized to prevent OOM)
    LaunchedEffect(recommendations, recentMovies, recentSeries, popularMovies, popularSeries, watchHistory) {
        val moviesPool = (recentMovies + popularMovies).take(40)
        val seriesPool = (recentSeries + popularSeries).take(40)
        homeViewModel.setSourceData(moviesPool, seriesPool, recommendations, watchHistory)
    }

    if (initialGenreId != null) {
        LaunchedEffect(initialGenreId) {
            if (homeViewModel.selectedGenreId.value == null) {
                homeViewModel.selectGenre(initialGenreId)
            }
        }
    }

    // State management via HomeViewModel
    val selectedGenreId by homeViewModel.selectedGenreId.collectAsState()
    val activeGenreCategory by homeViewModel.activeGenreCategory.collectAsState()
    val vmRecommendations by homeViewModel.filteredRecommendations.collectAsState()

    // Content pool for recommendations & genre filtering (strictly limited to displayed items to prevent OOM)
    val contentPool = remember(recommendations, recentMovies, recentSeries, popularMovies, popularSeries) {
        val list = mutableListOf<PlaylistItem>()
        list.addAll(recommendations)
        list.addAll(recentMovies)
        list.addAll(recentSeries)
        list.addAll(popularMovies)
        list.addAll(popularSeries)
        list.distinctBy { it.id }
    }

    // 1. Recommendations and genre-filtered items from HomeViewModel (with fallback pool)
    val activeRecommendations = if (vmRecommendations.isNotEmpty()) {
        vmRecommendations
    } else {
        homeViewModel.filterItemsByGenre(contentPool, watchHistory, selectedGenreId)
    }

    val activeGenre = activeGenreCategory ?: remember(selectedGenreId) {
        GenreHelper.getGenreById(selectedGenreId)?.let {
            com.example.ui.viewmodel.GenreFilterCategory(
                genreId = it.genreId,
                name = it.name,
                relatedGenreIds = it.relatedGenreIds,
                categoryKeywords = it.keywords
            )
        }
    }

    val hasAnyContent = popularMovies.isNotEmpty() || popularSeries.isNotEmpty() ||
            recentMovies.isNotEmpty() || recentSeries.isNotEmpty() || liveChannels.isNotEmpty() ||
            contentPool.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppBrush)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TivionsTopBar(
                title = "Tivions",
                showBack = false,
                showCast = true,
                showSettings = true,
                onSettingsClick = onOpenSettings,
                onCastClick = {}
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (!hasAnyContent && continueWatching.isEmpty()) {
                    item {
                        EmptyHomeCard(onConnectClick = { onNavigate("connect_source") })
                    }
                } else {
                    // ==========================================
                    // 1. POPÜLER (BÖLÜM 6)
                    // ==========================================
                    val hasPopularMovies = popularMovies.isNotEmpty()
                    val hasPopularSeries = popularSeries.isNotEmpty()
                    if (hasPopularMovies || hasPopularSeries) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Popüler",
                                        color = TextPrimary,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Toggle: Filmler / Diziler (Her zaman gösterilir)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(BgCardDark)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(if (popularTab == 0) AccentPurple else Color.Transparent)
                                                .clickable { popularTab = 0 }
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "Filmler",
                                                color = if (popularTab == 0) Color.White else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(if (popularTab == 1) AccentPurple else Color.Transparent)
                                                .clickable { popularTab = 1 }
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "Diziler",
                                                color = if (popularTab == 1) Color.White else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val activePopularList = if (popularTab == 0) popularMovies else popularSeries
                                if (activePopularList.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        itemsIndexed(activePopularList.take(10)) { index, item ->
                                            PopularPosterCard(
                                                rank = index + 1,
                                                item = item,
                                                onClick = { onOpenDetail(item) }
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BgCardDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (popularTab == 0) "2026 yılına ait popüler film bulunamadı" else "Popüler dizi bulunamadı",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 2. İZLEMEYE DEVAM ET (BÖLÜM 7)
                    // ==========================================
                    if (continueWatching.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Text(
                                    text = "İzlemeye Devam Et",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(continueWatching) { record ->
                                        ContinueWatchingCard(
                                            record = record,
                                            onClick = { onResumeWatchRecord(record) },
                                            onLongClick = { recordToDelete = record }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 3. SON EKLENEN FİLMLER (BÖLÜM 8)
                    // ==========================================
                    if (recentMovies.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Son Eklenen Filmler",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tümü",
                                        color = AccentCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable { onNavigate("movies") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                 LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentMovies.take(20)) { movie ->
                                        RecentPosterCard(
                                            item = movie,
                                            onClick = { onOpenDetail(movie) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 4. SON EKLENEN DİZİLER (BÖLÜM 9)
                    // ==========================================
                    if (recentSeries.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Son Eklenen Diziler",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tümü",
                                        color = AccentCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable { onNavigate("series") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentSeries.take(20)) { ser ->
                                        RecentPosterCard(
                                            item = ser,
                                            onClick = { onOpenDetail(ser) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 5. ÖNERİLEN (BÖLÜM 10)
                    // ==========================================
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Senin İçin Önerilenler",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    val subTitle = if (activeGenre != null) {
                                        "Seçilen Tür: ${activeGenre.name} (${activeRecommendations.size} içerik) • Kaldırmak için dokunun"
                                    } else if (watchHistory.isNotEmpty()) {
                                        "İzleme geçmişinize göre derlenen özel öneriler:"
                                    } else {
                                        "Hangi türleri seversin? Dokunarak filtrele:"
                                    }
                                    Text(
                                        text = subTitle,
                                        color = if (activeGenre != null) AccentCyan else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (activeGenre != null) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }

                                if (selectedGenreId != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x26FF0055))
                                            .border(1.dp, Color(0x66FF0055), RoundedCornerShape(12.dp))
                                            .clickable {
                                                homeViewModel.resetGenreFilter()
                                                onGenreSelected?.invoke(null)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("reset_genre_filter_btn")
                                    ) {
                                        Text(
                                            text = "✕ Filtreyi Sıfırla",
                                            color = Color(0xFFFF4081),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Genre Buttons (ALWAYS VISIBLE!)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("genre_filter_row")
                            ) {
                                // "Tümü" option for explicit all/reset
                                item(key = "all_genres") {
                                    val isAllSelected = selectedGenreId == null
                                    val allBgModifier = if (isAllSelected) {
                                        Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
                                    } else {
                                        Modifier.background(BgCardDark)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .then(allBgModifier)
                                            .border(
                                                width = if (isAllSelected) 2.dp else 1.dp,
                                                color = if (isAllSelected) AccentCyan else BorderSubtle,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                homeViewModel.resetGenreFilter()
                                                onGenreSelected?.invoke(null)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("genre_btn_all")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isAllSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = "Tümü",
                                                color = if (isAllSelected) Color.White else TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                items(homeViewModel.genreCategories, key = { it.genreId }) { genre ->
                                    val isSelected = selectedGenreId == genre.genreId
                                    val bgModifier = if (isSelected) {
                                        Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
                                    } else {
                                        Modifier.background(BgCardDark)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .then(bgModifier)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) AccentCyan else BorderSubtle,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                homeViewModel.selectGenre(genre.genreId)
                                                onGenreSelected?.invoke(homeViewModel.selectedGenreId.value)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("genre_btn_${genre.genreId}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = genre.name,
                                                color = if (isSelected) Color.White else TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (activeRecommendations.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.testTag("recommendations_row")
                                ) {
                                    items(activeRecommendations.take(10), key = { it.id }) { rec ->
                                        RecentPosterCard(
                                            item = rec,
                                            onClick = { onOpenDetail(rec) }
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (activeGenre != null) {
                                            "${activeGenre.name} türünde henüz içerik bulunamadı."
                                        } else {
                                            "Bu türde henüz içerik bulunamadı."
                                        },
                                        color = TextTertiary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            // Bottom Navigation Dock
            TivionsBottomNavBar(
                selectedRoute = "home",
                onNavigate = onNavigate
            )
        }
    }

    // Long press confirmation to remove from Continue Watching
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = {
                Text(
                    text = "İzleme Geçmişinden Kaldır",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "\"${record.title}\" izleme geçmişinizden kaldırılsın mı?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWatchRecord(record.id)
                        recordToDelete = null
                    }
                ) {
                    Text("Kaldır", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E2138)
        )
    }
}

/**
 * BÖLÜM 6: Popular Poster Card (2:3 aspect ratio, rank badge, rating badge)
 */
@Composable
fun PopularPosterCard(
    rank: Int,
    item: PlaylistItem,
    onClick: () -> Unit
) {
    val isSeries = item.type == ItemType.VOD_SERIES || item.type == ItemType.EPISODE

    val cachedMeta = com.example.data.parser.XtreamClient.getCachedCardMeta(item.id)

    val displayPoster = cachedMeta?.posterUrl?.takeIf { it.isNotBlank() } ?: item.logoUrl

    val displayRating = if ((cachedMeta?.rating ?: 0.0) > 0.0) {
        cachedMeta!!.rating
    } else if (item.rating > 0.0) {
        item.rating
    } else {
        0.0
    }

    val meta = remember(item.name) {
        if (isSeries) TitleNormalizer.extractSeriesMeta(item.name) else TitleNormalizer.extractMovieMeta(item.name)
    }

    val cleanTitle = cachedMeta?.title?.takeIf { it.isNotBlank() } ?: meta.cleanTitle.ifBlank { item.name }

    val genreText = cachedMeta?.genre?.takeIf { it.isNotBlank() } ?: item.category.ifBlank { if (isSeries) "Dizi" else "Film" }

    val yearText = cachedMeta?.year?.takeIf { it.isNotBlank() } ?: meta.year ?: item.year.ifBlank { "2024" }

    val qualityText = cachedMeta?.quality ?: meta.quality ?: "HD"

    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
            .testTag("popular_card_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF3B2D54), Color(0xFF1A1B2E))
                    )
                )
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            // Poster image (stream_icon / cover or fallback)
            if (!displayPoster.isNullOrBlank()) {
                AsyncImage(
                    model = displayPoster,
                    contentDescription = cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cleanTitle.take(2).uppercase(),
                        color = Color(0xFF818CF8),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top-left Rank badge (1, 2, 3...)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(GradientPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Top-right Rating badge
            if (displayRating > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", displayRating),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = cleanTitle,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle: Quality • Genre • Year
        val subLine = buildString {
            append(qualityText)
            if (genreText.isNotBlank() && genreText != "Tümü") {
                append(" • ").append(genreText.split("/").first().trim())
            }
            if (yearText.isNotBlank()) {
                append(" • ").append(yearText)
            }
        }
        Text(
            text = subLine,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * BÖLÜM 7: Continue Watching Card (1:1 square aspect ratio, progress bar at bottom)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingCard(
    record: WatchRecord,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val effectivePoster = record.stillUrl

    Column(
        modifier = Modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("continue_card_${record.id}")
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2E234A), Color(0xFF16182B))
                    )
                )
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            // Still image
            if (!effectivePoster.isNullOrBlank()) {
                AsyncImage(
                    model = effectivePoster,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = record.title.take(2).uppercase(),
                        color = Color(0xFFA78BFA),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Play Icon overlay in center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Oynat",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Bottom Progress Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0x66000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(record.progress.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .background(GradientPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = record.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle: S02B05 or Quality • Genre • Year
        val subLine = if (record.type == "tv" || record.seriesKey != null) {
            "S%02dB%02d • %s".format(record.seasonNumber, record.episodeNumber, record.quality ?: "HD")
        } else {
            "%s • %s".format(record.quality ?: "HD", record.year ?: "")
        }.trim(' ', '•')

        Text(
            text = subLine,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * BÖLÜM 8 & 9: Recent Poster Card (3 items fit per screen page, 2:3 ratio)
 */
@Composable
fun RecentPosterCard(
    item: PlaylistItem,
    onClick: () -> Unit
) {
    val isSeries = item.type == ItemType.VOD_SERIES || item.type == ItemType.EPISODE

    val cachedMeta = com.example.data.parser.XtreamClient.getCachedCardMeta(item.id)

    val displayPoster = cachedMeta?.posterUrl?.takeIf { it.isNotBlank() } ?: item.logoUrl

    val displayRating = if ((cachedMeta?.rating ?: 0.0) > 0.0) {
        cachedMeta!!.rating
    } else if (item.rating > 0.0) {
        item.rating
    } else {
        0.0
    }

    val meta = remember(item.name) {
        if (isSeries) TitleNormalizer.extractSeriesMeta(item.name) else TitleNormalizer.extractMovieMeta(item.name)
    }

    val cleanTitle = cachedMeta?.title?.takeIf { it.isNotBlank() } ?: meta.cleanTitle.ifBlank { item.name }

    val genreText = cachedMeta?.genre?.takeIf { it.isNotBlank() } ?: item.category.ifBlank { if (isSeries) "Dizi" else "Film" }

    val yearText = cachedMeta?.year?.takeIf { it.isNotBlank() } ?: meta.year ?: item.year.ifBlank { "2024" }

    val qualityText = cachedMeta?.quality ?: meta.quality ?: "HD"

    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() }
            .testTag("recent_card_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF24263E), Color(0xFF141524))
                    )
                )
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
        ) {
            if (!displayPoster.isNullOrBlank()) {
                AsyncImage(
                    model = displayPoster,
                    contentDescription = cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cleanTitle.take(2).uppercase(),
                        color = Color(0xFF60A5FA),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top-right rating badge if available
            if (displayRating > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", displayRating),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quality tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = qualityText,
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = cleanTitle,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val subInfo = buildString {
            if (genreText.isNotBlank() && genreText != "Tümü") {
                append(genreText.split("/").first().trim())
            }
            if (yearText.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(yearText)
            }
        }.ifBlank { qualityText }

        Text(
            text = subInfo,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyHomeCard(onConnectClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        TivionsCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConnectClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Yayın Listesi Bulunamadı",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "İçerikleri görüntülemek için M3U veya Xtream hesabınızı bağlayın.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                TivionsPrimaryButton(
                    text = "Kaynak Bağla",
                    onClick = onConnectClick
                )
            }
        }
    }
}
