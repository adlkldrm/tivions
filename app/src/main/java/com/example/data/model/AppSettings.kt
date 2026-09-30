package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val language: String = "Türkçe",
    val autoRefreshInterval: String = "3 Günlük", // Günlük, 3 Günlük, 1 Haftalık, Uygulama Girişinde
    val liveStreamFormat: String = ".ts", // .ts, .m3u8
    val videoPlayerEngine: String = "EXO", // EXO, VLC Player
    val livePlayerEngine: String = "Advanced Exo Player", // Advanced Exo Player, Default Exo Player, VLC Player
    val backgroundPlayback: Boolean = true,
    val epgSource: String = "M3U/XMLTV'den",
    val epgAutoMatch: Boolean = true,
    val epgMatchByName: Boolean = true,
    val epgCache: Boolean = true,
    val epgAutoUpdate: String = "Otomatik",
    val epgShowPastPrograms: Boolean = false,
    val epgTimeShiftMinutes: Int = 0,
    val bufferSetting: String = "Otomatik", // Otomatik, 3 saniye, 5 saniye, 7 saniye
    val isDarkMode: Boolean = true,
    val isParentalControlEnabled: Boolean = false,
    val parentalPin: String = "0000"
)
