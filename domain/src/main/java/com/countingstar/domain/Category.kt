package com.countingstar.domain

data class Category(
    val id: String,
    val ledgerId: String,
    val type: CategoryType,
    val parentId: String? = null,
    val name: String,
    val sort: Int,
    val isPinned: Boolean,
)

enum class CategoryType {
    INCOME,
    EXPENSE,
}
