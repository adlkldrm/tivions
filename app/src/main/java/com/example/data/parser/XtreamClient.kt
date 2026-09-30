package com.example.data.parser

import android.util.Base64
import android.util.JsonReader
import android.util.Log
import com.example.data.local.ItemDao
import com.example.data.model.CategoryCountEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

data class XtreamCategory(
    val categoryId: String,
    val categoryName: String,
    val type: ItemType,
    val orderIndex: Int = 0
)

data class XtreamVodDetails(
    val name: String?,
    val movieImage: String?,
    val coverBig: String?,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: Double,
    val duration: String?,
    val director: String?,
    val actors: String?,
    val cast: String?,
    val country: String?,
    val youtubeTrailer: String?,
    val backdropPath: String?,
    val quality: String = "HD",
    val tmdbId: String? = null
)

data class XtreamEpisodeInfo(
    val id: String,
    val seasonNum: Int,
    val episodeNum: Int,
    val title: String,
    val streamUrl: String,
    val logoUrl: String?,
    val duration: String,
    val plot: String
)

data class XtreamSeriesDetails(
    val name: String?,
    val cover: String?,
    val backdrop: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: Double,
    val seasons: List<Int>,
    val episodes: List<XtreamEpisodeInfo>,
    val youtubeTrailer: String? = null,
    val episodeRunTime: String? = null
)

data class XtreamCardMeta(
    val title: String,
    val genre: String,
    val year: String,
    val rating: Double,
    val quality: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null
)

object XtreamClient {
    private const val TAG = "XtreamClient"

    // readTimeout >= 120s as specified
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Concurrency limiter for visible card lazy enrichment (max 3 concurrent requests)
    private val cardEnrichmentSemaphore = Semaphore(3)
    private val cardMetaCache = ConcurrentHashMap<String, XtreamCardMeta>()

    val MOVIE_NAME_YEAR_REGEX = Regex("""\((\d{4})\)\s*$""")

