package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteChannelDao
import com.example.data.local.ItemDao
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FavoriteChannelRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var favoriteDao: FavoriteChannelDao
    private lateinit var itemDao: ItemDao
    private lateinit var repository: FavoriteChannelRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favoriteDao = database.favoriteChannelDao()
        itemDao = database.itemDao()
        repository = FavoriteChannelRepository(favoriteDao, itemDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testToggleFavorite_addsAndRemovesFavoriteChannel() = runBlocking {
        val channelId = "ch_trt1"
        val channelName = "TRT 1 HD"
        val streamUrl = "https://stream.example.com/trt1.m3u8"
        val group = "Ulusal"

        // 1. Initially not favorited
        assertFalse(repository.isChannelFavorite(channelId))

        // 2. Toggle to favorite (should return true)
        val added = repository.toggleFavorite(
            channelId = channelId,
            channelName = channelName,
            streamUrl = streamUrl,
            groupTitle = group
        )
        assertTrue(added)
        assertTrue(repository.isChannelFavorite(channelId))

        // 3. Verify retrieved favorites list
        val favorites = repository.getAllFavoriteChannels().first()
        assertEquals(1, favorites.size)
        assertEquals(channelId, favorites[0].channelId)
        assertEquals(channelName, favorites[0].name)
        assertEquals(streamUrl, favorites[0].streamUrl)
        assertEquals(group, favorites[0].groupTitle)

        // 4. Toggle again to unfavorite (should return false)
        val removed = repository.toggleFavorite(
            channelId = channelId,
            channelName = channelName,
            streamUrl = streamUrl,
            groupTitle = group
        )
        assertFalse(removed)
        assertFalse(repository.isChannelFavorite(channelId))

        val emptyFavorites = repository.getAllFavoriteChannels().first()
        assertTrue(emptyFavorites.isEmpty())
    }

    @Test
    fun testToggleFavoritePlaylistItem_syncsWithItemDao() = runBlocking {
        val testItem = PlaylistItem(
            id = "ch_sport1",
            name = "beIN Sports 1",
            streamUrl = "https://stream.example.com/sport1.m3u8",
            category = "Spor",
            type = ItemType.LIVE_TV,
            isFavorite = false
        )
        itemDao.insertItem(testItem)

        // Toggle favorite via PlaylistItem
        val result = repository.toggleFavorite(testItem)
        assertTrue(result)

        // Verify in FavoriteChannel table
        val favChannel = favoriteDao.getFavoriteById("ch_sport1")
        assertNotNull(favChannel)
        assertEquals("beIN Sports 1", favChannel?.name)
        assertEquals("Spor", favChannel?.groupTitle)

        // Verify in playlist_items table that isFavorite flag is updated to true
        val updatedItem = itemDao.getItemById("ch_sport1")
        assertNotNull(updatedItem)
        assertTrue(updatedItem!!.isFavorite)

        // Toggle back to false
        val result2 = repository.toggleFavorite(testItem.copy(isFavorite = true))
        assertFalse(result2)

        val removedFav = favoriteDao.getFavoriteById("ch_sport1")
        assertNull(removedFav)

        val updatedItem2 = itemDao.getItemById("ch_sport1")
        assertFalse(updatedItem2!!.isFavorite)
    }

    @Test
    fun testGetFavoriteChannelsByGroup() = runBlocking {
        repository.addFavorite("ch_1", "Kanal D", "url1", "Ulusal")
        repository.addFavorite("ch_2", "ATV", "url2", "Ulusal")
        repository.addFavorite("ch_3", "S Sport", "url3", "Spor")

        val ulusalFavorites = repository.getFavoriteChannelsByGroup("Ulusal").first()
        assertEquals(2, ulusalFavorites.size)

        val sporFavorites = repository.getFavoriteChannelsByGroup("Spor").first()
        assertEquals(1, sporFavorites.size)
        assertEquals("S Sport", sporFavorites[0].name)
    }

    @Test
    fun testClearAllFavorites() = runBlocking {
        repository.addFavorite("ch_1", "Kanal 1", "url1", "Genel")
        repository.addFavorite("ch_2", "Kanal 2", "url2", "Genel")

        assertEquals(2, favoriteDao.getFavoriteCount())

        repository.clearAllFavorites()

        assertEquals(0, favoriteDao.getFavoriteCount())
        val list = repository.getAllFavoriteChannels().first()
        assertTrue(list.isEmpty())
    }
}
