package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ItemDaoOrderTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ItemDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.itemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testItemsPreserveM3uOrderIndexNotAlphabetical() = runBlocking {
        // Items inserted out of alphabetical order (Z, A, M) with explicit orderIndex 1, 2, 3
        val item1 = PlaylistItem(
            id = "ch_z",
            name = "Z Channel",
            streamUrl = "http://stream.com/z.ts",
            category = "General",
            type = ItemType.LIVE_TV,
            orderIndex = 1
        )
        val item2 = PlaylistItem(
            id = "ch_a",
            name = "A Channel",
            streamUrl = "http://stream.com/a.ts",
            category = "General",
            type = ItemType.LIVE_TV,
            orderIndex = 2
        )
        val item3 = PlaylistItem(
            id = "ch_m",
            name = "M Channel",
            streamUrl = "http://stream.com/m.ts",
            category = "General",
            type = ItemType.LIVE_TV,
            orderIndex = 3
        )

        dao.insertAll(listOf(item1, item2, item3))

        val retrievedItems = dao.getItemsByType(ItemType.LIVE_TV).first()
        assertEquals(3, retrievedItems.size)
        // Must match orderIndex (Z, A, M), NOT alphabetical (A, M, Z)
        assertEquals("Z Channel", retrievedItems[0].name)
        assertEquals("A Channel", retrievedItems[1].name)
        assertEquals("M Channel", retrievedItems[2].name)

        val retrievedCategoryItems = dao.getItemsByTypeAndCategory(ItemType.LIVE_TV, "General").first()
        assertEquals("Z Channel", retrievedCategoryItems[0].name)
        assertEquals("A Channel", retrievedCategoryItems[1].name)
        assertEquals("M Channel", retrievedCategoryItems[2].name)
    }

    @Test
    fun testCategoriesPreserveFirstOccurrenceOrder() = runBlocking {
        // Categories first seen in order: Sports (1), News (2), Cinema (3)
        val items = listOf(
            PlaylistItem(id = "1", name = "Spor 1", streamUrl = "http://s1", category = "Spor", type = ItemType.LIVE_TV, orderIndex = 1),
            PlaylistItem(id = "2", name = "Haber 1", streamUrl = "http://h1", category = "Haber", type = ItemType.LIVE_TV, orderIndex = 2),
            PlaylistItem(id = "3", name = "Spor 2", streamUrl = "http://s2", category = "Spor", type = ItemType.LIVE_TV, orderIndex = 3),
            PlaylistItem(id = "4", name = "Sinema 1", streamUrl = "http://m1", category = "Sinema", type = ItemType.LIVE_TV, orderIndex = 4)
        )

        dao.insertAll(items)

        val categories = dao.getCategoriesFromPlaylistItems(ItemType.LIVE_TV).first()
        assertEquals(listOf("Spor", "Haber", "Sinema"), categories)
    }

    @Test
    fun testCategoryEntityPreservesApiOrderIndex() = runBlocking {
        val cats = listOf(
            com.example.data.model.CategoryEntity(type = "LIVE", categoryId = "1", categoryName = "TR-SPOR", orderIndex = 0, itemCount = 93),
            com.example.data.model.CategoryEntity(type = "LIVE", categoryId = "2", categoryName = "TR-SINEMA", orderIndex = 1, itemCount = 45),
            com.example.data.model.CategoryEntity(type = "LIVE", categoryId = "3", categoryName = "TR-ULUSAL", orderIndex = 2, itemCount = 120)
        )
        dao.insertCategories(cats)

        val retrieved = dao.getCategoriesByType(ItemType.LIVE_TV).first()
        assertEquals(listOf("TR-SPOR", "TR-SINEMA", "TR-ULUSAL"), retrieved)

        val entities = dao.getCategoriesEntitiesByType("LIVE").first()
        assertEquals(3, entities.size)
        assertEquals("TR-SPOR", entities[0].categoryName)
        assertEquals(93, entities[0].itemCount)
        assertEquals("TR-SINEMA", entities[1].categoryName)
        assertEquals(45, entities[1].itemCount)
        assertEquals("TR-ULUSAL", entities[2].categoryName)
        assertEquals(120, entities[2].itemCount)
    }

    @Test
    fun testCategoryHidingAndPersist() = runBlocking {
        dao.saveCategoryPref(
            com.example.data.model.CategoryPref(
                playlistId = "default",
                section = "LIVE",
                categoryKey = "spor",
                position = 0,
                hidden = true
            )
        )
        dao.saveCategoryPref(
            com.example.data.model.CategoryPref(
                playlistId = "default",
                section = "LIVE",
                categoryKey = "haber",
                position = 1,
                hidden = false
            )
        )

        val hidden = dao.getHiddenCategories("default", "LIVE").first()
        assertEquals(1, hidden.size)
        assertEquals("spor", hidden[0])
    }

    @Test
    fun testCategoryReorderingPersistence() = runBlocking {
        // User reorders categories: Belgesel (0), Sinema (1), Spor (2)
        val prefs = listOf(
            com.example.data.model.CategoryPref(playlistId = "default", section = "LIVE", categoryKey = "belgesel", position = 0),
            com.example.data.model.CategoryPref(playlistId = "default", section = "LIVE", categoryKey = "sinema", position = 1),
            com.example.data.model.CategoryPref(playlistId = "default", section = "LIVE", categoryKey = "spor", position = 2)
        )
        dao.saveCategoryPrefs(prefs)

        val savedPrefs = dao.getCategoryPrefs("default", "LIVE").first()
        assertEquals(3, savedPrefs.size)
        assertEquals("belgesel", savedPrefs[0].categoryKey)
        assertEquals(0, savedPrefs[0].position)
        assertEquals("sinema", savedPrefs[1].categoryKey)
        assertEquals(1, savedPrefs[1].position)
        assertEquals("spor", savedPrefs[2].categoryKey)
        assertEquals(2, savedPrefs[2].position)

        // Reset preferences
        dao.resetCategoryPrefs("default", "LIVE")
        val afterReset = dao.getCategoryPrefs("default", "LIVE").first()
        assertEquals(0, afterReset.size)
    }

    @Test
    fun testMovieSectionCategoryIndependence() = runBlocking {
        // Save category prefs for MOVIE section
        dao.saveCategoryPref(
            com.example.data.model.CategoryPref(
                playlistId = "default",
                section = "MOVIE",
                categoryKey = "aksiyon",
                position = 0,
                hidden = true
            )
        )
        dao.saveCategoryPref(
            com.example.data.model.CategoryPref(
                playlistId = "default",
                section = "LIVE",
                categoryKey = "spor",
                position = 0,
                hidden = false
            )
        )

        val movieHidden = dao.getHiddenCategories("default", "MOVIE").first()
        val liveHidden = dao.getHiddenCategories("default", "LIVE").first()

        assertEquals(1, movieHidden.size)
        assertEquals("aksiyon", movieHidden[0])
        assertEquals(0, liveHidden.size)
    }

    @Test
    fun testClearMovieWatchHistoryAndDownloads() = runBlocking {
        val m1 = PlaylistItem(
            id = "mov_1",
            name = "Movie 1",
            streamUrl = "http://m1",
            category = "Aksiyon",
            type = ItemType.VOD_MOVIE,
            lastWatchedTimestamp = 1000L,
            isDownloaded = true,
            downloadLocalPath = "/tmp/m1.mp4"
        )
        val m2 = PlaylistItem(
            id = "mov_2",
            name = "Movie 2",
            streamUrl = "http://m2",
            category = "Komedi",
            type = ItemType.VOD_MOVIE,
            lastWatchedTimestamp = 2000L,
            isDownloaded = true,
            downloadLocalPath = "/tmp/m2.mp4"
        )

        dao.insertAll(listOf(m1, m2))

        // Single item history cleanup
        dao.clearItemWatchHistory("mov_1")
        val afterSingle = dao.getItemById("mov_1")
        assertEquals(0L, afterSingle?.lastWatchedTimestamp)

        // Bulk history cleanup for movies
        dao.clearWatchHistoryForType(ItemType.VOD_MOVIE)
        val afterBulk = dao.getItemById("mov_2")
        assertEquals(0L, afterBulk?.lastWatchedTimestamp)

        // Clear download status
        dao.clearItemDownload("mov_1")
        val afterDownloadClear = dao.getItemById("mov_1")
        assertEquals(false, afterDownloadClear?.isDownloaded)
        assertEquals(null, afterDownloadClear?.downloadLocalPath)
    }

    @Test
    fun testPopularMoviesCurrentYearOnlyFilterAndScoreOrdering() = runBlocking {
        val currentYear = 2026
        val movies = listOf(
            // Current year movies (both pass rating >= 7 and rating5based >= 3)
            PlaylistItem(id = "m_2026_low", name = "Movie 2026 Low", streamUrl = "http://m1", category = "Aksiyon", type = ItemType.VOD_MOVIE, ratingValue = 7.5, rating5based = 3.5, releaseYear = 2026),
            PlaylistItem(id = "m_2026_high", name = "Movie 2026 High", streamUrl = "http://m2", category = "Aksiyon", type = ItemType.VOD_MOVIE, ratingValue = 9.0, rating5based = 4.5, releaseYear = 2026),
            // Older year movies (passes rating thresholds with higher score - MUST BE EXCLUDED)
            PlaylistItem(id = "m_2024_top", name = "Movie 2024 Top", streamUrl = "http://m3", category = "Aksiyon", type = ItemType.VOD_MOVIE, ratingValue = 9.8, rating5based = 4.9, releaseYear = 2024),
            PlaylistItem(id = "m_2025_mid", name = "Movie 2025 Mid", streamUrl = "http://m4", category = "Aksiyon", type = ItemType.VOD_MOVIE, ratingValue = 8.0, rating5based = 4.0, releaseYear = 2025),
            // Disqualified movies (fail one of the rating thresholds)
            PlaylistItem(id = "m_fail_rating", name = "Movie Fail Rating", streamUrl = "http://m5", category = "Aksiyon", type = ItemType.VOD_MOVIE, ratingValue = 6.9, rating5based = 4.5, releaseYear = 2026),
            PlaylistItem(id = "m_fail_rating5", name = "Movie Fail Rating5", streamUrl = "http://m6", category = "Aksiyon", type = ItemType.VOD_MOVIE, ratingValue = 8.5, rating5based = 2.9, releaseYear = 2026),
            // Disqualified movie (releaseYear = 0 / null)
            PlaylistItem(id = "m_fail_no_year", name = "Movie No Year", streamUrl = "http://m7", category = "Aksiyon", type = ItemType.VOD_MOVIE, ratingValue = 8.5, rating5based = 4.0, releaseYear = 0),
            // Different type (SERIES shouldn't be included in movies query)
            PlaylistItem(id = "s_2026", name = "Series 2026", streamUrl = "http://s1", category = "Diziler", type = ItemType.VOD_SERIES, ratingValue = 9.5, rating5based = 4.8, releaseYear = 2026)
        )
        dao.insertAll(movies)

        val result = dao.getPopularMoviesOnce(currentYear)
        // Only 2 current year movies satisfy ratingValue >= 7 AND rating5based >= 3
        assertEquals(2, result.size)

        // 1. Current year with higher score
        assertEquals("m_2026_high", result[0].id)
        // 2. Current year with lower score
        assertEquals("m_2026_low", result[1].id)
    }

    @Test
    fun testMovieReleaseYearRegexExtraction() {
        val y1 = com.example.data.parser.XtreamClient.extractMovieReleaseYear("UNABOMBER (2026)")
        assertEquals(2026, y1)

        val y2 = com.example.data.parser.XtreamClient.extractMovieReleaseYear("Dune: Part Two (2024) ")
        assertEquals(2024, y2)

        val y3 = com.example.data.parser.XtreamClient.extractMovieReleaseYear("Matrix (1999)")
        assertEquals(1999, y3)

        val y4 = com.example.data.parser.XtreamClient.extractMovieReleaseYear("No Year Movie")
        assertEquals(0, y4)

        val y5 = com.example.data.parser.XtreamClient.extractMovieReleaseYear("Invalid (20)")
        assertEquals(0, y5)
    }
}
