package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BÖLÜM 7 & BÖLÜM 10: WatchRecord
 * Tracks user watch progress, favorite status, and recency for Continue Watching & Recommendations.
 */
@Entity(
    tableName = "watch_records",
    indices = [
        Index(value = ["m3uKey"]),
        Index(value = ["type"]),
        Index(value = ["lastWatchedAt"]),
        Index(value = ["isFavorite"])
    ]
)
data class WatchRecord(
    @PrimaryKey val id: String, // item ID or compound key
    val m3uKey: String,         // normalizedTitle(name), without season/episode
    val itemId: String,
    val title: String,
    val tmdbId: Long? = null,
    val type: String = "movie", // "movie" | "tv"
    val progress: Float = 0f,   // 0.0 to 1.0 (between 0.02 and 0.92 for Continue Watching)
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val genreIds: String = "",  // Comma separated genre ids
    val quality: String? = null,// 4K, 1080p, HD, SD
    val year: String? = null,
    val seriesKey: String? = null,
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val stillUrl: String? = null,
    val streamUrl: String = ""
)

/**
 * BÖLÜM 8 & BÖLÜM 9: LibraryItemMeta
 * Tracks when a movie or series was first seen in the playlist for "Son Eklenenler".
 */
@Entity(
    tableName = "library_item_meta",
    primaryKeys = ["playlistId", "itemKey"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["firstSeenAt"])
    ]
)
data class LibraryItemMeta(
    val playlistId: String,
    val itemKey: String,
    val type: String, // "movie" | "tv"
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis()
)

/**
 * BÖLÜM 14: CategoryPref
 * Stores custom category order and hidden status for Live TV, Movies, and Series.
 */
@Entity(
    tableName = "category_prefs",
    primaryKeys = ["playlistId", "section", "categoryKey"],
    indices = [
        Index(value = ["playlistId", "section"])
    ]
)
data class CategoryPref(
    val playlistId: String,
    val section: String,     // "LIVE" | "MOVIE" | "SERIES"
    val categoryKey: String,  // lowercase normalized group title
    val position: Int = 0,
    val hidden: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * BÖLÜM 13: EpisodeProgress
 * Tracks per-episode watching progress for TV series.
 */
@Entity(
    tableName = "episode_progress",
    primaryKeys = ["seriesKey", "seasonNumber", "episodeNumber"]
)
data class EpisodeProgress(
    val seriesKey: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val progress: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * BÖLÜM 10 & 11: ProgramReminder
 * Electronic Program Guide reminder alarms.
 */
@Entity(tableName = "program_reminders")
data class ProgramReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: String,
    val channelId: String,
    val channelName: String,
    val programTitle: String,
    val startTimeMillis: Long,
    val leadMinutes: Int = 5
)

/**
 * Persisted Category Item Counts for Live TV, Movies, and Series.
 * Computed after import to provide instant, unpaginated category counts.
 */
@Entity(
    tableName = "category_counts",
    primaryKeys = ["section", "category"]
)
data class CategoryCountEntity(
    val section: String, // "LIVE" | "MOVIE" | "SERIES"
    val category: String,
    val categoryId: String = "",
    val itemCount: Int = 0,
    val orderIndex: Int = 0
)

/**
 * Persisted Category for Live TV, Movies, and Series.
 * Preserves exact API orderIndex and tracks itemCount and user reordering.
 */
@Entity(
    tableName = "categories",
    primaryKeys = ["type", "categoryId"]
)
data class CategoryEntity(
    val type: String, // "LIVE", "MOVIE", "SERIES"
    val categoryId: String,
    val categoryName: String,
    val orderIndex: Int = 0,
    val userOrderIndex: Int? = null,
    val itemCount: Int = 0,
    val isHidden: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

