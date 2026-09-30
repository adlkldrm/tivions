package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HomeViewModelTest {

    private lateinit var application: Application
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        val savedStateHandle = SavedStateHandle()
        viewModel = HomeViewModel(application, savedStateHandle)
    }

    @Test
    fun testGenreCategoriesMapping() {
        val categories = viewModel.genreCategories
        assertTrue(categories.isNotEmpty())

        // 1. Action (28)
        val actionCategory = viewModel.mapGenreIdToCategory(28)
        assertNotNull(actionCategory)
        assertEquals("Aksiyon", actionCategory?.name)
        assertTrue(actionCategory!!.relatedGenreIds.contains(28))
        assertTrue(actionCategory.relatedGenreIds.contains(10759))

        // 2. Comedy (35)
        val comedyCategory = viewModel.mapGenreIdToCategory(35)
        assertNotNull(comedyCategory)
        assertEquals("Komedi", comedyCategory?.name)

        // 3. Sci-Fi (878)
        val sciFiCategory = viewModel.mapGenreIdToCategory(878)
        assertNotNull(sciFiCategory)
        assertEquals("Bilim Kurgu", sciFiCategory?.name)
        assertTrue(sciFiCategory!!.relatedGenreIds.contains(878))

        // 4. Horror (27)
        val horrorCategory = viewModel.mapGenreIdToCategory(27)
        assertNotNull(horrorCategory)
        assertEquals("Korku", horrorCategory?.name)

        // 5. Drama (18)
        val dramaCategory = viewModel.mapGenreIdToCategory(18)
        assertNotNull(dramaCategory)
        assertEquals("Dram", dramaCategory?.name)
    }

    @Test
    fun testSelectGenreStateManagement() {
        assertNull(viewModel.selectedGenreId.value)
        assertNull(viewModel.activeGenreCategory.value)

        // Select Action (28)
        viewModel.selectGenre(28)
        assertEquals(28, viewModel.selectedGenreId.value)
        assertEquals("Aksiyon", viewModel.activeGenreCategory.value?.name)

        // Select Comedy (35)
        viewModel.selectGenre(35)
        assertEquals(35, viewModel.selectedGenreId.value)
        assertEquals("Komedi", viewModel.activeGenreCategory.value?.name)

        // Toggle Comedy (clicking same genre resets)
        viewModel.selectGenre(35)
        assertNull(viewModel.selectedGenreId.value)
        assertNull(viewModel.activeGenreCategory.value)

        // Select and explicit reset
        viewModel.selectGenre(878)
        assertEquals(878, viewModel.selectedGenreId.value)
        viewModel.resetGenreFilter()
        assertNull(viewModel.selectedGenreId.value)
    }

    @Test
    fun testFilterItemsByGenre() {
        val actionItem = PlaylistItem(
            id = "act_1",
            name = "Aksiyon Dolu",
            streamUrl = "http://test.com/stream1.mp4",
            category = "Aksiyon Filmleri",
            type = ItemType.VOD_MOVIE,
            rating = 8.8
        )
        val comedyItem = PlaylistItem(
            id = "com_1",
            name = "Komik Şeyler",
            streamUrl = "http://test.com/stream2.mp4",
            category = "Komedi Filmleri",
            type = ItemType.VOD_MOVIE,
            rating = 7.4
        )
        val dramaItem = PlaylistItem(
            id = "dra_1",
            name = "Ağlatan Hikaye",
            streamUrl = "http://test.com/stream3.mp4",
            category = "Dram",
            type = ItemType.VOD_MOVIE,
            rating = 8.1
        )

        val pool = listOf(actionItem, comedyItem, dramaItem)

        // Filter Action (28)
        val actionFiltered = viewModel.filterItemsByGenre(pool, emptyList(), 28)
        assertEquals(1, actionFiltered.size)
        assertEquals("act_1", actionFiltered[0].id)

        // Filter Comedy (35)
        val comedyFiltered = viewModel.filterItemsByGenre(pool, emptyList(), 35)
        assertEquals(1, comedyFiltered.size)
        assertEquals("com_1", comedyFiltered[0].id)

        // Null genreId returns all/personalized items
        val allFiltered = viewModel.filterItemsByGenre(pool, emptyList(), null)
        assertTrue(allFiltered.isNotEmpty())
    }
}
