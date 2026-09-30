package com.example.data.repository

import com.example.data.local.ItemDao
import com.example.data.model.AppSettings
import com.example.data.model.EpgProgram
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSource
import com.example.data.model.SourceType
import com.example.data.model.UserProfile
import com.example.data.parser.DemoDataProvider
import com.example.data.parser.M3uParser
import com.example.data.parser.XmlTvParser
import com.example.data.parser.XtreamClient
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.UUID

class IptvRepository(
    private val dao: ItemDao,
    val favoriteRepository: FavoriteChannelRepository? = null
) {
    var activeXtreamCreds: com.example.data.parser.XtreamCredentials? = null
    val categoryIdMap = java.util.concurrent.ConcurrentHashMap<String, String>() // categoryName -> categoryId
    val categoryNameMap = java.util.concurrent.ConcurrentHashMap<String, String>() // categoryId -> categoryName

    fun setXtreamCredentials(serverUrl: String, username: String, password: String) {
        activeXtreamCreds = com.example.data.parser.XtreamCredentials(serverUrl.trim(), username.trim(), password.trim())
    }

    val m3uRepository = M3uPlaylistRepository(dao)
    val epgService: com.example.data.epg.IEpgService = com.example.data.epg.EpgService(dao)
    val favoriteChannelRepo = favoriteRepository ?: FavoriteChannelRepository(
        favoriteChannelDao = object : com.example.data.local.FavoriteChannelDao {
            // Safe fallback if not injected directly
            override fun getAllFavoriteChannels(): Flow<List<com.example.data.model.FavoriteChannel>> = kotlinx.coroutines.flow.emptyFlow()
            override fun getFavoriteChannelsByGroup(groupTitle: String): Flow<List<com.example.data.model.FavoriteChannel>> = kotlinx.coroutines.flow.emptyFlow()
            override fun getFavoritedPlaylistItems(): Flow<List<PlaylistItem>> = dao.getFavoritesByType(ItemType.LIVE_TV)
            override fun isChannelFavoriteFlow(channelId: String): Flow<Boolean> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun isChannelFavorite(channelId: String): Boolean = false
            override suspend fun getFavoriteById(channelId: String): com.example.data.model.FavoriteChannel? = null
            override suspend fun insertFavorite(favorite: com.example.data.model.FavoriteChannel) {}
            override suspend fun deleteFavoriteById(channelId: String) {}
            override suspend fun clearAllFavorites() {}
            override suspend fun getFavoriteCount(): Int = 0
        },
        itemDao = dao
    )

    val allItems: Flow<List<PlaylistItem>> = dao.getAllItems()
    val allSources: Flow<List<PlaylistSource>> = dao.getAllSources()
    val profiles: Flow<List<UserProfile>> = dao.getAllProfiles()
    val settings: Flow<AppSettings?> = dao.getSettings()
    val allEpgPrograms: Flow<List<EpgProgram>> = dao.getAllPrograms()

    fun getPagedItems(pageSize: Int = 50): Flow<PagingData<PlaylistItem>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false, prefetchDistance = 15),
            pagingSourceFactory = { dao.getAllItemsPaged() }
        ).flow
    }

    fun getPagedItemsByType(type: ItemType, pageSize: Int = 50): Flow<PagingData<PlaylistItem>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false, prefetchDistance = 15),
            pagingSourceFactory = { dao.getItemsByTypePaged(type) }
        ).flow
    }

    fun getPagedItemsByTypeAndCategory(type: ItemType, category: String, pageSize: Int = 50): Flow<PagingData<PlaylistItem>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false, prefetchDistance = 15),
            pagingSourceFactory = { dao.getItemsByTypeAndCategoryPaged(type, category) }
        ).flow
    }

    fun getPagedItemsByCategory(category: String, pageSize: Int = 50): Flow<PagingData<PlaylistItem>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false, prefetchDistance = 15),
            pagingSourceFactory = { dao.getItemsByCategoryPaged(category) }
        ).flow
    }

    fun getPagedFavorites(type: ItemType? = null, pageSize: Int = 50): Flow<PagingData<PlaylistItem>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false, prefetchDistance = 15),
            pagingSourceFactory = {
                if (type != null) dao.getFavoritesByTypePaged(type) else dao.getAllFavoritesPaged()
            }
        ).flow
    }

    fun getItemsByType(type: ItemType): Flow<List<PlaylistItem>> = dao.getItemsByType(type)

    fun getItemsByTypeAndCategory(type: ItemType, category: String): Flow<List<PlaylistItem>> =
        dao.getItemsByTypeAndCategory(type, category)

    fun getAllFavorites(): Flow<List<PlaylistItem>> = dao.getAllFavorites()

    fun getFavoritesByType(type: ItemType): Flow<List<PlaylistItem>> = dao.getFavoritesByType(type)

    fun getWatchHistory(): Flow<List<PlaylistItem>> = dao.getWatchHistory()

    fun getDownloads(): Flow<List<PlaylistItem>> = dao.getDownloads()

    fun search(query: String): Flow<List<PlaylistItem>> = dao.searchItems(query)

    fun getRecommendationCandidates(): Flow<List<PlaylistItem>> = dao.getRecommendationCandidates()

    fun getRecentMovies(): Flow<List<PlaylistItem>> = dao.getRecentMovies()

    fun getRecentSeries(): Flow<List<PlaylistItem>> = dao.getRecentSeries()

    fun getPopularMovies(): Flow<List<PlaylistItem>> {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return dao.getPopularMovies(currentYear)
    }

    fun getPopularSeries(): Flow<List<PlaylistItem>> {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return dao.getPopularSeries(currentYear)
    }

    private val _cachedRecentMovies = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val cachedRecentMovies: StateFlow<List<PlaylistItem>> = _cachedRecentMovies.asStateFlow()

    private val _cachedRecentSeries = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val cachedRecentSeries: StateFlow<List<PlaylistItem>> = _cachedRecentSeries.asStateFlow()

    private val _cachedPopularMovies = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val cachedPopularMovies: StateFlow<List<PlaylistItem>> = _cachedPopularMovies.asStateFlow()

    private val _cachedPopularSeries = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val cachedPopularSeries: StateFlow<List<PlaylistItem>> = _cachedPopularSeries.asStateFlow()

    private val _cachedRecommendationsByTab = MutableStateFlow<Map<String, List<PlaylistItem>>>(emptyMap())
    val cachedRecommendationsByTab: StateFlow<Map<String, List<PlaylistItem>>> = _cachedRecommendationsByTab.asStateFlow()

    suspend fun refreshHomeSectionsCache() = withContext(Dispatchers.IO) {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        try {
            dao.backfillMissingMovieMetadata()
            val zeroYearMovies = dao.getMoviesWithZeroReleaseYear()
            for (m in zeroYearMovies) {
                val yr = XtreamClient.extractMovieReleaseYear(m.name)
                if (yr > 0) {
                    dao.updateReleaseYear(m.id, yr)
                }
            }
        } catch (_: Exception) {}

        _cachedRecentMovies.value = dao.getRecentMoviesOnce()
        _cachedRecentSeries.value = dao.getRecentSeriesOnce()
        _cachedPopularMovies.value = dao.getPopularMoviesOnce(currentYear)
        _cachedPopularSeries.value = dao.getPopularSeriesOnce(currentYear)

        val recMap = mutableMapOf<String, List<PlaylistItem>>()
        val history = dao.getWatchHistoryOnce()
        val topGenres = extractTopGenres(history)
        if (topGenres.isNotEmpty()) {
            val g1 = topGenres.getOrNull(0) ?: ""
            val g2 = topGenres.getOrNull(1) ?: g1
            val personalized = dao.getPersonalizedRecommendations(g1, g2)
            recMap["Tümü"] = if (personalized.isNotEmpty()) personalized else dao.getRecommendationsAll()
        } else {
            recMap["Tümü"] = dao.getRecommendationsAll()
        }

        val genreTabs = listOf(
            "Aksiyon", "Komedi", "Dram", "Bilim Kurgu", "Korku",
            "Romantik", "Animasyon", "Belgesel", "Suç", "Macera"
        )
        for (tab in genreTabs) {
            recMap[tab] = dao.getRecommendationsForGenre(tab)
        }
        _cachedRecommendationsByTab.value = recMap
    }

    private fun extractTopGenres(history: List<PlaylistItem>): List<String> {
        if (history.isEmpty()) return emptyList()
        val genreCounts = mutableMapOf<String, Int>()
        for (item in history) {
            val genres = item.genre.split(",", ";", "/", "&").map { it.trim() }.filter { it.isNotBlank() }
            for (g in genres) {
                genreCounts[g] = (genreCounts[g] ?: 0) + 1
            }
        }
        return genreCounts.entries.sortedByDescending { it.value }.map { it.key }.take(2)
    }

    suspend fun getItemById(id: String): PlaylistItem? = withContext(Dispatchers.IO) {
        dao.getItemById(id)
    }

    fun getEpisodesForSeason(seriesId: String, seasonNumber: Int): Flow<List<PlaylistItem>> =
        dao.getEpisodesForSeason(seriesId, seasonNumber)

    fun getSeasonsForSeries(seriesId: String): Flow<List<Int>> = dao.getSeasonsForSeries(seriesId)

    fun getAllEpisodesForSeries(seriesId: String): Flow<List<PlaylistItem>> =
        dao.getAllEpisodesForSeries(seriesId)

    fun getCategoriesByType(type: ItemType): Flow<List<String>> {
        return dao.getCategoriesByType(type).combine(dao.getCategoriesFromPlaylistItems(type)) { fromCatTable, fromItems ->
            if (fromCatTable.isNotEmpty()) fromCatTable else fromItems
        }
    }

    fun getCategoryCountsMap(section: String): Flow<Map<String, Int>> =
        dao.getCategoriesEntitiesByType(section).map { list ->
            list.associate { it.categoryName to it.itemCount }
        }

    fun getCategoryEntities(section: String): Flow<List<com.example.data.model.CategoryEntity>> =
        dao.getCategoriesEntitiesByType(section)

    private var lastCategoryCountTime: Long = 0L

    suspend fun syncCategoryCounts(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val creds = activeXtreamCreds ?: return@withContext false
        val now = System.currentTimeMillis()
        if (!force && (now - lastCategoryCountTime < 6 * 3600 * 1000L)) {
            return@withContext true
        }
        val ok = XtreamClient.calculateCategoryCountsSequentially(
            baseUrl = creds.serverUrl,
            username = creds.username,
            password = creds.password,
            dao = dao
        )
        if (ok) {
            lastCategoryCountTime = now
        }
        ok
    }

    suspend fun toggleFavorite(id: String, currentVal: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(id, !currentVal)
    }

    suspend fun recordWatch(id: String) = withContext(Dispatchers.IO) {
        dao.updateWatchHistory(id, System.currentTimeMillis())
    }

    suspend fun toggleDownload(id: String, isCurrentlyDownloaded: Boolean) = withContext(Dispatchers.IO) {
        val targetState = !isCurrentlyDownloaded
        val path = if (targetState) "/data/user/0/tivions/offline/$id.ts" else null
        dao.setDownloaded(id, targetState, path)
    }

    suspend fun ensureInitialData() = withContext(Dispatchers.IO) {
        // Ensure default settings exist
        dao.saveSettings(DemoDataProvider.defaultSettings)
        // Ensure default profiles exist
        for (p in DemoDataProvider.defaultProfiles) {
            dao.insertProfile(p)
        }
        if (_cachedRecentMovies.value.isEmpty()) {
            try {
                refreshHomeSectionsCache()
            } catch (_: Exception) {}
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        dao.clearAllItems()
        dao.clearAllPrograms()
    }

    suspend fun loadDemoData() = withContext(Dispatchers.IO) {
        dao.clearAllItems()
        val demoItems = DemoDataProvider.getDemoItems()
        demoItems.chunked(250).forEach { chunk ->
            dao.insertAll(chunk)
        }
        dao.insertSource(
            PlaylistSource(
                id = DemoDataProvider.DEMO_SOURCE_ID,
                name = "Demo Koleksiyonu",
                type = SourceType.M3U,
                url = "https://tivions.iptv/demo.m3u"
            )
        )
        // Parse and seed XMLTV EPG schedules
        loadDemoEpgData()
    }

    suspend fun loadDemoEpgData(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val xmlString = DemoDataProvider.generateDemoXmlTv()
            val parseResult = XmlTvParser.parse(xmlString)
            if (parseResult.programs.isNotEmpty()) {
                dao.clearAllPrograms()
                dao.insertPrograms(parseResult.programs)
            }
            Result.success(parseResult.programs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadEpgFromUrl(url: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parseResult = XmlTvParser.fetchAndParse(url)
            if (parseResult.programs.isEmpty()) {
                return@withContext Result.failure(Exception("EPG dosyasında program verisi bulunamadı."))
            }
            dao.clearAllPrograms()
            dao.insertPrograms(parseResult.programs)
            Result.success(parseResult.programs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connectM3u(
        name: String,
        url: String,
        remember: Boolean,
        onProgress: ((Int, String) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Lütfen geçerli bir M3U çalma listesi adresi girin."))
        }
        if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Playlist adresi http:// veya https:// ile başlamalıdır."))
        }

        // M3U URL'sinden Xtream bilgisi otomatik çıkarılır:
        // Eğer sunucu, kullanıcı adı ve şifre mevcutsa doğrudan Xtream Codes API kullanılır.
        val xtreamCreds = com.example.data.parser.XtreamClient.extractXtreamCredentials(cleanUrl)
        if (xtreamCreds != null) {
            android.util.Log.i("IPTV_CONNECTION", "M3U URL'den Xtream Codes API parametreleri tespit edildi. Server: ${xtreamCreds.serverUrl}, User: ${xtreamCreds.username}")
            return@withContext connectXtream(
                name = name.ifBlank { "Xtream Listesi" },
                serverUrl = xtreamCreds.serverUrl,
                username = xtreamCreds.username,
                password = xtreamCreds.password,
                remember = remember,
                onProgress = onProgress
            )
        }

        try {
            val sourceId = "m3u_${UUID.randomUUID()}"
            var totalCount = 0
            var firstChunk = true

            android.util.Log.i("M3U_IMPORT", "Düz M3U içe aktarımı başlatılıyor: $cleanUrl")

            M3uParser.fetchAndParseStreaming(
                url = cleanUrl,
                playlistSourceId = sourceId,
                chunkSize = 300,
                onProgress = { progress ->
                    onProgress?.invoke(progress.parsedCount, progress.lastGroup)
                }
            ).collect { chunk ->
                if (firstChunk) {
                    firstChunk = false
                    dao.clearAllItems()
                }
                dao.insertAll(chunk)
                totalCount += chunk.size
            }

            if (totalCount == 0) {
                return@withContext Result.failure(Exception("M3U dosyasında oynatılabilir geçerli bir kanal veya film bulunamadı."))
            }

            val dbCount = dao.getItemCountBySource(sourceId).let { if (it > 0) it else dao.getItemCount() }
            if (dbCount == totalCount) {
                android.util.Log.i("M3U_IMPORT", "M3U'da $totalCount kayıt bulundu, veritabanına $dbCount kayıt yazıldı ✅")
            } else {
                android.util.Log.w("M3U_IMPORT", "⚠️ UYARI: M3U'da $totalCount kayıt bulundu, veritabanına $dbCount kayıt yazıldı. Fark: ${totalCount - dbCount}")
            }

            if (remember) {
                dao.insertSource(
                    PlaylistSource(
                        id = sourceId,
                        name = name.ifBlank { "M3U Listesi" },
                        type = SourceType.M3U,
                        url = cleanUrl,
                        remember = true
                    )
                )
            }
            Result.success(totalCount)
        } catch (t: Throwable) {
            val errorMsg = if (t.message?.startsWith("İçe aktarma sırasında hata oluştu") == true) {
                t.message!!
            } else {
                "İçe aktarma sırasında hata oluştu: ${t.localizedMessage ?: t.javaClass.simpleName}"
            }
            android.util.Log.e("M3U_IMPORT", errorMsg, t)
            Result.failure(Exception(errorMsg, t))
        }
    }

    suspend fun connectXtream(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
        remember: Boolean,
        onProgress: ((Int, String) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trim().removeSuffix("/")
            if (cleanUrl.isBlank() || username.trim().isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Sunucu adresi ve kullanıcı adı boş bırakılamaz."))
            }

            setXtreamCredentials(cleanUrl, username.trim(), password.trim())

            val sourceId = "xtream_${UUID.randomUUID()}"

            // Kategorileri önceden hafızaya al ve sayıları tek istek + akış ile sıralı hesapla
            try {
                XtreamClient.calculateCategoryCountsSequentially(
                    baseUrl = cleanUrl,
                    username = username.trim(),
                    password = password.trim(),
                    dao = dao
                )
                lastCategoryCountTime = System.currentTimeMillis()

                val liveCats = dao.getCategoriesListByType("LIVE")
                val vodCats = dao.getCategoriesListByType("MOVIE")
                val seriesCats = dao.getCategoriesListByType("SERIES")

                for (cat in liveCats + vodCats + seriesCats) {
                    categoryIdMap[cat.categoryName] = cat.categoryId
                    categoryNameMap[cat.categoryId] = cat.categoryName
                }
            } catch (_: Exception) {}

            val importResult = XtreamClient.streamAndImportAllContent(
                baseUrl = cleanUrl,
                username = username.trim(),
                password = password.trim(),
                playlistSourceId = sourceId,
                dao = dao,
                onProgress = onProgress
            )

            val totalCount = importResult.getOrThrow()
            if (totalCount == 0) {
                return@withContext Result.failure(Exception("Xtream Codes sunucusunda oynatılabilir içerik bulunamadı."))
            }
            // Senkronizasyon bitince anasayfa bölümlerini SQL sorgularıyla bir kere hesapla ve cache'le
            try {
                refreshHomeSectionsCache()
            } catch (e: Exception) {
                Log.e("IptvRepository", "refreshHomeSectionsCache error: ${e.message}", e)
            }
            if (remember) {
                dao.insertSource(
                    PlaylistSource(
                        id = sourceId,
                        name = name.ifBlank { "Xtream Listesi" },
                        type = SourceType.XTREAM,
                        url = cleanUrl,
                        username = username.trim(),
                        password = password.trim(),
                        remember = true
                    )
                )
            }
            Result.success(totalCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcı bir kategoriye girdiğinde ilgili Xtream API endpointini çağırır:
     * - Canlı TV: player_api.php?action=get_live_streams&category_id=...
     * - Film: player_api.php?action=get_vod_streams&category_id=...
     * - Dizi: player_api.php?action=get_series&category_id=...
     */
    suspend fun refreshCategoryStreams(type: ItemType, categoryName: String) = withContext(Dispatchers.IO) {
        val creds = activeXtreamCreds ?: return@withContext
        try {
            val catId = if (categoryName == "Tümü" || categoryName.isBlank()) null else categoryIdMap[categoryName]

            val items = when (type) {
                ItemType.LIVE_TV -> {
                    XtreamClient.fetchLiveStreams(
                        baseUrl = creds.serverUrl,
                        username = creds.username,
                        password = creds.password,
                        categoryId = catId,
                        categoryMap = categoryNameMap
                    )
                }
                ItemType.VOD_MOVIE -> {
                    XtreamClient.fetchVodStreams(
                        baseUrl = creds.serverUrl,
                        username = creds.username,
                        password = creds.password,
                        categoryId = catId,
                        categoryName = categoryName,
                        categoryMap = categoryNameMap
                    )
                }
                ItemType.VOD_SERIES -> {
                    XtreamClient.fetchSeries(
                        baseUrl = creds.serverUrl,
                        username = creds.username,
                        password = creds.password,
                        categoryId = catId,
                        categoryName = categoryName,
                        categoryMap = categoryNameMap
                    )
                }
                else -> emptyList()
            }

            if (items.isNotEmpty()) {
                dao.insertAll(items)
            }
        } catch (e: Exception) {
            android.util.Log.e("IPTV_REPO", "Kategori içerikleri güncellenirken hata ($categoryName): ${e.message}")
        }
    }

    /**
     * Bir filme tıklandığında:
     * player_api.php?action=get_vod_info&vod_id=...&username=...&password=...
     * API sonucundan detayları çeker ve öğeyi zenginleştirir.
     */
    suspend fun fetchVodInfo(item: PlaylistItem): PlaylistItem? = withContext(Dispatchers.IO) {
        val creds = activeXtreamCreds ?: return@withContext null
        try {
            val vodId = item.getXtreamStreamId()
            val details = XtreamClient.fetchVodInfo(
                baseUrl = creds.serverUrl,
                username = creds.username,
                password = creds.password,
                vodId = vodId
            ) ?: return@withContext null

            val poster = details.movieImage ?: details.coverBig ?: details.backdropPath ?: item.logoUrl
            val backdrop = details.backdropPath ?: details.coverBig ?: details.movieImage ?: item.downloadLocalPath
            val overview = details.plot ?: item.description
            val genre = details.genre ?: item.category
            val year = details.releaseDate?.take(4) ?: item.year
            val rating = if (details.rating > 0.0) details.rating else item.rating
            val duration = details.duration ?: item.duration
            val cast = details.cast ?: details.actors ?: details.director ?: item.cast
            val trailer = if (!details.youtubeTrailer.isNullOrBlank()) "yt:${details.youtubeTrailer}" else item.subtitleInfo

            val enriched = item.copy(
                logoUrl = poster,
                description = overview,
                category = genre,
                year = year,
                rating = rating,
                duration = duration,
                cast = cast,
                subtitleInfo = trailer,
                downloadLocalPath = backdrop
            )
            dao.insertItem(enriched)
            enriched
        } catch (e: Exception) {
            android.util.Log.e("IPTV_REPO", "Film detayları çekilirken hata: ${e.message}")
            null
        }
    }

    /**
     * Bir diziye tıklandığında:
     * player_api.php?action=get_series_info&series_id=...&username=...&password=...
     * API sonucundan dizi bilgileri, sezonlar ve bölümler oluşturulur.
     */
    suspend fun fetchSeriesInfo(item: PlaylistItem): PlaylistItem? = withContext(Dispatchers.IO) {
        val creds = activeXtreamCreds ?: return@withContext null
        try {
            val sId = item.getXtreamSeriesId()
            val details = XtreamClient.fetchSeriesInfo(
                baseUrl = creds.serverUrl,
                username = creds.username,
                password = creds.password,
                seriesId = sId,
                seriesName = item.name,
                playlistSourceId = item.playlistSourceId
            ) ?: return@withContext null

            val sRoomId = item.seriesId ?: item.id

            // Eski bölümleri temizleyip API'den gelen gerçek bölümleri ekle
            dao.deleteEpisodesForSeries(sRoomId)

            val episodeEntities = details.episodes.map { ep ->
                PlaylistItem(
                    id = "xtream_ep_${ep.id}_${ep.seasonNum}_${ep.episodeNum}",
                    name = ep.title,
                    logoUrl = ep.logoUrl ?: details.cover ?: item.logoUrl,
                    streamUrl = ep.streamUrl,
                    category = details.genre ?: item.category,
                    type = ItemType.EPISODE,
                    rating = details.rating,
                    year = details.releaseDate?.take(4) ?: item.year,
                    duration = ep.duration,
                    description = ep.plot,
                    playlistSourceId = item.playlistSourceId,
                    seriesId = sRoomId,
                    seasonNumber = ep.seasonNum,
                    episodeNumber = ep.episodeNum
                )
            }

            if (episodeEntities.isNotEmpty()) {
                dao.insertAll(episodeEntities)
            }

            val poster = details.cover ?: item.logoUrl
            val backdrop = details.backdrop ?: details.cover ?: item.downloadLocalPath
            val overview = details.plot ?: item.description
            val genre = details.genre ?: item.category
            val year = details.releaseDate?.take(4) ?: item.year
            val rating = if (details.rating > 0.0) details.rating else item.rating
            val cast = details.cast ?: details.director ?: item.cast
            val maxSeason = details.seasons.maxOrNull() ?: 1

            val enriched = item.copy(
                logoUrl = poster,
                description = overview,
                category = genre,
                year = year,
                rating = rating,
                cast = cast,
                downloadLocalPath = backdrop,
                seasonNumber = maxSeason
            )
            dao.insertItem(enriched)
            enriched
        } catch (e: Exception) {
            android.util.Log.e("IPTV_REPO", "Dizi detayları çekilirken hata: ${e.message}")
            null
        }
    }

    // Personalized recommendation algorithm
    fun getPersonalizedRecommendations(): Flow<List<PlaylistItem>> {
        return combine(dao.getRecommendationCandidates(), dao.getWatchHistory(), dao.getAllFavorites()) { candidates, history, favorites ->
            val favoriteCategories = (history.map { it.category } + favorites.map { it.category })
                .groupingBy { it }.eachCount()

            candidates.filter { it.type != ItemType.EPISODE }
                .map { item ->
                    var score = item.rating * 1.5
                    val catBonus = favoriteCategories[item.category] ?: 0
                    score += catBonus * 3.0
                    if (item.isFavorite) score += 5.0
                    if (item.watchCount > 0) score += 2.0
                    Pair(item, score)
                }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(12)
        }
    }

    suspend fun setActiveProfile(profileId: String) = withContext(Dispatchers.IO) {
        dao.setActiveProfile(profileId)
    }

    suspend fun createProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        dao.insertProfile(profile)
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dao.saveSettings(settings)
    }

    // BÖLÜM 7: Continue Watching
    fun getContinueWatching(): Flow<List<com.example.data.model.WatchRecord>> = dao.getContinueWatching()

    suspend fun deleteWatchRecord(id: String) = withContext(Dispatchers.IO) {
        dao.deleteWatchRecord(id)
    }

    suspend fun recordWatchProgress(item: PlaylistItem, currentPosMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val dur = if (durationMs > 0) durationMs else 3600000L
        val prog = (currentPosMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        val m3uKey = com.example.util.TitleNormalizer.normalizeTitle(item.name)
        val meta = com.example.util.TitleNormalizer.extractMovieMeta(item.name)

        val record = com.example.data.model.WatchRecord(
            id = item.id,
            m3uKey = m3uKey,
            itemId = item.id,
            title = meta.cleanTitle,
            type = if (item.type == ItemType.VOD_SERIES || item.type == ItemType.EPISODE) "tv" else "movie",
            progress = prog,
            isFavorite = item.isFavorite,
            lastWatchedAt = System.currentTimeMillis(),
            quality = meta.quality ?: "HD",
            year = meta.year ?: item.year,
            seriesKey = item.seriesId ?: m3uKey,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
            stillUrl = item.logoUrl,
            streamUrl = item.streamUrl
        )
        dao.insertWatchRecord(record)
        dao.updateWatchHistory(item.id, System.currentTimeMillis())

        if (item.type == ItemType.EPISODE && item.seriesId != null) {
            dao.saveEpisodeProgress(
                com.example.data.model.EpisodeProgress(
                    seriesKey = item.seriesId,
                    seasonNumber = item.seasonNumber,
                    episodeNumber = item.episodeNumber,
                    progress = prog
                )
            )
        }
    }

    // BÖLÜM 8 & 9: Library Item Meta (Recently Added)
    suspend fun syncLibraryMeta(playlistId: String, items: List<PlaylistItem>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val metaList = items.filter { it.type != ItemType.LIVE_TV }.map { item ->
            val key = com.example.util.TitleNormalizer.normalizeTitle(item.name)
            val type = if (item.type == ItemType.VOD_SERIES || item.type == ItemType.EPISODE) "tv" else "movie"
            com.example.data.model.LibraryItemMeta(
                playlistId = playlistId,
                itemKey = key,
                type = type,
                firstSeenAt = now,
                lastSeenAt = now
            )
        }.distinctBy { "${it.playlistId}_${it.itemKey}" }

        if (metaList.isNotEmpty()) {
            dao.insertLibraryItemMeta(metaList)
        }
    }

    // BÖLÜM 14: Category Preferences
    fun getCategoryPrefs(playlistId: String, section: String): Flow<List<com.example.data.model.CategoryPref>> =
        dao.getCategoryPrefs(playlistId, section)

    fun getHiddenCategories(playlistId: String, section: String): Flow<List<String>> =
        dao.getHiddenCategories(playlistId, section)

    suspend fun setCategoryHidden(playlistId: String, section: String, categoryKey: String, hidden: Boolean) = withContext(Dispatchers.IO) {
        dao.updateCategoryHidden(section, categoryKey, hidden)
        val normalized = categoryKey.trim().lowercase(java.util.Locale("tr"))
        val existing = dao.getCategoryPref(playlistId, section, normalized)
        if (existing != null) {
            dao.saveCategoryPref(existing.copy(hidden = hidden, updatedAt = System.currentTimeMillis()))
        } else {
            dao.saveCategoryPref(
                com.example.data.model.CategoryPref(
                    playlistId = playlistId,
                    section = section,
                    categoryKey = normalized,
                    position = 9999,
                    hidden = hidden,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun reorderCategories(playlistId: String, section: String, orderedCategoryKeys: List<String>) = withContext(Dispatchers.IO) {
        orderedCategoryKeys.forEachIndexed { index, catName ->
            dao.updateCategoryUserOrder(section, catName, index)
        }
        val prefs = orderedCategoryKeys.mapIndexed { index, catKey ->
            val normalized = catKey.trim().lowercase(java.util.Locale("tr"))
            val existing = dao.getCategoryPref(playlistId, section, normalized)
            com.example.data.model.CategoryPref(
                playlistId = playlistId,
                section = section,
                categoryKey = normalized,
                position = index,
                hidden = existing?.hidden ?: false,
                updatedAt = System.currentTimeMillis()
            )
        }
        dao.saveCategoryPrefs(prefs)
    }

    suspend fun resetCategoryPrefs(playlistId: String, section: String) = withContext(Dispatchers.IO) {
        dao.resetCategoryUserOrder(section)
        dao.resetCategoryPrefs(playlistId, section)
    }

    suspend fun clearWatchHistoryForType(type: ItemType) = withContext(Dispatchers.IO) {
        dao.clearWatchHistoryForType(type)
    }

    suspend fun clearItemWatchHistory(id: String) = withContext(Dispatchers.IO) {
        dao.clearItemWatchHistory(id)
        dao.deleteWatchRecord(id)
    }

    suspend fun deleteDownloadedItem(item: PlaylistItem) = withContext(Dispatchers.IO) {
        if (!item.downloadLocalPath.isNullOrBlank()) {
            try {
                val file = java.io.File(item.downloadLocalPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
        dao.clearItemDownload(item.id)
    }

    suspend fun clearAllDownloadedItems(items: List<PlaylistItem>) = withContext(Dispatchers.IO) {
        for (item in items) {
            if (!item.downloadLocalPath.isNullOrBlank()) {
                try {
                    val file = java.io.File(item.downloadLocalPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
        }
        dao.clearDownloadsForType(ItemType.VOD_MOVIE)
    }

    suspend fun clearAllDownloadedSeries(items: List<PlaylistItem>) = withContext(Dispatchers.IO) {
        for (item in items) {
            if (!item.downloadLocalPath.isNullOrBlank()) {
                try {
                    val file = java.io.File(item.downloadLocalPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
        }
        dao.clearDownloadsForType(ItemType.VOD_SERIES)
        dao.clearDownloadsForType(ItemType.EPISODE)
    }

    // Reminders
    fun getAllReminders(): Flow<List<com.example.data.model.ProgramReminder>> = dao.getAllReminders()

    suspend fun addReminder(reminder: com.example.data.model.ProgramReminder) = withContext(Dispatchers.IO) {
        dao.insertReminder(reminder)
    }

    suspend fun removeReminder(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteReminder(id)
    }
}

fun PlaylistItem.getXtreamStreamId(): String {
    return if (id.startsWith("xtream_vod_")) id.removePrefix("xtream_vod_")
    else if (id.startsWith("xtream_live_")) id.removePrefix("xtream_live_")
    else id.substringAfterLast("_")
}

fun PlaylistItem.getXtreamSeriesId(): String {
    val raw = seriesId ?: id
    return if (raw.startsWith("series_xtream_")) raw.removePrefix("series_xtream_")
    else if (raw.startsWith("series_")) raw.removePrefix("series_")
    else raw.substringAfterLast("_")
}