    fun extractMovieReleaseYear(name: String): Int {
        val match = MOVIE_NAME_YEAR_REGEX.find(name)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    /**
     * M3U URL'den server, port, username, password bilgilerini ayıklar.
     * Kullanıcı get.php?username=U&password=P... formatında link girerse Xtream modudur.
     */
    fun extractXtreamCredentials(url: String): XtreamCredentials? {
        try {
            val trimmed = url.trim()
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") return null
            val host = uri.host ?: return null
            val port = uri.port
            val server = if (port != -1 && port != 80 && port != 443) {
                "$scheme://$host:$port"
            } else {
                "$scheme://$host"
            }

            // 1. Query parameters
            val query = uri.rawQuery ?: ""
            if (query.isNotBlank()) {
                val params = mutableMapOf<String, String>()
                for (pair in query.split("&")) {
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        try {
                            val key = URLDecoder.decode(parts[0], "UTF-8").lowercase()
                            val value = URLDecoder.decode(parts[1], "UTF-8")
                            params[key] = value
                        } catch (_: Exception) {}
                    }
                }
                val user = params["username"] ?: params["user"]
                val pass = params["password"] ?: params["pass"]
                if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
                    return XtreamCredentials(server, user, pass)
                }
            }

            // 2. Path fallback: /live/USER/PASS/
            val path = uri.path ?: ""
            val segments = path.split("/").filter { it.isNotBlank() }
            if (segments.size >= 3 && (segments[0].equals("live", true) || segments[0].equals("movie", true) || segments[0].equals("series", true))) {
                val user = segments[1]
                val pass = segments[2]
                if (user.isNotBlank() && pass.isNotBlank() && !user.contains(".") && !pass.contains(".")) {
                    return XtreamCredentials(server, user, pass)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Xtream bilgileri URL'den çözümlenemedi: ${e.message}")
        }
        return null
    }

    /**
     * Güvenli ve tek '&' içeren API URL oluşturucu.
     */
    fun buildApiUrl(
        baseUrl: String,
        action: String? = null,
        params: Map<String, String> = emptyMap(),
        username: String,
        password: String
    ): String {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val encodedUser = URLEncoder.encode(username, "UTF-8")
        val encodedPass = URLEncoder.encode(password, "UTF-8")

        val queryParts = mutableListOf<String>()
        if (!action.isNullOrBlank()) {
            queryParts.add("action=$action")
        }
        for ((key, value) in params) {
            if (value.isNotBlank()) {
                queryParts.add("$key=${URLEncoder.encode(value, "UTF-8")}")
            }
        }
        queryParts.add("username=$encodedUser")
        queryParts.add("password=$encodedPass")

        return "$cleanBase/player_api.php?" + queryParts.joinToString("&")
    }

    /**
     * Oynatma URL'leri:
     * Canlı: {base}/live/U/P/{stream_id}.ts
     * Film: {base}/movie/U/P/{stream_id}.{container_extension}
     * Dizi bölümü: {base}/series/U/P/{episode.id}.{episode.container_extension}
     */
    fun buildLiveStreamUrl(baseUrl: String, user: String, pass: String, streamId: String): String {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        return "$cleanBase/live/$user/$pass/$streamId.ts"
    }

    fun buildVodStreamUrl(baseUrl: String, user: String, pass: String, streamId: String, extension: String?): String {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val ext = extension?.trim()?.removePrefix(".")?.ifBlank { "mp4" } ?: "mp4"
        return "$cleanBase/movie/$user/$pass/$streamId.$ext"
    }

    fun buildSeriesStreamUrl(baseUrl: String, user: String, pass: String, episodeId: String, extension: String?): String {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val ext = extension?.trim()?.removePrefix(".")?.ifBlank { "mp4" } ?: "mp4"
        return "$cleanBase/series/$user/$pass/$episodeId.$ext"
    }

    suspend fun authenticate(baseUrl: String, user: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        val url = buildApiUrl(baseUrl, username = user, password = pass)
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: return@withContext false
                if (body.startsWith("{")) {
                    val root = JSONObject(body)
                    val userInfo = root.optJSONObject("user_info")
                    if (userInfo != null) {
                        val auth = userInfo.optInt("auth", 1)
                        val status = userInfo.optString("status")
                        if (auth == 0 || status.equals("Disabled", true) || status.equals("Expired", true)) {
                            return@withContext false
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth request error: ${e.message}")
            false
        }
    }

    /**
     * Kategorileri API dizisindeki sırayla çeker ve orderIndex atar (alfabetik sıralama yapılmaz).
     */
    suspend fun fetchCategories(
        baseUrl: String,
        username: String,
        password: String,
        type: ItemType
    ): List<XtreamCategory> = withContext(Dispatchers.IO) {
        val action = when (type) {
            ItemType.LIVE_TV -> "get_live_categories"
            ItemType.VOD_MOVIE -> "get_vod_categories"
            ItemType.VOD_SERIES, ItemType.EPISODE -> "get_series_categories"
        }

        val url = buildApiUrl(baseUrl, action = action, username = username, password = password)
        val list = mutableListOf<XtreamCategory>()

        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                if (body.startsWith("[")) {
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.optString("category_id")
                        val name = obj.optString("category_name")
                        if (id.isNotBlank() && name.isNotBlank()) {
                            list.add(XtreamCategory(id, name, type, orderIndex = i))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Categories fetch error ($action): ${e.message}")
        }
        list
    }

    /**
     * Kategori sayılarını (itemCount) tek istek + JsonReader akışı ile hesaplar.
     * Sıralı (sequential) çalışır: önce canlı, sonra film, sonra dizi.
     * Kategori bazlı istek atılmaz; get_live_streams, get_vod_streams, get_series
     * tek seferde akış olarak okunup HashMap<String, Int> sayaçta artırılır.
     * Akış bitince toplu veritabanı güncellemesi yapılır.
     */
    suspend fun calculateCategoryCountsSequentially(
        baseUrl: String,
        username: String,
        password: String,
        dao: ItemDao,
        onError: ((String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        try {
            // 1. Önce kategorileri çek ve orderIndex ile kaydet (sıralama garantisi)
            val liveCats = fetchCategories(cleanBase, username, password, ItemType.LIVE_TV)
            val movieCats = fetchCategories(cleanBase, username, password, ItemType.VOD_MOVIE)
            val seriesCats = fetchCategories(cleanBase, username, password, ItemType.VOD_SERIES)

            saveCategoriesWithApiOrder(dao, "LIVE", liveCats)
            saveCategoriesWithApiOrder(dao, "MOVIE", movieCats)
            saveCategoriesWithApiOrder(dao, "SERIES", seriesCats)

            // 2. Sıralı olarak sayıları hesapla (çakışma olmaması için sıralı):
            // a) Canlı TV
            val liveSuccess = countItemsForAction(
                action = "get_live_streams",
                section = "LIVE",
                categories = liveCats,
                baseUrl = cleanBase,
                username = username,
                password = password,
                dao = dao
            )

            // b) Filmler
            val movieSuccess = countItemsForAction(
                action = "get_vod_streams",
                section = "MOVIE",
                categories = movieCats,
                baseUrl = cleanBase,
                username = username,
                password = password,
                dao = dao
            )

            // c) Diziler
            val seriesSuccess = countItemsForAction(
                action = "get_series",
                section = "SERIES",
                categories = seriesCats,
                baseUrl = cleanBase,
                username = username,
                password = password,
                dao = dao
            )

            val allOk = liveSuccess && movieSuccess && seriesSuccess
            if (!allOk) {
                onError?.invoke("Bazı kategori sayıları güncellenemedi.")
            }
            allOk
        } catch (e: Exception) {
            Log.e(TAG, "calculateCategoryCountsSequentially genel hata: ${e.message}", e)
            onError?.invoke("Kategori sayıları güncellenemedi: ${e.message}")
            false
        }
    }

    suspend fun saveCategoriesWithApiOrder(
        dao: ItemDao,
        type: String,
        cats: List<XtreamCategory>
    ) {
        if (cats.isEmpty()) return
        val existing = dao.getCategoriesListByType(type).associateBy { it.categoryId }
        val entities = cats.mapIndexed { index, cat ->
            val prev = existing[cat.categoryId]
            CategoryEntity(
                type = type,
                categoryId = cat.categoryId,
                categoryName = cat.categoryName,
                orderIndex = index,
                userOrderIndex = prev?.userOrderIndex,
                itemCount = prev?.itemCount ?: 0,
                isHidden = prev?.isHidden ?: false
            )
        }
        dao.insertCategories(entities)
    }

    private suspend fun countItemsForAction(
        action: String,
        section: String,
        categories: List<XtreamCategory>,
        baseUrl: String,
        username: String,
        password: String,
        dao: ItemDao
    ): Boolean {
        if (categories.isEmpty()) return true
        val url = buildApiUrl(baseUrl, action = action, username = username, password = password)
        var attempts = 0
        var success = false
        val maxAttempts = 3

        while (attempts < maxAttempts && !success) {
            attempts++
            val counts = HashMap<String, Int>()
            try {
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code} ($action)")
                    }
                    val body = response.body ?: throw Exception("Boş gövde ($action)")
                    val charStream = BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8), 32768)
                    val jsonReader = JsonReader(charStream)

                    if (jsonReader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                        jsonReader.beginArray()
                        while (jsonReader.hasNext()) {
                            jsonReader.beginObject()
                            var catId = ""
                            while (jsonReader.hasNext()) {
                                if (jsonReader.nextName() == "category_id") {
                                    catId = jsonReader.nextStringSafe()
                                } else {
                                    jsonReader.skipValue()
                                }
                            }
                            jsonReader.endObject()
                            if (catId.isNotBlank()) {
                                counts[catId] = (counts[catId] ?: 0) + 1
                            }
                        }
                        jsonReader.endArray()
                    }
                    jsonReader.close()
                }

                // Toplu veritabanı güncellemesi (tüm kategoriler için itemCount doldurulur, bulunamayanlar 0)
                val existing = dao.getCategoriesListByType(section).associateBy { it.categoryId }
                val updatedEntities = categories.mapIndexed { index, cat ->
                    val c = counts[cat.categoryId] ?: 0
                    val prev = existing[cat.categoryId]
                    CategoryEntity(
                        type = section,
                        categoryId = cat.categoryId,
                        categoryName = cat.categoryName,
                        orderIndex = index,
                        userOrderIndex = prev?.userOrderIndex,
                        itemCount = c,
                        isHidden = prev?.isHidden ?: false
                    )
                }
                dao.insertCategories(updatedEntities)

                // CategoryCountEntity (geriye dönük uyumluluk için)
                val legacyCounts = categories.mapIndexed { index, cat ->
                    val c = counts[cat.categoryId] ?: 0
                    CategoryCountEntity(
                        section = section,
                        category = cat.categoryName,
                        categoryId = cat.categoryId,
                        itemCount = c,
                        orderIndex = index
                    )
                }
                dao.insertCategoryCounts(legacyCounts)

                Log.i(TAG, "$section için kategori sayıları güncellendi (${categories.size} kategori)")
                success = true
            } catch (e: Exception) {
                Log.w(TAG, "$section kategori sayım deneme $attempts/$maxAttempts başarısız: ${e.message}")
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(1000L * attempts)
                }
            }
        }
        return success
    }

