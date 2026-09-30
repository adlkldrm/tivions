package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.repository.IptvRepository
import com.example.util.GenreHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

/**
 * Filter category mapping genre IDs to display names, related IDs, and matching keywords.
 */
data class GenreFilterCategory(
    val genreId: Int,
    val name: String,
    val relatedGenreIds: Set<Int> = setOf(genreId),
    val categoryKeywords: List<String> = emptyList()
)

/**
 * ViewModel responsible for Home screen state, specifically managing genre ID
 * mapping to filter categories and recommendations filtering for 'Senin İçin Önerilenler'.
 */
class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val repository: IptvRepository = IptvRepository(AppDatabase.getInstance(application).itemDao())
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_SELECTED_GENRE_ID = "home_selected_genre_id"

        /**
         * Master list of filter categories mapped to genre IDs.
         */
        val GENRE_CATEGORIES: List<GenreFilterCategory> = listOf(
            GenreFilterCategory(
                genreId = 28,
                name = "Aksiyon",
                relatedGenreIds = setOf(28, 10759, 12),
                categoryKeywords = listOf("aksiyon", "action")
            ),
            GenreFilterCategory(
                genreId = 35,
                name = "Komedi",
                relatedGenreIds = setOf(35),
                categoryKeywords = listOf("komedi", "comedy", "mizah")
            ),
            GenreFilterCategory(
                genreId = 18,
                name = "Dram",
                relatedGenreIds = setOf(18),
                categoryKeywords = listOf("dram", "drama")
            ),
            GenreFilterCategory(
                genreId = 878,
                name = "Bilim Kurgu",
                relatedGenreIds = setOf(878, 10765, 14),
                categoryKeywords = listOf("bilim kurgu", "bilimkurgu", "sci-fi", "science fiction", "fantastik", "fantasy")
            ),
            GenreFilterCategory(
                genreId = 27,
                name = "Korku",
                relatedGenreIds = setOf(27, 53, 9648),
                categoryKeywords = listOf("korku", "horror", "gerilim", "thriller")
            ),
            GenreFilterCategory(
                genreId = 10749,
                name = "Romantik",
                relatedGenreIds = setOf(10749),
                categoryKeywords = listOf("romantik", "romance", "aşk")
            ),
            GenreFilterCategory(
                genreId = 16,
                name = "Animasyon",
                relatedGenreIds = setOf(16),
                categoryKeywords = listOf("animasyon", "animation", "çizgi", "anime", "cartoon")
            ),
            GenreFilterCategory(
                genreId = 99,
                name = "Belgesel",
                relatedGenreIds = setOf(99),
                categoryKeywords = listOf("belgesel", "documentary", "doc")
            ),
            GenreFilterCategory(
                genreId = 80,
                name = "Suç",
                relatedGenreIds = setOf(80, 9648),
                categoryKeywords = listOf("suç", "crime", "polisiye")
            ),
            GenreFilterCategory(
                genreId = 12,
                name = "Macera",
                relatedGenreIds = setOf(12, 10759),
                categoryKeywords = listOf("macera", "adventure")
            )
        )
    }

    val genreCategories: List<GenreFilterCategory> = GENRE_CATEGORIES

    // State: Selected Genre ID (retained via SavedStateHandle)
    private val _selectedGenreId = MutableStateFlow<Int?>(savedStateHandle.get<Int>(KEY_SELECTED_GENRE_ID))
    val selectedGenreId: StateFlow<Int?> = _selectedGenreId.asStateFlow()

    // State: Active Category mapped from selected genre ID
    private val _activeGenreCategory = MutableStateFlow<GenreFilterCategory?>(
        _selectedGenreId.value?.let { id -> genreCategories.find { it.genreId == id } }
    )
    val activeGenreCategory: StateFlow<GenreFilterCategory?> = _activeGenreCategory.asStateFlow()

    // Source data flows (can be supplied from repository or set from screen)
    private val _sourceRecommendations = MutableStateFlow<List<PlaylistItem>>(emptyList())
    private val _sourceWatchHistory = MutableStateFlow<List<PlaylistItem>>(emptyList())

    private val repoCandidates: Flow<List<PlaylistItem>> = repository.getRecommendationCandidates()
    private val repoHistory: Flow<List<PlaylistItem>> = repository.getWatchHistory()
    private val cachedRecsFlow: StateFlow<Map<String, List<PlaylistItem>>> = repository.cachedRecommendationsByTab

    /**
     * Filtered recommendations for 'Senin İçin Önerilenler' based on selected genre ID or watch history.
     */
    val filteredRecommendations: StateFlow<List<PlaylistItem>> = combine(
        cachedRecsFlow,
        _selectedGenreId,
        combine(_sourceRecommendations, repoCandidates) { src, repo -> if (src.isNotEmpty()) src else repo },
        combine(_sourceWatchHistory, repoHistory) { src, repo -> if (src.isNotEmpty()) src else repo }
    ) { cachedRecs: Map<String, List<PlaylistItem>>, genreId: Int?, pool: List<PlaylistItem>, history: List<PlaylistItem> ->
        val activeCatName = if (genreId == null) "Tümü" else mapGenreIdToCategory(genreId)?.name ?: ""
        val cachedList = if (activeCatName.isNotBlank()) cachedRecs[activeCatName] else null
        if (!cachedList.isNullOrEmpty()) {
            cachedList
        } else {
            filterItemsByGenre(pool, history, genreId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Maps a given genre ID to its corresponding filter category.
     */
    fun mapGenreIdToCategory(genreId: Int): GenreFilterCategory? {
        return genreCategories.find { it.genreId == genreId || it.relatedGenreIds.contains(genreId) }
    }

    /**
     * Toggles or selects a genre ID. Clicking the same genre toggles it off.
     */
    fun selectGenre(genreId: Int?) {
        val nextId = if (_selectedGenreId.value == genreId) null else genreId
        _selectedGenreId.value = nextId
        _activeGenreCategory.value = nextId?.let { id -> genreCategories.find { it.genreId == id } }
        savedStateHandle[KEY_SELECTED_GENRE_ID] = nextId
    }

    /**
     * Explicitly resets the genre filter to show all recommendations.
     */
    fun resetGenreFilter() {
        _selectedGenreId.value = null
        _activeGenreCategory.value = null
        savedStateHandle[KEY_SELECTED_GENRE_ID] = null
    }

    /**
     * Supplies source content from parent composables or data loaders.
     */
    fun setSourceData(
        movies: List<PlaylistItem>,
        series: List<PlaylistItem>,
        recommendations: List<PlaylistItem>,
        watchHistory: List<PlaylistItem>
    ) {
        val combinedRecs = mutableListOf<PlaylistItem>()
        combinedRecs.addAll(recommendations.take(30))
        combinedRecs.addAll(movies.take(30))
        combinedRecs.addAll(series.take(30))
        _sourceRecommendations.value = combinedRecs.distinctBy { it.id }.take(60)
        _sourceWatchHistory.value = watchHistory.take(30)
    }

    /**
     * Filters playlist items by genre ID or personalizes using watch history when null.
     */
    fun filterItemsByGenre(
        pool: List<PlaylistItem>,
        history: List<PlaylistItem>,
        genreId: Int?
    ): List<PlaylistItem> {
        val sortComparator = compareByDescending<PlaylistItem> { it.rating5based }
            .thenByDescending { it.ratingValue }
            .thenByDescending { it.rating }

        if (genreId == null) {
            // Personalized recommendations based on user watch history
            if (history.isNotEmpty()) {
                val genreCounts = mutableMapOf<String, Int>()
                for (item in history) {
                    val genres = item.genre.split(",", ";", "/", "&").map { it.trim() }.filter { it.isNotBlank() }
                    if (genres.isNotEmpty()) {
                        for (g in genres) {
                            genreCounts[g.lowercase(Locale("tr"))] = (genreCounts[g.lowercase(Locale("tr"))] ?: 0) + 1
                        }
                    } else if (item.category.isNotBlank()) {
                        genreCounts[item.category.lowercase(Locale("tr"))] = (genreCounts[item.category.lowercase(Locale("tr"))] ?: 0) + 1
                    }
                }
                val topGenres = genreCounts.entries.sortedByDescending { it.value }.map { it.key }.take(2)
                if (topGenres.isNotEmpty()) {
                    val matched = pool.filter { item ->
                        val itemGenreLower = item.genre.lowercase(Locale("tr"))
                        val itemCatLower = item.category.lowercase(Locale("tr"))
                        topGenres.any { tg -> itemGenreLower.contains(tg) || itemCatLower.contains(tg) }
                    }.sortedWith(sortComparator)
                    if (matched.isNotEmpty()) {
                        return matched.take(10)
                    }
                }
            }

            // Default behavior when no history or no matches: top 10 by rating
            return pool.sortedWith(sortComparator).take(10)
        } else {
            val category = mapGenreIdToCategory(genreId)
            val catName = category?.name ?: ""
            val catKeywords = category?.categoryKeywords ?: emptyList()
            val catNameLower = catName.lowercase(Locale("tr"))

            val matched = pool.filter { item ->
                val itemGenreLower = item.genre.lowercase(Locale("tr"))
                val itemCatLower = item.category.lowercase(Locale("tr"))
                val itemSubLower = item.subtitleInfo.lowercase(Locale("tr"))
                val itemNameLower = item.name.lowercase(Locale("tr"))

                (catNameLower.isNotBlank() && (itemGenreLower.contains(catNameLower) || itemCatLower.contains(catNameLower))) ||
                catKeywords.any { kw -> itemGenreLower.contains(kw) || itemCatLower.contains(kw) || itemSubLower.contains(kw) || itemNameLower.contains(kw) } ||
                GenreHelper.matchesGenreId(item, genreId)
            }.sortedWith(sortComparator)

            return matched.take(10)
        }
    }
}
