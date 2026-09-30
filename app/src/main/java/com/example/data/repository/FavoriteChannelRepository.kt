package com.example.data.repository

import com.example.data.local.FavoriteChannelDao
import com.example.data.local.ItemDao
import com.example.data.model.FavoriteChannel
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository interface defining operations for managing user favorite channels.
 */
interface IFavoriteChannelRepository {
    /**
     * Observes the list of all favorite channels.
     */
    fun getAllFavoriteChannels(): Flow<List<FavoriteChannel>>

    /**
     * Observes favorite channels belonging to a specific group/category.
     */
    fun getFavoriteChannelsByGroup(groupTitle: String): Flow<List<FavoriteChannel>>

    /**
     * Observes the favorited channels as full [PlaylistItem] entities.
     */
    fun getFavoritedPlaylistItems(): Flow<List<PlaylistItem>>

    /**
     * Observes whether a specific channel is marked as favorite.
     */
    fun isChannelFavoriteFlow(channelId: String): Flow<Boolean>

    /**
     * Checks if a channel is favorited.
     */
    suspend fun isChannelFavorite(channelId: String): Boolean

    /**
     * Toggles the favorite status of a channel by ID.
     * If favorited, it will be removed. If not favorited, it will be added.
     * Returns the updated favorite state (true if now favorited, false if removed).
     */
    suspend fun toggleFavorite(
        channelId: String,
        channelName: String = "",
        streamUrl: String = "",
        groupTitle: String = "Genel",
        logoUrl: String? = null,
        epgChannelId: String? = null
    ): Boolean

    /**
     * Toggles the favorite status directly from a [PlaylistItem].
     */
    suspend fun toggleFavorite(item: PlaylistItem): Boolean

    /**
     * Adds a channel to favorites.
     */
    suspend fun addFavorite(
        channelId: String,
        channelName: String,
        streamUrl: String,
        groupTitle: String = "Genel",
        logoUrl: String? = null,
        epgChannelId: String? = null
    )

    /**
     * Removes a channel from favorites.
     */
    suspend fun removeFavorite(channelId: String)

    /**
     * Clears all favorite channels.
     */
    suspend fun clearAllFavorites()
}

/**
 * Production implementation of [IFavoriteChannelRepository].
 * Synchronizes favorite state across both the dedicated [FavoriteChannelDao]
 * and the main [ItemDao] (keeping [PlaylistItem.isFavorite] in sync).
 */
class FavoriteChannelRepository(
    private val favoriteChannelDao: FavoriteChannelDao,
    private val itemDao: ItemDao? = null
) : IFavoriteChannelRepository {

    override fun getAllFavoriteChannels(): Flow<List<FavoriteChannel>> {
        return favoriteChannelDao.getAllFavoriteChannels()
    }

    override fun getFavoriteChannelsByGroup(groupTitle: String): Flow<List<FavoriteChannel>> {
        return favoriteChannelDao.getFavoriteChannelsByGroup(groupTitle)
    }

    override fun getFavoritedPlaylistItems(): Flow<List<PlaylistItem>> {
        // If joined table query has results, use it, or fallback to itemDao favorite query
        return if (itemDao != null) {
            itemDao.getFavoritesByType(ItemType.LIVE_TV)
        } else {
            favoriteChannelDao.getFavoritedPlaylistItems()
        }
    }

    override fun isChannelFavoriteFlow(channelId: String): Flow<Boolean> {
        return favoriteChannelDao.isChannelFavoriteFlow(channelId)
    }

    override suspend fun isChannelFavorite(channelId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteChannelDao.isChannelFavorite(channelId)
    }

    override suspend fun toggleFavorite(
        channelId: String,
        channelName: String,
        streamUrl: String,
        groupTitle: String,
        logoUrl: String?,
        epgChannelId: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val currentlyFavorite = favoriteChannelDao.isChannelFavorite(channelId)
        val newStatus = !currentlyFavorite

        if (newStatus) {
            val entity = FavoriteChannel(
                channelId = channelId,
                name = channelName.ifBlank { "Kanal" },
                streamUrl = streamUrl,
                groupTitle = groupTitle.ifBlank { "Genel" },
                logoUrl = logoUrl,
                epgChannelId = epgChannelId,
                createdAt = System.currentTimeMillis()
            )
            favoriteChannelDao.insertFavorite(entity)
        } else {
            favoriteChannelDao.deleteFavoriteById(channelId)
        }

        // Keep playlist_items.isFavorite in sync if itemDao is provided
        itemDao?.setFavorite(channelId, newStatus)

        newStatus
    }

    override suspend fun toggleFavorite(item: PlaylistItem): Boolean = withContext(Dispatchers.IO) {
        toggleFavorite(
            channelId = item.id,
            channelName = item.name,
            streamUrl = item.streamUrl,
            groupTitle = item.category,
            logoUrl = item.logoUrl,
            epgChannelId = item.epgChannelId
        )
    }

    override suspend fun addFavorite(
        channelId: String,
        channelName: String,
        streamUrl: String,
        groupTitle: String,
        logoUrl: String?,
        epgChannelId: String?
    ): Unit = withContext(Dispatchers.IO) {
        val entity = FavoriteChannel(
            channelId = channelId,
            name = channelName,
            streamUrl = streamUrl,
            groupTitle = groupTitle,
            logoUrl = logoUrl,
            epgChannelId = epgChannelId,
            createdAt = System.currentTimeMillis()
        )
        favoriteChannelDao.insertFavorite(entity)
        itemDao?.setFavorite(channelId, true)
    }

    override suspend fun removeFavorite(channelId: String): Unit = withContext(Dispatchers.IO) {
        favoriteChannelDao.deleteFavoriteById(channelId)
        itemDao?.setFavorite(channelId, false)
    }

    override suspend fun clearAllFavorites(): Unit = withContext(Dispatchers.IO) {
        favoriteChannelDao.clearAllFavorites()
    }
}
