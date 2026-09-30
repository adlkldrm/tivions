package com.example.data.repository

import com.example.data.local.ItemDao
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSource
import com.example.data.model.SourceType
import com.example.data.parser.M3uChannelMetadata
import com.example.data.parser.M3uParseProgress
import com.example.data.parser.M3uParseSummary
import com.example.data.parser.M3uPlaylistHeader
import com.example.data.parser.M3uParsingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID

/**
 * Repository interface for parsing M3U/M3U_PLUS playlist files,
 * extracting channel metadata (names, stream URLs, group titles, TVG logos, EPG IDs),
 * and managing playlist persistence.
 */
interface IM3uPlaylistRepository {
    suspend fun parseChannels(m3uContent: String): List<M3uChannelMetadata>
    fun parseChannelsFromStream(inputStream: InputStream, baseUrl: String? = null): Flow<List<M3uChannelMetadata>>
    fun parseChannelsFromFile(file: File): Flow<List<M3uChannelMetadata>>
    fun parseChannelsFromUrl(url: String): Flow<List<M3uChannelMetadata>>
    suspend fun extractGroupTitles(m3uContent: String): List<String>
    suspend fun extractChannelsByGroup(m3uContent: String, groupTitle: String): List<M3uChannelMetadata>
    suspend fun getPlaylistSummary(m3uContent: String): M3uParseSummary
    suspend fun importPlaylistFromUrl(
        sourceName: String,
        url: String,
        remember: Boolean = true,
        onProgress: ((M3uParseProgress) -> Unit)? = null
    ): Result<Int>
    suspend fun saveChannels(
        channels: List<M3uChannelMetadata>,
        playlistSourceId: String,
        sourceName: String = "M3U Playlist"
    )
    fun getSavedChannels(playlistSourceId: String): Flow<List<PlaylistItem>>
}

/**
 * Default production implementation of [IM3uPlaylistRepository].
 * Provides streaming parsing for massive playlists (100k+ channels)
 * while preserving low memory footprint and high extraction accuracy.
 */
