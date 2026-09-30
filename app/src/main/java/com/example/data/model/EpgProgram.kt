package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["startTimeMillis"]),
        Index(value = ["endTimeMillis"])
    ]
)
data class EpgProgram(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String = "",
    val category: String = "Genel",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val iconUrl: String? = null,
    val episodeTitle: String? = null
) {
    val durationMinutes: Int
        get() = ((endTimeMillis - startTimeMillis) / 60000).toInt().coerceAtLeast(1)

    fun isCurrentlyPlaying(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime in startTimeMillis until endTimeMillis
    }

    fun isPast(currentTime: Long = System.currentTimeMillis()): Boolean {
        return endTimeMillis <= currentTime
    }

    fun isUpcoming(currentTime: Long = System.currentTimeMillis()): Boolean {
        return startTimeMillis > currentTime
    }

    fun progressFraction(currentTime: Long = System.currentTimeMillis()): Float {
        if (endTimeMillis <= startTimeMillis) return 0f
        val elapsed = currentTime - startTimeMillis
        val total = endTimeMillis - startTimeMillis
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun formattedTimeRange(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val start = sdf.format(Date(startTimeMillis))
        val end = sdf.format(Date(endTimeMillis))
        return "$start - $end"
    }

    fun formattedStartTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(startTimeMillis))
    }
}

data class EpgChannel(
    val id: String,
    val displayName: String,
    val iconUrl: String? = null
)

data class XmlTvParseResult(
    val channels: List<EpgChannel>,
    val programs: List<EpgProgram>
)
