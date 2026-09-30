package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.CategorySummary
import com.example.data.model.EpgProgram
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.UserProfile
import com.example.data.parser.XmlTvParser
import com.example.data.repository.IptvRepository
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val favoriteRepository = com.example.data.repository.FavoriteChannelRepository(
        favoriteChannelDao = database.favoriteChannelDao(),
        itemDao = database.itemDao()
    )
    private val repository = IptvRepository(
        dao = database.itemDao(),
        favoriteRepository = favoriteRepository
    )
    val epgService: com.example.data.epg.IEpgService = repository.epgService

    // Network connectivity monitor
    private val networkMonitor = com.example.util.NetworkMonitor.getInstance(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    // All Items (kept empty to prevent OutOfMemoryError on large playlists, individual items loaded on demand)
    val allItems: StateFlow<List<PlaylistItem>> = MutableStateFlow(emptyList())

    // EPG Programs
    val allEpgPrograms: StateFlow<List<EpgProgram>> = repository.allEpgPrograms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct category list flows from Room database
    val liveCategories: StateFlow<List<String>> = repository.getCategoriesByType(ItemType.LIVE_TV)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movieCategories: StateFlow<List<String>> = repository.getCategoriesByType(ItemType.VOD_MOVIE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesCategories: StateFlow<List<String>> = repository.getCategoriesByType(ItemType.VOD_SERIES)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveCategoryCounts: StateFlow<Map<String, Int>> = repository.getCategoryCountsMap("LIVE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val movieCategoryCounts: StateFlow<Map<String, Int>> = repository.getCategoryCountsMap("MOVIE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val seriesCategoryCounts: StateFlow<Map<String, Int>> = repository.getCategoryCountsMap("SERIES")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun getPagedChannelsForCategory(category: String): Flow<PagingData<PlaylistItem>> {
        return if (category == "Tümü" || category.isBlank()) {
            repository.getPagedItemsByType(ItemType.LIVE_TV, pageSize = 50)
                .cachedIn(viewModelScope)
        } else {
            repository.getPagedItemsByTypeAndCategory(ItemType.LIVE_TV, category, pageSize = 50)
                .cachedIn(viewModelScope)
        }
    }

    fun getPagedMoviesForCategory(category: String): Flow<PagingData<PlaylistItem>> {
        return if (category == "Tümü" || category.isBlank()) {
            repository.getPagedItemsByType(ItemType.VOD_MOVIE, pageSize = 50)
                .cachedIn(viewModelScope)
        } else {
            repository.getPagedItemsByTypeAndCategory(ItemType.VOD_MOVIE, category, pageSize = 50)
                .cachedIn(viewModelScope)
        }
    }

    fun getPagedSeriesForCategory(category: String): Flow<PagingData<PlaylistItem>> {
        return if (category == "Tümü" || category.isBlank()) {
            repository.getPagedItemsByType(ItemType.VOD_SERIES, pageSize = 50)
                .cachedIn(viewModelScope)
        } else {
            repository.getPagedItemsByTypeAndCategory(ItemType.VOD_SERIES, category, pageSize = 50)
                .cachedIn(viewModelScope)
        }
    }

    fun getPagedFavorites(type: ItemType? = null): Flow<PagingData<PlaylistItem>> {
        return repository.getPagedFavorites(type, pageSize = 50).cachedIn(viewModelScope)
    }

    // Live Channels preview (limited to 50 for preview and EPG mapping)
    val liveChannels: StateFlow<List<PlaylistItem>> = repository.getItemsByType(ItemType.LIVE_TV)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Channel to EPG program list mapping
    val channelEpgMap: StateFlow<Map<String, List<EpgProgram>>> = combine(liveChannels, allEpgPrograms) { channels, programs ->
        val map = mutableMapOf<String, List<EpgProgram>>()
        val programsByChannelId = programs.groupBy { it.channelId.lowercase() }

        for (channel in channels) {
            val key = channel.id.lowercase()
            val epgKey = channel.epgChannelId?.lowercase()
            val cleanName = XmlTvParser.normalizeChannelName(channel.name)

            // Look up by direct channelId, epgChannelId, or normalized name
            val matchedProgs = programsByChannelId[key]
                ?: (if (epgKey != null) programsByChannelId[epgKey] else null)
                ?: programs.filter { prog ->
                    val progChannelClean = XmlTvParser.normalizeChannelName(prog.channelId)
                    progChannelClean == cleanName || prog.channelId.equals(channel.name, ignoreCase = true)
                }.sortedBy { it.startTimeMillis }

            map[channel.id] = matchedProgs ?: emptyList()
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Settings
    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Profiles
    val profiles: StateFlow<List<UserProfile>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<UserProfile?> = repository.profiles
        .combine(MutableStateFlow(Unit)) { list, _ ->
            list.firstOrNull { it.isActive } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Personalized Recommendations
    val recommendations: StateFlow<List<PlaylistItem>> = repository.getPersonalizedRecommendations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // TV Channels
    val liveChannelsList: StateFlow<List<PlaylistItem>> = liveChannels

    // Movies
    val movies: StateFlow<List<PlaylistItem>> = repository.getItemsByType(ItemType.VOD_MOVIE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Series
    val series: StateFlow<List<PlaylistItem>> = repository.getItemsByType(ItemType.VOD_SERIES)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites
    val favorites: StateFlow<List<PlaylistItem>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<com.example.data.model.FavoriteChannel>> = favoriteRepository.getAllFavoriteChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveCategoryPrefs: StateFlow<List<com.example.data.model.CategoryPref>> = repository.getCategoryPrefs("default", "LIVE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenLiveCategories: StateFlow<List<String>> = repository.getHiddenCategories("default", "LIVE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movieCategoryPrefs: StateFlow<List<com.example.data.model.CategoryPref>> = repository.getCategoryPrefs("default", "MOVIE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenMovieCategories: StateFlow<List<String>> = repository.getHiddenCategories("default", "MOVIE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesCategoryPrefs: StateFlow<List<com.example.data.model.CategoryPref>> = repository.getCategoryPrefs("default", "SERIES")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenSeriesCategories: StateFlow<List<String>> = repository.getHiddenCategories("default", "SERIES")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Watch History
    val history: StateFlow<List<PlaylistItem>> = repository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Downloads
    val downloads: StateFlow<List<PlaylistItem>> = repository.getDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // BÖLÜM 7: Continue Watching (WatchRecord with progress between 0.02 and 0.92)
    val continueWatching: StateFlow<List<com.example.data.model.WatchRecord>> = repository.getContinueWatching()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // BÖLÜM 6: Popular Movies & Series (Cached SQL queries)
    val popularMovies: StateFlow<List<PlaylistItem>> = combine(
        repository.cachedPopularMovies,
        repository.getPopularMovies()
    ) { cached: List<PlaylistItem>, live: List<PlaylistItem> ->
        if (cached.isNotEmpty()) cached else live
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularSeries: StateFlow<List<PlaylistItem>> = combine(
        repository.cachedPopularSeries,
        repository.getPopularSeries()
    ) { cached: List<PlaylistItem>, live: List<PlaylistItem> ->
        if (cached.isNotEmpty()) cached else live
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // BÖLÜM 8 & 9: Recently Added Movies & Series (Cached SQL queries)
    val recentMoviesList: StateFlow<List<PlaylistItem>> = combine(
        repository.cachedRecentMovies,
        repository.getRecentMovies()
    ) { cached: List<PlaylistItem>, live: List<PlaylistItem> ->
        if (cached.isNotEmpty()) cached else live
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSeriesList: StateFlow<List<PlaylistItem>> = combine(
        repository.cachedRecentSeries,
        repository.getRecentSeries()
    ) { cached: List<PlaylistItem>, live: List<PlaylistItem> ->
        if (cached.isNotEmpty()) cached else live
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resumeWatchRecord(record: com.example.data.model.WatchRecord) {
        viewModelScope.launch {
            val item = repository.getItemById(record.itemId)
            if (item != null) {
                playItem(item)
            }
        }
    }

    // BÖLÜM 14: Category Preferences (Hidden Categories)
    val hiddenCategoriesLive: StateFlow<List<String>> = repository.getHiddenCategories("default", "LIVE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenCategoriesMovie: StateFlow<List<String>> = repository.getHiddenCategories("default", "MOVIE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenCategoriesSeries: StateFlow<List<String>> = repository.getHiddenCategories("default", "SERIES")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state - Starts directly at connect_source unless remembered
    private val prefs = application.getSharedPreferences("tivions_auth_prefs", android.content.Context.MODE_PRIVATE)

    private val _currentRoute = MutableStateFlow(
        if (prefs.getBoolean("key_remember", false)) "home" else "connect_source"
    )
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val _selectedItemForDetail = MutableStateFlow<PlaylistItem?>(null)
    val selectedItemForDetail: StateFlow<PlaylistItem?> = _selectedItemForDetail.asStateFlow()

    private val _currentlyPlayingItem = MutableStateFlow<PlaylistItem?>(null)
    val currentlyPlayingItem: StateFlow<PlaylistItem?> = _currentlyPlayingItem.asStateFlow()

    private val _tvCategoryFilter = MutableStateFlow("Tümü")
    val tvCategoryFilter: StateFlow<String> = _tvCategoryFilter.asStateFlow()

    private val _movieGenreFilter = MutableStateFlow("Tümü")
    val movieGenreFilter: StateFlow<String> = _movieGenreFilter.asStateFlow()

    private val _seriesGenreFilter = MutableStateFlow("Tümü")
    val seriesGenreFilter: StateFlow<String> = _seriesGenreFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<PlaylistItem>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.search(query.trim())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasAnyItems: StateFlow<Boolean> = repository.allSources
        .combine(repository.settings) { sources, _ ->
            sources.isNotEmpty()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Loading & connection state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _connectionMessage = MutableStateFlow<String?>(null)
    val connectionMessage: StateFlow<String?> = _connectionMessage.asStateFlow()

    private val _parentalDialogActive = MutableStateFlow(false)
    val parentalDialogActive: StateFlow<Boolean> = _parentalDialogActive.asStateFlow()

    private val _selectedChannelForEpg = MutableStateFlow<PlaylistItem?>(null)
    val selectedChannelForEpg: StateFlow<PlaylistItem?> = _selectedChannelForEpg.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureInitialData()
                val isRemembered = prefs.getBoolean("key_remember", false)
                if (!isRemembered) {
                    // Clear any leftover data so user starts clean on connect_source
                    repository.clearAllData()
                    _currentRoute.value = "connect_source"
                } else {
                    // User chose to remember. Check if we need to auto-load in background
                    val itemCount = try { database.itemDao().getItemCount() } catch (_: Exception) { 0 }
                    if (itemCount > 0) {
                        _currentRoute.value = "home"
                        launch {
                            try {
                                repository.syncCategoryCounts(force = false)
                            } catch (_: Exception) {}
                        }
                    } else {
                        // DB is empty, auto-connect using saved credentials
                        val sourceType = prefs.getString("key_source_type", "m3u")
                        if (sourceType == "m3u") {
                            val url = prefs.getString("key_m3u_url", "")
                            val name = prefs.getString("key_m3u_name", "Ev Listem") ?: "Ev Listem"
                            if (!url.isNullOrBlank()) {
                                val creds = com.example.data.parser.XtreamClient.extractXtreamCredentials(url)
                                if (creds != null) {
                                    repository.setXtreamCredentials(creds.serverUrl, creds.username, creds.password)
                                }
                                connectM3u(name, url, remember = true)
                            } else {
                                _currentRoute.value = "connect_source"
                            }
                        } else if (sourceType == "xtream") {
                            val host = prefs.getString("key_xtream_host", "")
                            val user = prefs.getString("key_xtream_user", "")
                            val pass = prefs.getString("key_xtream_pass", "")
                            val name = prefs.getString("key_xtream_name", "Xtream") ?: "Xtream"
                            if (!host.isNullOrBlank() && !user.isNullOrBlank() && !pass.isNullOrBlank()) {
                                repository.setXtreamCredentials(host, user, pass)
                                connectXtream(name, host, user, pass, remember = true)
                            } else {
                                _currentRoute.value = "connect_source"
                            }
                        } else {
                            _currentRoute.value = "connect_source"
                        }
                    }
                }
            } catch (_: Exception) {
                // Safe fallback to clean connection screen on corrupted persistence
                _currentRoute.value = "connect_source"
            }
        }
    }

    fun getSeasonsForSeries(seriesId: String): Flow<List<Int>> = repository.getSeasonsForSeries(seriesId)

    fun getEpisodesForSeason(seriesId: String, seasonNumber: Int): Flow<List<PlaylistItem>> =
        repository.getEpisodesForSeason(seriesId, seasonNumber)

    fun disconnectPlaylist() {
        viewModelScope.launch {
            prefs.edit().clear().apply()
            repository.clearAllData()
            _currentRoute.value = "connect_source"
        }
    }

    fun setRoute(route: String) {
        _currentRoute.value = route
    }

    fun setTvCategory(cat: String) {
        _tvCategoryFilter.value = cat
        viewModelScope.launch {
            repository.refreshCategoryStreams(ItemType.LIVE_TV, cat)
        }
    }

    fun setMovieGenre(genre: String) {
        _movieGenreFilter.value = genre
        viewModelScope.launch {
            repository.refreshCategoryStreams(ItemType.VOD_MOVIE, genre)
        }
    }

    fun setSeriesGenre(genre: String) {
        _seriesGenreFilter.value = genre
        viewModelScope.launch {
            repository.refreshCategoryStreams(ItemType.VOD_SERIES, genre)
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun openDetail(item: PlaylistItem) {
        _selectedItemForDetail.value = item
        viewModelScope.launch {
            if (item.type == ItemType.VOD_MOVIE) {
                val enriched = repository.fetchVodInfo(item)
                if (enriched != null && _selectedItemForDetail.value?.id == item.id) {
                    _selectedItemForDetail.value = enriched
                }
            } else if (item.type == ItemType.VOD_SERIES) {
                val enriched = repository.fetchSeriesInfo(item)
                if (enriched != null && _selectedItemForDetail.value?.id == item.id) {
                    _selectedItemForDetail.value = enriched
                }
            }
        }
    }

    fun closeDetail() {
        _selectedItemForDetail.value = null
    }

    fun playItem(item: PlaylistItem) {
        _currentlyPlayingItem.value = item
        viewModelScope.launch {
            repository.recordWatch(item.id)
        }
    }

    fun stopPlayback() {
        _currentlyPlayingItem.value = null
    }

    fun toggleFavorite(item: PlaylistItem) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(item)
        }
    }

    fun toggleDownload(item: PlaylistItem) {
        viewModelScope.launch {
            repository.toggleDownload(item.id, item.isDownloaded)
        }
    }

    fun connectM3u(name: String, url: String, remember: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _connectionMessage.value = "Yayın kaynağı analiz ediliyor..."
            val result = repository.connectM3u(name, url, remember) { count, lastGroup ->
                _connectionMessage.value = "$count içerik aktarıldı ($lastGroup)..."
            }
            _isLoading.value = false
            result.onSuccess { count ->
                val xtreamCreds = com.example.data.parser.XtreamClient.extractXtreamCredentials(url)
                if (remember) {
                    val editor = prefs.edit().putBoolean("key_remember", true)
                    if (xtreamCreds != null) {
                        editor.putString("key_source_type", "xtream")
                            .putString("key_xtream_name", name)
                            .putString("key_xtream_host", xtreamCreds.serverUrl)
                            .putString("key_xtream_user", xtreamCreds.username)
                            .putString("key_xtream_pass", xtreamCreds.password)
                    } else {
                        editor.putString("key_source_type", "m3u")
                            .putString("key_m3u_name", name)
                            .putString("key_m3u_url", url)
                    }
                    editor.apply()
                } else {
                    prefs.edit().clear().apply()
                }
                _connectionMessage.value = "Başarıyla bağlandı! $count içerik aktarıldı ✅"
                _currentRoute.value = "home"
            }.onFailure { err ->
                val userMsg = when {
                    err.message?.startsWith("İçe aktarma sırasında hata oluştu") == true -> err.message!!
                    err is java.net.UnknownHostException -> "Sunucuya ulaşılamadı. İnternet bağlantınızı veya yayın adresinizi kontrol edin."
                    err is java.net.SocketTimeoutException -> "Bağlantı zaman aşımına uğradı. Sunucu yanıt vermiyor, tekrar deneyin."
                    err is java.lang.IllegalArgumentException -> err.message ?: "Geçersiz playlist adresi."
                    err.message?.contains("401") == true || err.message?.contains("403") == true -> "Yayın sunucusu erişim izni vermedi (Yetki hatası)."
                    err.message?.contains("404") == true -> "M3U listesi sunucuda bulunamadı (404)."
                    err.message?.contains("oynatılabilir") == true -> "M3U dosyasında oynatılabilir geçerli bir kanal veya film bulunamadı."
                    else -> err.localizedMessage ?: "Bağlantı kurulamadı, linki kontrol edip tekrar deneyin."
                }
                _connectionMessage.value = "Hata: $userMsg"
            }
        }
    }

    fun connectXtream(name: String, serverUrl: String, user: String, pass: String, remember: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _connectionMessage.value = "Xtream Codes sunucusuna bağlanılıyor..."
            val result = repository.connectXtream(name, serverUrl, user, pass, remember) { count, lastGroup ->
                _connectionMessage.value = "$count içerik aktarıldı ($lastGroup)..."
            }
            _isLoading.value = false
            result.onSuccess { count ->
                if (remember) {
                    prefs.edit()
                        .putBoolean("key_remember", true)
                        .putString("key_source_type", "xtream")
                        .putString("key_xtream_name", name)
                        .putString("key_xtream_host", serverUrl)
                        .putString("key_xtream_user", user)
                        .putString("key_xtream_pass", pass)
                        .apply()
                } else {
                    prefs.edit().clear().apply()
                }
                _connectionMessage.value = "$count içerik başarıyla aktarıldı!"
                _currentRoute.value = "home"
            }.onFailure { err ->
                val userMsg = when {
                    err is java.net.UnknownHostException -> "Xtream sunucusuna ulaşılamadı. İnternet bağlantınızı veya sunucu adresini kontrol edin."
                    err is java.net.SocketTimeoutException -> "Sunucu bağlantısı zaman aşımına uğradı. Lütfen tekrar deneyin."
                    err is java.lang.IllegalArgumentException -> err.message ?: "Lütfen sunucu adresi ve kullanıcı bilgilerini eksiksiz girin."
                    err.message?.contains("Kullanıcı") == true || err.message?.contains("şifre") == true || err.message?.contains("geçersiz") == true -> "Kullanıcı adı veya şifre hatalı."
                    err.message?.contains("oynatılabilir") == true || err.message?.contains("içerik bulunamadı") == true -> "Sunucuda oynatılabilir içerik bulunamadı."
                    else -> err.localizedMessage ?: "Bağlantı kurulamadı, bilgileri kontrol edin."
                }
                _connectionMessage.value = "Hata: $userMsg"
            }
        }
    }

    fun loadDemoLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.loadDemoData()
            _isLoading.value = false
            _connectionMessage.value = "Demo listesi başarıyla yüklendi!"
            _currentRoute.value = "home"
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
        }
    }

    fun clearConnectionMessage() {
        _connectionMessage.value = null
    }

    fun openParentalDialog() {
        _parentalDialogActive.value = true
    }

    fun closeParentalDialog() {
        _parentalDialogActive.value = false
    }

    fun selectChannelForEpg(channel: PlaylistItem?) {
        _selectedChannelForEpg.value = channel
    }

    fun refreshEpg(customUrl: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _connectionMessage.value = "EPG Program Rehberi güncelleniyor..."
            val result = if (!customUrl.isNullOrBlank() && customUrl.startsWith("http")) {
                repository.loadEpgFromUrl(customUrl)
            } else {
                repository.loadDemoEpgData()
            }
            _isLoading.value = false
            result.onSuccess { count ->
                _connectionMessage.value = "EPG Rehberi güncellendi: $count program yüklendi."
            }.onFailure { err ->
                val userMsg = when {
                    err is java.net.UnknownHostException -> "EPG sunucusuna ulaşılamadı. İnternet bağlantınızı kontrol edin."
                    err is java.net.SocketTimeoutException -> "EPG indirme zaman aşımına uğradı."
                    else -> err.localizedMessage ?: "Program rehberi güncellenemedi."
                }
                _connectionMessage.value = "Hata: $userMsg"
            }
        }
    }

    // BÖLÜM 7 & 11: Record watch progress every 10s during playback
    fun recordWatchProgress(item: PlaylistItem, currentPosMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.recordWatchProgress(item, currentPosMs, durationMs)
        }
    }

    fun deleteFromContinueWatching(id: String) {
        viewModelScope.launch {
            repository.deleteWatchRecord(id)
        }
    }

    // BÖLÜM 14: Category customization
    fun setCategoryHidden(section: String, category: String, hidden: Boolean) {
        viewModelScope.launch {
            repository.setCategoryHidden("default", section, category, hidden)
        }
    }

    fun reorderCategories(section: String, categoryKeys: List<String>) {
        viewModelScope.launch {
            repository.reorderCategories("default", section, categoryKeys)
        }
    }

    fun resetCategoryPrefs(section: String) {
        viewModelScope.launch {
            repository.resetCategoryPrefs("default", section)
        }
    }

    fun clearMovieHistory() {
        viewModelScope.launch {
            repository.clearWatchHistoryForType(ItemType.VOD_MOVIE)
        }
    }

    fun removeMovieFromHistory(id: String) {
        viewModelScope.launch {
            repository.clearItemWatchHistory(id)
        }
    }

    fun deleteDownloadedMovie(movie: PlaylistItem) {
        viewModelScope.launch {
            repository.deleteDownloadedItem(movie)
        }
    }

    fun clearAllDownloadedMovies(movies: List<PlaylistItem>) {
        viewModelScope.launch {
            repository.clearAllDownloadedItems(movies)
        }
    }

    fun clearSeriesHistory() {
        viewModelScope.launch {
            repository.clearWatchHistoryForType(ItemType.VOD_SERIES)
        }
    }

    fun removeSeriesFromHistory(id: String) {
        viewModelScope.launch {
            repository.clearItemWatchHistory(id)
        }
    }

    fun deleteDownloadedSeries(series: PlaylistItem) {
        viewModelScope.launch {
            repository.deleteDownloadedItem(series)
        }
    }

    fun clearAllDownloadedSeries(seriesList: List<PlaylistItem>) {
        viewModelScope.launch {
            repository.clearAllDownloadedSeries(seriesList)
        }
    }
}
