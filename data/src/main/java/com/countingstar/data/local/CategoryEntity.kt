package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val type: String,
    val parentId: String? = null,
    val name: String,
    val sort: Int,
    val isPinned: Boolean,
)
