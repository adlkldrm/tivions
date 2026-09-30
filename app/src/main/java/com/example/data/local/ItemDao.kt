package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSettings
import com.example.data.model.EpgProgram
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSource
import com.example.data.model.UserProfile
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM playlist_items ORDER BY orderIndex ASC LIMIT 50")
    fun getAllItems(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items ORDER BY orderIndex ASC")
    fun getAllItemsPaged(): PagingSource<Int, PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE type = :type ORDER BY orderIndex ASC")
    fun getItemsByTypePaged(type: ItemType): PagingSource<Int, PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE type = :type AND category = :category ORDER BY orderIndex ASC")
    fun getItemsByTypeAndCategoryPaged(type: ItemType, category: String): PagingSource<Int, PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE category = :category ORDER BY orderIndex ASC")
    fun getItemsByCategoryPaged(category: String): PagingSource<Int, PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE isFavorite = 1 ORDER BY orderIndex ASC")
    fun getAllFavoritesPaged(): PagingSource<Int, PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE type = :type AND isFavorite = 1 ORDER BY orderIndex ASC")
    fun getFavoritesByTypePaged(type: ItemType): PagingSource<Int, PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE type != 'EPISODE' AND type != 'LIVE_TV' ORDER BY rating5based DESC, ratingValue DESC LIMIT 60")
    fun getRecommendationCandidates(): Flow<List<PlaylistItem>>

    // 1. Son Eklenen Filmler
    @Query("SELECT * FROM playlist_items WHERE type = 'VOD_MOVIE' ORDER BY globalOrderIndex ASC LIMIT 20")
    fun getRecentMovies(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE type = 'VOD_MOVIE' ORDER BY globalOrderIndex ASC LIMIT 20")
    suspend fun getRecentMoviesOnce(): List<PlaylistItem>

    // 2. Son Eklenen Diziler
    @Query("SELECT * FROM playlist_items WHERE type = 'VOD_SERIES' ORDER BY globalOrderIndex DESC LIMIT 20")
    fun getRecentSeries(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE type = 'VOD_SERIES' ORDER BY globalOrderIndex DESC LIMIT 20")
    suspend fun getRecentSeriesOnce(): List<PlaylistItem>

    // 3. Popüler Filmler (Yalnızca güncel yıl, ratingValue >= 7 VE rating5based >= 3)
    @Query("""
        SELECT * FROM playlist_items 
        WHERE type = 'VOD_MOVIE' 
          AND releaseYear = :currentYear 
          AND ratingValue >= 7 
          AND rating5based >= 3 
        ORDER BY ratingValue DESC, rating5based DESC 
        LIMIT 10
    """)
    fun getPopularMovies(currentYear: Int): Flow<List<PlaylistItem>>

    @Query("""
        SELECT * FROM playlist_items 
        WHERE type = 'VOD_MOVIE' 
          AND releaseYear = :currentYear 
          AND ratingValue >= 7 
          AND rating5based >= 3 
        ORDER BY ratingValue DESC, rating5based DESC 
        LIMIT 10
    """)
    suspend fun getPopularMoviesOnce(currentYear: Int): List<PlaylistItem>

    // 4. Popüler Diziler (ratingValue >= 7 VE rating5based >= 3, releaseYear = :currentYear öncelikli)
    @Query("""
        SELECT * FROM playlist_items 
        WHERE type = 'VOD_SERIES' AND ratingValue >= 7 AND rating5based >= 3 
        ORDER BY CASE WHEN releaseYear = :currentYear THEN 0 ELSE 1 END, ratingValue DESC, rating5based DESC 
        LIMIT 10
    """)
    fun getPopularSeries(currentYear: Int): Flow<List<PlaylistItem>>

    @Query("""
        SELECT * FROM playlist_items 
        WHERE type = 'VOD_SERIES' AND ratingValue >= 7 AND rating5based >= 3 
        ORDER BY CASE WHEN releaseYear = :currentYear THEN 0 ELSE 1 END, ratingValue DESC, rating5based DESC 
        LIMIT 10
    """)
    suspend fun getPopularSeriesOnce(currentYear: Int): List<PlaylistItem>

    // 5. Senin İçin Önerilenler
    @Query("""
        SELECT * FROM playlist_items 
        WHERE (type = 'VOD_MOVIE' OR type = 'VOD_SERIES') 
        ORDER BY rating5based DESC, ratingValue DESC 
        LIMIT 10
    """)
    suspend fun getRecommendationsAll(): List<PlaylistItem>

    @Query("""
        SELECT * FROM playlist_items 
        WHERE (type = 'VOD_MOVIE' OR type = 'VOD_SERIES') 
          AND (genre LIKE '%' || :genreName || '%' OR category LIKE '%' || :genreName || '%') 
        ORDER BY rating5based DESC, ratingValue DESC 
        LIMIT 10
    """)
    suspend fun getRecommendationsForGenre(genreName: String): List<PlaylistItem>

    @Query("""
        SELECT * FROM playlist_items 
        WHERE (type = 'VOD_MOVIE' OR type = 'VOD_SERIES') 
          AND (genre LIKE '%' || :genre1 || '%' OR category LIKE '%' || :genre1 || '%'
               OR genre LIKE '%' || :genre2 || '%' OR category LIKE '%' || :genre2 || '%') 
        ORDER BY rating5based DESC, ratingValue DESC 
        LIMIT 10
    """)
    suspend fun getPersonalizedRecommendations(genre1: String, genre2: String): List<PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC LIMIT 30")
    suspend fun getWatchHistoryOnce(): List<PlaylistItem>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE type = 'LIVE_TV'")
    fun getLiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE type = 'VOD_MOVIE'")
    fun getMovieCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE type = 'VOD_SERIES' OR type = 'EPISODE'")
    fun getSeriesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM epg_programs")
    fun getProgramCount(): Flow<Int>

    @Query("SELECT * FROM playlist_items WHERE type = :type ORDER BY orderIndex ASC LIMIT 100")
    fun getItemsByType(type: ItemType): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE type = :type AND category = :category ORDER BY orderIndex ASC LIMIT 100")
    fun getItemsByTypeAndCategory(type: ItemType, category: String): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE isFavorite = 1 ORDER BY orderIndex ASC")
    fun getAllFavorites(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE type = :type AND isFavorite = 1 ORDER BY orderIndex ASC")
    fun getFavoritesByType(type: ItemType): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC LIMIT 30")
    fun getWatchHistory(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE isDownloaded = 1 ORDER BY orderIndex ASC")
    fun getDownloads(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR actors LIKE '%' || :query || '%' LIMIT 100")
    fun searchItems(query: String): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): PlaylistItem?

    @Query("SELECT * FROM playlist_items WHERE seriesId = :seriesId AND seasonNumber = :seasonNumber AND type = 'EPISODE' ORDER BY episodeNumber ASC")
    fun getEpisodesForSeason(seriesId: String, seasonNumber: Int): Flow<List<PlaylistItem>>

    @Query("SELECT DISTINCT seasonNumber FROM playlist_items WHERE seriesId = :seriesId AND type = 'EPISODE' ORDER BY seasonNumber ASC")
    fun getSeasonsForSeries(seriesId: String): Flow<List<Int>>

    @Query("SELECT * FROM playlist_items WHERE seriesId = :seriesId AND type = 'EPISODE' ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getAllEpisodesForSeries(seriesId: String): Flow<List<PlaylistItem>>

    @Query("DELETE FROM playlist_items WHERE seriesId = :seriesId AND type = 'EPISODE'")
    suspend fun deleteEpisodesForSeries(seriesId: String)

    @Query("""
        SELECT categoryName FROM categories 
        WHERE type = CASE 
            WHEN :type = 'LIVE_TV' THEN 'LIVE' 
            WHEN :type = 'VOD_MOVIE' THEN 'MOVIE' 
            ELSE 'SERIES' END 
        ORDER BY CASE WHEN userOrderIndex IS NOT NULL THEN userOrderIndex ELSE orderIndex END ASC
    """)
    fun getCategoriesByType(type: ItemType): Flow<List<String>>

    @Query("""
        SELECT category FROM playlist_items 
        WHERE type = :type 
        GROUP BY category 
        ORDER BY MIN(orderIndex) ASC
    """)
    fun getCategoriesFromPlaylistItems(type: ItemType): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM playlist_items")
    suspend fun getItemCount(): Int

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistSourceId = :sourceId")
    suspend fun getItemCountBySource(sourceId: String): Int

    @Query("UPDATE playlist_items SET type = :majorityType WHERE category = :category AND type != :majorityType")
    suspend fun alignCategoryType(category: String, majorityType: ItemType): Int

    @Query("SELECT DISTINCT category FROM playlist_items WHERE category IS NOT NULL AND category != ''")
    suspend fun getAllDistinctCategories(): List<String>

    @Query("SELECT streamUrl FROM playlist_items WHERE category = :category")
    suspend fun getStreamUrlsForCategory(category: String): List<String>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE streamUrl LIKE '%gtv-videos-bucket%' OR streamUrl LIKE '%commondatastorage%' OR streamUrl LIKE '%media.w3.org%' OR streamUrl LIKE '%akamaized.net%'")
    suspend fun getOutdatedItemCount(): Int

    // Categories Entity DAO (Preserves API orderIndex & itemCount)
    @Query("""
        SELECT * FROM categories 
        WHERE type = :type 
        ORDER BY CASE WHEN userOrderIndex IS NOT NULL THEN userOrderIndex ELSE orderIndex END ASC
    """)
    fun getCategoriesEntitiesByType(type: String): Flow<List<com.example.data.model.CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategoriesListByType(type: String): List<com.example.data.model.CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<com.example.data.model.CategoryEntity>)

    @Query("UPDATE categories SET itemCount = :count WHERE type = :type AND categoryId = :categoryId")
    suspend fun updateCategoryItemCount(type: String, categoryId: String, count: Int)

    @Query("UPDATE categories SET userOrderIndex = :userOrderIndex WHERE type = :type AND categoryName = :categoryName")
    suspend fun updateCategoryUserOrder(type: String, categoryName: String, userOrderIndex: Int?)

    @Query("UPDATE categories SET isHidden = :hidden WHERE type = :type AND categoryName = :categoryName")
    suspend fun updateCategoryHidden(type: String, categoryName: String, hidden: Boolean)

    @Query("UPDATE categories SET userOrderIndex = NULL WHERE type = :type")
    suspend fun resetCategoryUserOrder(type: String)

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteCategoriesByType(type: String)

    // Category Counts
    @Query("SELECT * FROM category_counts WHERE section = :section ORDER BY orderIndex ASC")
    fun getCategoryCounts(section: String): Flow<List<com.example.data.model.CategoryCountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryCounts(counts: List<com.example.data.model.CategoryCountEntity>)

    @Query("DELETE FROM category_counts")
    suspend fun clearCategoryCounts()

    @Query("SELECT COUNT(*) FROM playlist_items WHERE type = 'VOD_SERIES' AND category = :category")
    suspend fun getSeriesCountForCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM playlist_items WHERE type = 'VOD_MOVIE' AND category = :category")
    suspend fun getMovieCountForCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM playlist_items WHERE type = 'LIVE_TV' AND category = :category")
    suspend fun getLiveCountForCategory(category: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlaylistItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlaylistItem)

    @Update
    suspend fun updateItem(item: PlaylistItem)

    @Query("UPDATE playlist_items SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: String, isFav: Boolean)

    @Query("UPDATE playlist_items SET isDownloaded = :isDown, downloadLocalPath = :path WHERE id = :id")
    suspend fun setDownloaded(id: String, isDown: Boolean, path: String?)

    @Query("UPDATE playlist_items SET lastWatchedTimestamp = :timestamp, watchCount = watchCount + 1 WHERE id = :id")
    suspend fun updateWatchHistory(id: String, timestamp: Long)

    @Query("UPDATE playlist_items SET lastWatchedTimestamp = 0 WHERE id = :id")
    suspend fun clearItemWatchHistory(id: String)

    @Query("UPDATE playlist_items SET lastWatchedTimestamp = 0 WHERE type = :type")
    suspend fun clearWatchHistoryForType(type: ItemType)

    @Query("UPDATE playlist_items SET isDownloaded = 0, downloadLocalPath = NULL WHERE id = :id")
    suspend fun clearItemDownload(id: String)

    @Query("UPDATE playlist_items SET isDownloaded = 0, downloadLocalPath = NULL WHERE type = :type")
    suspend fun clearDownloadsForType(type: ItemType)

    @Query("DELETE FROM playlist_items WHERE playlistSourceId = :sourceId")
    suspend fun deleteBySourceId(sourceId: String)

    @Query("DELETE FROM playlist_items")
    suspend fun clearAllItems()

    // Playlist Sources
    @Query("SELECT * FROM playlist_sources ORDER BY lastSyncTimestamp DESC")
    fun getAllSources(): Flow<List<PlaylistSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: PlaylistSource)

    @Query("DELETE FROM playlist_sources WHERE id = :id")
    suspend fun deleteSource(id: String)

    // Profiles
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Query("UPDATE user_profiles SET isActive = CASE WHEN id = :profileId THEN 1 ELSE 0 END")
    suspend fun setActiveProfile(profileId: String)

    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    // EPG (Electronic Program Guide)
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId ORDER BY startTimeMillis ASC")
    fun getProgramsForChannel(channelId: String): Flow<List<EpgProgram>>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endTimeMillis > :currentTime ORDER BY startTimeMillis ASC")
    fun getUpcomingProgramsForChannel(channelId: String, currentTime: Long): Flow<List<EpgProgram>>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND :currentTime >= startTimeMillis AND :currentTime < endTimeMillis LIMIT 1")
    suspend fun getCurrentProgramForChannel(channelId: String, currentTime: Long): EpgProgram?

    @Query("SELECT * FROM epg_programs WHERE endTimeMillis > :minTime ORDER BY startTimeMillis ASC")
    fun getAllUpcomingPrograms(minTime: Long): Flow<List<EpgProgram>>

    @Query("SELECT * FROM epg_programs ORDER BY startTimeMillis ASC")
    fun getAllPrograms(): Flow<List<EpgProgram>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgram>)

    @Query("DELETE FROM epg_programs WHERE endTimeMillis < :cutoffTime")
    suspend fun deleteOldPrograms(cutoffTime: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun clearAllPrograms()

    @Query("SELECT COUNT(*) FROM epg_programs")
    suspend fun getEpgCount(): Int

    @Query("SELECT COUNT(*) FROM playlist_items WHERE type = :type")
    suspend fun getItemCountByType(type: ItemType): Int

    @Query("SELECT COUNT(DISTINCT category) FROM playlist_items WHERE type = :type")
    suspend fun getCategoryCountByType(type: ItemType): Int

    @Query("SELECT * FROM epg_programs WHERE channelId IN (:channelIds) AND endTimeMillis > :currentTime ORDER BY startTimeMillis ASC")
    suspend fun getUpcomingProgramsForChannels(channelIds: List<String>, currentTime: Long): List<EpgProgram>

    // Cache Metadata
    @Query("SELECT * FROM playlist_cache_metadata WHERE sourceId = :sourceId LIMIT 1")
    suspend fun getCacheMetadata(sourceId: String): com.example.data.model.PlaylistCacheMetadata?

    @Query("SELECT * FROM playlist_cache_metadata ORDER BY cachedAtTimestamp DESC")
    fun getAllCacheMetadata(): Flow<List<com.example.data.model.PlaylistCacheMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheMetadata(metadata: com.example.data.model.PlaylistCacheMetadata)

    @Query("UPDATE playlist_cache_metadata SET lastValidatedTimestamp = :timestamp WHERE sourceId = :sourceId")
    suspend fun updateCacheLastValidated(sourceId: String, timestamp: Long)

    @Query("DELETE FROM playlist_cache_metadata WHERE sourceId = :sourceId")
    suspend fun deleteCacheMetadata(sourceId: String)

    @Query("DELETE FROM playlist_cache_metadata")
    suspend fun clearAllCacheMetadata()

    @androidx.room.Transaction
    suspend fun replacePlaylistCache(
        sourceId: String,
        items: List<PlaylistItem>,
        metadata: com.example.data.model.PlaylistCacheMetadata,
        clearExisting: Boolean = true
    ) {
        if (clearExisting) {
            clearAllItems()
            clearAllCacheMetadata()
        } else {
            deleteBySourceId(sourceId)
            deleteCacheMetadata(sourceId)
        }
        items.chunked(250).forEach { chunk ->
            insertAll(chunk)
        }
        insertCacheMetadata(metadata)
    }

    @androidx.room.Transaction
    suspend fun replaceEpgCache(
        programs: List<EpgProgram>,
        purgeOldPrograms: Boolean = true,
        cutoffTime: Long = System.currentTimeMillis() - 24 * 3600 * 1000L
    ) {
        if (purgeOldPrograms) {
            deleteOldPrograms(cutoffTime)
        }
        programs.chunked(250).forEach { chunk ->
            insertPrograms(chunk)
        }
    }

    // BÖLÜM 7 & 10: Watch Records (Continue Watching & History)
    @Query("SELECT * FROM watch_records ORDER BY lastWatchedAt DESC")
    fun getAllWatchRecords(): Flow<List<com.example.data.model.WatchRecord>>

    @Query("SELECT * FROM watch_records WHERE progress > 0.02 AND progress < 0.92 ORDER BY lastWatchedAt DESC LIMIT 20")
    fun getContinueWatching(): Flow<List<com.example.data.model.WatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchRecord(record: com.example.data.model.WatchRecord)

    @Query("DELETE FROM watch_records WHERE id = :id OR m3uKey = :id")
    suspend fun deleteWatchRecord(id: String)

    @Query("DELETE FROM watch_records")
    suspend fun clearWatchRecords()

    // BÖLÜM 8 & 9: Library Item Meta (Recently Added)
    @Query("SELECT * FROM library_item_meta WHERE type = :type ORDER BY firstSeenAt DESC LIMIT 20")
    fun getRecentItemsMeta(type: String): Flow<List<com.example.data.model.LibraryItemMeta>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLibraryItemMeta(meta: List<com.example.data.model.LibraryItemMeta>)

    @Query("UPDATE library_item_meta SET lastSeenAt = :timestamp WHERE playlistId = :playlistId AND itemKey = :itemKey")
    suspend fun updateLibraryItemLastSeen(playlistId: String, itemKey: String, timestamp: Long)

    // BÖLÜM 14: Category Preferences (Hide / Reorder)
    @Query("SELECT * FROM category_prefs WHERE playlistId = :playlistId AND section = :section ORDER BY position ASC")
    fun getCategoryPrefs(playlistId: String, section: String): Flow<List<com.example.data.model.CategoryPref>>

    @Query("SELECT categoryKey FROM category_prefs WHERE playlistId = :playlistId AND section = :section AND hidden = 1")
    fun getHiddenCategories(playlistId: String, section: String): Flow<List<String>>

    @Query("SELECT * FROM category_prefs WHERE playlistId = :playlistId AND section = :section AND categoryKey = :categoryKey LIMIT 1")
    suspend fun getCategoryPref(playlistId: String, section: String, categoryKey: String): com.example.data.model.CategoryPref?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategoryPref(pref: com.example.data.model.CategoryPref)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategoryPrefs(prefs: List<com.example.data.model.CategoryPref>)

    @Query("UPDATE category_prefs SET hidden = :hidden, updatedAt = :updatedAt WHERE playlistId = :playlistId AND section = :section AND categoryKey = :categoryKey")
    suspend fun setCategoryHidden(playlistId: String, section: String, categoryKey: String, hidden: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM category_prefs WHERE playlistId = :playlistId AND section = :section")
    suspend fun resetCategoryPrefs(playlistId: String, section: String)

    // BÖLÜM 13: Episode Progress
    @Query("SELECT * FROM episode_progress WHERE seriesKey = :seriesKey AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getEpisodeProgress(seriesKey: String, seasonNumber: Int, episodeNumber: Int): com.example.data.model.EpisodeProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEpisodeProgress(ep: com.example.data.model.EpisodeProgress)

    // Program Reminders
    @Query("SELECT * FROM program_reminders ORDER BY startTimeMillis ASC")
    fun getAllReminders(): Flow<List<com.example.data.model.ProgramReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: com.example.data.model.ProgramReminder)

    @Query("DELETE FROM program_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("""
        UPDATE playlist_items 
        SET releaseYear = CASE 
            WHEN (releaseYear IS NULL OR releaseYear = 0) AND name LIKE '%(2026)%' THEN 2026
            WHEN (releaseYear IS NULL OR releaseYear = 0) AND name LIKE '%(2025)%' THEN 2025
            WHEN (releaseYear IS NULL OR releaseYear = 0) AND name LIKE '%(2024)%' THEN 2024
            WHEN (releaseYear IS NULL OR releaseYear = 0) AND name LIKE '%(2023)%' THEN 2023
            ELSE releaseYear END,
            ratingValue = CASE WHEN (ratingValue IS NULL OR ratingValue = 0.0) AND rating > 0.0 THEN rating ELSE ratingValue END,
            rating5based = CASE WHEN (rating5based IS NULL OR rating5based = 0.0) AND rating > 0.0 THEN rating / 2.0 ELSE rating5based END
        WHERE type = 'VOD_MOVIE' OR type = 'VOD_SERIES'
    """)
    suspend fun backfillMissingMovieMetadata()

    @Query("SELECT * FROM playlist_items WHERE type = 'VOD_MOVIE' AND (releaseYear = 0 OR releaseYear IS NULL) LIMIT 1000")
    suspend fun getMoviesWithZeroReleaseYear(): List<PlaylistItem>

    @Query("UPDATE playlist_items SET releaseYear = :year WHERE id = :id")
    suspend fun updateReleaseYear(id: String, year: Int)
}
