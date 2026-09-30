package com.example.data.repository

import com.example.data.parser.M3uChannelMetadata
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class M3uPlaylistRepositoryTest {

    private val repository = M3uPlaylistRepository()

    private val sampleM3u = """
        #EXTM3U url-tvg="http://guide.example.com/epg.xml"
        #EXTINF:-1 tvg-id="trt1.tr" tvg-name="TRT 1" tvg-logo="https://cdn.example.com/trt1.png" group-title="Ulusal Kanallar",TRT 1 HD
        https://stream.example.com/live/trt1.m3u8
        #EXTINF:-1 tvg-id="bein1.tr" tvg-name="beIN Sports 1" tvg-logo="https://cdn.example.com/bein1.png" group-title="Spor Kanalları",beIN Sports 1 HD
        https://stream.example.com/live/bein1.m3u8|User-Agent=TivionsPlayer&Referer=https://stream.example.com
        #EXTINF:0 group-title="Belgesel",National Geographic Wild
        #EXTGRP:Belgesel & Doğa
        https://stream.example.com/live/natgeo.ts
    """.trimIndent()

    @Test
    fun testParseChannelsExtractsMetadataNamesStreamUrlsGroupTitles() = runBlocking {
        val channels = repository.parseChannels(sampleM3u)

        assertEquals(3, channels.size)

        // 1. Channel
        val ch1 = channels[0]
        assertEquals("TRT 1 HD", ch1.name)
        assertEquals("https://stream.example.com/live/trt1.m3u8", ch1.streamUrl)
        assertEquals("Ulusal Kanallar", ch1.groupTitle)
        assertEquals("https://cdn.example.com/trt1.png", ch1.logoUrl)
        assertEquals("trt1.tr", ch1.tvgId)
        assertEquals("TRT 1", ch1.tvgName)

        // 2. Channel with pipe-delimited headers
        val ch2 = channels[1]
        assertEquals("beIN Sports 1 HD", ch2.name)
        assertEquals("https://stream.example.com/live/bein1.m3u8", ch2.streamUrl)
        assertEquals("Spor Kanalları", ch2.groupTitle)
        assertEquals("TivionsPlayer", ch2.httpHeaders["User-Agent"])
        assertEquals("https://stream.example.com", ch2.httpHeaders["Referer"])

        // 3. Channel with #EXTGRP override
        val ch3 = channels[2]
        assertEquals("National Geographic Wild", ch3.name)
        assertEquals("https://stream.example.com/live/natgeo.ts", ch3.streamUrl)
        assertEquals("Belgesel & Doğa", ch3.groupTitle) // #EXTGRP took precedence over group-title
    }

    @Test
    fun testExtractGroupTitles() = runBlocking {
        val groups = repository.extractGroupTitles(sampleM3u)

        assertEquals(3, groups.size)
        assertTrue(groups.contains("Ulusal Kanallar"))
        assertTrue(groups.contains("Spor Kanalları"))
        assertTrue(groups.contains("Belgesel & Doğa"))
    }

    @Test
    fun testExtractChannelsByGroup() = runBlocking {
        val sporChannels = repository.extractChannelsByGroup(sampleM3u, "Spor Kanalları")

        assertEquals(1, sporChannels.size)
        assertEquals("beIN Sports 1 HD", sporChannels[0].name)
        assertEquals("https://stream.example.com/live/bein1.m3u8", sporChannels[0].streamUrl)
    }

    @Test
    fun testParseChannelsFromStream() = runBlocking {
        val inputStream = ByteArrayInputStream(sampleM3u.toByteArray(StandardCharsets.UTF_8))
        val chunks = repository.parseChannelsFromStream(inputStream).toList()

        val allChannels = chunks.flatten()
        assertEquals(3, allChannels.size)
        assertEquals("TRT 1 HD", allChannels[0].name)
        assertEquals("Ulusal Kanallar", allChannels[0].groupTitle)
    }

    @Test
    fun testMetadataToPlaylistItemConversion() {
        val metadata = M3uChannelMetadata(
            name = "Eurosport 1",
            streamUrl = "http://stream.example.com/euro1.m3u8",
            groupTitle = "Spor",
            logoUrl = "http://logo.com/euro.png",
            tvgId = "euro1.eu",
            tvgName = "Eurosport 1 HD",
            durationSeconds = 0L
        )

        val item = metadata.toPlaylistItem("source_test")
        assertEquals("Eurosport 1", item.name)
        assertEquals("http://stream.example.com/euro1.m3u8", item.streamUrl)
        assertEquals("Spor", item.category)
        assertEquals("http://logo.com/euro.png", item.logoUrl)
        assertEquals("euro1.eu", item.epgChannelId)
    }
}
