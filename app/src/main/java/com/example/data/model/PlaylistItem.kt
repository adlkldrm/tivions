package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ItemType {
    LIVE_TV,
    VOD_MOVIE,
    VOD_SERIES,
    EPISODE
}

@Entity(
    tableName = "playlist_items",
    indices = [
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["type", "category"]),
        Index(value = ["isFavorite"]),
        Index(value = ["playlistSourceId"]),
        Index(value = ["epgChannelId"]),
        Index(value = ["lastWatchedTimestamp"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["orderIndex"]),
        Index(value = ["globalOrderIndex"]),
        Index(value = ["ratingValue"]),
        Index(value = ["rating5based"]),
        Index(value = ["releaseYear"])
    ]
)
data class PlaylistItem(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String? = null,
    val streamUrl: String,
    val category: String,
    val type: ItemType,
    val rating: Double = 0.0,
    val year: String = "2024",
    val duration: String = "",
    val description: String = "",
    @ColumnInfo(name = "actors") val cast: String = "",
    val subtitleInfo: String = "",
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadLocalPath: String? = null,
    val isAdult: Boolean = false,
    val playlistSourceId: String = "default",
    val lastWatchedTimestamp: Long = 0L,
    val watchCount: Int = 0,
    val seriesId: String? = null,
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val epgChannelId: String? = null,
    val categoryId: String? = null,
    val orderIndex: Int = 0,
    val tmdbId: Long? = null,
    val mediaType: String? = null,
    val ratingValue: Double = 0.0,
    val rating5based: Double = 0.0,
    val releaseYear: Int = 0,
    val globalOrderIndex: Int = 0,
    val genre: String = ""
)

data class CategorySummary(
    val category: String,
    val count: Int,
    val type: ItemType
)

data class ActorInfo(
    val initials: String,
    val name: String,
    val colorHex: Long
)
