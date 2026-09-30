package com.example.util

import java.util.Locale

data class MovieMeta(
    val cleanTitle: String,
    val year: String?,
    val quality: String?
)

data class ParsedEpisodeInfo(
    val seriesName: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitleHint: String?,
    val quality: String?
)

object TitleNormalizer {

    private val QUALITY_REGEX = Regex("""(?i)\b(4k|uhd|2160p|1080p|fhd|720p|hd|sd|bluray|web-dl|webrip)\b""")
    private val YEAR_REGEX = Regex("""\((\d{4})\)""")
    private val BARE_YEAR_REGEX = Regex("""\b(19\d\d|20[0-2]\d)\b""")
    private val EPISODE_S_E_REGEX = Regex("""(?i)\bs(\d{1,2})\s?e(\d{1,3})\b""")
    private val EPISODE_X_REGEX = Regex("""(?i)\b(\d{1,2})x(\d{1,3})\b""")
    private val EPISODE_TURKISH_REGEX = Regex("""(?i)(\d{1,2})\.\s*sezon\s*(\d{1,3})\.\s*b[oö]l[uü]m""")

    private val TAGS_TO_REMOVE = listOf(
        "tr", "en", "de", "fr", "dual", "dublaj", "altyazili", "altyazılı",
        "4k", "uhd", "fhd", "hd", "sd", "1080p", "720p", "hdr",
        "web-dl", "webrip", "hdrip", "bluray", "brrip"
    )

    /**
     * BÖLÜM 6 & 10: Strict Title Normalization
     */
    fun normalizeTitle(rawTitle: String): String {
        if (rawTitle.isBlank()) return ""

        // 1. Lowercase with Turkish locale
        var text = rawTitle.replace("İ", "i").replace("I", "ı").lowercase(Locale("tr"))

        // 2. ASCII replacement for Turkish characters
        text = text.replace("ı", "i")
            .replace("ş", "s")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ö", "o")
            .replace("ç", "c")

        // 3. Remove brackets and parentheses content like [4K], (2010)
        text = text.replace(Regex("""\[[^\]]*]"""), " ")
        text = text.replace(Regex("""\([^)]*\)"""), " ")

        // 4. Remove season/episode patterns
        text = text.replace(EPISODE_S_E_REGEX, " ")
        text = text.replace(EPISODE_X_REGEX, " ")
        text = text.replace(EPISODE_TURKISH_REGEX, " ")

        // 5. Replace non-alphanumeric with spaces
        text = text.replace(Regex("""[^a-z0-9\s]"""), " ")

        // 6. Remove specific IPTV media tags
        val words = text.split(Regex("""\s+""")).filter { word ->
            word.isNotBlank() && !TAGS_TO_REMOVE.contains(word)
        }

        // 7. Collapse spaces and trim
        return words.joinToString(" ").trim()
    }

    /**
     * BÖLÜM 12: Extract clean title, 4-digit year, and video quality
     */
    fun extractMovieMeta(rawTitle: String): MovieMeta {
        val yearMatch = YEAR_REGEX.find(rawTitle) ?: BARE_YEAR_REGEX.find(rawTitle)
        val year = yearMatch?.groupValues?.getOrNull(1)

        val quality = extractQualityFromTitle(rawTitle)

        // Clean title: remove tags, resolution, year
        var clean = rawTitle
            .replace(YEAR_REGEX, " ")
            .replace(Regex("""\[[^\]]*]"""), " ")

        val cleaned = normalizeTitle(clean)
        val finalTitle = cleaned.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            .ifBlank { rawTitle.trim() }

        return MovieMeta(
            cleanTitle = finalTitle,
            year = year,
            quality = quality
        )
    }

    /**
     * BÖLÜM 3 & 7: Quality extraction from title ("4K", "1080p", "HD", "SD")
     */
    fun extractQualityFromTitle(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("4k") || lower.contains("2160p") || lower.contains("uhd") -> "4K"
            lower.contains("1080p") || lower.contains("fhd") -> "1080p"
            lower.contains("720p") || lower.contains("hd") -> "720p"
            lower.contains("sd") -> "SD"
            else -> "HD"
        }
    }

    /**
     * BÖLÜM 13: Extract Season and Episode information from M3U title
     */
    fun parseEpisodeInfo(rawTitle: String): ParsedEpisodeInfo {
        val quality = extractQualityFromTitle(rawTitle)

        // Try SxxExx
        val seMatch = EPISODE_S_E_REGEX.find(rawTitle)
        if (seMatch != null) {
            val season = seMatch.groupValues[1].toIntOrNull() ?: 1
            val episode = seMatch.groupValues[2].toIntOrNull() ?: 1
            val seriesPart = rawTitle.substring(0, seMatch.range.first).trim()
            val afterPart = rawTitle.substring((seMatch.range.last + 1).coerceAtMost(rawTitle.length)).trim()
            val hint = afterPart.trim('-', ':', ' ').ifBlank { null }
            val cleanSeries = normalizeTitle(seriesPart).split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { seriesPart }

            return ParsedEpisodeInfo(cleanSeries, season, episode, hint, quality)
        }

        // Try 1x05
        val xMatch = EPISODE_X_REGEX.find(rawTitle)
        if (xMatch != null) {
            val season = xMatch.groupValues[1].toIntOrNull() ?: 1
            val episode = xMatch.groupValues[2].toIntOrNull() ?: 1
            val seriesPart = rawTitle.substring(0, xMatch.range.first).trim()
            val afterPart = rawTitle.substring((xMatch.range.last + 1).coerceAtMost(rawTitle.length)).trim()
            val hint = afterPart.trim('-', ':', ' ').ifBlank { null }
            val cleanSeries = normalizeTitle(seriesPart).split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { seriesPart }

            return ParsedEpisodeInfo(cleanSeries, season, episode, hint, quality)
        }

        // Try Turkish format (1. Sezon 2. Bölüm)
        val trMatch = EPISODE_TURKISH_REGEX.find(rawTitle)
        if (trMatch != null) {
            val season = trMatch.groupValues[1].toIntOrNull() ?: 1
            val episode = trMatch.groupValues[2].toIntOrNull() ?: 1
            val seriesPart = rawTitle.substring(0, trMatch.range.first).trim()
            val cleanSeries = normalizeTitle(seriesPart).split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { seriesPart }

            return ParsedEpisodeInfo(cleanSeries, season, episode, null, quality)
        }

        val cleanSeries = normalizeTitle(rawTitle).split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .ifBlank { rawTitle }

        return ParsedEpisodeInfo(cleanSeries, 1, 1, null, quality)
    }

    /**
     * Series Title & Meta extraction
     */
    fun extractSeriesMeta(rawTitle: String): MovieMeta {
        val epInfo = parseEpisodeInfo(rawTitle)
        val seriesCandidate = if (epInfo.seriesName.isNotBlank() && epInfo.seriesName != rawTitle) {
            epInfo.seriesName
        } else {
            rawTitle
        }
        return extractMovieMeta(seriesCandidate)
    }

    /**
     * Resolve panel-provided relative image URL
     */
    fun resolvePanelImageUrl(path: String?, size: String = "w342"): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
            return path
        }
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "https://image.tmdb.org/t/p/$size$cleanPath"
    }
}
