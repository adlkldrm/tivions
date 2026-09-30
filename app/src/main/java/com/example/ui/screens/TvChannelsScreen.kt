package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.model.EpgProgram
import com.example.data.model.PlaylistItem
import com.example.ui.components.LiveStreamPlayerView
import com.example.ui.components.TivionsBottomNavBar
import com.example.ui.components.TivionsCard
import com.example.ui.components.TivionsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BackgroundAppBrush
import com.example.ui.theme.BgCardDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvChannelsScreen(
    channels: List<PlaylistItem> = emptyList(),
    categories: List<String> = emptyList(),
    categoryCounts: Map<String, Int> = emptyMap(),
    pagedChannelsFlow: ((String) -> Flow<PagingData<PlaylistItem>>)? = null,
    pagedFavoritesFlow: Flow<PagingData<PlaylistItem>>? = null,
    selectedCategory: String = "Tümü",
    channelEpgMap: Map<String, List<EpgProgram>> = emptyMap(),
    categoryPrefs: List<CategoryPref> = emptyList(),
    hiddenCategories: List<String> = emptyList(),
    onHideCategory: (String) -> Unit = {},
    onReorderCategories: (List<String>) -> Unit = {},
    onResetCategoryPrefs: () -> Unit = {},
    onSelectCategory: (String) -> Unit = {},
    onPlayChannel: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current

    // Orientation state
    val isPhysicalLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isManualFullscreen by remember { mutableStateOf(false) }
    val isFullscreen = isPhysicalLandscape || isManualFullscreen

    // 0: Tüm Kanallar, 1: Favoriler
    var activeSubTab by remember { mutableIntStateOf(0) }

    // When null, we show the main Category List. When non-null, we show Category Detail with Player & Channels.
    var currentCategoryViewing by remember { mutableStateOf<String?>(null) }

    // Dialog state for Settings button in TopBar
    var showTvSettingsDialog by remember { mutableStateOf(false) }

    // BottomSheet state for Daily EPG schedule
    var showEpgBottomSheet by remember { mutableStateOf(false) }

    // Natural order of categories directly from categories list or fallback to M3U channels
    val m3uOriginalCategories = remember(categories, channels) {
        if (categories.isNotEmpty()) categories
        else channels.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    // Local mutable state for categories supporting real-time drag-and-drop reorder & swipe-to-delete
    var displayedCategories by remember(m3uOriginalCategories, categoryPrefs, hiddenCategories) {
        val hiddenNormalizedSet = hiddenCategories.map { it.trim().lowercase(Locale("tr")) }.toSet()
        val visible = m3uOriginalCategories.filter { cat ->
            val norm = cat.trim().lowercase(Locale("tr"))
            !hiddenNormalizedSet.contains(norm) && !hiddenCategories.contains(cat)
        }

        // Custom order from categoryPrefs if user has ever reordered
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
            // Default to pristine M3U line order
            visible
        }

        mutableStateOf(sorted)
    }

    // Drag-and-drop state
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    // Paged channels for current category
    val pagedChannels = if (pagedChannelsFlow != null && currentCategoryViewing != null) {
        remember(currentCategoryViewing) { pagedChannelsFlow(currentCategoryViewing ?: "") }.collectAsLazyPagingItems()
    } else null

    // Paged favorites for favorites tab
    val pagedFavorites = if (pagedFavoritesFlow != null && activeSubTab == 1) {
        pagedFavoritesFlow.collectAsLazyPagingItems()
    } else null

    // Channels in current category (fallback when pagedChannels is not provided)
    val currentCategoryChannels = remember(channels, currentCategoryViewing) {
        if (currentCategoryViewing != null) {
            channels.filter { it.category.equals(currentCategoryViewing, ignoreCase = true) }
        } else emptyList()
    }

    // Currently playing channel in embedded player
    var currentlyPlayingChannel by remember(currentCategoryViewing) {
        mutableStateOf(currentCategoryChannels.firstOrNull())
    }

    // Auto-update first channel when category opens or channels change
    LaunchedEffect(currentCategoryViewing, currentCategoryChannels, pagedChannels?.itemCount) {
        if (currentCategoryViewing != null && currentlyPlayingChannel == null) {
            if (pagedChannels != null && pagedChannels.itemCount > 0) {
                val first = pagedChannels[0]
                if (first != null) {
                    currentlyPlayingChannel = first
                }
            } else if (currentCategoryChannels.isNotEmpty()) {
                currentlyPlayingChannel = currentCategoryChannels.first()
            }
        }
    }

    // Keep currentlyPlayingChannel synchronized with favorite updates
    LaunchedEffect(channels) {
        if (currentlyPlayingChannel != null) {
            val updated = channels.firstOrNull { it.id == currentlyPlayingChannel?.id }
            if (updated != null && updated != currentlyPlayingChannel) {
                currentlyPlayingChannel = updated
            }
        }
    }

    // EPG resolution for currently playing channel
    val channelEpgList = remember(currentlyPlayingChannel, channelEpgMap) {
        if (currentlyPlayingChannel == null) emptyList()
        else {
            val ch = currentlyPlayingChannel!!
            channelEpgMap[ch.id]
                ?: (if (!ch.epgChannelId.isNullOrBlank()) channelEpgMap[ch.epgChannelId] else null)
                ?: channelEpgMap.entries.firstOrNull {
                    it.key.equals(ch.name, ignoreCase = true)
                }?.value
                ?: emptyList()
        }
    }

    val activeCurrentProgram = remember(channelEpgList) {
        channelEpgList.firstOrNull { it.isCurrentlyPlaying() } ?: channelEpgList.firstOrNull()
    }

    // Fullscreen system bars management
    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            hideSystemUi(activity)
        } else {
            showSystemUi(activity)
        }
        onDispose {
            showSystemUi(activity)
        }
    }

    // Reset requested orientation when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Handle Back Press with proper hierarchical priority
    BackHandler(enabled = isFullscreen) {
        isManualFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    BackHandler(enabled = currentCategoryViewing != null && !isFullscreen) {
        currentCategoryViewing = null
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    BackHandler(enabled = currentCategoryViewing == null && !isFullscreen) {
        onNavigate("home")
    }

    val topBarTitle = when {
        currentCategoryViewing != null -> currentCategoryViewing!!
        activeSubTab == 1 -> "Favoriler"
        else -> "Canlı TV"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppBrush)
            .statusBarsPadding()
    ) {
        if (isFullscreen && currentlyPlayingChannel != null) {
            // FULLSCREEN PLAYER MODE (Triggered by physical landscape or manual toggle)
            // Other UI elements are hidden, player takes full screen
            LiveStreamPlayerView(
                streamUrl = currentlyPlayingChannel!!.streamUrl,
                channelName = currentlyPlayingChannel!!.name,
                epgTitle = activeCurrentProgram?.title,
                isFullscreen = true,
                showBackButton = true,
                autoPlay = true,
                modifier = Modifier.fillMaxSize(),
                onFullscreenToggle = { reqFullscreen ->
                    if (!reqFullscreen) {
                        isManualFullscreen = false
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                },
                onBackClick = {
                    isManualFullscreen = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            )
        } else {
            // PORTRAIT / NORMAL MODE
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP BAR
                // Left: Back button (preserves navigation hierarchy)
                // Right: Settings button (gear icon)
                TivionsTopBar(
                    title = topBarTitle,
                    showBack = true,
                    showCast = false,
                    showSettings = true,
                    showSearch = false,
                    onBackClick = {
                        if (currentCategoryViewing != null) {
                            currentCategoryViewing = null
                        } else {
                            onNavigate("home")
                        }
                    },
                    onSettingsClick = {
                        showTvSettingsDialog = true
                    }
                )

                if (currentCategoryViewing != null) {
                    // CATEGORY DETAIL / IN-CATEGORY PLAYER SCREEN
                    // 2. Video Player: 16:9 aspect ratio at the top
                    if (currentlyPlayingChannel != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(1.dp, Color(0xFF2E3856), RoundedCornerShape(16.dp))
                        ) {
                            LiveStreamPlayerView(
                                streamUrl = currentlyPlayingChannel!!.streamUrl,
                                channelName = currentlyPlayingChannel!!.name,
                                epgTitle = activeCurrentProgram?.title,
                                isFullscreen = false,
                                showBackButton = false,
                                autoPlay = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                                onFullscreenToggle = { reqFullscreen ->
                                    if (reqFullscreen) {
                                        isManualFullscreen = true
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                }
                            )
                        }

                        // 3. Player'ın Altındaki Bilgi Alanı:
                        // 1. satır: O an oynatılan kanalın adı
                        // 2. satır: Şu an yayınlanan programın adı (veya "Program bilgisi bulunamadı")
                        // Sağda: "Yayın Akışı" butonu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentlyPlayingChannel!!.name,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (activeCurrentProgram != null) Color(0xFF22C55E) else TextTertiary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeCurrentProgram?.title ?: "Program bilgisi bulunamadı",
                                        color = if (activeCurrentProgram != null) AccentCyan else TextTertiary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // "Yayın Akışı" Butonu
                            Button(
                                onClick = { showEpgBottomSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E243A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3856)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("btn_yayin_akisi")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Yayın Akışı",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 4. Kanal Listesi (Player'ın Altında, Kategori İçindeki Kanallar)
                    if (pagedChannels != null) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("in_category_channels_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Kanallar (${pagedChannels.itemCount})",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            items(
                                count = pagedChannels.itemCount,
                                key = pagedChannels.itemKey { it.id }
                            ) { index ->
                                val channel = pagedChannels[index]
                                if (channel != null) {
                                    val isCurrentlySelected = channel.id == currentlyPlayingChannel?.id
                                    CategoryDetailChannelRow(
                                        channel = channel,
                                        isSelected = isCurrentlySelected,
                                        onClick = {
                                            currentlyPlayingChannel = channel
                                        },
                                        onToggleFav = {
                                            onToggleFavorite(channel)
                                        }
                                    )
                                }
                            }
                        }
                    } else if (currentCategoryChannels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bu kategoride henüz kanal bulunmuyor.",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("in_category_channels_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Kanallar (${currentCategoryChannels.size})",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            items(currentCategoryChannels, key = { it.id }) { channel ->
                                val isCurrentlySelected = channel.id == currentlyPlayingChannel?.id
                                CategoryDetailChannelRow(
                                    channel = channel,
                                    isSelected = isCurrentlySelected,
                                    onClick = {
                                        // Update currently playing channel in upper player without leaving screen
                                        currentlyPlayingChannel = channel
                                    },
                                    onToggleFav = {
                                        onToggleFavorite(channel)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // MAIN CATEGORY LIST / FAVORITES ROOT VIEW
                    // 2. Sub-Bar: Exactly 2 buttons ("Tüm Kanallar" & "Favoriler")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // "Tüm Kanallar" Button
                        val isAllActive = activeSubTab == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isAllActive) Brush.horizontalGradient(
                                        listOf(AccentCyan, Color(0xFF0284C7))
                                    ) else Brush.horizontalGradient(
                                        listOf(Color(0xFF1E243A), Color(0xFF1A1F33))
                                    )
                                )
                                .border(
                                    width = if (isAllActive) 0.dp else 1.dp,
                                    color = if (isAllActive) Color.Transparent else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    activeSubTab = 0
                                    currentCategoryViewing = null
                                }
                                .testTag("tab_all_channels"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = if (isAllActive) Color(0xFF0A0E1A) else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tüm Kanallar",
                                    color = if (isAllActive) Color(0xFF0A0E1A) else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isAllActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // "Favoriler" Button
                        val isFavActive = activeSubTab == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isFavActive) Brush.horizontalGradient(
                                        listOf(AccentGold, Color(0xFFD97706))
                                    ) else Brush.horizontalGradient(
                                        listOf(Color(0xFF1E243A), Color(0xFF1A1F33))
                                    )
                                )
                                .border(
                                    width = if (isFavActive) 0.dp else 1.dp,
                                    color = if (isFavActive) Color.Transparent else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    activeSubTab = 1
                                    currentCategoryViewing = null
                                }
                                .testTag("tab_favorites"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isFavActive) Color(0xFF0A0E1A) else AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Favoriler",
                                    color = if (isFavActive) Color(0xFF0A0E1A) else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isFavActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (activeSubTab == 0) {
                            // Category List View with Swipe-to-Delete and Drag & Drop Reordering
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
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = TextTertiary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Gösterilecek kategori bulunamadı.",
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
                                        .testTag("categories_lazy_column"),
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
                                                text = "Kategoriler (${displayedCategories.size})",
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
                                        val count = categoryCounts[categoryName] ?: channels.count { it.category.equals(categoryName, ignoreCase = true) }
                                        val isBeingDragged = draggingIndex == index

                                        SwipeAndDraggableCategoryRow(
                                            categoryName = categoryName,
                                            channelCount = count,
                                            isDragging = isBeingDragged,
                                            onClick = {
                                                currentCategoryViewing = categoryName
                                                onSelectCategory(categoryName)
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
                            // FAVORİLER TAB
                            val favoriteChannels = remember(channels) {
                                channels.filter { it.isFavorite }
                            }

                            if (pagedFavorites != null) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("favorites_list"),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "Favori Kanallar (${pagedFavorites.itemCount})",
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
                                        val channel = pagedFavorites[index]
                                        if (channel != null) {
                                            CategoryDetailChannelRow(
                                                channel = channel,
                                                isSelected = false,
                                                onClick = {
                                                    onPlayChannel(channel)
                                                },
                                                onToggleFav = {
                                                    onToggleFavorite(channel)
                                                }
                                            )
                                        }
                                    }
                                }
                            } else if (favoriteChannels.isEmpty()) {
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
                                                .background(Color(0x22EF4444)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Henüz Favori Kanal Eklenmedi",
                                            color = TextPrimary,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Kanalların yanındaki kalp ikonuna dokunarak favorilerinize ekleyebilirsiniz.",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("favorites_list"),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "Favori Kanallar (${favoriteChannels.size})",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    items(favoriteChannels, key = { it.id }) { channel ->
                                        CategoryDetailChannelRow(
                                            channel = channel,
                                            isSelected = false,
                                            onClick = {
                                                // When clicked from favorites, play directly or open in category
                                                currentCategoryViewing = channel.category.ifBlank { "Favoriler" }
                                                currentlyPlayingChannel = channel
                                            },
                                            onToggleFav = {
                                                onToggleFavorite(channel)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Navigation Bar
                TivionsBottomNavBar(
                    selectedRoute = "tv",
                    onNavigate = onNavigate
                )
            }
        }

        // 3. EPG Daily Program Guide BottomSheet
        if (showEpgBottomSheet && currentlyPlayingChannel != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showEpgBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF131828),
                dragHandle = null
            ) {
                EpgScheduleBottomSheetContent(
                    channel = currentlyPlayingChannel!!,
                    programs = channelEpgList,
                    onDismiss = { showEpgBottomSheet = false }
                )
            }
        }

        // TV Settings Modal Dialog (Triggered by TopBar Settings Gear Icon)
        if (showTvSettingsDialog) {
            TvSettingsModalDialog(
                hiddenCategoriesCount = hiddenCategories.size,
                onDismiss = { showTvSettingsDialog = false },
                onNavigateSettings = {
                    showTvSettingsDialog = false
                    onNavigate("settings")
                },
                onResetCategories = {
                    onResetCategoryPrefs()
                    showTvSettingsDialog = false
                }
            )
        }
    }
}

/**
 * Clean channel row inside category detail:
 * - Left: Logo
 * - Middle: Name
 * - Right: Heart Favorite Button (turns red when favorited, gray/passive when not)
 */
@Composable
fun CategoryDetailChannelRow(
    channel: PlaylistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    TivionsCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ch_row_${channel.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) Color(0x2238BDF8) else Color.Transparent
                )
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) AccentCyan else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Solda: Kanal Logosu
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E243A))
                    .border(0.5.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Text(
                        text = channel.name.take(2).uppercase(Locale.ROOT),
                        color = AccentCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Ortada: Kanal Adı
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = if (isSelected) AccentCyan else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isSelected) "OYNATILIYOR" else "CANLI",
                        color = if (isSelected) AccentCyan else Color(0xFF22C55E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // En Sağda: Favori (kalp) butonu
            // Tıklandığında kırmızı renge döner (favorilendiğini gösterir).
            // Tekrar tıklanırsa gri/pasif renge döner ve favorilerden çıkar.
            IconButton(
                onClick = onToggleFav,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("fav_btn_${channel.id}")
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (channel.isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                    tint = if (channel.isFavorite) Color(0xFFEF4444) else TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Daily EPG Program Schedule Bottom Sheet Content
 */
@Composable
fun EpgScheduleBottomSheetContent(
    channel: PlaylistItem,
    programs: List<EpgProgram>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .size(40.dp, 4.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E3856))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Header: Channel name & Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = channel.name,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Günün Yayın Akışı (EPG)",
                    color = AccentCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E243A))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule program list
        if (programs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bu kanal için güncel yayın akışı bulunamadı.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(programs) { prog ->
                    val isLive = prog.isCurrentlyPlaying()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLive) Color(0xFF1C2740) else Color(0xFF161B2E)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isLive) 1.dp else 0.5.dp,
                            color = if (isLive) AccentCyan else BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Time range badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isLive) AccentCyan else Color(0xFF22283E))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = prog.formattedTimeRange(),
                                    color = if (isLive) Color(0xFF0A0E1A) else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Program details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prog.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isLive) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (prog.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = prog.description,
                                        color = TextTertiary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isLive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3322C55E))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "ŞU AN",
                                        color = Color(0xFF22C55E),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
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

