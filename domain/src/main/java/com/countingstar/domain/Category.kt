package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

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

data class CategoryWithChildren(
    val parent: Category,
    val children: List<Category>,
)

interface CategoryRepository {
    fun observeCategories(
        ledgerId: String,
        type: CategoryType,
    ): Flow<List<Category>>

    fun observeCategoryTree(
        ledgerId: String,
        type: CategoryType,
    ): Flow<List<CategoryWithChildren>>

    suspend fun getCategoryById(id: String): Category?

    suspend fun upsert(category: Category)

    suspend fun update(category: Category)

    suspend fun deleteById(id: String)

    suspend fun updateSort(
        id: String,
        sort: Int,
    )

    suspend fun updatePinned(
        id: String,
        isPinned: Boolean,
    )
}
