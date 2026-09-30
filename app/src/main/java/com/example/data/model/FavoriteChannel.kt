package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Dedicated Room Entity to store favorite TV channels and playlist items.
 *
 * Indexed on:
 * - [channelId]: for rapid lookup and join queries.
 * - [groupTitle]: for categorized filtering of favorite channels.
 * - [createdAt]: to allow sorting by date added.
 */
@Entity(
    tableName = "favorite_channels",
    indices = [
        Index(value = ["channel_id"], unique = true),
        Index(value = ["group_title"]),
        Index(value = ["created_at"])
    ]
)
data class FavoriteChannel(
    @PrimaryKey
    @ColumnInfo(name = "channel_id")
    val channelId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "stream_url")
    val streamUrl: String,

    @ColumnInfo(name = "group_title")
    val groupTitle: String = "Genel",

    @ColumnInfo(name = "logo_url")
    val logoUrl: String? = null,

    @ColumnInfo(name = "epg_channel_id")
    val epgChannelId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