    /**
     * 16.000+ içerikte OutOfMemoryError oluşmaz, category_counts tablosundaki itemCount'ları günceller
     * ve toplam kayıt sayısını API verisiyle doğrular.
     */
    suspend fun streamAndImportAllContent(
        baseUrl: String,
        username: String,
        password: String,
        playlistSourceId: String,
        dao: ItemDao,
        onProgress: ((Int, String) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        var totalImported = 0

        try {
            // 1. Kategorileri çek ve API sırasıyla kaydet
            val liveCategories = fetchCategories(cleanBase, username, password, ItemType.LIVE_TV)
            val vodCategories = fetchCategories(cleanBase, username, password, ItemType.VOD_MOVIE)
            val seriesCategories = fetchCategories(cleanBase, username, password, ItemType.VOD_SERIES)

            val liveCatMap = liveCategories.associate { it.categoryId to it.categoryName }
            val vodCatMap = vodCategories.associate { it.categoryId to it.categoryName }
            val seriesCatMap = seriesCategories.associate { it.categoryId to it.categoryName }

            val categoryItemCounters = ConcurrentHashMap<String, Int>()

            // 2. Canlı TV Akışlarını O(1) Streaming ile oku ve 400'lük partiler halinde yaz
            totalImported += streamActionItems(
                url = buildApiUrl(cleanBase, action = "get_live_streams", username = username, password = password),
                type = ItemType.LIVE_TV,
                catMap = liveCatMap,
                counters = categoryItemCounters,
                baseUrl = cleanBase,
                username = username,
                password = password,
                playlistSourceId = playlistSourceId,
                dao = dao,
                startingIndex = totalImported,
                onProgress = onProgress
            )

            // 3. Film Akışlarını Streaming ile oku
            totalImported += streamActionItems(
                url = buildApiUrl(cleanBase, action = "get_vod_streams", username = username, password = password),
                type = ItemType.VOD_MOVIE,
                catMap = vodCatMap,
                counters = categoryItemCounters,
                baseUrl = cleanBase,
                username = username,
                password = password,
                playlistSourceId = playlistSourceId,
                dao = dao,
                startingIndex = totalImported,
                onProgress = onProgress
            )

            // 4. Dizi Akışlarını Streaming ile oku (bölümler bu aşamada çekilmez, sadece dizi kartı)
            totalImported += streamActionItems(
                url = buildApiUrl(cleanBase, action = "get_series", username = username, password = password),
                type = ItemType.VOD_SERIES,
                catMap = seriesCatMap,
                counters = categoryItemCounters,
                baseUrl = cleanBase,
                username = username,
                password = password,
                playlistSourceId = playlistSourceId,
                dao = dao,
                startingIndex = totalImported,
                onProgress = onProgress
            )

            // 5. Kategori sayıları tablosunu güncelle (API sırası korunarak)
            val countEntities = mutableListOf<CategoryCountEntity>()
            for (cat in liveCategories) {
                val count = categoryItemCounters[cat.categoryId] ?: 0
                countEntities.add(CategoryCountEntity(section = "LIVE", category = cat.categoryName, categoryId = cat.categoryId, itemCount = count, orderIndex = cat.orderIndex))
            }
            for (cat in vodCategories) {
                val count = categoryItemCounters[cat.categoryId] ?: 0
                countEntities.add(CategoryCountEntity(section = "MOVIE", category = cat.categoryName, categoryId = cat.categoryId, itemCount = count, orderIndex = cat.orderIndex))
            }
            for (cat in seriesCategories) {
                val count = categoryItemCounters[cat.categoryId] ?: 0
                countEntities.add(CategoryCountEntity(section = "SERIES", category = cat.categoryName, categoryId = cat.categoryId, itemCount = count, orderIndex = cat.orderIndex))
            }
            if (countEntities.isNotEmpty()) {
                dao.insertCategoryCounts(countEntities)
            }

            val categoryEntities = mutableListOf<CategoryEntity>()
            for (cat in liveCategories) {
                val count = categoryItemCounters[cat.categoryId] ?: 0
                categoryEntities.add(CategoryEntity(type = "LIVE", categoryId = cat.categoryId, categoryName = cat.categoryName, itemCount = count, orderIndex = cat.orderIndex))
            }
            for (cat in vodCategories) {
                val count = categoryItemCounters[cat.categoryId] ?: 0
                categoryEntities.add(CategoryEntity(type = "MOVIE", categoryId = cat.categoryId, categoryName = cat.categoryName, itemCount = count, orderIndex = cat.orderIndex))
            }
            for (cat in seriesCategories) {
                val count = categoryItemCounters[cat.categoryId] ?: 0
                categoryEntities.add(CategoryEntity(type = "SERIES", categoryId = cat.categoryId, categoryName = cat.categoryName, itemCount = count, orderIndex = cat.orderIndex))
            }
            if (categoryEntities.isNotEmpty()) {
                dao.insertCategories(categoryEntities)
            }

            // 6. Toplam yazılan kayıt sayısını doğrula
            val dbCount = dao.getItemCountBySource(playlistSourceId)
            Log.i(TAG, "Import doğrulaması: Okunan = $totalImported, Room DB = $dbCount")

            Result.success(totalImported)
        } catch (e: Exception) {
            Log.e(TAG, "Streaming import hatası: ${e.message}", e)
            Result.failure(Exception("Yayın listesi indirilirken hata oluştu: ${e.message}"))
        }
    }

    private suspend fun streamActionItems(
        url: String,
        type: ItemType,
        catMap: Map<String, String>,
        counters: ConcurrentHashMap<String, Int>,
        baseUrl: String,
        username: String,
        password: String,
        playlistSourceId: String,
        dao: ItemDao,
        startingIndex: Int,
        onProgress: ((Int, String) -> Unit)?
    ): Int {
        var count = 0
        val batch = ArrayList<PlaylistItem>(400)
        val req = Request.Builder().url(url).build()

        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code} yanıtı alındı")
            }
            val body = response.body ?: throw Exception("Boş yanıt gövdesi")
            val charStream = BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8), 32768)
            val jsonReader = JsonReader(charStream)

            if (jsonReader.peek() != android.util.JsonToken.BEGIN_ARRAY) {
                return 0
            }

            jsonReader.beginArray()
            var streamItemIndex = 0
            while (jsonReader.hasNext()) {
                jsonReader.beginObject()

                var streamId = ""
                var name = ""
                var streamIcon: String? = null
                var epgChannelId: String? = null
                var categoryId = ""
                var ratingValue = 0.0
                var rating5based = 0.0
                var releaseYear = 0
                var genre = ""
                var containerExtension = "mp4"

                while (jsonReader.hasNext()) {
                    val propName = jsonReader.nextName()
                    when (propName) {
                        "stream_id", "series_id" -> streamId = jsonReader.nextStringSafe()
                        "name", "title" -> name = jsonReader.nextStringSafe()
                        "stream_icon", "cover" -> streamIcon = jsonReader.nextStringSafe().takeIf { it.isNotBlank() }
                        "epg_channel_id" -> epgChannelId = jsonReader.nextStringSafe().takeIf { it.isNotBlank() }
                        "category_id" -> categoryId = jsonReader.nextStringSafe()
                        "rating" -> ratingValue = jsonReader.nextDoubleSafe()
                        "rating_5based" -> rating5based = jsonReader.nextDoubleSafe()
                        "releasedate", "releaseDate", "release_date", "year" -> {
                            val yr = jsonReader.nextStringSafe()
                            val match = Regex("""\b(19\d\d|20\d\d)\b""").find(yr)
                            if (match != null) {
                                releaseYear = match.value.toIntOrNull() ?: 0
                            }
                        }
                        "genre", "genres" -> genre = jsonReader.nextStringSafe()
                        "container_extension" -> containerExtension = jsonReader.nextStringSafe().ifBlank { "mp4" }
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                if (streamId.isNotBlank() && name.isNotBlank()) {
                    count++
                    counters[categoryId] = (counters[categoryId] ?: 0) + 1

                    val categoryName = catMap[categoryId] ?: "Genel"
                    val globalIndex = startingIndex + count
                    val currentGlobalOrder = streamItemIndex++
                    val displayRating = if (ratingValue > 0.0) ratingValue else if (rating5based > 0.0) rating5based * 2.0 else 0.0
                    val movieReleaseYear = if (type == ItemType.VOD_MOVIE) extractMovieReleaseYear(name) else releaseYear
                    val displayYear = if (movieReleaseYear > 0) movieReleaseYear.toString() else "2024"

                    val item = when (type) {
                        ItemType.LIVE_TV -> PlaylistItem(
                            id = "xtream_live_$streamId",
                            name = name,
                            logoUrl = streamIcon,
                            streamUrl = buildLiveStreamUrl(baseUrl, username, password, streamId),
                            category = categoryName,
                            type = ItemType.LIVE_TV,
                            rating = 0.0,
                            year = displayYear,
                            duration = "",
                            description = "Canlı TV Yayını",
                            playlistSourceId = playlistSourceId,
                            epgChannelId = epgChannelId,
                            categoryId = categoryId,
                            orderIndex = globalIndex,
                            ratingValue = 0.0,
                            rating5based = 0.0,
                            releaseYear = 0,
                            globalOrderIndex = currentGlobalOrder,
                            genre = genre
                        )
                        ItemType.VOD_MOVIE -> PlaylistItem(
                            id = "xtream_vod_$streamId",
                            name = name,
                            logoUrl = streamIcon,
                            streamUrl = buildVodStreamUrl(baseUrl, username, password, streamId, containerExtension),
                            category = categoryName,
                            type = ItemType.VOD_MOVIE,
                            rating = displayRating,
                            year = displayYear,
                            duration = "115 dk",
                            description = "$name Filmi",
                            playlistSourceId = playlistSourceId,
                            categoryId = categoryId,
                            orderIndex = globalIndex,
                            ratingValue = ratingValue,
                            rating5based = rating5based,
                            releaseYear = movieReleaseYear,
                            globalOrderIndex = currentGlobalOrder,
                            genre = genre
                        )
                        ItemType.VOD_SERIES, ItemType.EPISODE -> PlaylistItem(
                            id = "series_xtream_$streamId",
                            name = name,
                            logoUrl = streamIcon,
                            streamUrl = "",
                            category = categoryName,
                            type = ItemType.VOD_SERIES,
                            rating = displayRating,
                            year = displayYear,
                            duration = "Dizi",
                            description = "$name Dizisi",
                            playlistSourceId = playlistSourceId,
                            seriesId = "series_xtream_$streamId",
                            categoryId = categoryId,
                            orderIndex = globalIndex,
                            ratingValue = ratingValue,
                            rating5based = rating5based,
                            releaseYear = releaseYear,
                            globalOrderIndex = currentGlobalOrder,
                            genre = genre
                        )
                    }

                    batch.add(item)
                    if (batch.size >= 400) {
                        dao.insertAll(batch)
                        batch.clear()
                        onProgress?.invoke(startingIndex + count, categoryName)
                    }
                }
            }
            jsonReader.endArray()
            jsonReader.close()
        }

        if (batch.isNotEmpty()) {
            dao.insertAll(batch)
            batch.clear()
        }

        return count
    }

    /**
     * Canlı TV kategorisine girildiğinde:
     * player_api.php?action=get_live_streams&category_id=X&username=U&password=P
     */
    suspend fun fetchLiveStreams(
        baseUrl: String,
        username: String,
        password: String,
        categoryId: String? = null,
        categoryMap: Map<String, String> = emptyMap(),
        playlistSourceId: String = "default"
    ): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val params = if (!categoryId.isNullOrBlank()) mapOf("category_id" to categoryId) else emptyMap()
        val url = buildApiUrl(baseUrl, action = "get_live_streams", params = params, username = username, password = password)
        val list = mutableListOf<PlaylistItem>()
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body ?: return@withContext emptyList()
                val charStream = BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8), 32768)
                val jsonReader = JsonReader(charStream)
                if (jsonReader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                jsonReader.beginArray()
                var idx = 0
                while (jsonReader.hasNext()) {
                    jsonReader.beginObject()
                    var streamId = ""
                    var name = ""
                    var streamIcon: String? = null
                    var epgChannelId: String? = null
                    var catId = categoryId ?: ""
                    while (jsonReader.hasNext()) {
                        when (jsonReader.nextName()) {
                            "stream_id" -> streamId = jsonReader.nextStringSafe()
                            "name" -> name = jsonReader.nextStringSafe()
                            "stream_icon" -> streamIcon = jsonReader.nextStringSafe().takeIf { it.isNotBlank() }
                            "epg_channel_id" -> epgChannelId = jsonReader.nextStringSafe().takeIf { it.isNotBlank() }
                            "category_id" -> catId = jsonReader.nextStringSafe().ifBlank { catId }
                            else -> jsonReader.skipValue()
                        }
                    }
                    jsonReader.endObject()
                    if (streamId.isNotBlank() && name.isNotBlank()) {
                        idx++
                        val catName = categoryMap[catId] ?: "Genel"
                        list.add(
                            PlaylistItem(
                                id = "xtream_live_$streamId",
                                name = name,
                                logoUrl = streamIcon,
                                streamUrl = buildLiveStreamUrl(baseUrl, username, password, streamId),
                                category = catName,
                                type = ItemType.LIVE_TV,
                                rating = 0.0,
                                year = "2024",
                                duration = "",
                                description = "Canlı TV Yayını",
                                playlistSourceId = playlistSourceId,
                                epgChannelId = epgChannelId,
                                categoryId = catId,
                                orderIndex = idx
                            )
                        )
                    }
                }
                jsonReader.endArray()
                jsonReader.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchLiveStreams error: ${e.message}")
        }
        list
    }

    /**
     * Film kategorisine girildiğinde:
     * player_api.php?action=get_vod_streams&category_id=X&username=U&password=P
     */
    suspend fun fetchVodStreams(
        baseUrl: String,
        username: String,
        password: String,
        categoryId: String? = null,
        categoryName: String? = null,
        categoryMap: Map<String, String> = emptyMap(),
        playlistSourceId: String = "default"
    ): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val params = if (!categoryId.isNullOrBlank()) mapOf("category_id" to categoryId) else emptyMap()
        val url = buildApiUrl(baseUrl, action = "get_vod_streams", params = params, username = username, password = password)
        val list = mutableListOf<PlaylistItem>()
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body ?: return@withContext emptyList()
                val charStream = BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8), 32768)
                val jsonReader = JsonReader(charStream)
                if (jsonReader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                jsonReader.beginArray()
                var idx = 0
                while (jsonReader.hasNext()) {
                    jsonReader.beginObject()
                    var streamId = ""
                    var name = ""
                    var streamIcon: String? = null
                    var catId = categoryId ?: ""
                    var rating = 0.0
                    var containerExtension = "mp4"
                    while (jsonReader.hasNext()) {
                        when (jsonReader.nextName()) {
                            "stream_id" -> streamId = jsonReader.nextStringSafe()
                            "name" -> name = jsonReader.nextStringSafe()
                            "stream_icon" -> streamIcon = jsonReader.nextStringSafe().takeIf { it.isNotBlank() }
                            "category_id" -> catId = jsonReader.nextStringSafe().ifBlank { catId }
                            "rating" -> rating = jsonReader.nextDoubleSafe()
                            "rating_5based" -> {
                                val r5 = jsonReader.nextDoubleSafe()
                                if (rating == 0.0) rating = r5 * 2.0
                            }
                            "container_extension" -> containerExtension = jsonReader.nextStringSafe().ifBlank { "mp4" }
                            else -> jsonReader.skipValue()
                        }
                    }
                    jsonReader.endObject()
                    if (streamId.isNotBlank() && name.isNotBlank()) {
                        idx++
                        val cName = categoryName ?: categoryMap[catId] ?: "Genel"
                        list.add(
                            PlaylistItem(
                                id = "xtream_vod_$streamId",
                                name = name,
                                logoUrl = streamIcon,
                                streamUrl = buildVodStreamUrl(baseUrl, username, password, streamId, containerExtension),
                                category = cName,
                                type = ItemType.VOD_MOVIE,
                                rating = if (rating > 0.0) rating else 7.0,
                                year = "2024",
                                duration = "115 dk",
                                description = "$name Filmi",
                                playlistSourceId = playlistSourceId,
                                categoryId = catId,
                                orderIndex = idx
                            )
                        )
                    }
                }
                jsonReader.endArray()
                jsonReader.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchVodStreams error: ${e.message}")
        }
        list
    }

    /**
     * Dizi kategorisine girildiğinde:
     * player_api.php?action=get_series&category_id=X&username=U&password=P
     */
    suspend fun fetchSeries(
        baseUrl: String,
        username: String,
        password: String,
        categoryId: String? = null,
        categoryName: String? = null,
        categoryMap: Map<String, String> = emptyMap(),
        playlistSourceId: String = "default"
    ): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val params = if (!categoryId.isNullOrBlank()) mapOf("category_id" to categoryId) else emptyMap()
        val url = buildApiUrl(baseUrl, action = "get_series", params = params, username = username, password = password)
        val list = mutableListOf<PlaylistItem>()
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body ?: return@withContext emptyList()
                val charStream = BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8), 32768)
                val jsonReader = JsonReader(charStream)
                if (jsonReader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                jsonReader.beginArray()
                var idx = 0
                while (jsonReader.hasNext()) {
                    jsonReader.beginObject()
                    var seriesId = ""
                    var name = ""
                    var cover: String? = null
                    var catId = categoryId ?: ""
                    var rating = 0.0
                    while (jsonReader.hasNext()) {
                        when (jsonReader.nextName()) {
                            "series_id" -> seriesId = jsonReader.nextStringSafe()
                            "name", "title" -> name = jsonReader.nextStringSafe()
                            "cover" -> cover = jsonReader.nextStringSafe().takeIf { it.isNotBlank() }
                            "category_id" -> catId = jsonReader.nextStringSafe().ifBlank { catId }
                            "rating" -> rating = jsonReader.nextDoubleSafe()
                            "rating_5based" -> {
                                val r5 = jsonReader.nextDoubleSafe()
                                if (rating == 0.0) rating = r5 * 2.0
                            }
                            else -> jsonReader.skipValue()
                        }
                    }
                    jsonReader.endObject()
                    if (seriesId.isNotBlank() && name.isNotBlank()) {
                        idx++
                        val cName = categoryName ?: categoryMap[catId] ?: "Genel"
                        list.add(
                            PlaylistItem(
                                id = "series_xtream_$seriesId",
                                name = name,
                                logoUrl = cover,
                                streamUrl = "",
                                category = cName,
                                type = ItemType.VOD_SERIES,
                                rating = if (rating > 0.0) rating else 8.0,
                                year = "2024",
                                duration = "Dizi",
                                description = "$name Dizisi",
                                playlistSourceId = playlistSourceId,
                                seriesId = "series_xtream_$seriesId",
                                categoryId = catId,
                                orderIndex = idx
                            )
                        )
                    }
                }
                jsonReader.endArray()
                jsonReader.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchSeries error: ${e.message}")
        }
        list
    }

    /**
     * Film detayı (film kartına tıklanınca): get_vod_info&vod_id=ID çağrılır.
     * Kullanılan alanlar: name, description/plot, genre, releasedate, duration, actors/cast, director,
     * backdrop_path[0], youtube_trailer, rating, video.width (kalite tespiti).
     * tmdb_id alanı sadece referans olarak saklanır, hiçbir API çağrısında kullanılmaz.
     */
    suspend fun fetchVodInfo(
        baseUrl: String,
        username: String,
        password: String,
        vodId: String
    ): XtreamVodDetails? = withContext(Dispatchers.IO) {
        val url = buildApiUrl(baseUrl, action = "get_vod_info", params = mapOf("vod_id" to vodId), username = username, password = password)
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                if (!body.startsWith("{")) return@withContext null

                val root = JSONObject(body)
                val info = root.optJSONObject("info") ?: JSONObject()
                val movieData = root.optJSONObject("movie_data") ?: JSONObject()

                val name = movieData.optString("name").takeIf { it.isNotBlank() }
                    ?: info.optString("name").takeIf { it.isNotBlank() }
                val movieImage = info.optString("movie_image").takeIf { it.isNotBlank() }
                val coverBig = info.optString("cover_big").takeIf { it.isNotBlank() }
                val plot = info.optString("plot").takeIf { it.isNotBlank() }
                    ?: info.optString("description").takeIf { it.isNotBlank() }
                val genre = info.optString("genre").takeIf { it.isNotBlank() }
                val releaseDate = info.optString("release_date").takeIf { it.isNotBlank() }
                    ?: info.optString("releasedate").takeIf { it.isNotBlank() }
                val rating = info.optDouble("rating", info.optDouble("rating_5based", 0.0) * 2.0)
                val duration = info.optString("duration").takeIf { it.isNotBlank() }
                    ?: info.optInt("duration_secs", 0).let { if (it > 0) "${it / 60} dk" else null }
                val director = info.optString("director").takeIf { it.isNotBlank() }
                val actors = info.optString("actors").takeIf { it.isNotBlank() }
                val cast = info.optString("cast").takeIf { it.isNotBlank() }
                val country = info.optString("country").takeIf { it.isNotBlank() }
                val youtubeTrailer = info.optString("youtube_trailer").takeIf { it.isNotBlank() }
                val tmdbIdRef = info.optString("tmdb_id").takeIf { it.isNotBlank() }

                val backdropPath = if (info.has("backdrop_path")) {
                    val bdArray = info.optJSONArray("backdrop_path")
                    if (bdArray != null && bdArray.length() > 0) {
                        bdArray.optString(0).takeIf { it.isNotBlank() }
                    } else {
                        info.optString("backdrop_path").takeIf { it.isNotBlank() }
                    }
                } else null

                // Video width kalite tespiti: ≥3800→4K, ≥1900→1080p, ≥1200→720p, altı→SD
                val videoObj = info.optJSONObject("video")
                val width = videoObj?.optInt("width", 0) ?: info.optInt("width", 0)
                val quality = when {
                    width >= 3800 -> "4K"
                    width >= 1900 -> "1080p"
                    width >= 1200 -> "720p"
                    width > 0 -> "SD"
                    else -> "HD"
                }

                return@withContext XtreamVodDetails(
                    name = name,
                    movieImage = movieImage,
                    coverBig = coverBig,
                    plot = plot,
                    genre = genre,
                    releaseDate = releaseDate,
                    rating = rating,
                    duration = duration,
                    director = director,
                    actors = actors,
                    cast = cast,
                    country = country,
                    youtubeTrailer = youtubeTrailer,
                    backdropPath = backdropPath,
                    quality = quality,
                    tmdbId = tmdbIdRef
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "get_vod_info error ($vodId): ${e.message}")
            null
        }
    }

    /**
     * Dizi detayı (dizi kartına tıklanınca): get_series_info&series_id=ID çağrılır.
     * Bu istek yalnızca dizi açıldığında yapılır, kategori/grid ekranında asla çağrılmaz.
     */
    suspend fun fetchSeriesInfo(
        baseUrl: String,
        username: String,
        password: String,
        seriesId: String,
        seriesName: String? = null,
        playlistSourceId: String = "default"
    ): XtreamSeriesDetails? = withContext(Dispatchers.IO) {
        val url = buildApiUrl(baseUrl, action = "get_series_info", params = mapOf("series_id" to seriesId), username = username, password = password)
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                if (!body.startsWith("{")) return@withContext null

                val root = JSONObject(body)
                val info = root.optJSONObject("info") ?: JSONObject()

                val name = info.optString("name").takeIf { it.isNotBlank() } ?: seriesName
                val cover = info.optString("cover").takeIf { it.isNotBlank() }
                val plot = info.optString("plot").takeIf { it.isNotBlank() }
                val cast = info.optString("cast").takeIf { it.isNotBlank() }
                val director = info.optString("director").takeIf { it.isNotBlank() }
                val genre = info.optString("genre").takeIf { it.isNotBlank() }
                val releaseDate = info.optString("releaseDate").takeIf { it.isNotBlank() }
                val rating = info.optDouble("rating", 8.0)
                val youtubeTrailer = info.optString("youtube_trailer").takeIf { it.isNotBlank() }
                val episodeRunTime = info.optString("episode_run_time").takeIf { it.isNotBlank() }

                val backdrop = if (info.has("backdrop_path")) {
                    val bdArray = info.optJSONArray("backdrop_path")
                    if (bdArray != null && bdArray.length() > 0) {
                        bdArray.optString(0).takeIf { it.isNotBlank() }
                    } else {
                        info.optString("backdrop_path").takeIf { it.isNotBlank() }
                    }
                } else null

                // Sezon numaralarını key olarak taşıyan "episodes" objesi ("1", "2"...)
                val seasonsList = mutableListOf<Int>()
                val episodeList = mutableListOf<XtreamEpisodeInfo>()

                val episodesObj = root.optJSONObject("episodes")
                if (episodesObj != null) {
                    val seasonKeys = episodesObj.keys()
                    while (seasonKeys.hasNext()) {
                        val seasonKey = seasonKeys.next()
                        val seasonNum = seasonKey.toIntOrNull() ?: 1
                        if (!seasonsList.contains(seasonNum)) {
                            seasonsList.add(seasonNum)
                        }

                        val epArray = episodesObj.optJSONArray(seasonKey)
                        if (epArray != null) {
                            for (epIdx in 0 until epArray.length()) {
                                val epObj = epArray.getJSONObject(epIdx)
                                val epId = epObj.optString("id")
                                val epNum = epObj.optInt("episode_num", epIdx + 1)
                                val rawTitle = epObj.optString("title")
                                val cleanEpTitle = formatEpisodeTitle(rawTitle, epNum)

                                val epExt = epObj.optString("container_extension", "mp4").let { if (it.isBlank()) "mp4" else it }
                                val epInfo = epObj.optJSONObject("info")

                                // Bölüm küçük resmi info.movie_image varsa o, yoksa dizinin cover görseli kullanılır
                                val epImage = epInfo?.optString("movie_image")?.takeIf { it.isNotBlank() }
                                    ?: epObj.optString("movie_image").takeIf { it.isNotBlank() }
                                    ?: cover

                                val epDuration = epInfo?.optString("duration")?.takeIf { it.isNotBlank() }
                                    ?: epInfo?.optInt("duration_secs", 0)?.let { if (it > 0) "${it / 60} dk" else null }
                                    ?: episodeRunTime?.let { "${it} dk" }
                                    ?: "45 dk"

                                val epPlot = epInfo?.optString("plot")?.takeIf { it.isNotBlank() } ?: "$name - $cleanEpTitle"
                                val epStreamUrl = buildSeriesStreamUrl(baseUrl, username, password, epId, epExt)

                                episodeList.add(
                                    XtreamEpisodeInfo(
                                        id = epId,
                                        seasonNum = seasonNum,
                                        episodeNum = epNum,
                                        title = cleanEpTitle,
                                        streamUrl = epStreamUrl,
                                        logoUrl = epImage,
                                        duration = epDuration,
                                        plot = epPlot
                                    )
                                )
                            }
                        }
                    }
                }

                seasonsList.sort()

                return@withContext XtreamSeriesDetails(
                    name = name,
                    cover = cover,
                    backdrop = backdrop,
                    plot = plot,
                    cast = cast,
                    director = director,
                    genre = genre,
                    releaseDate = releaseDate,
                    rating = rating,
                    seasons = if (seasonsList.isEmpty()) listOf(1) else seasonsList,
                    episodes = episodeList,
                    youtubeTrailer = youtubeTrailer,
                    episodeRunTime = episodeRunTime
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "get_series_info error ($seriesId): ${e.message}")
            null
        }
    }

    /**
     * Bölüm başlığı biçimlendirme:
     * İçinden sadece "X. Bölüm" kısmı gösterilir; dizi adı ve SxxExx tekrar yazılmaz.
     * Regex eşleşmezse "Bölüm {episode_num}" yazılır.
     */
    fun formatEpisodeTitle(rawTitle: String?, episodeNum: Int): String {
        if (rawTitle.isNullOrBlank()) return "Bölüm $episodeNum"
        val regex = Regex("""(?i)(\d{1,3})\.\s*b[oö]l[uü]m""")
        val match = regex.find(rawTitle)
        return if (match != null) {
            val num = match.groupValues[1]
            "$num. Bölüm"
        } else {
            "Bölüm $episodeNum"
        }
    }

    /**
     * Grid/liste kartları için tembel zenginleştirici (en fazla 3 eşzamanlı istek, kalıcı cache).
     */
    suspend fun fetchVodCardInfoLazy(
        baseUrl: String,
        username: String,
        password: String,
        item: PlaylistItem
    ): XtreamCardMeta? = withContext(Dispatchers.IO) {
        val cached = cardMetaCache[item.id]
        if (cached != null) return@withContext cached

        val vodId = if (item.id.startsWith("xtream_vod_")) item.id.removePrefix("xtream_vod_") else item.id.substringAfterLast("_")
        cardEnrichmentSemaphore.withPermit {
            val details = fetchVodInfo(baseUrl, username, password, vodId) ?: return@withContext null
            val meta = XtreamCardMeta(
                title = details.name ?: item.name,
                genre = details.genre ?: item.category,
                year = details.releaseDate?.take(4) ?: item.year,
                rating = if (details.rating > 0.0) details.rating else item.rating,
                quality = details.quality,
                posterUrl = details.movieImage ?: details.coverBig ?: item.logoUrl,
                backdropUrl = details.backdropPath ?: details.coverBig
            )
            cardMetaCache[item.id] = meta
            meta
        }
    }

    suspend fun fetchSeriesCardInfoLazy(
        baseUrl: String,
        username: String,
        password: String,
        item: PlaylistItem
    ): XtreamCardMeta? = withContext(Dispatchers.IO) {
        val cached = cardMetaCache[item.id]
        if (cached != null) return@withContext cached

        val sId = if (item.id.startsWith("series_xtream_")) item.id.removePrefix("series_xtream_") else item.id.substringAfterLast("_")
        cardEnrichmentSemaphore.withPermit {
            // Sadece info çekmek için get_series_info çağrısı
            val details = fetchSeriesInfo(baseUrl, username, password, sId, item.name) ?: return@withContext null
            val meta = XtreamCardMeta(
                title = details.name ?: item.name,
                genre = details.genre ?: item.category,
                year = details.releaseDate?.take(4) ?: item.year,
                rating = if (details.rating > 0.0) details.rating else item.rating,
                quality = "HD",
                posterUrl = details.cover ?: item.logoUrl,
                backdropUrl = details.backdrop
            )
            cardMetaCache[item.id] = meta
            meta
        }
    }

    fun getCachedCardMeta(itemId: String): XtreamCardMeta? = cardMetaCache[itemId]

    /**
     * EPG İstekleri:
     * - get_short_epg&stream_id=ID&limit=2 (şimdiki program)
     * - get_simple_data_table&stream_id=ID (tüm akış)
     * Başlık/açıklama alanları base64 kodludur, decode edilir.
     */
    suspend fun fetchShortEpg(
        baseUrl: String,
        username: String,
        password: String,
        streamId: String
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val url = buildApiUrl(baseUrl, action = "get_short_epg", params = mapOf("stream_id" to streamId, "limit" to "2"), username = username, password = password)
        val programs = mutableListOf<Pair<String, String>>()
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                if (body.startsWith("{")) {
                    val root = JSONObject(body)
                    val listings = root.optJSONArray("epg_listings")
                    if (listings != null) {
                        for (i in 0 until listings.length()) {
                            val obj = listings.getJSONObject(i)
                            val rawTitle = obj.optString("title")
                            val rawDesc = obj.optString("description")
                            val decodedTitle = decodeBase64Safe(rawTitle).ifBlank { "Program bilgisi bulunamadı" }
                            val decodedDesc = decodeBase64Safe(rawDesc)
                            programs.add(Pair(decodedTitle, decodedDesc))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "get_short_epg error: ${e.message}")
        }
        programs
    }

    fun decodeBase64Safe(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        return try {
            val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8).trim()
        } catch (_: Exception) {
            encoded.trim()
        }
    }

    private fun JsonReader.nextStringSafe(): String {
        return try {
            if (peek() == android.util.JsonToken.NULL) {
                nextNull()
                ""
            } else {
                nextString().trim()
            }
        } catch (_: Exception) {
            skipValue()
            ""
        }
    }

    private fun JsonReader.nextDoubleSafe(): Double {
        return try {
            if (peek() == android.util.JsonToken.NULL) {
                nextNull()
                0.0
            } else if (peek() == android.util.JsonToken.STRING) {
                nextString().toDoubleOrNull() ?: 0.0
            } else {
                nextDouble()
            }
        } catch (_: Exception) {
            skipValue()
            0.0
        }
    }
}
