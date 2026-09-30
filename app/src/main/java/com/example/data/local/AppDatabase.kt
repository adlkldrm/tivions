package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppSettings
import com.example.data.model.EpgProgram
import com.example.data.model.FavoriteChannel
import com.example.data.model.PlaylistCacheMetadata
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSource
import com.example.data.model.UserProfile

@Database(
    entities = [
        PlaylistItem::class,
        PlaylistSource::class,
        UserProfile::class,
        AppSettings::class,
        EpgProgram::class,
        PlaylistCacheMetadata::class,
        FavoriteChannel::class,
        com.example.data.model.WatchRecord::class,
        com.example.data.model.LibraryItemMeta::class,
        com.example.data.model.CategoryPref::class,
        com.example.data.model.EpisodeProgress::class,
        com.example.data.model.ProgramReminder::class,
        com.example.data.model.CategoryCountEntity::class,
        com.example.data.model.CategoryEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tivions_iptv.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
