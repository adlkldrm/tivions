package com.example.data.parser

import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.repository.getXtreamSeriesId
import com.example.data.repository.getXtreamStreamId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamClientTest {

    @Test
    fun testExtractXtreamCredentialsFromUserExample() {
        val m3uUrl = "http://xxxxxx.xxxx:8080/get.php?username=USERNAME&password=PASSWORD&type=m3u_plus&output=ts"
        val creds = XtreamClient.extractXtreamCredentials(m3uUrl)

        assertNotNull(creds)
        assertEquals("http://xxxxxx.xxxx:8080", creds?.serverUrl)
        assertEquals("USERNAME", creds?.username)
        assertEquals("PASSWORD", creds?.password)
    }

    @Test
    fun testExtractXtreamCredentialsWithCustomPortAndEncodedParams() {
        val m3uUrl = "http://iptv.server.net:2095/get.php?username=test_user&password=secret%23123&type=m3u_plus"
        val creds = XtreamClient.extractXtreamCredentials(m3uUrl)

        assertNotNull(creds)
        assertEquals("http://iptv.server.net:2095", creds?.serverUrl)
        assertEquals("test_user", creds?.username)
        assertEquals("secret#123", creds?.password)
    }

    @Test
    fun testNonXtreamM3uReturnsNull() {
        val rawM3u = "https://example.com/playlist.m3u"
        val creds = XtreamClient.extractXtreamCredentials(rawM3u)

        assertNull(creds)
    }

    @Test
    fun testBuildApiUrlSingleAmpersandAndEncoding() {
        val url = XtreamClient.buildApiUrl(
            baseUrl = "http://xxxxxx.xxxx:8080",
            action = "get_vod_streams",
            params = mapOf("category_id" to "103"),
            username = "USERNAME",
            password = "PASSWORD"
        )

        assertEquals(
            "http://xxxxxx.xxxx:8080/player_api.php?action=get_vod_streams&category_id=103&username=USERNAME&password=PASSWORD",
            url
        )
        // Ensure NO double ampersands
        assertTrue(!url.contains("&&"))
    }

    @Test
    fun testDynamicIdExtraction() {
        val movie = PlaylistItem(
            id = "xtream_vod_594310",
            name = "Test Movie",
            streamUrl = "http://server/movie/u/p/594310.mp4",
            category = "Filmler",
            type = ItemType.VOD_MOVIE
        )
        assertEquals("594310", movie.getXtreamStreamId())

        val series = PlaylistItem(
            id = "series_xtream_12300",
            name = "Test Series",
            streamUrl = "",
            category = "Diziler",
            type = ItemType.VOD_SERIES,
            seriesId = "series_xtream_12300"
        )
        assertEquals("12300", series.getXtreamSeriesId())
    }
}
