package com.example.util

import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.parser.XtreamClient
import java.util.Locale

/**
 * Represents a genre option with genre ID and display metadata.
 */
data class GenreOption(
    val genreId: Int,
    val name: String,
    val relatedGenreIds: Set<Int> = setOf(genreId),
    val keywords: List<String> = emptyList()
)

/**
 * Helper to match playlist items with genres, analyze user watch history,
 * and provide genre-filtered recommendations for HomeScreen.
 */
object GenreHelper {

    val GENRE_OPTIONS: List<GenreOption> = listOf(
        GenreOption(
            genreId = 28,
            name = "Aksiyon",
            relatedGenreIds = setOf(28, 10759, 12),
            keywords = listOf("aksiyon", "action")
        ),
        GenreOption(
            genreId = 35,
            name = "Komedi",
            relatedGenreIds = setOf(35),
            keywords = listOf("komedi", "comedy", "mizah")
        ),
        GenreOption(
            genreId = 18,
            name = "Dram",
            relatedGenreIds = setOf(18),
            keywords = listOf("dram", "drama")
        ),
        GenreOption(
            genreId = 878,
            name = "Bilim Kurgu",
            relatedGenreIds = setOf(878, 10765, 14),
            keywords = listOf("bilim kurgu", "bilimkurgu", "sci-fi", "science fiction", "fantastik", "fantasy")
        ),
        GenreOption(
            genreId = 27,
            name = "Korku",
            relatedGenreIds = setOf(27, 53, 9648),
            keywords = listOf("korku", "horror", "gerilim", "thriller")
        ),
        GenreOption(
            genreId = 10749,
            name = "Romantik",
            relatedGenreIds = setOf(10749),
            keywords = listOf("romantik", "romance", "aşk")
        ),
        GenreOption(
            genreId = 16,
            name = "Animasyon",
            relatedGenreIds = setOf(16),
            keywords = listOf("animasyon", "animation", "çizgi", "anime", "cartoon")
        ),
        GenreOption(
            genreId = 99,
            name = "Belgesel",
            relatedGenreIds = setOf(99),
            keywords = listOf("belgesel", "documentary", "doc")
        ),
        GenreOption(
            genreId = 80,
            name = "Suç",
            relatedGenreIds = setOf(80, 9648),
            keywords = listOf("suç", "crime", "polisiye")
        ),
        GenreOption(
            genreId = 12,
            name = "Macera",
            relatedGenreIds = setOf(12, 10759),
            keywords = listOf("macera", "adventure")
        )
    )

    val ALL_GENRES: List<String> = GENRE_OPTIONS.map { it.name }

    fun getGenreById(genreId: Int?): GenreOption? {
        if (genreId == null) return null
        return GENRE_OPTIONS.find { it.genreId == genreId }
    }

    fun getGenreByName(name: String?): GenreOption? {
        if (name.isNullOrBlank()) return null
        return GENRE_OPTIONS.find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Checks if a playlist item matches the specified genreId.
     */
    fun matchesGenreId(item: PlaylistItem, targetGenreId: Int): Boolean {
        val option = getGenreById(targetGenreId)
        val targetName = option?.name ?: ""
        val keywords = option?.keywords ?: if (targetName.isNotBlank()) listOf(targetName.lowercase(Locale("tr"))) else emptyList()

        // 1. Check Xtream Cached Card Meta
        val cachedMeta = XtreamClient.getCachedCardMeta(item.id)
        if (cachedMeta != null) {
            val genreLower = cachedMeta.genre.lowercase(Locale("tr"))
            if (targetName.isNotBlank() && genreLower.contains(targetName.lowercase(Locale("tr")))) return true
            if (keywords.any { kw -> kw.isNotBlank() && genreLower.contains(kw) }) return true
        }

        // 2. Check Item Category
        val catLower = item.category.lowercase(Locale("tr"))
        if (targetName.isNotBlank() && catLower.contains(targetName.lowercase(Locale("tr")))) return true
        for (kw in keywords) {
            if (kw.isNotBlank() && catLower.contains(kw)) return true
        }

        // 3. Check SubtitleInfo & Description
        val subLower = item.subtitleInfo.lowercase(Locale("tr"))
        if (targetName.isNotBlank() && subLower.contains(targetName.lowercase(Locale("tr")))) return true
        for (kw in keywords) {
            if (kw.isNotBlank() && subLower.contains(kw)) return true
        }

        val descLower = item.description.lowercase(Locale("tr"))
        if (targetName.isNotBlank() && descLower.contains(targetName.lowercase(Locale("tr")))) return true

        // 4. Check Item Clean Title
        val isSeries = item.type == ItemType.VOD_SERIES || item.type == ItemType.EPISODE
        val cleanTitle = if (isSeries) {
            TitleNormalizer.extractSeriesMeta(item.name).cleanTitle
        } else {
            TitleNormalizer.extractMovieMeta(item.name).cleanTitle
        }.lowercase(Locale("tr"))

        for (kw in keywords) {
            if (kw.isNotBlank() && cleanTitle.contains(kw) && !cleanTitle.contains("sezon") && !cleanTitle.contains("bölüm")) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if a playlist item matches the specified genre by name (backward compatibility).
     */
    fun matchesGenre(item: PlaylistItem, targetGenre: String): Boolean {
        val option = getGenreByName(targetGenre)
        return if (option != null) {
            matchesGenreId(item, option.genreId)
        } else {
            val targetLower = targetGenre.lowercase(Locale("tr"))
            item.category.lowercase(Locale("tr")).contains(targetLower) ||
            item.subtitleInfo.lowercase(Locale("tr")).contains(targetLower) ||
            item.name.lowercase(Locale("tr")).contains(targetLower)
        }
    }

    /**
     * Analyzes user watch history and extracts top 1 or 2 most watched genres.
     */
    fun findTopGenresFromHistory(history: List<PlaylistItem>): List<String> {
        if (history.isEmpty()) return emptyList()

        val counts = mutableMapOf<String, Int>()
        for (item in history) {
            for (genre in GENRE_OPTIONS) {
                if (matchesGenreId(item, genre.genreId)) {
                    counts[genre.name] = (counts[genre.name] ?: 0) + 1
                }
            }
        }

        return counts.entries
            .sortedByDescending { it.value }
            .filter { it.value > 0 }
            .map { it.key }
            .take(2)
    }
}
