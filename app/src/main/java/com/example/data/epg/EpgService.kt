package com.example.data.epg

import com.example.data.local.ItemDao
import com.example.data.model.EpgProgram
import com.example.data.model.PlaylistItem
import com.example.data.parser.XmlTvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

/**
 * Result representing current and upcoming programs for a given channel.
 */
data class ChannelProgramGuide(
    val channelId: String,
    val currentProgram: EpgProgram?,
    val upcomingPrograms: List<EpgProgram>,
    val progressFraction: Float = currentProgram?.progressFraction() ?: 0f
)

/**
 * Interface defining the EPG integration and parsing service.
 */
interface IEpgService {
    /**
     * Parses an XMLTV input stream (supports both uncompressed and GZIP compressed)
     * and persists the programs into the database.
     */
    suspend fun parseAndStoreXmlTv(inputStream: InputStream): Result<Int>

    /**
     * Parses an XMLTV string and stores the programs.
     */
    suspend fun parseAndStoreXmlTv(xmlContent: String): Result<Int>

    /**
     * Fetches XMLTV data from a remote URL (handles gzip transparently) and stores the programs.
     */
    suspend fun fetchAndStoreXmlTv(url: String): Result<Int>

    /**
     * Retrieves the current playing program for a specific channel.
     */
    suspend fun getCurrentProgram(channelId: String, currentTime: Long = System.currentTimeMillis()): EpgProgram?

    /**
     * Retrieves the upcoming programs for a specific channel, up to [limit].
     */
    suspend fun getUpcomingPrograms(channelId: String, limit: Int = 5, currentTime: Long = System.currentTimeMillis()): List<EpgProgram>

    /**
     * Retrieves full program guide ([ChannelProgramGuide]) containing current and upcoming programs.
     */
    suspend fun getChannelProgramGuide(channelId: String, upcomingLimit: Int = 5, currentTime: Long = System.currentTimeMillis()): ChannelProgramGuide

    /**
     * Observes upcoming programs for a channel as a live Flow.
     */
    fun observeUpcomingPrograms(channelId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<EpgProgram>>

    /**
     * Observes all programs for a channel.
     */
    fun observeProgramsForChannel(channelId: String): Flow<List<EpgProgram>>

    /**
     * Automatically correlates and maps a list of channels against all stored EPG programs.
     * Produces a Map of [PlaylistItem.id] to its associated list of [EpgProgram].
     */
    fun observeChannelEpgMap(channelsFlow: Flow<List<PlaylistItem>>): Flow<Map<String, List<EpgProgram>>>

    /**
     * Cleans up expired programs older than the specified cutoff timestamp.
     */
    suspend fun pruneOldPrograms(cutoffTime: Long = System.currentTimeMillis() - 86400000L)
}

/**
 * Production implementation of [IEpgService] using [ItemDao] and [XmlTvParser].
 */
class EpgService(
    private val dao: ItemDao
) : IEpgService {

    override suspend fun parseAndStoreXmlTv(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val reader = InputStreamReader(inputStream, Charsets.UTF_8)
            val parseResult = XmlTvParser.parse(reader)
            if (parseResult.programs.isNotEmpty()) {
                dao.insertPrograms(parseResult.programs)
            }
            Result.success(parseResult.programs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun parseAndStoreXmlTv(xmlContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parseResult = XmlTvParser.parse(xmlContent)
            if (parseResult.programs.isNotEmpty()) {
                dao.insertPrograms(parseResult.programs)
            }
            Result.success(parseResult.programs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAndStoreXmlTv(url: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parseResult = XmlTvParser.fetchAndParse(url)
            if (parseResult.programs.isEmpty()) {
                return@withContext Result.failure(Exception("XMLTV dosyasında program bulunamadı."))
            }
            dao.clearAllPrograms()
            dao.insertPrograms(parseResult.programs)
            Result.success(parseResult.programs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentProgram(channelId: String, currentTime: Long): EpgProgram? = withContext(Dispatchers.IO) {
        dao.getCurrentProgramForChannel(channelId, currentTime)
    }

    override suspend fun getUpcomingPrograms(channelId: String, limit: Int, currentTime: Long): List<EpgProgram> = withContext(Dispatchers.IO) {
        val all = dao.getProgramsForChannel(channelId).first()
        all.filter { it.startTimeMillis > currentTime }
            .sortedBy { it.startTimeMillis }
            .take(limit)
    }

    override suspend fun getChannelProgramGuide(
        channelId: String,
        upcomingLimit: Int,
        currentTime: Long
    ): ChannelProgramGuide = withContext(Dispatchers.IO) {
        val current = dao.getCurrentProgramForChannel(channelId, currentTime)
        val all = dao.getProgramsForChannel(channelId).first()
        val upcoming = all.filter { it.startTimeMillis > currentTime }
            .sortedBy { it.startTimeMillis }
            .take(upcomingLimit)

        ChannelProgramGuide(
            channelId = channelId,
            currentProgram = current,
            upcomingPrograms = upcoming,
            progressFraction = current?.progressFraction(currentTime) ?: 0f
        )
    }

    override fun observeUpcomingPrograms(channelId: String, currentTime: Long): Flow<List<EpgProgram>> {
        return dao.getUpcomingProgramsForChannel(channelId, currentTime)
    }

    override fun observeProgramsForChannel(channelId: String): Flow<List<EpgProgram>> {
        return dao.getProgramsForChannel(channelId)
    }

    override fun observeChannelEpgMap(channelsFlow: Flow<List<PlaylistItem>>): Flow<Map<String, List<EpgProgram>>> {
        return kotlinx.coroutines.flow.combine(channelsFlow, dao.getAllPrograms()) { channels, programs ->
            val map = mutableMapOf<String, List<EpgProgram>>()
            val programsByChannelId = programs.groupBy { it.channelId.lowercase() }

            for (channel in channels) {
                val key = channel.id.lowercase()
                val epgKey = channel.epgChannelId?.lowercase()
                val cleanName = XmlTvParser.normalizeChannelName(channel.name)

                val matchedProgs = programsByChannelId[key]
                    ?: (if (epgKey != null) programsByChannelId[epgKey] else null)
                    ?: programs.filter { prog ->
                        val progChannelClean = XmlTvParser.normalizeChannelName(prog.channelId)
                        progChannelClean == cleanName || prog.channelId.equals(channel.name, ignoreCase = true)
                    }.sortedBy { it.startTimeMillis }

                map[channel.id] = matchedProgs ?: emptyList()
            }
            map
        }
    }

    override suspend fun pruneOldPrograms(cutoffTime: Long) = withContext(Dispatchers.IO) {
        dao.deleteOldPrograms(cutoffTime)
    }
}
