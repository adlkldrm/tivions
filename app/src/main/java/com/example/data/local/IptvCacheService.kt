package com.example.data.local

import com.example.data.model.CacheSummary
import com.example.data.model.EpgProgram
import com.example.data.model.ItemType
import com.example.data.model.PlaylistCacheMetadata
import com.example.data.model.PlaylistItem
import com.example.data.parser.M3uParseProgress
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production-ready caching service powered by Room database.
 * Caches parsed M3U playlists and XMLTV EPG data locally, ensuring complete offline
 * functionality, instant app responsiveness, and atomic cache invalidation.
 */
class IptvCacheService(private val dao: ItemDao) {

    companion object {
        const val DEFAULT_BATCH_SIZE = 500
        const val EPG_RETENTION_PAST_HOURS = 24L // Keep programs from last 24h for timeshift / catchup
    }

    /**
     * Caches a full list of parsed [PlaylistItem] entities into Room database atomically.
     * Updates [PlaylistCacheMetadata] to track sync status, item counts, and offline availability.
     */
    suspend fun cachePlaylistItems(
        sourceId: String,
        sourceName: String,
        sourceUrl: String,
        items: List<PlaylistItem>,
        clearExisting: Boolean = true,
        epgSourceUrl: String? = null
    ): CacheSummary = withContext(Dispatchers.IO) {
        val liveCount = items.count { it.type == ItemType.LIVE_TV }
        val movieCount = items.count { it.type == ItemType.VOD_MOVIE }
        val seriesCount = items.count { it.type == ItemType.VOD_SERIES || it.type == ItemType.EPISODE }
        val epgCount = dao.getEpgCount()

        // Estimate size based on rough entity byte footprint (~180 bytes per item)
        val estimatedBytes = items.size * 180L

        val metadata = PlaylistCacheMetadata(
            sourceId = sourceId,
            sourceName = sourceName.ifBlank { "M3U Listesi" },
            sourceUrl = sourceUrl,
            totalItems = items.size,
            liveCount = liveCount,
            movieCount = movieCount,
            seriesCount = seriesCount,
            epgCount = epgCount,
            cachedAtTimestamp = System.currentTimeMillis(),
            lastValidatedTimestamp = System.currentTimeMillis(),
            isOfflineAvailable = items.isNotEmpty(),
            epgSourceUrl = epgSourceUrl,
            estimatedSizeBytes = estimatedBytes
        )

        // Insert atomically inside database transaction
        dao.replacePlaylistCache(
            sourceId = sourceId,
            items = items,
            metadata = metadata,
            clearExisting = clearExisting
        )

        CacheSummary(
            totalCachedItems = items.size,
            liveChannelsCount = liveCount,
            moviesCount = movieCount,
            seriesCount = seriesCount,
            epgProgramsCount = epgCount,
            activeSourcesCount = 1,
            lastCachedTimestamp = System.currentTimeMillis(),
            isOfflineReady = items.isNotEmpty(),
            estimatedSizeFormatted = formatBytes(estimatedBytes)
        )
    }

