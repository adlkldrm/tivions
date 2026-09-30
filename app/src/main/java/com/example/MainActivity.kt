package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaylistItem
import com.example.data.parser.DemoDataProvider
import com.example.ui.screens.ConnectSourceScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TvChannelsScreen
import com.example.ui.theme.BgDarkNavy
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.IptvViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: IptvViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDarkNavy
                ) {
                    TivionsApp(viewModel = viewModel, homeViewModel = homeViewModel)
                }
            }
        }
    }
}

@Composable
fun TivionsApp(
    viewModel: IptvViewModel,
    homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentRoute by viewModel.currentRoute.collectAsState()
    val playingItem by viewModel.currentlyPlayingItem.collectAsState()
    val detailItem by viewModel.selectedItemForDetail.collectAsState()

    val liveChannels by viewModel.liveChannels.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val series by viewModel.series.collectAsState()
    val liveCategories by viewModel.liveCategories.collectAsState()
    val movieCategories by viewModel.movieCategories.collectAsState()
    val seriesCategories by viewModel.seriesCategories.collectAsState()
    val liveCategoryCounts by viewModel.liveCategoryCounts.collectAsState()
    val movieCategoryCounts by viewModel.movieCategoryCounts.collectAsState()
    val seriesCategoryCounts by viewModel.seriesCategoryCounts.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val hasAnyItems by viewModel.hasAnyItems.collectAsState()
    val allItems = androidx.compose.runtime.remember(liveChannels, movies, series) {
        (liveChannels.take(50) + movies.take(50) + series.take(50))
    }
    val recommendations by viewModel.recommendations.collectAsState()
    val popularMovies by viewModel.popularMovies.collectAsState()
    val popularSeries by viewModel.popularSeries.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val history by viewModel.history.collectAsState()
    val recentMoviesList by viewModel.recentMoviesList.collectAsState()
    val recentSeriesList by viewModel.recentSeriesList.collectAsState()

    val settings by viewModel.settings.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()

    val tvCategory by viewModel.tvCategoryFilter.collectAsState()
    val liveCategoryPrefs by viewModel.liveCategoryPrefs.collectAsState()
    val hiddenLiveCategories by viewModel.hiddenLiveCategories.collectAsState()
    val movieCategoryPrefs by viewModel.movieCategoryPrefs.collectAsState()
    val hiddenMovieCategories by viewModel.hiddenMovieCategories.collectAsState()
    val seriesCategoryPrefs by viewModel.seriesCategoryPrefs.collectAsState()
    val hiddenSeriesCategories by viewModel.hiddenSeriesCategories.collectAsState()
    val movieGenre by viewModel.movieGenreFilter.collectAsState()
    val seriesGenre by viewModel.seriesGenreFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val channelEpgMap by viewModel.channelEpgMap.collectAsState()
    val selectedChannelForEpg by viewModel.selectedChannelForEpg.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    // 1. Fullscreen Video Player takes priority if playing
    if (playingItem != null) {
        val currentEpg = channelEpgMap[playingItem!!.id]?.firstOrNull { it.isCurrentlyPlaying() }
        BackHandler {
            viewModel.stopPlayback()
        }
        PlayerScreen(
            item = playingItem!!,
            epgProgram = currentEpg,
            onProgressUpdate = { item, currentMs, totalMs ->
                viewModel.recordWatchProgress(item, currentMs, totalMs)
            },
            onBack = { viewModel.stopPlayback() }
        )
        return
    }

    // 2. Fullscreen Detail Screen takes secondary priority
    if (detailItem != null) {
        val item = detailItem!!
        val seriesId = item.seriesId ?: item.id
        val seasons: List<Int> by viewModel.getSeasonsForSeries(seriesId).collectAsState(initial = emptyList())
        val (selectedSeasonNum, setSelectedSeasonNum) = androidx.compose.runtime.remember(item.id) {
            androidx.compose.runtime.mutableIntStateOf(1)
        }
        val episodes: List<PlaylistItem> by viewModel.getEpisodesForSeason(seriesId, selectedSeasonNum).collectAsState(initial = emptyList())

        BackHandler {
            viewModel.closeDetail()
        }
        DetailScreen(
            item = item,
            availableSeasons = seasons,
            episodes = episodes,
            selectedSeason = selectedSeasonNum,
            onSelectSeason = { s -> setSelectedSeasonNum(s) },
            onBack = { viewModel.closeDetail() },
            onPlay = { itm -> viewModel.playItem(itm) },
            onToggleFavorite = { itm -> viewModel.toggleFavorite(itm) },
            onToggleDownload = { itm -> viewModel.toggleDownload(itm) }
        )
        return
    }

    // 3. Main Navigation Destinations with Offline Status Bar
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !isOnline && currentRoute != "connect_source") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "İnternet bağlantısı yok - Çevrimdışı moddasınız",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = currentRoute, label = "RouteFade") { route ->
                when (route) {
                    "connect_source" -> {
                        BackHandler(enabled = hasAnyItems) {
                            viewModel.setRoute("home")
                        }
                        ConnectSourceScreen(
                            isLoading = isLoading,
                            statusMessage = connectionMessage,
                            isOnline = isOnline,
                            onConnectM3u = { name, url, remember ->
                                viewModel.connectM3u(name, url, remember)
                            },
                            onConnectXtream = { name, serverUrl, user, pass, remember ->
                                viewModel.connectXtream(name, serverUrl, user, pass, remember)
                            },
                            onBack = if (hasAnyItems) { { viewModel.setRoute("home") } } else null
                        )
                    }

            "tv" -> {
                TvChannelsScreen(
                    channels = liveChannels,
                    categories = liveCategories,
                    categoryCounts = liveCategoryCounts,
                    pagedChannelsFlow = { cat -> viewModel.getPagedChannelsForCategory(cat) },
                    pagedFavoritesFlow = viewModel.getPagedFavorites(com.example.data.model.ItemType.LIVE_TV),
                    selectedCategory = tvCategory,
                    channelEpgMap = channelEpgMap,
                    categoryPrefs = liveCategoryPrefs,
                    hiddenCategories = hiddenLiveCategories,
                    onHideCategory = { cat -> viewModel.setCategoryHidden("LIVE", cat, true) },
                    onReorderCategories = { list -> viewModel.reorderCategories("LIVE", list) },
                    onResetCategoryPrefs = { viewModel.resetCategoryPrefs("LIVE") },
                    onSelectCategory = { cat -> viewModel.setTvCategory(cat) },
                    onPlayChannel = { ch -> viewModel.playItem(ch) },
                    onToggleFavorite = { ch -> viewModel.toggleFavorite(ch) },
                    onNavigate = { dest -> viewModel.setRoute(dest) }
                )
            }

            "movies" -> {
                MoviesScreen(
                    movies = movies,
                    categories = movieCategories,
                    categoryCounts = movieCategoryCounts,
                    pagedMoviesFlow = { cat -> viewModel.getPagedMoviesForCategory(cat) },
                    pagedFavoritesFlow = viewModel.getPagedFavorites(com.example.data.model.ItemType.VOD_MOVIE),
                    selectedGenre = movieGenre,
                    categoryPrefs = movieCategoryPrefs,
                    hiddenCategories = hiddenMovieCategories,
                    onHideCategory = { cat -> viewModel.setCategoryHidden("MOVIE", cat, true) },
                    onReorderCategories = { list -> viewModel.reorderCategories("MOVIE", list) },
                    onResetCategoryPrefs = { viewModel.resetCategoryPrefs("MOVIE") },
                    onSelectGenre = { g -> viewModel.setMovieGenre(g) },
                    onOpenMovieDetail = { m -> viewModel.openDetail(m) },
                    onPlayMovie = { m -> viewModel.playItem(m) },
                    onToggleFavorite = { m -> viewModel.toggleFavorite(m) },
                    onClearHistory = { viewModel.clearMovieHistory() },
                    onRemoveHistoryItem = { id -> viewModel.removeMovieFromHistory(id) },
                    onDeleteDownload = { m -> viewModel.deleteDownloadedMovie(m) },
                    onClearAllDownloads = { list -> viewModel.clearAllDownloadedMovies(list) },
                    onNavigate = { dest -> viewModel.setRoute(dest) }
                )
            }

            "series" -> {
                SeriesScreen(
                    seriesList = series,
                    categories = seriesCategories,
                    categoryCounts = seriesCategoryCounts,
                    pagedSeriesFlow = { cat -> viewModel.getPagedSeriesForCategory(cat) },
                    pagedFavoritesFlow = viewModel.getPagedFavorites(com.example.data.model.ItemType.VOD_SERIES),
                    selectedGenre = seriesGenre,
                    categoryPrefs = seriesCategoryPrefs,
                    hiddenCategories = hiddenSeriesCategories,
                    onHideCategory = { cat -> viewModel.setCategoryHidden("SERIES", cat, true) },
                    onReorderCategories = { list -> viewModel.reorderCategories("SERIES", list) },
                    onResetCategoryPrefs = { viewModel.resetCategoryPrefs("SERIES") },
                    onSelectGenre = { g -> viewModel.setSeriesGenre(g) },
                    onOpenSeriesDetail = { s -> viewModel.openDetail(s) },
                    onPlaySeries = { s -> viewModel.playItem(s) },
                    onToggleFavorite = { s -> viewModel.toggleFavorite(s) },
                    onClearHistory = { viewModel.clearSeriesHistory() },
                    onRemoveHistoryItem = { id -> viewModel.removeSeriesFromHistory(id) },
                    onDeleteDownload = { s -> viewModel.deleteDownloadedSeries(s) },
                    onClearAllDownloads = { list -> viewModel.clearAllDownloadedSeries(list) },
                    onNavigate = { dest -> viewModel.setRoute(dest) }
                )
            }

            "search" -> {
                BackHandler {
                    viewModel.setRoute("home")
                }
                SearchScreen(
                    allItems = allItems,
                    searchResults = searchResults,
                    searchQuery = searchQuery,
                    onQueryChange = { q -> viewModel.setSearchQuery(q) },
                    onPlayItem = { it -> viewModel.playItem(it) },
                    onOpenDetail = { it -> viewModel.openDetail(it) },
                    onNavigate = { dest -> viewModel.setRoute(dest) }
                )
            }

            "settings" -> {
                BackHandler {
                    viewModel.setRoute("home")
                }
                SettingsScreen(
                    settings = settings ?: DemoDataProvider.defaultSettings,
                    profiles = profiles,
                    activeProfile = activeProfile,
                    onUpdateSettings = { newSet -> viewModel.updateSettings(newSet) },
                    onSwitchProfile = { pId -> viewModel.switchProfile(pId) },
                    onOpenConnectSource = { viewModel.setRoute("connect_source") },
                    onRefreshEpg = { viewModel.refreshEpg() },
                    onBack = { viewModel.setRoute("home") }
                )
            }

            else -> {
                // "home"
                HomeScreen(
                    popularMovies = popularMovies,
                    popularSeries = popularSeries,
                    continueWatching = continueWatching,
                    recentMovies = recentMoviesList,
                    recentSeries = recentSeriesList,
                    recommendations = recommendations,
                    liveChannels = liveChannels,
                    allMovies = emptyList(),
                    allSeries = emptyList(),
                    watchHistory = history,
                    homeViewModel = homeViewModel,
                    onPlayItem = { item -> viewModel.playItem(item) },
                    onOpenDetail = { item -> viewModel.openDetail(item) },
                    onResumeWatchRecord = { record -> viewModel.resumeWatchRecord(record) },
                    onDeleteWatchRecord = { id -> viewModel.deleteFromContinueWatching(id) },
                    onNavigate = { dest -> viewModel.setRoute(dest) },
                    onOpenSettings = { viewModel.setRoute("settings") }
                )
            }
        }
    }
}
}
}
