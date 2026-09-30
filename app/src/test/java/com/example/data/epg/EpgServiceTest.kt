package com.example.data.epg

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.ItemDao
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EpgServiceTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ItemDao
    private lateinit var epgService: EpgService

    private val sampleXmlTv = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE tv SYSTEM "xmltv.dtd">
        <tv>
            <channel id="trt1">
                <display-name>TRT 1 HD</display-name>
            </channel>
            <channel id="atv">
                <display-name>ATV HD</display-name>
            </channel>
            <programme start="20260924080000 +0000" stop="20260924100000 +0000" channel="trt1">
                <title>Sabah Haberleri</title>
                <desc>Günün sıcak gelişmeleri ve manşetler.</desc>
                <category>Haber</category>
            </programme>
            <programme start="20260924100000 +0000" stop="20260924123000 +0000" channel="trt1">
                <title>Kudüs Fatihi Selahaddin Eyyubi</title>
                <desc>Tarihi dizi serisi yeni bölüm.</desc>
                <category>Dizi</category>
            </programme>
            <programme start="20260924123000 +0000" stop="20260924140000 +0000" channel="trt1">
                <title>Öğle Bülteni</title>
                <desc>Günün ikinci yarısı gelişmeleri.</desc>
                <category>Haber</category>
            </programme>
            <programme start="20260924090000 +0000" stop="20260924110000 +0000" channel="atv">
                <title>Müge Anlı ile Tatlı Sert</title>
                <desc>Gündemdeki olaylar ve arananlar.</desc>
                <category>Reality</category>
            </programme>
        </tv>
    """.trimIndent()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.itemDao()
        epgService = EpgService(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testParseAndStoreXmlTv_stringInput() = runBlocking {
        val result = epgService.parseAndStoreXmlTv(sampleXmlTv)
        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull())

        val allPrograms = dao.getAllPrograms().first()
        assertEquals(4, allPrograms.size)

        val trt1Programs = dao.getProgramsForChannel("trt1").first()
        assertEquals(3, trt1Programs.size)
        assertEquals("Sabah Haberleri", trt1Programs[0].title)
        assertEquals("Kudüs Fatihi Selahaddin Eyyubi", trt1Programs[1].title)
        assertEquals("Öğle Bülteni", trt1Programs[2].title)
    }

    @Test
    fun testParseAndStoreXmlTv_streamInput() = runBlocking {
        val stream = ByteArrayInputStream(sampleXmlTv.toByteArray(Charsets.UTF_8))
        val result = epgService.parseAndStoreXmlTv(stream)
        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull())
    }

    @Test
    fun testGetCurrentAndUpcomingPrograms() = runBlocking {
        epgService.parseAndStoreXmlTv(sampleXmlTv)

        // 2026-09-24 10:30:00 UTC timestamp = 1790245800000L
        // During this time:
        // - "Kudüs Fatihi Selahaddin Eyyubi" (10:00 - 12:30 UTC) is currently playing
        // - "Öğle Bülteni" (12:30 - 14:00 UTC) is upcoming
        val fakeCurrentTime = java.text.SimpleDateFormat("yyyyMMddHHmmss Z", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse("20260924103000 +0000")!!.time

        val currentProg = epgService.getCurrentProgram("trt1", fakeCurrentTime)
        assertNotNull(currentProg)
        assertEquals("Kudüs Fatihi Selahaddin Eyyubi", currentProg?.title)
        assertTrue(currentProg!!.isCurrentlyPlaying(fakeCurrentTime))

        val upcomingList = epgService.getUpcomingPrograms("trt1", limit = 5, currentTime = fakeCurrentTime)
        assertEquals(1, upcomingList.size)
        assertEquals("Öğle Bülteni", upcomingList[0].title)

        val guide = epgService.getChannelProgramGuide("trt1", upcomingLimit = 3, currentTime = fakeCurrentTime)
        assertEquals("trt1", guide.channelId)
        assertEquals("Kudüs Fatihi Selahaddin Eyyubi", guide.currentProgram?.title)
        assertEquals(1, guide.upcomingPrograms.size)
        assertTrue(guide.progressFraction > 0f)
    }

    @Test
    fun testObserveChannelEpgMap_matchesChannelsCorrectly() = runBlocking {
        epgService.parseAndStoreXmlTv(sampleXmlTv)

        val channelList = listOf(
            PlaylistItem(
                id = "trt1",
                name = "TRT 1 HD",
                category = "Ulusal",
                streamUrl = "http://stream.trt1.m3u8",
                type = ItemType.LIVE_TV,
                epgChannelId = "trt1"
            ),
            PlaylistItem(
                id = "atv",
                name = "ATV",
                category = "Ulusal",
                streamUrl = "http://stream.atv.m3u8",
                type = ItemType.LIVE_TV,
                epgChannelId = "atv"
            )
        )

        val epgMap = epgService.observeChannelEpgMap(flowOf(channelList)).first()
        assertEquals(2, epgMap.size)
        assertEquals(3, epgMap["trt1"]?.size)
        assertEquals(1, epgMap["atv"]?.size)
        assertEquals("Müge Anlı ile Tatlı Sert", epgMap["atv"]?.first()?.title)
    }
}
