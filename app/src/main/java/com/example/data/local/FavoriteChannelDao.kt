package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FavoriteChannel
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for favorite channels persistence and operations.
 */
@Dao
interface FavoriteChannelDao {

    /**
     * Observes all favorite channels ordered by most recently favorited.
     */
    @Query("SELECT * FROM favorite_channels ORDER BY created_at DESC")
    fun getAllFavoriteChannels(): Flow<List<FavoriteChannel>>

    /**
     * Observes favorite channels filtered by group/category title.
     */
    @Query("SELECT * FROM favorite_channels WHERE group_title = :groupTitle ORDER BY name ASC")
    fun getFavoriteChannelsByGroup(groupTitle: String): Flow<List<FavoriteChannel>>

    /**
     * Observes full [PlaylistItem] records that have been favorited, joined from the playlist_items table.
     */
    @Query("""
        SELECT p.* FROM playlist_items p
        INNER JOIN favorite_channels f ON p.id = f.channel_id
        ORDER BY f.created_at DESC
    """)
    fun getFavoritedPlaylistItems(): Flow<List<PlaylistItem>>

    /**
     * Checks if a channel is marked as favorite.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE channel_id = :channelId)")
    fun isChannelFavoriteFlow(channelId: String): Flow<Boolean>

    /**
     * Synchronous check if a channel is currently marked as favorite.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE channel_id = :channelId)")
    suspend fun isChannelFavorite(channelId: String): Boolean

    /**
     * Returns the favorite channel by ID if exists.
     */
    @Query("SELECT * FROM favorite_channels WHERE channel_id = :channelId LIMIT 1")
    suspend fun getFavoriteById(channelId: String): FavoriteChannel?

    /**
     * Inserts or replaces a favorite channel record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteChannel)

    /**
     * Deletes a favorite channel by ID.
     */
    @Query("DELETE FROM favorite_channels WHERE channel_id = :channelId")
    suspend fun deleteFavoriteById(channelId: String)

    /**
     * Deletes all favorite channels.
     */
    @Query("DELETE FROM favorite_channels")
    suspend fun clearAllFavorites()

    /**
     * Counts the total number of favorite channels.
     */
    @Query("SELECT COUNT(*) FROM favorite_channels")
    suspend fun getFavoriteCount(): Int
}
