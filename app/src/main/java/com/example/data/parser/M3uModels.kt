package com.example.data.parser

import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem

/**
 * Detailed representation of an item extracted from an M3U / M3U_PLUS playlist.
 */
data class M3uEntry(
    val id: String,
    val title: String,
    val streamUrl: String,
    val group: String = "Genel",
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgLogo: String? = null,
    val tvgShift: String? = null,
    val tvgLanguage: String? = null,
    val tvgCountry: String? = null,
    val durationSeconds: Long = -1L,
    val type: ItemType = ItemType.LIVE_TV,
    val customHeaders: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    val referrer: String? = null,
    val catchup: String? = null,
    val catchupDays: Int? = null,
    val isRadio: Boolean = false,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val seriesName: String? = null
) {
    /**
     * Converts the parsed M3uEntry into a PlaylistItem database entity.
     */
    fun toPlaylistItem(playlistSourceId: String): PlaylistItem {
        val finalSeriesId = if (type == ItemType.VOD_SERIES || episodeNumber != null) {
            seriesName?.trim()?.lowercase()?.replace(Regex("[^a-z0-9]"), "_") ?: id
        } else null

        val finalType = when {
            episodeNumber != null -> ItemType.EPISODE
            else -> type
        }

        return PlaylistItem(
            id = id,
            name = title.ifBlank { tvgName ?: "Kanal" },
            logoUrl = logoUrl?.takeIf { it.isNotBlank() } ?: tvgLogo?.takeIf { it.isNotBlank() },
            streamUrl = streamUrl,
            category = group.ifBlank { "Genel" },
            type = finalType,
            rating = if (finalType != ItemType.LIVE_TV) 7.5 else 0.0,
            year = "2024",
            duration = if (durationSeconds > 0) "${durationSeconds / 60} dk" else "",
            description = when (finalType) {
                ItemType.LIVE_TV -> "Canlı Yayın"
                ItemType.VOD_MOVIE -> "VOD Sinema Filmi"
                ItemType.VOD_SERIES, ItemType.EPISODE -> "Dizi İçeriği"
            },
            cast = "",
            subtitleInfo = group,
            isFavorite = false,
            isDownloaded = false,
            playlistSourceId = playlistSourceId,
            epgChannelId = tvgId?.takeIf { it.isNotBlank() } ?: tvgName?.takeIf { it.isNotBlank() },
            seriesId = finalSeriesId,
            seasonNumber = seasonNumber ?: 1,
            episodeNumber = episodeNumber ?: 1
        )
    }
}

/**
 * Metadata extracted from the #EXTM3U header line.
 */
data class M3uPlaylistHeader(
    val tvgUrl: String? = null,
    val refreshIntervalHours: Int? = null,
    val userAgent: String? = null,
    val rawAttributes: Map<String, String> = emptyMap()
)

/**
 * Extracted channel metadata from an M3U playlist entry.
 * Captures core IPTV metadata: channel name, stream URL, and group title,
 * along with rich attributes like logo URL, EPG/tvg ID, tvg name, duration, and custom HTTP headers.
 */
data class M3uChannelMetadata(
    val name: String,
    val streamUrl: String,
    val groupTitle: String = "Genel",
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgShift: String? = null,
    val tvgLanguage: String? = null,
    val tvgCountry: String? = null,
    val durationSeconds: Long = -1L,
    val isRadio: Boolean = false,
    val httpHeaders: Map<String, String> = emptyMap(),
    val rawAttributes: Map<String, String> = emptyMap()
) {
    /**
     * Converts this channel metadata into a database PlaylistItem entity.
     */
    fun toPlaylistItem(
        playlistSourceId: String,
        itemType: ItemType = ItemType.LIVE_TV,
        idOverride: String? = null
    ): PlaylistItem {
        val uniqueId = idOverride
            ?: (tvgId?.takeIf { it.isNotBlank() }
                ?: "${playlistSourceId}_${(streamUrl + name).hashCode().toUInt().toString(16)}")

        return PlaylistItem(
            id = uniqueId,
            name = name.ifBlank { tvgName ?: "Kanal" },
            logoUrl = logoUrl?.takeIf { it.isNotBlank() },
            streamUrl = streamUrl,
            category = groupTitle.ifBlank { "Genel" },
            type = itemType,
            rating = 0.0,
            year = "2024",
            duration = if (durationSeconds > 0) "${durationSeconds / 60} dk" else "",
            description = if (isRadio) "Radyo Yayını" else "Canlı TV Yayını",
            playlistSourceId = playlistSourceId,
            epgChannelId = tvgId?.takeIf { it.isNotBlank() } ?: tvgName
        )
    }
}

/**
 * Real-time progress update during large playlist streaming parse.
 */
data class M3uParseProgress(
    val parsedCount: Int,
    val lastGroup: String,
    val bytesRead: Long = 0L,
    val totalBytes: Long = -1L,
    val percent: Int? = null
)

/**
 * Summary of a completed M3U parse operation.
 */
data class M3uParseSummary(
    val totalCount: Int,
    val liveCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val groups: Set<String>,
    val header: M3uPlaylistHeader
)
