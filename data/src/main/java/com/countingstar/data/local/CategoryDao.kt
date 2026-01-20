package com.countingstar.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        "SELECT * FROM category " +
            "WHERE ledgerId = :ledgerId AND type = :type " +
            "ORDER BY isPinned DESC, sort ASC, name ASC",
    )
    fun observeCategories(
        ledgerId: String,
        type: String,
    ): Flow<List<CategoryEntity>>

    @Transaction
    @Query(
        "SELECT * FROM category " +
            "WHERE ledgerId = :ledgerId AND type = :type AND parentId IS NULL " +
            "ORDER BY isPinned DESC, sort ASC, name ASC",
    )
    fun observeCategoryTree(
        ledgerId: String,
        type: String,
    ): Flow<List<CategoryWithChildren>>

    @Query("SELECT * FROM category WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE category SET sort = :sort WHERE id = :id")
    suspend fun updateSort(
        id: String,
        sort: Int,
    )

    @Query("UPDATE category SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(
        id: String,
        isPinned: Boolean,
    )
}

data class CategoryWithChildren(
    @Embedded val parent: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId",
    )
    val children: List<CategoryEntity>,
)