class M3uPlaylistRepository(
    private val itemDao: ItemDao? = null,
    private val parserService: M3uParsingService = M3uParsingService.INSTANCE
) : IM3uPlaylistRepository {

    /**
     * Parses in-memory M3U text and returns all extracted channels with metadata:
     * - Channel name
     * - Stream URL
     * - Group title / Category
     * - Logo URL (tvg-logo)
     * - EPG channel identifier (tvg-id)
     * - TVG Name (tvg-name)
     * - Custom headers (User-Agent, Referer, pipe delimiters)
     */
    override suspend fun parseChannels(m3uContent: String): List<M3uChannelMetadata> =
        withContext(Dispatchers.Default) {
            parserService.parseChannels(m3uContent)
        }

    /**
     * Streams an M3U [InputStream] and emits chunks of [M3uChannelMetadata].
     */
    override fun parseChannelsFromStream(
        inputStream: InputStream,
        baseUrl: String?
    ): Flow<List<M3uChannelMetadata>> {
        return parserService.parseChannelsStream(inputStream, baseUrl)
    }

    /**
     * Parses a local M3U file in streaming chunks without loading the entire file into memory.
     */
    override fun parseChannelsFromFile(file: File): Flow<List<M3uChannelMetadata>> = flow {
        FileInputStream(file).use { stream ->
            parserService.parseChannelsStream(stream, null).collect { chunk ->
                emit(chunk)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Downloads an M3U playlist from a remote URL and emits chunks of [M3uChannelMetadata].
     */
    override fun parseChannelsFromUrl(url: String): Flow<List<M3uChannelMetadata>> {
        return parserService.fetchAndParseChannelsStreaming(url)
    }

    /**
     * Extracts distinct group titles / categories found across the channels in the M3U content.
     */
    override suspend fun extractGroupTitles(m3uContent: String): List<String> =
        withContext(Dispatchers.Default) {
            val channels = parseChannels(m3uContent)
            channels.map { it.groupTitle }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }

    /**
     * Filters and returns channels matching a specific group title.
     */
    override suspend fun extractChannelsByGroup(
        m3uContent: String,
        groupTitle: String
    ): List<M3uChannelMetadata> = withContext(Dispatchers.Default) {
        val channels = parseChannels(m3uContent)
        channels.filter { it.groupTitle.equals(groupTitle, ignoreCase = true) }
    }

    /**
     * Generates a statistical summary of the parsed M3U content,
     * including group counts and header info.
     */
    override suspend fun getPlaylistSummary(m3uContent: String): M3uParseSummary =
        withContext(Dispatchers.Default) {
            val channels = parseChannels(m3uContent)
            val groups = channels.map { it.groupTitle }.toSet()
            val radioCount = channels.count { it.isRadio }
            val liveCount = channels.count { !it.isRadio }

            M3uParseSummary(
                totalCount = channels.size,
                liveCount = liveCount,
                movieCount = 0,
                seriesCount = 0,
                groups = groups,
                header = M3uPlaylistHeader()
            )
        }

    /**
     * Downloads, streams, parses, and saves a remote M3U playlist into the Room database.
     */
    override suspend fun importPlaylistFromUrl(
        sourceName: String,
        url: String,
        remember: Boolean,
        onProgress: ((M3uParseProgress) -> Unit)?
    ): Result<Int> = withContext(Dispatchers.IO) {
        val dao = itemDao ?: return@withContext Result.failure(
            IllegalStateException("ItemDao is not provided to M3uPlaylistRepository")
        )

        try {
            val sourceId = "m3u_${UUID.randomUUID()}"
            var totalCount = 0
            var firstChunk = true

            android.util.Log.i("M3U_IMPORT", "M3U içe aktarımı başlatılıyor: $url")

            parserService.fetchAndParseStreaming(
                url = url,
                playlistSourceId = sourceId,
                chunkSize = 300,
                onProgress = onProgress
            ).collect { chunk ->
                if (firstChunk) {
                    firstChunk = false
                    dao.clearAllItems()
                }
                dao.insertAll(chunk)
                totalCount += chunk.size
            }

            if (totalCount == 0) {
                return@withContext Result.failure(Exception("M3U dosyasında oynatılabilir geçerli bir kanal veya film bulunamadı."))
            }

            val dbCount = dao.getItemCountBySource(sourceId).let { if (it > 0) it else dao.getItemCount() }
            if (dbCount == totalCount) {
                android.util.Log.i("M3U_IMPORT", "M3U'da $totalCount kayıt bulundu, veritabanına $dbCount kayıt yazıldı ✅")
            } else {
                android.util.Log.w("M3U_IMPORT", "⚠️ UYARI: M3U'da $totalCount kayıt bulundu, veritabanına $dbCount kayıt yazıldı. Fark: ${totalCount - dbCount}")
            }

            if (remember) {
                dao.insertSource(
                    PlaylistSource(
                        id = sourceId,
                        name = sourceName.ifBlank { "M3U Playlist" },
                        type = SourceType.M3U,
                        url = url
                    )
                )
            }

            Result.success(totalCount)
        } catch (t: Throwable) {
            val errorMsg = if (t.message?.startsWith("İçe aktarma sırasında hata oluştu") == true) {
                t.message!!
            } else {
                "İçe aktarma sırasında hata oluştu: ${t.localizedMessage ?: t.javaClass.simpleName}"
            }
            android.util.Log.e("M3U_IMPORT", errorMsg, t)
            Result.failure(Exception(errorMsg, t))
        }
    }

    /**
     * Saves a list of extracted [M3uChannelMetadata] channels into the database as [PlaylistItem] entries.
     */
    override suspend fun saveChannels(
        channels: List<M3uChannelMetadata>,
        playlistSourceId: String,
        sourceName: String
    ) = withContext(Dispatchers.IO) {
        val dao = itemDao ?: return@withContext
        val items = channels.map { meta ->
            meta.toPlaylistItem(
                playlistSourceId = playlistSourceId,
                itemType = if (meta.isRadio) ItemType.LIVE_TV else ItemType.LIVE_TV
            )
        }
        dao.insertAll(items)
        dao.insertSource(
            PlaylistSource(
                id = playlistSourceId,
                name = sourceName,
                type = SourceType.M3U,
                url = ""
            )
        )
    }

    /**
     * Retrieves all saved channels for a given playlist source ID.
     */
    override fun getSavedChannels(playlistSourceId: String): Flow<List<PlaylistItem>> {
        val dao = itemDao ?: throw IllegalStateException("ItemDao is not configured")
        return dao.getAllItems()
    }
}
