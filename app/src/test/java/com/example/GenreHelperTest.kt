package com.example

import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.util.GenreHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreHelperTest {

    @Test
    fun testGenreOptionsConfiguredCorrectly() {
        val options = GenreHelper.GENRE_OPTIONS
        assertTrue(options.isNotEmpty())

        val action = GenreHelper.getGenreById(28)
        assertNotNull(action)
        assertEquals("Aksiyon", action?.name)
        assertTrue(action!!.relatedGenreIds.contains(28))

        val comedy = GenreHelper.getGenreById(35)
        assertNotNull(comedy)
        assertEquals("Komedi", comedy?.name)

        val drama = GenreHelper.getGenreById(18)
        assertNotNull(drama)
        assertEquals("Dram", drama?.name)

        val sciFi = GenreHelper.getGenreById(878)
        assertNotNull(sciFi)
        assertEquals("Bilim Kurgu", sciFi?.name)

        val horror = GenreHelper.getGenreById(27)
        assertNotNull(horror)
        assertEquals("Korku", horror?.name)
    }

    @Test
    fun testMatchesGenreIdByCategoryAndKeywords() {
        val actionItem = PlaylistItem(
            id = "act1",
            name = "Kızıl Ufuk",
            streamUrl = "http://test.com/stream.mp4",
            category = "Aksiyon",
            type = ItemType.VOD_MOVIE,
            rating = 8.5
        )
        assertTrue(GenreHelper.matchesGenreId(actionItem, 28)) // Aksiyon ID = 28
        assertFalse(GenreHelper.matchesGenreId(actionItem, 35)) // Komedi ID = 35

        val comedyItem = PlaylistItem(
            id = "com1",
            name = "Kahkaha Ekspresi",
            streamUrl = "http://test.com/stream.mp4",
            category = "Komedi",
            subtitleInfo = "Komedi / Macera",
            type = ItemType.VOD_MOVIE,
            rating = 7.5
        )
        assertTrue(GenreHelper.matchesGenreId(comedyItem, 35)) // Komedi ID = 35
        assertFalse(GenreHelper.matchesGenreId(comedyItem, 27)) // Korku ID = 27

        val horrorItem = PlaylistItem(
            id = "hor1",
            name = "Karanlık Zindan",
            streamUrl = "http://test.com/stream.mp4",
            category = "Korku & Gerilim",
            type = ItemType.VOD_MOVIE,
            rating = 7.0
        )
        assertTrue(GenreHelper.matchesGenreId(horrorItem, 27)) // Korku ID = 27
    }

    @Test
    fun testGetGenreByNameAndId() {
        val actionOption = GenreHelper.getGenreByName("Aksiyon")
        assertNotNull(actionOption)
        assertEquals(28, actionOption?.genreId)

        val nonExistent = GenreHelper.getGenreById(99999)
        assertNull(nonExistent)
    }

    @Test
    fun testFindTopGenresFromHistory() {
        val history = listOf(
            PlaylistItem(id = "1", name = "Aksiyon 1", streamUrl = "http://test.com/1", category = "Aksiyon", type = ItemType.VOD_MOVIE),
            PlaylistItem(id = "2", name = "Aksiyon 2", streamUrl = "http://test.com/2", category = "Aksiyon", type = ItemType.VOD_MOVIE),
            PlaylistItem(id = "3", name = "Komedi 1", streamUrl = "http://test.com/3", category = "Komedi", type = ItemType.VOD_MOVIE)
        )
        val top = GenreHelper.findTopGenresFromHistory(history)
        assertTrue(top.contains("Aksiyon"))
    }
}
