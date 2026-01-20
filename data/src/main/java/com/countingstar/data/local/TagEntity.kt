package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
)
