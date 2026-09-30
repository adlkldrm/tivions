package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import com.example.data.model.CategoryPref
import com.example.data.model.PlaylistItem
import com.example.ui.components.RatingBadge
import com.example.ui.components.TivionsBottomNavBar
import com.example.ui.components.TivionsCard
import com.example.ui.components.TivionsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundAppBrush
import com.example.ui.theme.BgCardDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.TitleNormalizer
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SeriesScreen(
    seriesList: List<PlaylistItem> = emptyList(),
    categories: List<String> = emptyList(),
    categoryCounts: Map<String, Int> = emptyMap(),
    pagedSeriesFlow: ((String) -> Flow<PagingData<PlaylistItem>>)? = null,
    pagedFavoritesFlow: Flow<PagingData<PlaylistItem>>? = null,
    selectedGenre: String = "Tümü",
    categoryPrefs: List<CategoryPref> = emptyList(),
    hiddenCategories: List<String> = emptyList(),
    onHideCategory: (String) -> Unit = {},
    onReorderCategories: (List<String>) -> Unit = {},
    onResetCategoryPrefs: () -> Unit = {},
    onSelectGenre: (String) -> Unit = {},
    onOpenSeriesDetail: (PlaylistItem) -> Unit,
    onPlaySeries: (PlaylistItem) -> Unit = {},
    onToggleFavorite: (PlaylistItem) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onRemoveHistoryItem: (String) -> Unit = {},
    onDeleteDownload: (PlaylistItem) -> Unit = {},
    onClearAllDownloads: (List<PlaylistItem>) -> Unit = {},
    onNavigate: (String) -> Unit
) {
    // 0: Kategori Listesi / Tümü, 1: Favoriler, 2: Geçmiş, 3: İndirilenler
    var selectedFilterTab by remember { mutableIntStateOf(0) }

    // When null, we show the main Series Category List. When non-null, we show 3-column series grid of that category.
    var currentCategoryViewing by remember { mutableStateOf<String?>(null) }

    // Dialog state for Settings button in TopBar
    var showSeriesSettingsDialog by remember { mutableStateOf(false) }

    // Confirmation dialogs
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    // Natural order of series categories from categories list or fallback to M3U/Xtream
    val m3uOriginalGenres = remember(categories, seriesList) {
        if (categories.isNotEmpty()) categories
        else seriesList.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    // Paged series for current category
    val pagedCategorySeries = if (pagedSeriesFlow != null && currentCategoryViewing != null) {
        remember(currentCategoryViewing) { pagedSeriesFlow(currentCategoryViewing ?: "") }.collectAsLazyPagingItems()
    } else null

    // Paged favorites for favorites tab
    val pagedFavorites = if (pagedFavoritesFlow != null && selectedFilterTab == 1) {
        pagedFavoritesFlow.collectAsLazyPagingItems()
    } else null

    // Local mutable state for categories supporting real-time drag-and-drop reorder & swipe-to-delete
    var displayedCategories by remember(m3uOriginalGenres, categoryPrefs, hiddenCategories) {
        val hiddenNormalizedSet = hiddenCategories.map { it.trim().lowercase(Locale("tr")) }.toSet()
        val visible = m3uOriginalGenres.filter { cat ->
            val norm = cat.trim().lowercase(Locale("tr"))
            !hiddenNormalizedSet.contains(norm) && !hiddenCategories.contains(cat)
        }

        // Custom order from categoryPrefs (section = "SERIES")
        val activePrefs = categoryPrefs.filter { !it.hidden }
        val hasCustomOrder = activePrefs.any { it.position < 9000 }

        val sorted = if (hasCustomOrder) {
            val prefMap = activePrefs.associateBy { it.categoryKey }
            visible.sortedWith { a, b ->
                val posA = prefMap[a.trim().lowercase(Locale("tr"))]?.position ?: Int.MAX_VALUE
                val posB = prefMap[b.trim().lowercase(Locale("tr"))]?.position ?: Int.MAX_VALUE
                if (posA != posB) posA.compareTo(posB) else 0
            }
        } else {
            visible
        }

        mutableStateOf(sorted)
    }

    // Drag-and-drop state
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    // Handle Back Press: If inside category or a sub-tab, return to main category list. Otherwise navigate home.
    BackHandler {
        if (currentCategoryViewing != null) {
            currentCategoryViewing = null
        } else if (selectedFilterTab != 0) {
            selectedFilterTab = 0
        } else {
            onNavigate("home")
        }
    }

    val topBarTitle = when {
        currentCategoryViewing != null -> currentCategoryViewing!!
        selectedFilterTab == 1 -> "Favori Diziler"
        selectedFilterTab == 2 -> "İzleme Geçmişi"
        selectedFilterTab == 3 -> "İndirilen Diziler"
        else -> "Diziler"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppBrush)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. ÜST BAR
            // Sol üst: Geri (back) butonu
            // Sağ üst: Ayarlar (settings) butonu
            TivionsTopBar(
                title = topBarTitle,
                showBack = true,
                showCast = false,
                showSettings = true,
                showSearch = false,
                onBackClick = {
                    if (currentCategoryViewing != null) {
                        currentCategoryViewing = null
                    } else if (selectedFilterTab != 0) {
                        selectedFilterTab = 0
                    } else {
                        onNavigate("home")
                    }
                },
                onSettingsClick = {
                    showSeriesSettingsDialog = true
                }
            )

            // 2. ÜST BAR'IN HEMEN ALTI — 3 BUTON: "Favoriler", "Geçmiş", "İndirilenler"
            // Sadece ana dizi kategorileri ekranında görünür; kategori içine girildiğinde üst bar'ın hemen altında doğrudan dizi grid'i yer alır.
            if (currentCategoryViewing == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Favoriler" Butonu
                    val isFavActive = selectedFilterTab == 1
                    SeriesHeaderFilterButton(
                        text = "Favoriler",
                        icon = Icons.Default.Favorite,
                        isActive = isFavActive,
                        activeColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilterTab = if (isFavActive) 0 else 1
                            currentCategoryViewing = null
                        },
                        testTag = "btn_series_fav"
                    )

                    // "Geçmiş" Butonu
                    val isHistoryActive = selectedFilterTab == 2
                    SeriesHeaderFilterButton(
                        text = "Geçmiş",
                        icon = Icons.Default.History,
                        isActive = isHistoryActive,
                        activeColor = AccentCyan,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilterTab = if (isHistoryActive) 0 else 2
                            currentCategoryViewing = null
                        },
                        testTag = "btn_series_history"
                    )

                    // "İndirilenler" Butonu
                    val isDownloadsActive = selectedFilterTab == 3
                    SeriesHeaderFilterButton(
                        text = "İndirilenler",
                        icon = Icons.Default.Download,
                        isActive = isDownloadsActive,
                        activeColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedFilterTab = if (isDownloadsActive) 0 else 3
                            currentCategoryViewing = null
                        },
                        testTag = "btn_series_downloads"
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // 3. MAIN BODY
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedFilterTab) {
                    1 -> {
                        // BÖLÜM: FAVORİLER
                        val favoriteSeries = remember(seriesList) {
                            seriesList.filter { it.isFavorite }
                        }

                        if (pagedFavorites != null) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("series_fav_list"),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Favoriler (${pagedFavorites.itemCount})",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                items(
                                    count = pagedFavorites.itemCount,
                                    key = pagedFavorites.itemKey { it.id }
                                ) { index ->
                                    val series = pagedFavorites[index]
                                    if (series != null) {
                                        SeriesItemSwipeRow(
                                            series = series,
                                            canDelete = false,
                                            onClick = { onOpenSeriesDetail(series) },
                                            onPlay = { onPlaySeries(series) },
                                            onToggleFav = { onToggleFavorite(series) },
                                            onDelete = {}
                                        )
                                    }
                                }
                            }
                        } else if (favoriteSeries.isEmpty()) {
                            SeriesEmptyState(
                                icon = Icons.Default.Favorite,
                                iconTint = Color(0xFFEF4444),
                                title = "Henüz Favori Dizi Eklenmedi",
                                subtitle = "Dizilerin üzerindeki kalp ikonuna dokunarak favorilerinize ekleyebilirsiniz."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("series_fav_list"),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Favoriler (${favoriteSeries.size})",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                items(favoriteSeries, key = { it.id }) { series ->
                                    SeriesItemSwipeRow(
                                        series = series,
                                        canDelete = false,
                                        onClick = { onOpenSeriesDetail(series) },
                                        onPlay = { onPlaySeries(series) },
                                        onToggleFav = { onToggleFavorite(series) },
                                        onDelete = {}
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // 3. BÖLÜM: GEÇMİŞ (Toplu silme + Swipe-to-delete)
                        val historySeries = remember(seriesList) {
                            seriesList.filter { it.lastWatchedTimestamp > 0 }
                                .sortedByDescending { it.lastWatchedTimestamp }
                        }

                        if (historySeries.isEmpty()) {
                            SeriesEmptyState(
                                icon = Icons.Default.History,
                                iconTint = AccentCyan,
                                title = "İzleme Geçmişi Boş",
                                subtitle = "İzlediğiniz diziler ve bölümler en son izleme tarihi sırasıyla burada listelenecektir."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("series_history_list"),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "İzleme Geçmişi (${historySeries.size})",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        // Toplu silme butonu
                                        Button(
                                            onClick = { showClearHistoryDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                tint = DangerRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Geçmişi Temizle",
                                                color = DangerRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                items(historySeries, key = { it.id }) { series ->
                                    SeriesItemSwipeRow(
                                        series = series,
                                        canDelete = true,
                                        deleteLabel = "Geçmişten Sil",
                                        onClick = { onOpenSeriesDetail(series) },
                                        onPlay = { onPlaySeries(series) },
                                        onToggleFav = { onToggleFavorite(series) },
                                        onDelete = { onRemoveHistoryItem(series.id) }
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        // 4. BÖLÜM: İNDİRİLENLER (Fiziksel diskten silme + Swipe-to-delete)
                        val downloadedSeries = remember(seriesList) {
                            seriesList.filter { it.isDownloaded }
                        }

                        if (downloadedSeries.isEmpty()) {
                            SeriesEmptyState(
                                icon = Icons.Default.Download,
                                iconTint = Color(0xFF10B981),
                                title = "İndirilen Dizi Bulunmuyor",
                                subtitle = "Cihazınıza indirdiğiniz bölümler çevrimdışı izlemek için burada listelenecektir."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("series_download_list"),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "İndirilenler (${downloadedSeries.size})",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Button(
                                            onClick = { showClearDownloadsDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                tint = DangerRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tümünü Sil",
                                                color = DangerRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                items(downloadedSeries, key = { it.id }) { series ->
                                    SeriesItemSwipeRow(
                                        series = series,
                                        canDelete = true,
                                        deleteLabel = "Dosyayı Sil",
                                        onClick = { onOpenSeriesDetail(series) },
                                        onPlay = { onPlaySeries(series) },
                                        onToggleFav = { onToggleFavorite(series) },
                                        onDelete = { onDeleteDownload(series) }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // 5. BÖLÜM: DİZİ KATEGORİLERİ LİSTESİ (Ana Yapı) VEYA KATEGORİ İÇİ FİLM/DİZİ GRİDİ
                        if (currentCategoryViewing == null) {
                            val density = LocalDensity.current
                            val itemHeightPx = with(density) { 76.dp.toPx() }

                            if (displayedCategories.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = TextTertiary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Gösterilecek dizi kategorisi bulunamadı.",
                                            color = TextSecondary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (hiddenCategories.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = onResetCategoryPrefs,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E243A))
                                            ) {
                                                Text("Gizlenen Kategorileri Geri Getir", color = AccentCyan)
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("series_categories_lazy_column"),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Dizi Kategorileri (${displayedCategories.size})",
                                                color = TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Sırala: Basılı tut • Sil: Sola kaydır",
                                                color = TextTertiary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    itemsIndexed(
                                        items = displayedCategories,
                                        key = { _, cat -> cat }
                                    ) { index, categoryName ->
                                        val count = categoryCounts[categoryName] ?: seriesList.count { it.category.equals(categoryName, ignoreCase = true) }
                                        val isBeingDragged = draggingIndex == index

                                        SeriesSwipeAndDraggableCategoryRow(
                                            categoryName = categoryName,
                                            seriesCount = count,
                                            isDragging = isBeingDragged,
                                            onClick = {
                                                currentCategoryViewing = categoryName
                                                onSelectGenre(categoryName)
                                            },
                                            onDelete = {
                                                val updated = displayedCategories.toMutableList()
                                                updated.remove(categoryName)
                                                displayedCategories = updated
                                                onHideCategory(categoryName)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerInput(displayedCategories) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            draggingIndex = index
                                                            dragAccumulatedY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragAccumulatedY += dragAmount.y
                                                            val currentIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                            val targetOffsetSteps = (dragAccumulatedY / itemHeightPx).roundToInt()
                                                            val targetIdx = (currentIdx + targetOffsetSteps).coerceIn(0, displayedCategories.lastIndex)

                                                            if (targetIdx != currentIdx) {
                                                                val updated = displayedCategories.toMutableList()
                                                                val item = updated.removeAt(currentIdx)
                                                                updated.add(targetIdx, item)
                                                                displayedCategories = updated
                                                                draggingIndex = targetIdx
                                                                dragAccumulatedY = 0f
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            draggingIndex = null
                                                            dragAccumulatedY = 0f
                                                            onReorderCategories(displayedCategories)
                                                        },
                                                        onDragCancel = {
                                                            draggingIndex = null
                                                            dragAccumulatedY = 0f
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        } else {
                            // BÖLÜM 2: Dizi Kategorisi İçi Ekran (3 Sütunlu Grid Ekranı)
                            if (pagedCategorySeries != null) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("category_series_grid"),
                                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(
                                        count = pagedCategorySeries.itemCount,
                                        key = pagedCategorySeries.itemKey { it.id }
                                    ) { index ->
                                        val series = pagedCategorySeries[index]
                                        if (series != null) {
                                            SeriesGridPosterCard(
                                                series = series,
                                                onClick = { onOpenSeriesDetail(series) },
                                                onToggleFav = { onToggleFavorite(series) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                val categorySeries = remember(seriesList, currentCategoryViewing) {
                                    seriesList.filter { it.category.equals(currentCategoryViewing, ignoreCase = true) }
                                }

                                if (categorySeries.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Bu kategoride henüz dizi bulunmuyor.",
                                            color = TextSecondary,
                                            fontSize = 15.sp
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("category_series_grid"),
                                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(categorySeries, key = { it.id }) { series ->
                                            SeriesGridPosterCard(
                                                series = series,
                                                onClick = { onOpenSeriesDetail(series) },
                                                onToggleFav = { onToggleFavorite(series) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. BOTTOM NAVIGATION BAR
            TivionsBottomNavBar(
                selectedRoute = "series",
                onNavigate = onNavigate
            )
        }

        // Dizi Ayarları Diyaloğu (Sağ üst dişli çark ikonuna basıldığında açılır)
        if (showSeriesSettingsDialog) {
            SeriesSettingsModalDialog(
                hiddenCategoriesCount = hiddenCategories.size,
                onDismiss = { showSeriesSettingsDialog = false },
                onNavigateSettings = {
                    showSeriesSettingsDialog = false
                    onNavigate("settings")
                },
                onResetCategories = {
                    onResetCategoryPrefs()
                    showSeriesSettingsDialog = false
                }
            )
        }

        // Geçmişi Temizleme Onay Diyaloğu
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("İzleme Geçmişini Temizle", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("İzleme geçmişinizdeki tüm dizi ve bölümler silinecek. Emin misiniz?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearHistory()
                            showClearHistoryDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Temizle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("İptal", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF1E243A)
            )
        }

        // İndirilenleri Temizleme Onay Diyaloğu
        if (showClearDownloadsDialog) {
            val downloadedSeries = remember(seriesList) { seriesList.filter { it.isDownloaded } }
            AlertDialog(
                onDismissRequest = { showClearDownloadsDialog = false },
                title = { Text("İndirilenleri Sil", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("İndirilen tüm dizi bölümleri ve dosyaları cihazınızın depolama alanından fiziksel olarak silinecektir. Emin misiniz?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearAllDownloads(downloadedSeries)
                            showClearDownloadsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Tümünü Sil", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDownloadsDialog = false }) {
                        Text("İptal", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF1E243A)
            )
        }
    }
}

/**
 * Üst Bar'ın hemen altındaki 3 Buton için özel tasarım ("Favoriler", "Geçmiş", "İndirilenler")
 */
@Composable
fun SeriesHeaderFilterButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) Brush.horizontalGradient(
                    listOf(activeColor, activeColor.copy(alpha = 0.8f))
                ) else Brush.horizontalGradient(
                    listOf(Color(0xFF1E243A), Color(0xFF161B2E))
                )
            )
            .border(
                width = if (isActive) 0.dp else 1.dp,
                color = if (isActive) Color.Transparent else BorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else activeColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isActive) Color.White else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 5. Dizi Kategorileri Satırı:
 * - 🎬 Türk Dizileri (38)
 * - Sola kaydırma (swipe left) -> Kırmızı "Sil" butonu (Gizleme)
 * - Basılı tutma (long press) -> Sürükle bırak ile sıra değiştirme
 */
@Composable
fun SeriesSwipeAndDraggableCategoryRow(
    categoryName: String,
    seriesCount: Int,
    isDragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var swipeOffsetPx by remember { mutableFloatStateOf(0f) }
    val maxSwipePx = with(density) { -86.dp.toPx() }

    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffsetPx,
        label = "seriesSwipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131828))
    ) {
        // Red Delete background revealed when swiped left
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(86.dp)
                .fillMaxSize()
                .background(DangerRed)
                .clickable {
                    swipeOffsetPx = 0f
                    onDelete()
                }
                .testTag("delete_series_cat_$categoryName"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Sil",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Foreground Category Item Card
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .graphicsLayer {
                    if (isDragging) {
                        scaleX = 1.03f
                        scaleY = 1.03f
                        shadowElevation = 14f
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (swipeOffsetPx + dragAmount).coerceIn(maxSwipePx, 0f)
                            swipeOffsetPx = newOffset
                        },
                        onDragEnd = {
                            if (swipeOffsetPx < maxSwipePx / 2f) {
                                swipeOffsetPx = maxSwipePx
                            } else {
                                swipeOffsetPx = 0f
                            }
                        },
                        onDragCancel = {
                            swipeOffsetPx = 0f
                        }
                    )
                }
                .clickable {
                    if (swipeOffsetPx < -10f) {
                        swipeOffsetPx = 0f
                    } else {
                        onClick()
                    }
                }
                .testTag("series_cat_row_$categoryName"),
            shape = RoundedCornerShape(14.dp),
            color = if (isDragging) Color(0xFF263252) else BgCardDark,
            border = if (isDragging) androidx.compose.foundation.BorderStroke(1.5.dp, AccentCyan)
            else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🎬 Dizi İkonu
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Kategori Adı
                Text(
                    text = categoryName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Parantez içi dizi sayısı: (38)
                Text(
                    text = "($seriesCount)",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Sürükleme tutamacı
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Sürükle",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Ok ikonu
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Geçmiş ve İndirilenler için Swipe-to-delete destekli dizi satırı
 */
@Composable
fun SeriesItemSwipeRow(
    series: PlaylistItem,
    canDelete: Boolean,
    deleteLabel: String = "Sil",
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    var swipeOffsetPx by remember { mutableFloatStateOf(0f) }
    val maxSwipePx = with(density) { -86.dp.toPx() }

    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffsetPx,
        label = "seriesItemSwipe"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131828))
    ) {
        if (canDelete) {
            // Sola kaydırınca ortaya çıkan kırmızı Sil butonu
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                .width(86.dp)
                .fillMaxSize()
                .background(DangerRed)
                .clickable {
                    swipeOffsetPx = 0f
                    onDelete()
                }
                .testTag("delete_series_${series.id}"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Sil",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .pointerInput(canDelete) {
                if (canDelete) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (swipeOffsetPx + dragAmount).coerceIn(maxSwipePx, 0f)
                            swipeOffsetPx = newOffset
                        },
                        onDragEnd = {
                            if (swipeOffsetPx < maxSwipePx / 2f) {
                                swipeOffsetPx = maxSwipePx
                            } else {
                                swipeOffsetPx = 0f
                            }
                        },
                        onDragCancel = {
                            swipeOffsetPx = 0f
                        }
                    )
                }
            }
            .clickable {
                if (swipeOffsetPx < -10f) {
                    swipeOffsetPx = 0f
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = BgCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .size(52.dp, 68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E243A)),
                contentAlignment = Alignment.Center
            ) {
                if (!series.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = series.logoUrl,
                        contentDescription = series.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Dizi Bilgileri
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = series.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (series.rating > 0.0) {
                        Text(
                            text = "⭐ ${String.format(Locale.US, "%.1f", series.rating)}",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = listOf(series.category, series.year.takeIf { it.isNotBlank() } ?: "2024")
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        color = TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Oynat butonu
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x2238BDF8))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Oynat",
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Favori butonu
            IconButton(
                onClick = onToggleFav,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (series.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (series.isFavorite) Color(0xFFEF4444) else TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
}

/**
 * BÖLÜM 2: Kategori içindeki dizilerin 3 sütunlu grid poster kartı (~2:3 dikey poster)
 */
@Composable
fun SeriesGridPosterCard(
    series: PlaylistItem,
    onClick: () -> Unit,
    onToggleFav: () -> Unit = {}
) {
    val cachedMeta = com.example.data.parser.XtreamClient.getCachedCardMeta(series.id)

    // Poster görseli URL'i (API cover alanı öncelikli)
    val displayPoster = cachedMeta?.posterUrl?.takeIf { it.isNotBlank() } ?: series.logoUrl

    // Puan (API rating)
    val displayRating = if ((cachedMeta?.rating ?: 0.0) > 0.0) {
        cachedMeta!!.rating
    } else if (series.rating > 0.0) {
        series.rating
    } else {
        0.0
    }

    // Temizlenmiş ve okunaklı dizi adı
    val cleanTitle = remember(cachedMeta, series.name) {
        cachedMeta?.title?.takeIf { it.isNotBlank() }
            ?: TitleNormalizer.extractSeriesMeta(series.name).cleanTitle.ifBlank { series.name }
    }

    // Tür (API türü veya kategorisi)
    val genreText = remember(cachedMeta, series.category) {
        val raw = cachedMeta?.genre?.takeIf { it.isNotBlank() } ?: series.category.takeIf { it.isNotBlank() } ?: "Dizi"
        raw.split("/").first().trim()
    }

    // Yapım yılı
    val yearText = remember(cachedMeta, series.year, series.name) {
        cachedMeta?.year?.takeIf { it.isNotBlank() }
            ?: series.year.takeIf { it.isNotBlank() }
            ?: TitleNormalizer.extractSeriesMeta(series.name).year
            ?: "2024"
    }

    // Kalite bilgisi (M3U stream/dosya adından regex ile: 1080p, 720p, vb.)
    val qualityText = remember(cachedMeta, series.name) {
        cachedMeta?.quality?.takeIf { it.isNotBlank() }
            ?: TitleNormalizer.extractQualityFromTitle(series.name)
    }

    // Alt metin: Tür • yapım yılı • kalite bilgisi (örnek: Dram • 2019 • 1080p)
    val subtitleText = remember(genreText, yearText, qualityText) {
        listOf(genreText, yearText, qualityText)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                onClick()
            }
            .testTag("series_grid_card_${series.id}")
    ) {
        // 1. Poster Görseli (Dikdörtgen, dikey — yaklaşık 2:3 aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E243A))
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E243A), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 2. Görselin sağ üst köşesinde overlay: Puan rozeti ("⭐ 8.4")
            if (displayRating > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD0A0E1A))
                        .border(0.5.dp, Color(0x33FFC107), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "⭐",
                            fontSize = 9.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", displayRating),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sol üst köşede favori butonu
            IconButton(
                onClick = onToggleFav,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = if (series.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (series.isFavorite) Color(0xFFEF4444) else Color(0xCCFFFFFF),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Dizi adı (posterin hemen altında, kalın/okunaklı)
        Text(
            text = cleanTitle,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 4. Dizi türü, yapım yılı ve kalite bilgisi (küçük ve gri tonlarda) — örnek: Dram • 2019 • 1080p
        Text(
            text = subtitleText,
            color = TextTertiary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Boş durum görünümü
 */
@Composable
fun SeriesEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Dizi Ayarları Modal Diyaloğu (Sağ üst dişli çarka basınca açılır)
 */
@Composable
fun SeriesSettingsModalDialog(
    hiddenCategoriesCount: Int,
    onDismiss: () -> Unit,
    onNavigateSettings: () -> Unit,
    onResetCategories: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F33)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3856)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x2238BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dizi Ayarları & Tercihleri",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (hiddenCategoriesCount > 0) {
                    Button(
                        onClick = onResetCategories,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gizlenen Kategorileri Sıfırla ($hiddenCategoriesCount)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = onNavigateSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Genel Ayarlar Ekranı",
                        color = Color(0xFF0A0E1A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Kapat", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun SeriesBrowseCard(
    series: PlaylistItem,
    onClick: () -> Unit
) {
    TivionsCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("series_browse_card_${series.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp, 68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E243A)),
                contentAlignment = Alignment.Center
            ) {
                if (!series.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = series.logoUrl,
                        contentDescription = series.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (series.rating > 0.0) {
                        RatingBadge(rating = series.rating)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = series.year.ifBlank { "2024" },
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                    if (series.category.isNotBlank()) {
                        Text(
                            text = " • ${series.category}",
                            color = TextTertiary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