/**
 * Category Row supporting:
 * 1. Clean folder design: 📁 Kategori Adı (count)
 * 2. Left swipe to reveal red "Sil" button (Swipe-to-delete)
 * 3. Drag Handle and elevation on long press (Drag & Drop Reorder)
 */
@Composable
fun SwipeAndDraggableCategoryRow(
    categoryName: String,
    channelCount: Int,
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
        label = "swipeOffset"
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
                .testTag("delete_cat_$categoryName"),
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
                .testTag("cat_row_$categoryName"),
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
                // Folder Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2E3856), Color(0xFF1E243A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Category Name
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

                // Channel count badge (e.g. "(24)")
                Text(
                    text = "($channelCount)",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Drag handle icon for intuitive ordering
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Sürükle",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Arrow right icon
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
 * TV Settings Dialog triggered by TopBar Gear icon
 */
@Composable
fun TvSettingsModalDialog(
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
                    text = "Canlı TV Seçenekleri",
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

/**
 * Extension to find host Activity
 */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Hides status and navigation bars for immersive fullscreen
 */
private fun hideSystemUi(activity: Activity?) {
    activity ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }
}

/**
 * Restores status and navigation bars
 */
private fun showSystemUi(activity: Activity?) {
    activity ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(
            WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        )
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}

/**
 * Backward compatibility components for other screens
 */
@Composable
fun CategoryFolderCard(
    categoryName: String,
    channelCount: Int,
    onClick: () -> Unit
) {
    val (icon, gradient) = when {
        categoryName.contains("spor", ignoreCase = true) ->
            Pair(Icons.Default.SportsSoccer, listOf(Color(0xFF38BDF8), Color(0xFF2563EB)))
        categoryName.contains("sinema", ignoreCase = true) || categoryName.contains("film", ignoreCase = true) ->
            Pair(Icons.Default.Movie, listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
        categoryName.contains("ulusal", ignoreCase = true) ->
            Pair(Icons.Default.Tv, listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
        categoryName.contains("haber", ignoreCase = true) ->
            Pair(Icons.Default.Newspaper, listOf(Color(0xFF06B6D4), Color(0xFF0284C7)))
        categoryName.contains("çocuk", ignoreCase = true) ->
            Pair(Icons.Default.ChildCare, listOf(Color(0xFF10B981), Color(0xFF059669)))
        else ->
            Pair(Icons.Default.Tv, listOf(Color(0xFF818CF8), Color(0xFF4F46E5)))
    }

    TivionsCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cat_card_$categoryName"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$channelCount içerik",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ChannelListItemCard(
    channel: PlaylistItem,
    programs: List<EpgProgram> = emptyList(),
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onOpenEpg: () -> Unit = {}
) {
    CategoryDetailChannelRow(
        channel = channel,
        isSelected = false,
        onClick = onPlay,
        onToggleFav = onToggleFav
    )
}
