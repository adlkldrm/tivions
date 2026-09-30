package com.example.data.parser

import com.example.data.model.ItemType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

class M3uParsingServiceTest {

    private val service = M3uParsingService.INSTANCE

    @Test
    fun testStandardM3uExtraction() = runBlocking {
        val m3u = """
            #EXTM3U url-tvg="http://epg.example.com/epg.xml"
            #EXTINF:-1 tvg-id="TRT1.tr" tvg-name="TRT 1 HD" tvg-logo="https://example.com/logos/trt1.png" group-title="Ulusal Kanallar",TRT 1 HD
            https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
            #EXTINF:-1 tvg-id="ATV.tr" tvg-logo="https://example.com/logos/atv.png" group-title="Ulusal Kanallar",ATV
            https://test-streams.mux.dev/test_001/stream.m3u8
        """.trimIndent()

        val items = service.parseString(m3u, "source_1")

        assertEquals(2, items.size)

        val item1 = items[0]
        assertEquals("TRT 1 HD", item1.name)
        assertEquals("Ulusal Kanallar", item1.category)
        assertEquals("https://example.com/logos/trt1.png", item1.logoUrl)
        assertEquals("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", item1.streamUrl)
        assertEquals(ItemType.LIVE_TV, item1.type)
        assertEquals("TRT1.tr", item1.epgChannelId)

        val item2 = items[1]
        assertEquals("ATV", item2.name)
        assertEquals("Ulusal Kanallar", item2.category)
        assertEquals("https://example.com/logos/atv.png", item2.logoUrl)
        assertEquals("https://test-streams.mux.dev/test_001/stream.m3u8", item2.streamUrl)
    }

    @Test
    fun testCommaInsideQuotedAttributes() = runBlocking {
        // Commas inside group-title or other attributes should not break channel title extraction!
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="NEWS.1" group-title="News, International & World" tvg-logo="http://logo.png",CNN News, London & Global
            https://example.com/cnn.m3u8
        """.trimIndent()

        val items = service.parseString(m3u, "source_1")
        assertEquals(1, items.size)
        val item = items[0]

        assertEquals("CNN News, London & Global", item.name)
        assertEquals("News, International & World", item.category)
        assertEquals("http://logo.png", item.logoUrl)
        assertEquals("https://example.com/cnn.m3u8", item.streamUrl)
    }

    @Test
    fun testStandaloneExtGrpAndAttributesVariations() = runBlocking {
        // Test single quotes, spaces around '=', unquoted attributes, and standalone #EXTGRP
        val m3u = """
            #EXTM3U
            #EXTINF:0 tvg-id='SPORTS1' tvg-logo = "http://sports.png" tvg-name='Spor TV',Spor TV Canlı
            #EXTGRP:Spor Kanalları
            http://stream.example.com/sports.m3u8
        """.trimIndent()

        val items = service.parseString(m3u, "source_1")
        assertEquals(1, items.size)
        val item = items[0]

        assertEquals("Spor TV Canlı", item.name)
        assertEquals("Spor Kanalları", item.category) // #EXTGRP overrides
        assertEquals("http://sports.png", item.logoUrl)
    }

    @Test
    fun testPipeDelimitedUrlHeaders() {
        val rawUrl = "http://stream.example.com/live.ts|User-Agent=Mozilla/5.0&Referer=http://foo.com&Authorization=Bearer 123"
        val (cleanUrl, headers) = service.extractPipeHeaders(rawUrl)

        assertEquals("http://stream.example.com/live.ts", cleanUrl)
        assertEquals("Mozilla/5.0", headers["User-Agent"])
        assertEquals("http://foo.com", headers["Referer"])
        assertEquals("Bearer 123", headers["Authorization"])
    }

    @Test
    fun testVodClassificationAndEpisodeExtraction() = runBlocking {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="http://interstellar.png" group-title="Yabancı Filmler",Yıldızlararası
            http://server.com/movie/interstellar.mp4
            #EXTINF:-1 tvg-logo="http://bb.png" group-title="Yabancı Diziler",Breaking Bad S02E05 - Breakage
            http://server.com/series/bb_s2_ep5.m3u8
        """.trimIndent()

        val items = service.parseString(m3u, "source_vod")
        // Produces 3 items: 1 movie + 1 episode + 1 generated parent series
        assertEquals(3, items.size)

        val movie = items[0]
        assertEquals(ItemType.VOD_MOVIE, movie.type)
        assertEquals("Yıldızlararası", movie.name)
        assertEquals("Yabancı Filmler", movie.category)

        val episode = items[1]
        assertEquals(ItemType.EPISODE, episode.type)
        assertEquals("Breaking Bad S02E05 - Breakage", episode.name)
        assertEquals(2, episode.seasonNumber)
        assertEquals(5, episode.episodeNumber)
        assertNotNull(episode.seriesId)

        val parentSeries = items[2]
        assertEquals(ItemType.VOD_SERIES, parentSeries.type)
        assertEquals("Breaking Bad", parentSeries.name)
    }

    @Test
    fun testUtf8BomAndMalformedResilience() = runBlocking {
        // Starts with UTF-8 BOM, interspersed with blank lines, comments, and missing commas
        val m3u = "\uFEFF#EXTM3U\n" +
                "\n" +
                "# Some server comment\n" +
                "#EXTINF:-1 tvg-name=\"Kanal D\" tvg-logo=\"http://logo.com/kd.png\"\n" +
                "http://stream.com/kd.m3u8\n" +
                "\n" +
                "#EXTINF:-1,Show TV\n" +
                "http://stream.com/show.m3u8\n"

        val items = service.parseString(m3u, "source_bom")
        assertEquals(2, items.size)
        assertEquals("Kanal D", items[0].name)
        assertEquals("Show TV", items[1].name)
    }

