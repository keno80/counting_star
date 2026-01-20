package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "greeting")
data class GreetingEntity(
    @PrimaryKey val id: Int = 0,
    val text: String,
)
