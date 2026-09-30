package com.example.data.parser

import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Production-ready M3U playlist parsing service.
 * Handles large playlists (100k+ entries) efficiently via streaming,
 * extracts comprehensive channel metadata (names, groups, logos, stream URLs, EPG IDs),
 * supports GZIP compression, custom headers, and resilient error recovery.
 */
class M3uParsingService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        val INSTANCE: M3uParsingService by lazy { M3uParsingService() }
        const val DEFAULT_CHUNK_SIZE = 300
        private const val BUFFER_SIZE = 65536 // 64KB read buffer
    }

    /**
     * Downloads an M3U playlist from a remote URL and streams parsed items in chunks.
     * Memory consumption is strictly bounded because the response body is streamed
     * line-by-line without buffering the whole file in memory.
     */
    fun fetchAndParseStreaming(
        url: String,
        playlistSourceId: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        onProgress: ((M3uParseProgress) -> Unit)? = null
    ): Flow<List<PlaylistItem>> = flow {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Tivions/1.0 (Android; IPTV; Mobile)")
            .header("Accept-Encoding", "gzip, deflate")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val body = response.body ?: throw Exception("Boş M3U yanıtı alındı.")
            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()

            parseStream(
                inputStream = inputStream,
                playlistSourceId = playlistSourceId,
                baseUrl = url,
                totalBytes = totalBytes,
                chunkSize = chunkSize,
                onProgress = onProgress
            ).collect { chunk ->
                emit(chunk)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parses a local M3U file in streaming chunks.
     */
    fun parseFileStreaming(
        file: File,
        playlistSourceId: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        onProgress: ((M3uParseProgress) -> Unit)? = null
    ): Flow<List<PlaylistItem>> = flow {
        val totalBytes = file.length()
        FileInputStream(file).use { inputStream ->
            parseStream(
                inputStream = inputStream,
                playlistSourceId = playlistSourceId,
                baseUrl = null,
                totalBytes = totalBytes,
                chunkSize = chunkSize,
                onProgress = onProgress
            ).collect { chunk ->
                emit(chunk)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Core streaming parser. Reads an InputStream line-by-line with BufferedReader,
     * emits chunks of 300 PlaylistItem objects, clears the batch list to maintain only
     * a 300-item memory window, tracks line counts, and provides robust error handling.
     */
    fun parseStream(
        inputStream: InputStream,
        playlistSourceId: String,
        baseUrl: String? = null,
        totalBytes: Long = -1L,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        onProgress: ((M3uParseProgress) -> Unit)? = null
    ): Flow<List<PlaylistItem>> = flow {
        val bufferedInput = BufferedInputStream(inputStream, BUFFER_SIZE)
        val decodedStream = wrapIfGzip(bufferedInput)
        val reader = BufferedReader(InputStreamReader(decodedStream, StandardCharsets.UTF_8), BUFFER_SIZE)

        var currentChunk = ArrayList<PlaylistItem>(chunkSize)
        val generatedSeries = LinkedHashMap<String, PlaylistItem>()
        var totalParsed = 0
        var lineCount = 0L

        var pendingExtInf: ExtInfData? = null
        var pendingGroupOverride: String? = null
        val pendingCustomHeaders = mutableMapOf<String, String>()

        try {
            var isFirstLine = true
            while (true) {
                lineCount++
                val raw = reader.readLine() ?: break
                var rawLine = raw.trim()
                if (rawLine.isEmpty()) continue

                if (lineCount % 300 == 0L) {
                    currentCoroutineContext().ensureActive()
                }

                if (isFirstLine) {
                    isFirstLine = false
                    if (rawLine.startsWith("\uFEFF")) {
                        rawLine = rawLine.substring(1).trim()
                    }
                    if (rawLine.startsWith("#EXTM3U", ignoreCase = true)) {
                        continue
                    }
                }

                when {
                    rawLine.startsWith("#EXTINF:", ignoreCase = true) -> {
                        pendingExtInf = parseExtInf(rawLine)
                        pendingGroupOverride = null
                        pendingCustomHeaders.clear()
                    }
                    rawLine.startsWith("#EXTGRP:", ignoreCase = true) -> {
                        val grp = rawLine.substring(8).trim()
                        if (grp.isNotEmpty()) {
                            pendingGroupOverride = grp
                        }
                    }
                    rawLine.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                        parseVlcOption(rawLine.substring(11).trim(), pendingCustomHeaders)
                    }
                    rawLine.startsWith("#KODIPROP:", ignoreCase = true) -> {
                        parseKodiProp(rawLine.substring(10).trim(), pendingCustomHeaders)
                    }
                    rawLine.startsWith("#") -> {
                        // Metadata comments - ignore safely
                    }
                    else -> {
                        // Stream URL line
                        val extInf = pendingExtInf
                        val streamLine = rawLine
                        val (cleanUrl, inlineHeaders) = extractPipeHeaders(streamLine)
                        val resolvedUrl = resolveRelativeUrl(cleanUrl, baseUrl)

                        val finalGroup = pendingGroupOverride?.takeIf { it.isNotBlank() }
                            ?: extInf?.groupTitle?.takeIf { it.isNotBlank() }
                            ?: "Genel"

                        totalParsed++
                        val parsedResult = buildPlaylistItem(
                            extInf = extInf,
                            streamUrl = resolvedUrl,
                            group = finalGroup,
                            playlistSourceId = playlistSourceId,
                            index = totalParsed
                        )

                        currentChunk.add(parsedResult.item)
                        parsedResult.parentSeries?.let { parent ->
                            if (!generatedSeries.containsKey(parent.id)) {
                                generatedSeries[parent.id] = parent
                            }
                        }

                        if (currentChunk.size >= chunkSize) {
                            emit(currentChunk)
                            currentChunk = ArrayList(chunkSize)

                            onProgress?.invoke(
                                M3uParseProgress(
                                    parsedCount = totalParsed,
                                    lastGroup = finalGroup,
                                    bytesRead = lineCount * 50,
                                    totalBytes = totalBytes,
                                    percent = if (totalBytes > 0) ((lineCount * 50 * 100) / totalBytes).toInt().coerceIn(0, 100) else null
                                )
                            )
                        }

                        pendingExtInf = null
                        pendingGroupOverride = null
                        pendingCustomHeaders.clear()
                    }
                }
            }

            for (parent in generatedSeries.values) {
                totalParsed++
                currentChunk.add(parent.copy(orderIndex = totalParsed))
                if (currentChunk.size >= chunkSize) {
                    emit(currentChunk)
                    currentChunk = ArrayList(chunkSize)
                }
            }

            if (currentChunk.isNotEmpty()) {
                emit(currentChunk)
                currentChunk = ArrayList(0)
            }

            onProgress?.invoke(
                M3uParseProgress(
                    parsedCount = totalParsed,
                    lastGroup = "Tamamlandı",
                    totalBytes = totalBytes,
                    percent = 100
                )
            )
        } catch (t: Throwable) {
            val errorMsg = "İçe aktarma sırasında hata oluştu, $lineCount. satırda durdu: ${t.localizedMessage ?: t.javaClass.simpleName}"
            android.util.Log.e("M3U_IMPORT", errorMsg, t)
            throw Exception(errorMsg, t)
        } finally {
            try {
                reader.close()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parses an in-memory string completely and returns the list of PlaylistItem entities.
     * Useful for smaller playlists, unit tests, and demo data.
     */
    suspend fun parseString(m3uContent: String, playlistSourceId: String): List<PlaylistItem> = withContext(Dispatchers.Default) {
        val result = mutableListOf<PlaylistItem>()
        val inputStream = m3uContent.byteInputStream(StandardCharsets.UTF_8)
        parseStream(inputStream, playlistSourceId).collect { chunk ->
            result.addAll(chunk)
        }
        result
    }

    /**
     * Parses an in-memory M3U text and extracts detailed [M3uChannelMetadata] objects
     * containing channel names, stream URLs, and group titles.
     */
    suspend fun parseChannels(m3uContent: String): List<M3uChannelMetadata> = withContext(Dispatchers.Default) {
        val result = mutableListOf<M3uChannelMetadata>()
        val inputStream = m3uContent.byteInputStream(StandardCharsets.UTF_8)
        parseChannelsStream(inputStream).collect { chunk ->
            result.addAll(chunk)
        }
        result
    }

    /**
     * Streams and parses an InputStream into chunks of [M3uChannelMetadata], extracting
     * channel names, stream URLs, group titles, TVG IDs, logos, and custom headers.
     */
    fun parseChannelsStream(
        inputStream: InputStream,
        baseUrl: String? = null,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ): Flow<List<M3uChannelMetadata>> = flow {
        val bufferedInput = BufferedInputStream(inputStream, BUFFER_SIZE)
        val decodedStream = wrapIfGzip(bufferedInput)
        val reader = BufferedReader(InputStreamReader(decodedStream, StandardCharsets.UTF_8), BUFFER_SIZE)

        var currentChunk = ArrayList<M3uChannelMetadata>(chunkSize)
        var pendingExtInf: ExtInfData? = null
        var pendingGroupOverride: String? = null
        val pendingCustomHeaders = mutableMapOf<String, String>()

        reader.useLines { lines ->
            val iterator = lines.iterator()
            var isFirstLine = true

            while (iterator.hasNext()) {
                var rawLine = iterator.next().trim()
                if (rawLine.isEmpty()) continue

                if (isFirstLine) {
                    isFirstLine = false
                    if (rawLine.startsWith("\uFEFF")) {
                        rawLine = rawLine.substring(1).trim()
                    }
                    if (rawLine.startsWith("#EXTM3U", ignoreCase = true)) {
                        continue
                    }
                }

                when {
                    rawLine.startsWith("#EXTINF:", ignoreCase = true) -> {
                        pendingExtInf = parseExtInf(rawLine)
                        pendingGroupOverride = null
                        pendingCustomHeaders.clear()
                    }
                    rawLine.startsWith("#EXTGRP:", ignoreCase = true) -> {
                        val grp = rawLine.substring(8).trim()
                        if (grp.isNotEmpty()) {
                            pendingGroupOverride = grp
                        }
                    }
                    rawLine.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                        parseVlcOption(rawLine.substring(11).trim(), pendingCustomHeaders)
                    }
                    rawLine.startsWith("#KODIPROP:", ignoreCase = true) -> {
                        parseKodiProp(rawLine.substring(10).trim(), pendingCustomHeaders)
                    }
                    rawLine.startsWith("#") -> {
                        // ignore other comment tags
                    }
                    else -> {
                        val extInf = pendingExtInf
                        val streamLine = rawLine
                        val (cleanUrl, inlineHeaders) = extractPipeHeaders(streamLine)
                        val resolvedUrl = resolveRelativeUrl(cleanUrl, baseUrl)

                        val allHeaders = HashMap(pendingCustomHeaders)
                        allHeaders.putAll(inlineHeaders)

                        val groupTitle = pendingGroupOverride?.takeIf { it.isNotBlank() }
                            ?: extInf?.groupTitle?.takeIf { it.isNotBlank() }
                            ?: "Genel"

                        val channelName = extInf?.title?.ifBlank { extInf.tvgName ?: "Kanal" } ?: "Kanal"

                        val metadata = M3uChannelMetadata(
                            name = channelName,
                            streamUrl = resolvedUrl,
                            groupTitle = groupTitle,
                            logoUrl = extInf?.tvgLogo?.takeIf { it.isNotBlank() },
                            tvgId = extInf?.tvgId?.takeIf { it.isNotBlank() },
                            tvgName = extInf?.tvgName?.takeIf { it.isNotBlank() },
                            tvgShift = extInf?.tvgShift,
                            tvgLanguage = extInf?.tvgLanguage,
                            tvgCountry = extInf?.tvgCountry,
                            durationSeconds = extInf?.duration ?: -1L,
                            isRadio = extInf?.isRadio ?: false,
                            httpHeaders = allHeaders,
                            rawAttributes = extInf?.attributes ?: emptyMap()
                        )

                        currentChunk.add(metadata)
                        if (currentChunk.size >= chunkSize) {
                            emit(currentChunk)
                            currentChunk = ArrayList(chunkSize)
                        }

                        pendingExtInf = null
                        pendingGroupOverride = null
                        pendingCustomHeaders.clear()
                    }
                }
            }
        }

        if (currentChunk.isNotEmpty()) {
            emit(currentChunk)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Downloads an M3U playlist from a remote URL and streams parsed [M3uChannelMetadata] chunks.
     */
    fun fetchAndParseChannelsStreaming(
        url: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ): Flow<List<M3uChannelMetadata>> = flow {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Tivions/1.0 (Android; IPTV; Mobile)")
            .header("Accept-Encoding", "gzip, deflate")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val body = response.body ?: throw Exception("Boş M3U yanıtı alındı.")
            val inputStream = body.byteStream()
            parseChannelsStream(
                inputStream = inputStream,
                baseUrl = url,
                chunkSize = chunkSize
            ).collect { chunk ->
                emit(chunk)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Legacy helper method matching the original M3uParser signature.
     */
    suspend fun fetchAndParse(url: String, playlistSourceId: String): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PlaylistItem>()
        fetchAndParseStreaming(url, playlistSourceId).collect { chunk ->
            result.addAll(chunk)
        }
        result
    }

    /**
     * Parses the #EXTM3U header line attributes like url-tvg="..." or x-tvg-url="...".
     */
    private fun parseHeaderLine(line: String): M3uPlaylistHeader {
        val attrs = extractAttributes(line.substring(7))
        val tvgUrl = attrs["url-tvg"] ?: attrs["x-tvg-url"] ?: attrs["tvg-url"]
        val refresh = attrs["refresh"]?.toIntOrNull()
        val userAgent = attrs["user-agent"]
        return M3uPlaylistHeader(
            tvgUrl = tvgUrl,
            refreshIntervalHours = refresh,
            userAgent = userAgent,
            rawAttributes = attrs
        )
    }

    /**
     * Intermediate data container for parsed #EXTINF tag.
     */
    private data class ExtInfData(
        val duration: Long = -1L,
        val attributes: Map<String, String> = emptyMap(),
        val title: String = "",
        val groupTitle: String? = null,
        val tvgId: String? = null,
        val tvgName: String? = null,
        val tvgLogo: String? = null,
        val tvgShift: String? = null,
        val tvgLanguage: String? = null,
        val tvgCountry: String? = null,
        val tvgType: String? = null,
        val isRadio: Boolean = false
    )

    /**
     * Highly resilient parser for #EXTINF lines:
     * Format: #EXTINF:<duration> <attributes>,<Title>
     * Extracts attributes (tvg-id, tvg-name, tvg-logo, group-title) using regex and comma separation.
     */
    private val tvgIdRegex = Regex("""tvg-id\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\s"'>]+))""", RegexOption.IGNORE_CASE)
    private val tvgNameRegex = Regex("""tvg-name\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\s"'>]+))""", RegexOption.IGNORE_CASE)
    private val tvgLogoRegex = Regex("""tvg-logo\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\s"'>]+))""", RegexOption.IGNORE_CASE)
    private val groupTitleRegex = Regex("""group-title\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\s"'>]+))""", RegexOption.IGNORE_CASE)
    private val tvgTypeRegex = Regex("""(?:tvg-type|type)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\s"'>]+))""", RegexOption.IGNORE_CASE)

    private fun extractRegexAttr(regex: Regex, line: String): String? {
        val match = regex.find(line) ?: return null
        return (match.groups[1]?.value ?: match.groups[2]?.value ?: match.groups[3]?.value)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun parseExtInf(line: String): ExtInfData {
        val content = line.substring(8).trim() // after #EXTINF:
        if (content.isEmpty()) return ExtInfData()

        var duration = -1L
        var attributesPart = ""
        var titlePart = ""

        // Find the comma separating duration/attributes from title (ignoring commas inside quotes)
        var inQuotes = false
        var quoteChar = '"'
        var commaIdx = -1

        for (i in content.indices) {
            val c = content[i]
            if ((c == '"' || c == '\'') && (i == 0 || content[i - 1] != '\\')) {
                if (!inQuotes) {
                    inQuotes = true
                    quoteChar = c
                } else if (c == quoteChar) {
                    inQuotes = false
                }
            } else if (c == ',' && !inQuotes) {
                commaIdx = i
                break
            }
        }

        if (commaIdx != -1) {
            val beforeComma = content.substring(0, commaIdx).trim()
            titlePart = content.substring(commaIdx + 1).trim()

            val firstSpace = beforeComma.indexOfFirst { it.isWhitespace() }
            if (firstSpace != -1) {
                val durStr = beforeComma.substring(0, firstSpace).trim()
                duration = durStr.toDoubleOrNull()?.toLong() ?: -1L
                attributesPart = beforeComma.substring(firstSpace + 1).trim()
            } else {
                duration = beforeComma.toDoubleOrNull()?.toLong() ?: -1L
            }
        } else {
            val firstSpace = content.indexOfFirst { it.isWhitespace() }
            if (firstSpace != -1) {
                val durStr = content.substring(0, firstSpace).trim()
                duration = durStr.toDoubleOrNull()?.toLong() ?: -1L
                attributesPart = content.substring(firstSpace + 1).trim()
            } else {
                duration = content.toDoubleOrNull()?.toLong() ?: -1L
            }
        }

        val attrs = extractAttributes(attributesPart)
        val tvgId = extractRegexAttr(tvgIdRegex, attributesPart) ?: attrs["tvg-id"]?.takeIf { it.isNotBlank() }
        val tvgName = extractRegexAttr(tvgNameRegex, attributesPart) ?: attrs["tvg-name"]?.takeIf { it.isNotBlank() }
        val tvgLogo = extractRegexAttr(tvgLogoRegex, attributesPart) ?: attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
        val groupTitle = extractRegexAttr(groupTitleRegex, attributesPart) ?: attrs["group-title"]?.takeIf { it.isNotBlank() }
        val tvgType = extractRegexAttr(tvgTypeRegex, attributesPart) ?: attrs["tvg-type"]?.takeIf { it.isNotBlank() } ?: attrs["type"]?.takeIf { it.isNotBlank() }
        val tvgShift = attrs["tvg-shift"] ?: attrs["timeshift"]
        val tvgLanguage = attrs["tvg-language"]
        val tvgCountry = attrs["tvg-country"]
        val isRadio = attrs["radio"]?.equals("true", ignoreCase = true) == true

        // Title: virgülden sonraki başlık, yoksa tvg-name, yoksa "Kanal"
        val cleanTitle = titlePart.ifBlank { tvgName ?: tvgId ?: "Kanal" }

        return ExtInfData(
            duration = duration,
            attributes = attrs,
            title = cleanTitle,
            groupTitle = groupTitle,
            tvgId = tvgId,
            tvgName = tvgName ?: cleanTitle,
            tvgLogo = tvgLogo,
            tvgShift = tvgShift,
            tvgLanguage = tvgLanguage,
            tvgCountry = tvgCountry,
            tvgType = tvgType,
            isRadio = isRadio
        )
    }

    /**
     * Extracts key-value pairs from an attributes string.
     * Robust against:
     * - `key="value"`
     * - `key='value'`
     * - `key=value`
     * - spaces around `=` (e.g. `group-title = "Sports"`)
     * - case-insensitive keys (normalized to lowercase)
     */
    fun extractAttributes(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val len = text.length
        var i = 0

        while (i < len) {
            // Skip whitespace
            while (i < len && text[i].isWhitespace()) i++
            if (i >= len) break

            // Read key
            val keyStart = i
            while (i < len && text[i] != '=' && !text[i].isWhitespace()) i++
            val key = text.substring(keyStart, i).trim().lowercase()

            // Skip whitespace around '='
            while (i < len && text[i].isWhitespace()) i++
            if (i >= len || text[i] != '=') {
                // Key without value (e.g. boolean flag)
                if (key.isNotEmpty()) result[key] = "true"
                continue
            }
            i++ // skip '='
            while (i < len && text[i].isWhitespace()) i++
            if (i >= len) break

            // Read value (quoted or unquoted)
            val quote = text[i]
            if (quote == '"' || quote == '\'') {
                i++ // skip opening quote
                val valStart = i
                while (i < len && text[i] != quote) {
                    if (text[i] == '\\' && i + 1 < len) i++ // skip escaped char
                    i++
                }
                val value = text.substring(valStart, i.coerceAtMost(len))
                if (i < len && text[i] == quote) i++ // skip closing quote
                if (key.isNotEmpty()) result[key] = value
            } else {
                val valStart = i
                while (i < len && !text[i].isWhitespace() && text[i] != ',') i++
                val value = text.substring(valStart, i)
                if (key.isNotEmpty()) result[key] = value
            }
        }

        return result
    }

    /**
     * Parses VLC option lines: `#EXTVLCOPT:http-user-agent=...`
     */
    private fun parseVlcOption(opt: String, headers: MutableMap<String, String>) {
        val eq = opt.indexOf('=')
        if (eq != -1) {
            val key = opt.substring(0, eq).trim()
            val value = opt.substring(eq + 1).trim()
            when (key.lowercase()) {
                "http-user-agent" -> headers["User-Agent"] = value
                "http-referrer" -> headers["Referer"] = value
                else -> headers[key] = value
            }
        }
    }

    /**
     * Parses Kodi property lines: `#KODIPROP:inputstream.adaptive.license_key=...`
     */
    private fun parseKodiProp(prop: String, headers: MutableMap<String, String>) {
        val eq = prop.indexOf('=')
        if (eq != -1) {
            val key = prop.substring(0, eq).trim()
            val value = prop.substring(eq + 1).trim()
            headers[key] = value
        }
    }

    /**
     * Extracts pipe-delimited headers from URLs:
     * Example: `http://server/live.m3u8|User-Agent=Custom&Referer=http://foo.com`
     */
    fun extractPipeHeaders(rawUrl: String): Pair<String, Map<String, String>> {
        val pipeIdx = rawUrl.indexOf('|')
        if (pipeIdx == -1) return Pair(rawUrl.trim(), emptyMap())

        val cleanUrl = rawUrl.substring(0, pipeIdx).trim()
        val headersStr = rawUrl.substring(pipeIdx + 1).trim()
        val headers = mutableMapOf<String, String>()

        headersStr.split('&').forEach { param ->
            val eq = param.indexOf('=')
            if (eq != -1) {
                val k = param.substring(0, eq).trim()
                val v = param.substring(eq + 1).trim()
                if (k.isNotEmpty()) headers[k] = v
            }
        }

        return Pair(cleanUrl, headers)
    }

    /**
     * Resolves relative stream URLs against base URL.
     */
    private fun resolveRelativeUrl(url: String, baseUrl: String?): String {
        if (baseUrl.isNullOrBlank() || url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true) || url.startsWith("rtmp://", ignoreCase = true)) {
            return url
        }
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(url).toString()
        } catch (_: Exception) {
            url
        }
    }

    private data class SeasonEpResult(
        val seriesTitle: String,
        val seasonNumber: Int,
        val episodeNumber: Int
    )

    private val sxxExxRegex = Regex("""\bS(\d{1,2})\s*E(?:P)?\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE)
    private val trSezonBolumRegex1 = Regex("""(\d{1,2})\.\s*Sezon\s*(\d{1,3})\.\s*B[oö]l[uü]m""", RegexOption.IGNORE_CASE)
    private val trSezonBolumRegex2 = Regex("""Sezon\s*(\d{1,2})\s*B[oö]l[uü]m\s*(\d{1,3})""", RegexOption.IGNORE_CASE)
    private val altXRegex = Regex("""\b(\d{1,2})x(\d{1,3})\b""", RegexOption.IGNORE_CASE)

    private fun normalize(text: String): String {
        return text.replace('İ', 'i')
            .replace('I', 'i')
            .replace('ı', 'i')
            .lowercase(java.util.Locale.ROOT)
    }

    private fun parseSeasonEp(title: String): SeasonEpResult? {
        val m1 = sxxExxRegex.find(title)
        if (m1 != null) {
            val s = m1.groupValues[1].toIntOrNull() ?: 1
            val e = m1.groupValues[2].toIntOrNull() ?: 1
            val before = title.substring(0, m1.range.first).trim().trimEnd('-', ':', '|', '.', '_', '/').trim()
            val sTitle = if (before.isNotBlank()) before else title
            return SeasonEpResult(sTitle, s, e)
        }
        val m2 = trSezonBolumRegex1.find(title) ?: trSezonBolumRegex2.find(title)
        if (m2 != null) {
            val s = m2.groupValues[1].toIntOrNull() ?: 1
            val e = m2.groupValues[2].toIntOrNull() ?: 1
            val before = title.substring(0, m2.range.first).trim().trimEnd('-', ':', '|', '.', '_', '/').trim()
            val sTitle = if (before.isNotBlank()) before else title
            return SeasonEpResult(sTitle, s, e)
        }
        val m3 = altXRegex.find(title)
        if (m3 != null) {
            val s = m3.groupValues[1].toIntOrNull() ?: 1
            val e = m3.groupValues[2].toIntOrNull() ?: 1
            val before = title.substring(0, m3.range.first).trim().trimEnd('-', ':', '|', '.', '_', '/').trim()
            val sTitle = if (before.isNotBlank()) before else title
            return SeasonEpResult(sTitle, s, e)
        }
        return null
    }

    /**
     * Determines content type (Canlı TV / Film / Dizi) purely based on stream URL structure
     * and explicit attributes, NEVER based on group-title/category name or list order.
     *
     * Rules:
     * - Canlı TV (TR-SPOR, EX-YU, etc.): http://.../61351 -> URL has no /movie/ or /series/, no static video extension.
     * - Film (TURK FILMLERI, etc.): http://.../594313.mkv -> URL contains /movie/ or has movie file extension (.mkv, .mp4, etc.).
     * - Dizi (YABANCI DIZILER, etc.): http://.../594183.mkv -> URL contains /series/.
     */
    fun classifyItemType(streamUrl: String, tvgType: String?): ItemType {
        val cleanUrl = streamUrl.substringBefore('|').trim()
        val lowerUrl = cleanUrl.lowercase(java.util.Locale.ROOT)

        // 1. Explicit /series/ in URL path
        if (lowerUrl.contains("/series/")) {
            return ItemType.VOD_SERIES
        }

        // 2. Explicit /movie/ or /vod/ in URL path
        if (lowerUrl.contains("/movie/") || lowerUrl.contains("/vod/")) {
            return ItemType.VOD_MOVIE
        }

        // 3. Static video file extensions (.mkv, .mp4, .avi, .mov, etc.)
        val urlWithoutQuery = lowerUrl.substringBefore('?')
        val isStaticVideo = urlWithoutQuery.endsWith(".mkv") ||
                urlWithoutQuery.endsWith(".mp4") ||
                urlWithoutQuery.endsWith(".avi") ||
                urlWithoutQuery.endsWith(".mov") ||
                urlWithoutQuery.endsWith(".flv") ||
                urlWithoutQuery.endsWith(".wmv") ||
                urlWithoutQuery.endsWith(".iso") ||
                urlWithoutQuery.endsWith(".m4v") ||
                urlWithoutQuery.endsWith(".webm") ||
                urlWithoutQuery.endsWith(".mpg") ||
                urlWithoutQuery.endsWith(".mpeg")

        if (isStaticVideo) {
            return ItemType.VOD_MOVIE
        }

        // 4. Explicit tvg-type or type attribute in #EXTINF tag
        val normTvgType = tvgType?.trim()?.lowercase(java.util.Locale.ROOT) ?: ""
        if (normTvgType.isNotEmpty()) {
            when (normTvgType) {
                "series", "tvshow", "episode" -> return ItemType.VOD_SERIES
                "movie", "vod", "film" -> return ItemType.VOD_MOVIE
                "live", "tv", "channel" -> return ItemType.LIVE_TV
            }
        }

        // 5. Default: Canlı TV (ItemType.LIVE_TV)
        // No /series/, no /movie/, no static video extension.
        // Purely Live TV regardless of category names ("EX-YU", "TR-SPOR", etc.).
        return ItemType.LIVE_TV
    }

    private data class ParsedItemResult(
        val item: PlaylistItem,
        val parentSeries: PlaylistItem? = null
    )

    /**
     * Builds a single PlaylistItem database entity for an #EXTINF + stream URL pair.
     * Guarantees 1-to-1 mapping with unique IDs, avoiding any lost records or memory accumulation.
     */
    private fun buildPlaylistItem(
        extInf: ExtInfData?,
        streamUrl: String,
        group: String,
        playlistSourceId: String,
        index: Int
    ): ParsedItemResult {
        val title = extInf?.title?.ifBlank { extInf.tvgName ?: "Kanal $index" } ?: "Kanal $index"
        val itemType = classifyItemType(streamUrl, extInf?.tvgType)

        var seasonNumber = 1
        var episodeNumber = 1
        var seriesId: String? = null
        var finalType = itemType
        var parentSeries: PlaylistItem? = null

        if (itemType == ItemType.VOD_SERIES) {
            val seasonEp = parseSeasonEp(title)
            if (seasonEp != null) {
                finalType = ItemType.EPISODE
                seasonNumber = seasonEp.seasonNumber
                episodeNumber = seasonEp.episodeNumber
                val sTitle = seasonEp.seriesTitle
                val sId = "series_" + normalize(sTitle).replace(Regex("[^a-z0-9]"), "_")
                seriesId = sId
                parentSeries = PlaylistItem(
                    id = sId,
                    name = sTitle,
                    logoUrl = extInf?.tvgLogo?.takeIf { it.isNotBlank() },
                    streamUrl = "",
                    category = group.ifBlank { "Diziler" },
                    type = ItemType.VOD_SERIES,
                    rating = 8.0,
                    year = "2024",
                    duration = "Dizi",
                    description = "$sTitle Dizisi",
                    playlistSourceId = playlistSourceId,
                    seriesId = sId
                )
            } else {
                seriesId = "series_" + normalize(title).replace(Regex("[^a-z0-9]"), "_")
            }
        }

        val hash = (streamUrl + title).hashCode().toUInt().toString(16)
        val uniqueId = "${playlistSourceId}_item_${index}_$hash"
        val logo = extInf?.tvgLogo?.takeIf { it.isNotBlank() }

        val description = when (finalType) {
            ItemType.LIVE_TV -> if (extInf?.isRadio == true) "Radyo Yayını" else "Canlı TV Yayını"
            ItemType.VOD_MOVIE -> "$title Filmi"
            ItemType.VOD_SERIES, ItemType.EPISODE -> "$title Dizisi"
        }

        val rating = when (finalType) {
            ItemType.LIVE_TV -> 0.0
            ItemType.VOD_MOVIE -> 7.5
            ItemType.VOD_SERIES, ItemType.EPISODE -> 8.0
        }

        val duration = if (extInf?.duration != null && extInf.duration > 0) {
            "${extInf.duration / 60} dk"
        } else if (finalType == ItemType.LIVE_TV) {
            ""
        } else {
            "115 dk"
        }

        val item = PlaylistItem(
            id = uniqueId,
            name = title,
            logoUrl = logo,
            streamUrl = streamUrl,
            category = group.ifBlank { "Genel" },
            type = finalType,
            rating = rating,
            year = "2024",
            duration = duration,
            description = description,
            playlistSourceId = playlistSourceId,
            epgChannelId = extInf?.tvgId?.takeIf { it.isNotBlank() } ?: extInf?.tvgName?.takeIf { it.isNotBlank() },
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            orderIndex = index
        )

        return ParsedItemResult(item, parentSeries)
    }

    /**
     * Detects GZIP magic header (0x1F, 0x8B) and wraps input stream in GZIPInputStream if present.
     */
    private fun wrapIfGzip(input: BufferedInputStream): InputStream {
        input.mark(2)
        val b1 = input.read()
        val b2 = input.read()
        input.reset()
        return if (b1 == 0x1F && b2 == 0x8B) {
            GZIPInputStream(input, BUFFER_SIZE)
        } else {
            input
        }
    }
}
