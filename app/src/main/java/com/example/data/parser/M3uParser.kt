package com.example.data.parser

import com.example.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.io.InputStream

/**
 * Facade object for M3U playlist parsing, delegating to [M3uParsingService].
 * Provides convenient streaming and batch methods for playlist operations.
 */
object M3uParser {
    val service: M3uParsingService
        get() = M3uParsingService.INSTANCE

    /**
     * Downloads and parses an M3U playlist using streaming chunks to prevent OOM errors.
     */
    fun fetchAndParseStreaming(
        url: String,
        playlistSourceId: String,
        chunkSize: Int = M3uParsingService.DEFAULT_CHUNK_SIZE,
        onProgress: ((M3uParseProgress) -> Unit)? = null
    ): Flow<List<PlaylistItem>> {
        return service.fetchAndParseStreaming(url, playlistSourceId, chunkSize, onProgress)
    }

    /**
     * Reads an InputStream in streaming chunks.
     */
    fun parseStream(
        inputStream: InputStream,
        playlistSourceId: String,
        baseUrl: String? = null,
        chunkSize: Int = M3uParsingService.DEFAULT_CHUNK_SIZE,
        onProgress: ((M3uParseProgress) -> Unit)? = null
    ): Flow<List<PlaylistItem>> {
        return service.parseStream(inputStream, playlistSourceId, baseUrl, -1L, chunkSize, onProgress)
    }

    /**
     * Suspend function to fetch and collect all items from a URL.
     */
    suspend fun fetchAndParse(url: String, playlistSourceId: String): List<PlaylistItem> {
        return service.fetchAndParse(url, playlistSourceId)
    }

    /**
     * Parses an in-memory M3U text content.
     */
    fun parse(m3uContent: String, playlistSourceId: String): List<PlaylistItem> {
        return runBlocking {
            service.parseString(m3uContent, playlistSourceId)
        }
    }

    /**
     * Parses an in-memory M3U text content and extracts channel metadata (names, stream URLs, group titles).
     */
    suspend fun parseChannels(m3uContent: String): List<M3uChannelMetadata> {
        return service.parseChannels(m3uContent)
    }

    /**
     * Streams an InputStream and emits chunks of parsed [M3uChannelMetadata].
     */
    fun parseChannelsStream(
        inputStream: InputStream,
        baseUrl: String? = null,
        chunkSize: Int = M3uParsingService.DEFAULT_CHUNK_SIZE
    ): Flow<List<M3uChannelMetadata>> {
        return service.parseChannelsStream(inputStream, baseUrl, chunkSize)
    }
}