    @Test
    fun testGzipStreamParsing() = runBlocking {
        val rawM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="CH1" group-title="Gzip Group",Gzip Channel
            http://example.com/gzip_stream.m3u8
        """.trimIndent()

        // Compress raw M3U into GZIP byte array
        val byteOut = ByteArrayOutputStream()
        GZIPOutputStream(byteOut).use { gzipOut ->
            gzipOut.write(rawM3u.toByteArray(StandardCharsets.UTF_8))
        }
        val gzippedBytes = byteOut.toByteArray()

        val chunks = service.parseStream(
            inputStream = gzippedBytes.inputStream(),
            playlistSourceId = "gzip_source",
            chunkSize = 10
        ).toList()

        val allItems = chunks.flatten()
        assertEquals(1, allItems.size)
        assertEquals("Gzip Channel", allItems[0].name)
        assertEquals("Gzip Group", allItems[0].category)
    }

    @Test
    fun testLargePlaylistStreamingChunking() = runBlocking {
        // Generate a large synthetic playlist with 1,250 channels
        val totalChannels = 1250
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        for (i in 1..totalChannels) {
            sb.append("#EXTINF:-1 tvg-id=\"id_$i\" tvg-logo=\"http://logo/$i.png\" group-title=\"Group ${i % 10}\",Channel $i\n")
            sb.append("http://stream.domain.com/live/$i.m3u8\n")
        }

        val inputStream = sb.toString().byteInputStream(StandardCharsets.UTF_8)
        var totalChunks = 0
        var totalParsed = 0

        service.parseStream(
            inputStream = inputStream,
            playlistSourceId = "large_src",
            chunkSize = 500
        ).collect { chunk ->
            totalChunks++
            totalParsed += chunk.size
            // Chunks should never exceed chunkSize (except potentially the last one which is <= chunkSize)
            assertTrue(chunk.size <= 500)
        }

        assertEquals(totalChannels, totalParsed)
        // 1250 items with chunkSize=500 should produce exactly 3 chunks: 500, 500, 250
        assertEquals(3, totalChunks)
    }

    @Test
    fun testTrDiziGroupClassifiedAsLiveTvWhenStreaming() = runBlocking {
        // Channels in group-title="TR-Dizi" with live stream URLs (.ts / .m3u8) must be LIVE_TV, NOT VOD/Series
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="KanalD.tr" tvg-name="Kanal D HD" group-title="TR-Dizi",Kanal D Canlı
            http://live.example.com/streams/kanald.ts
            #EXTINF:-1 tvg-id="ATV.tr" tvg-name="ATV HD" group-title="TR-Dizi",ATV Canlı
            http://live.example.com/streams/atv.m3u8
        """.trimIndent()

        val items = service.parseString(m3u, "source_tr_dizi")
        assertEquals(2, items.size)

        assertEquals("Kanal D Canlı", items[0].name)
        assertEquals("TR-Dizi", items[0].category)
        assertEquals(ItemType.LIVE_TV, items[0].type)
        assertEquals(1, items[0].orderIndex)

        assertEquals("ATV Canlı", items[1].name)
        assertEquals("TR-Dizi", items[1].category)
        assertEquals(ItemType.LIVE_TV, items[1].type)
        assertEquals(2, items[1].orderIndex)
    }

    @Test
    fun testM3uOriginalOrderPreservation() = runBlocking {
        // Items must preserve the exact chronological order of lines in M3U file (Z -> A -> M)
        val m3u = """
            #EXTM3U
            #EXTINF:-1 group-title="Haberler",Zeta TV
            http://live.example.com/zeta.m3u8
            #EXTINF:-1 group-title="Spor",Alpha TV
            http://live.example.com/alpha.m3u8
            #EXTINF:-1 group-title="Genel",Mega TV
            http://live.example.com/mega.m3u8
        """.trimIndent()

        val items = service.parseString(m3u, "source_order")
        assertEquals(3, items.size)

        assertEquals("Zeta TV", items[0].name)
        assertEquals(1, items[0].orderIndex)

        assertEquals("Alpha TV", items[1].name)
        assertEquals(2, items[1].orderIndex)

        assertEquals("Mega TV", items[2].name)
        assertEquals(3, items[2].orderIndex)
    }

    @Test
    fun testExplicitTvgTypeAndStaticFileVodClassification() = runBlocking {
        // Explicit tvg-type="movie" and .mp4 static file must be detected as VOD_MOVIE
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-type="movie" group-title="TR-Dizi",Dizi Özel Film
            http://live.example.com/film.ts
            #EXTINF:-1 group-title="Belgesel",Doğa Belgeseli
            http://live.example.com/vod/doga.mp4
        """.trimIndent()

        val items = service.parseString(m3u, "source_explicit")
        assertEquals(2, items.size)

        assertEquals("Dizi Özel Film", items[0].name)
        assertEquals(ItemType.VOD_MOVIE, items[0].type)

        assertEquals("Doğa Belgeseli", items[1].name)
        assertEquals(ItemType.VOD_MOVIE, items[1].type)
    }
}
