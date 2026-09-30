package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SourceType {
    M3U,
    XTREAM
}

@Entity(tableName = "playlist_sources")
data class PlaylistSource(
    @PrimaryKey val id: String,
    val name: String,
    val type: SourceType,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val remember: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
