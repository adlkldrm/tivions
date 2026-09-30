package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColorHex: Long = 0xFF6366F1,
    val isKidsProfile: Boolean = false,
    val pinCode: String? = null,
    val isActive: Boolean = false
)
