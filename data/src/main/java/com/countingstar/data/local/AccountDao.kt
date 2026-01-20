package com.countingstar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM account WHERE ledgerId = :ledgerId ORDER BY name ASC")
    fun observeAccountsByLedger(ledgerId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE account SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(
        id: String,
        isActive: Boolean,
    )

    @Query("UPDATE account SET currentBalance = :balance WHERE id = :id")
    suspend fun updateBalance(
        id: String,
        balance: Long,
    )
}