    /**
     * Downloads an M3U playlist from network and streams directly into Room in batch chunks,
     * guaranteeing OOM safety and providing real-time UI progress updates.
     */
    suspend fun cacheM3uStreaming(
        url: String,
        sourceName: String,
        sourceId: String = "m3u_${UUID.randomUUID()}",
        clearExisting: Boolean = true,
        onProgress: ((M3uParseProgress) -> Unit)? = null
    ): Result<CacheSummary> = withContext(Dispatchers.IO) {
        try {
            var totalCached = 0
            var liveCount = 0
            var movieCount = 0
            var seriesCount = 0
            var isFirstChunk = true

            M3uParser.fetchAndParseStreaming(
                url = url,
                playlistSourceId = sourceId,
                chunkSize = DEFAULT_BATCH_SIZE,
                onProgress = onProgress
            ).collect { chunk ->
                if (isFirstChunk) {
                    isFirstChunk = false
                    if (clearExisting) {
                        dao.clearAllItems()
                        dao.clearAllCacheMetadata()
                    } else {
                        dao.deleteBySourceId(sourceId)
                        dao.deleteCacheMetadata(sourceId)
                    }
                }

                // Batch insert into Room
                dao.insertAll(chunk)
                totalCached += chunk.size
                liveCount += chunk.count { it.type == ItemType.LIVE_TV }
                movieCount += chunk.count { it.type == ItemType.VOD_MOVIE }
                seriesCount += chunk.count { it.type == ItemType.VOD_SERIES || it.type == ItemType.EPISODE }
            }

            if (totalCached == 0) {
                return@withContext Result.failure(Exception("M3U akışından geçerli kanal veya film bulunamadı."))
            }

            val epgCount = dao.getEpgCount()
            val estimatedBytes = totalCached * 180L

            val metadata = PlaylistCacheMetadata(
                sourceId = sourceId,
                sourceName = sourceName.ifBlank { "M3U Çalma Listesi" },
                sourceUrl = url,
                totalItems = totalCached,
                liveCount = liveCount,
                movieCount = movieCount,
                seriesCount = seriesCount,
                epgCount = epgCount,
                cachedAtTimestamp = System.currentTimeMillis(),
                lastValidatedTimestamp = System.currentTimeMillis(),
                isOfflineAvailable = true,
                estimatedSizeBytes = estimatedBytes
            )
            dao.insertCacheMetadata(metadata)

            val summary = CacheSummary(
                totalCachedItems = totalCached,
                liveChannelsCount = liveCount,
                moviesCount = movieCount,
                seriesCount = seriesCount,
                epgProgramsCount = epgCount,
                activeSourcesCount = 1,
                lastCachedTimestamp = System.currentTimeMillis(),
                isOfflineReady = true,
                estimatedSizeFormatted = formatBytes(estimatedBytes)
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stores parsed EPG programs locally in Room database.
     * Optionally removes old expired programs that ended more than [EPG_RETENTION_PAST_HOURS] ago.
     */
    suspend fun cacheEpgPrograms(
        programs: List<EpgProgram>,
        purgeOldPrograms: Boolean = true,
        retentionPastHours: Long = EPG_RETENTION_PAST_HOURS
    ): Int = withContext(Dispatchers.IO) {
        if (programs.isEmpty()) return@withContext 0

        val cutoffTime = System.currentTimeMillis() - (retentionPastHours * 3600 * 1000L)
        dao.replaceEpgCache(
            programs = programs,
            purgeOldPrograms = purgeOldPrograms,
            cutoffTime = cutoffTime
        )
        programs.size
    }

    /**
     * Purges expired EPG programs from local database cache.
     */
    suspend fun pruneExpiredEpg(retentionPastHours: Long = EPG_RETENTION_PAST_HOURS): Unit = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (retentionPastHours * 3600 * 1000L)
        dao.deleteOldPrograms(cutoffTime)
    }

    /**
     * Checks whether offline local cache contains usable playlist items.
     */
    suspend fun isOfflineReady(): Boolean = withContext(Dispatchers.IO) {
        dao.getItemCount() > 0
    }

    /**
     * Returns total cached item count in Room.
     */
    suspend fun getCachedItemCount(): Int = withContext(Dispatchers.IO) {
        dao.getItemCount()
    }

    /**
     * Returns total cached EPG program count in Room.
     */
    suspend fun getCachedEpgCount(): Int = withContext(Dispatchers.IO) {
        dao.getEpgCount()
    }

    /**
     * Retrieves current program for channel from local Room cache.
     */
    suspend fun getCurrentProgramForChannel(channelId: String, currentTime: Long = System.currentTimeMillis()): EpgProgram? =
        withContext(Dispatchers.IO) {
            dao.getCurrentProgramForChannel(channelId, currentTime)
        }

    /**
     * Observes programs for a channel from Room cache reactively.
     */
    fun getProgramsForChannel(channelId: String): Flow<List<EpgProgram>> {
        return dao.getProgramsForChannel(channelId)
    }

    /**
     * Observes upcoming programs for a channel from Room cache reactively.
     */
    fun getUpcomingProgramsForChannel(channelId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<EpgProgram>> {
        return dao.getUpcomingProgramsForChannel(channelId, currentTime)
    }

    /**
     * Clears local Room cache for a specific playlist source or completely.
     */
    suspend fun clearCache(sourceId: String? = null) = withContext(Dispatchers.IO) {
        if (sourceId != null) {
            dao.deleteBySourceId(sourceId)
            dao.deleteCacheMetadata(sourceId)
        } else {
            dao.clearAllItems()
            dao.clearAllCacheMetadata()
            dao.clearAllPrograms()
        }
    }

    /**
     * Exposes a reactive summary of local Room cache status.
     */
    fun observeCacheSummary(): Flow<CacheSummary> {
        return combine(
            dao.getLiveCount(),
            dao.getMovieCount(),
            dao.getSeriesCount(),
            dao.getProgramCount(),
            dao.getAllCacheMetadata()
        ) { liveCount, movieCount, seriesCount, epgCount, metadataList ->
            val totalItems = liveCount + movieCount + seriesCount
            val latestCachedTime = metadataList.maxOfOrNull { it.cachedAtTimestamp } ?: 0L
            val estimatedBytes = (totalItems * 180L) + (epgCount * 120L)

            CacheSummary(
                totalCachedItems = totalItems,
                liveChannelsCount = liveCount,
                moviesCount = movieCount,
                seriesCount = seriesCount,
                epgProgramsCount = epgCount,
                activeSourcesCount = metadataList.size.coerceAtLeast(if (totalItems > 0) 1 else 0),
                lastCachedTimestamp = latestCachedTime,
                isOfflineReady = totalItems > 0,
                estimatedSizeFormatted = formatBytes(estimatedBytes)
            )
        }.flowOn(Dispatchers.IO)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        }
    }
}
