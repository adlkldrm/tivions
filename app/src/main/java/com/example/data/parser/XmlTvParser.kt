package com.example.data.parser

import android.util.Xml
import com.example.data.model.EpgChannel
import com.example.data.model.EpgProgram
import com.example.data.model.PlaylistItem
import com.example.data.model.XmlTvParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object XmlTvParser {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Downloads XMLTV data from a URL (supports gzip .xml.gz or plain .xml) and parses it.
     */
    suspend fun fetchAndParse(url: String): XmlTvParseResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Tivions/1.0 (Android; IPTV XMLTV EPG)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("EPG İndirilemedi: HTTP ${response.code} ${response.message}")
            }
            val body = response.body ?: throw Exception("EPG yanıt gövdesi boş")
            val rawStream = BufferedInputStream(body.byteStream())

            // Detect if stream is GZIP compressed (magic number 0x1f8b)
            rawStream.mark(2)
            val b1 = rawStream.read()
            val b2 = rawStream.read()
            rawStream.reset()

            val inputStream: InputStream = if (b1 == 0x1f && b2 == 0x8b) {
                GZIPInputStream(rawStream)
            } else {
                rawStream
            }

            parse(InputStreamReader(inputStream, Charsets.UTF_8))
        }
    }

    /**
     * Parses an XMLTV string.
     */
    fun parse(xmlString: String): XmlTvParseResult {
        return parse(StringReader(xmlString))
    }

    /**
     * Streaming parse of XMLTV reader using XmlPullParser.
     */
    fun parse(reader: Reader): XmlTvParseResult {
        val channels = mutableListOf<EpgChannel>()
        val programs = mutableListOf<EpgProgram>()

        val parser = Xml.newPullParser().apply {
            setInput(reader)
        }

        var eventType = parser.eventType

        // Temporary state for current channel
        var currentChannelId: String? = null
        var currentChannelName: String? = null
        var currentChannelIcon: String? = null

        // Temporary state for current programme
        var progChannel: String? = null
        var progStartStr: String? = null
        var progStopStr: String? = null
        var progTitle: String? = null
        var progSubTitle: String? = null
        var progDesc: String? = null
        var progCategory: String? = null
        var progIcon: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "channel" -> {
                            currentChannelId = parser.getAttributeValue(null, "id")
                            currentChannelName = null
                            currentChannelIcon = null
                        }
                        "display-name" -> {
                            if (currentChannelId != null) {
                                currentChannelName = parser.nextText().trim()
                            }
                        }
                        "icon" -> {
                            val src = parser.getAttributeValue(null, "src")
                            if (progChannel != null) {
                                progIcon = src
                            } else if (currentChannelId != null) {
                                currentChannelIcon = src
                            }
                        }
                        "programme" -> {
                            progChannel = parser.getAttributeValue(null, "channel")
                            progStartStr = parser.getAttributeValue(null, "start")
                            progStopStr = parser.getAttributeValue(null, "stop")
                            progTitle = null
                            progSubTitle = null
                            progDesc = null
                            progCategory = null
                            progIcon = null
                        }
                        "title" -> {
                            if (progChannel != null) {
                                progTitle = parser.nextText().trim()
                            }
                        }
                        "sub-title" -> {
                            if (progChannel != null) {
                                progSubTitle = parser.nextText().trim()
                            }
                        }
                        "desc" -> {
                            if (progChannel != null) {
                                progDesc = parser.nextText().trim()
                            }
                        }
                        "category" -> {
                            if (progChannel != null && progCategory == null) {
                                progCategory = parser.nextText().trim()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "channel" -> {
                            if (currentChannelId != null && !currentChannelName.isNullOrBlank()) {
                                channels.add(
                                    EpgChannel(
                                        id = currentChannelId,
                                        displayName = currentChannelName,
                                        iconUrl = currentChannelIcon
                                    )
                                )
                            }
                            currentChannelId = null
                            currentChannelName = null
                            currentChannelIcon = null
                        }
                        "programme" -> {
                            if (progChannel != null && progStartStr != null && progStopStr != null && !progTitle.isNullOrBlank()) {
                                val startTime = parseXmlTvDate(progStartStr)
                                val stopTime = parseXmlTvDate(progStopStr)

                                if (startTime != null && stopTime != null && stopTime > startTime) {
                                    val id = "${progChannel}_${startTime}"
                                    programs.add(
                                        EpgProgram(
                                            id = id,
                                            channelId = progChannel,
                                            title = progTitle,
                                            description = progDesc ?: "",
                                            category = progCategory ?: "Genel",
                                            startTimeMillis = startTime,
                                            endTimeMillis = stopTime,
                                            iconUrl = progIcon,
                                            episodeTitle = progSubTitle
                                        )
                                    )
                                }
                            }
                            progChannel = null
                            progStartStr = null
                            progStopStr = null
                            progTitle = null
                            progSubTitle = null
                            progDesc = null
                            progCategory = null
                            progIcon = null
                        }
                    }
                }
            }

            eventType = parser.next()
        }

        return XmlTvParseResult(channels, programs)
    }

    /**
     * Parses XMLTV date formats:
     * - "20260922180000 +0300"
     * - "20260922180000 +0000"
     * - "20260922180000"
     * - "202609221800"
     * - "2026-09-22T18:00:00Z"
     */
    fun parseXmlTvDate(rawDateStr: String?): Long? {
        if (rawDateStr.isNullOrBlank()) return null
        val trimmed = rawDateStr.trim()

        val patterns = listOf(
            "yyyyMMddHHmmss Z",
            "yyyyMMddHHmmss",
            "yyyyMMddHHmm Z",
            "yyyyMMddHHmm",
            "yyyyMMdd",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = format.parse(trimmed)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Try next pattern
            }
        }

        return null
    }

    /**
     * Matches a Live TV PlaylistItem to an EPG channel ID from parsed channels.
     * Uses priority:
     * 1. Match playlistItem.epgChannelId with epgChannel.id
     * 2. Match playlistItem.id with epgChannel.id
     * 3. Exact channel name with epgChannel.displayName
     * 4. Normalized name match (removing HD, 4K, brackets)
     */
    fun findMatchingChannelId(
        item: PlaylistItem,
        epgChannels: List<EpgChannel>
    ): String? {
        // 1. Direct epgChannelId match
        if (!item.epgChannelId.isNullOrBlank()) {
            val direct = epgChannels.firstOrNull { it.id.equals(item.epgChannelId, ignoreCase = true) }
            if (direct != null) return direct.id
        }

        // 2. Item ID match
        val byId = epgChannels.firstOrNull { it.id.equals(item.id, ignoreCase = true) }
        if (byId != null) return byId.id

        // 3. Exact display name
        val byName = epgChannels.firstOrNull { it.displayName.equals(item.name, ignoreCase = true) }
        if (byName != null) return byName.id

        // 4. Normalized name comparison
        val cleanItemName = normalizeChannelName(item.name)
        val byNormalized = epgChannels.firstOrNull {
            normalizeChannelName(it.displayName) == cleanItemName ||
                    it.id.contains(cleanItemName, ignoreCase = true) ||
                    cleanItemName.contains(it.id, ignoreCase = true)
        }
        return byNormalized?.id
    }

    fun normalizeChannelName(name: String): String {
        return name
            .lowercase(Locale.ROOT)
            .replace("hd", "")
            .replace("fhd", "")
            .replace("4k", "")
            .replace("uhd", "")
            .replace("hevc", "")
            .replace("[tr]", "")
            .replace("tr:", "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }
}
