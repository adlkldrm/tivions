package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metadata record tracking local Room cache state for a playlist source.
 * Records item counts, sync timestamps, EPG status, and offline availability.
 */
@Entity(tableName = "playlist_cache_metadata")
data class PlaylistCacheMetadata(
    @PrimaryKey val sourceId: String,
    val sourceName: String,
    val sourceUrl: String,
    val totalItems: Int,
    val liveCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val epgCount: Int,
    val cachedAtTimestamp: Long = System.currentTimeMillis(),
    val lastValidatedTimestamp: Long = System.currentTimeMillis(),
    val isOfflineAvailable: Boolean = true,
    val epgSourceUrl: String? = null,
    val estimatedSizeBytes: Long = 0L
) {
    fun formattedCacheTime(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(cachedAtTimestamp))
    }

    val isFresh: Boolean
        get() = (System.currentTimeMillis() - cachedAtTimestamp) < 24 * 3600 * 1000L // 24 hours
}

/**
 * Summary of the overall local cache state across all sources.
 */
data class CacheSummary(
    val totalCachedItems: Int = 0,
    val liveChannelsCount: Int = 0,
    val moviesCount: Int = 0,
    val seriesCount: Int = 0,
    val epgProgramsCount: Int = 0,
    val activeSourcesCount: Int = 0,
    val lastCachedTimestamp: Long = 0L,
    val isOfflineReady: Boolean = false,
    val estimatedSizeFormatted: String = "0 KB"
) {
    fun formattedLastSync(): String {
        if (lastCachedTimestamp <= 0L) return "Henüz önbelleğe alınmadı"
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(lastCachedTimestamp))
    }
}
