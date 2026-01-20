package com.countingstar.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.countingstar.data.local.AccountDao
import com.countingstar.data.local.AccountEntity
import com.countingstar.data.local.CategoryDao
import com.countingstar.data.local.CategoryEntity
import com.countingstar.data.local.GreetingDao
import com.countingstar.data.local.GreetingEntity
import com.countingstar.data.local.LedgerDao
import com.countingstar.data.local.LedgerEntity
import com.countingstar.data.local.MerchantDao
import com.countingstar.data.local.MerchantEntity
import com.countingstar.data.local.TagDao
import com.countingstar.data.local.TagEntity
import com.countingstar.data.local.TransactionDao
import com.countingstar.data.local.TransactionEntity
import com.countingstar.data.local.TransactionTagCrossRef
import com.countingstar.domain.Account
import com.countingstar.domain.AccountRepository
import com.countingstar.domain.AccountType
import com.countingstar.domain.Category
import com.countingstar.domain.CategoryRepository
import com.countingstar.domain.CategoryType
import com.countingstar.domain.CategoryWithChildren
import com.countingstar.domain.GreetingRepository
import com.countingstar.domain.Ledger
import com.countingstar.domain.LedgerRepository
import com.countingstar.domain.Merchant
import com.countingstar.domain.MerchantRepository
import com.countingstar.domain.Tag
import com.countingstar.domain.TagRepository
import com.countingstar.domain.Transaction
import com.countingstar.domain.TransactionRepository
import com.countingstar.domain.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GreetingRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val greetingDao: GreetingDao,
    ) : GreetingRepository {
        private val greetingKey = stringPreferencesKey("greeting_text")

        override fun greetingFlow(): Flow<String> {
            val dataStoreFlow =
                dataStore.data.map { preferences ->
                    preferences[greetingKey]
                }
            val roomFlow = greetingDao.observeGreeting().map { it?.text }
            return combine(dataStoreFlow, roomFlow) { dataStoreValue, roomValue ->
                dataStoreValue ?: roomValue ?: "Counting Star"
            }.distinctUntilChanged()
        }

        override suspend fun setGreeting(text: String) {
            dataStore.edit { preferences ->
                preferences[greetingKey] = text
            }
            greetingDao.upsert(GreetingEntity(text = text))
        }
    }

class LedgerRepositoryImpl
    @Inject
    constructor(
        private val ledgerDao: LedgerDao,
    ) : LedgerRepository {
        override fun observeLedgers(): Flow<List<Ledger>> =
            ledgerDao.observeLedgers().map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getLedgerById(id: String): Ledger? = ledgerDao.getLedgerById(id)?.toDomain()

        override suspend fun upsert(ledger: Ledger) {
            ledgerDao.upsert(ledger.toEntity())
        }

        override suspend fun update(ledger: Ledger) {
            ledgerDao.update(ledger.toEntity())
        }

        override suspend fun deleteById(id: String) {
            ledgerDao.deleteById(id)
        }

        override fun observeDefaultLedger(): Flow<Ledger?> = ledgerDao.observeDefaultLedger().map { it?.toDomain() }

        override suspend fun clearDefault() {
            ledgerDao.clearDefault()
        }

        override suspend fun setDefaultLedger(ledgerId: String) {
            ledgerDao.setDefaultLedger(ledgerId)
        }
    }

class AccountRepositoryImpl
    @Inject
    constructor(
        private val accountDao: AccountDao,
    ) : AccountRepository {
        override fun observeAccountsByLedger(ledgerId: String): Flow<List<Account>> =
            accountDao.observeAccountsByLedger(ledgerId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getAccountById(id: String): Account? = accountDao.getAccountById(id)?.toDomain()

        override suspend fun upsert(account: Account) {
            accountDao.upsert(account.toEntity())
        }

        override suspend fun update(account: Account) {
            accountDao.update(account.toEntity())
        }

        override suspend fun deleteById(id: String) {
            accountDao.deleteById(id)
        }

        override suspend fun setActive(
            id: String,
            isActive: Boolean,
        ) {
            accountDao.setActive(id, isActive)
        }

        override suspend fun updateBalance(
            id: String,
            balance: Long,
        ) {
            accountDao.updateBalance(id, balance)
        }
    }

class CategoryRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
    ) : CategoryRepository {
        override fun observeCategories(
            ledgerId: String,
            type: CategoryType,
        ): Flow<List<Category>> =
            categoryDao.observeCategories(ledgerId, type.name).map { entities ->
                entities.map { it.toDomain() }
            }

        override fun observeCategoryTree(
            ledgerId: String,
            type: CategoryType,
        ): Flow<List<CategoryWithChildren>> =
            categoryDao.observeCategoryTree(ledgerId, type.name).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getCategoryById(id: String): Category? = categoryDao.getCategoryById(id)?.toDomain()

        override suspend fun upsert(category: Category) {
            categoryDao.upsert(category.toEntity())
        }

        override suspend fun update(category: Category) {
            categoryDao.update(category.toEntity())
        }

        override suspend fun deleteById(id: String) {
            categoryDao.deleteById(id)
        }

        override suspend fun updateSort(
            id: String,
            sort: Int,
        ) {
            categoryDao.updateSort(id, sort)
        }

        override suspend fun updatePinned(
            id: String,
            isPinned: Boolean,
        ) {
            categoryDao.updatePinned(id, isPinned)
        }
    }

class TagRepositoryImpl
    @Inject
    constructor(
        private val tagDao: TagDao,
    ) : TagRepository {
        override fun observeTagsByLedger(ledgerId: String): Flow<List<Tag>> =
            tagDao.observeTagsByLedger(ledgerId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getTagById(id: String): Tag? = tagDao.getTagById(id)?.toDomain()

        override suspend fun upsert(tag: Tag) {
            tagDao.upsert(tag.toEntity())
        }

        override suspend fun update(tag: Tag) {
            tagDao.update(tag.toEntity())
        }

        override suspend fun deleteById(id: String) {
            tagDao.deleteById(id)
        }

        override fun observeTagsByTransaction(transactionId: String): Flow<List<Tag>> =
            tagDao.observeTagsByTransaction(transactionId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun upsertTransactionTags(
            transactionId: String,
            tagIds: List<String>,
        ) {
            tagDao.deleteTransactionTagsByTransaction(transactionId)
            if (tagIds.isEmpty()) {
                return
            }
            val crossRefs =
                tagIds.map { tagId ->
                    TransactionTagCrossRef(
                        transactionId = transactionId,
                        tagId = tagId,
                    )
                }
            tagDao.upsertTransactionTags(crossRefs)
        }

        override suspend fun deleteTransactionTagsByTransaction(transactionId: String) {
            tagDao.deleteTransactionTagsByTransaction(transactionId)
        }
    }

class MerchantRepositoryImpl
    @Inject
    constructor(
        private val merchantDao: MerchantDao,
    ) : MerchantRepository {
        override fun observeMerchantsByLedger(ledgerId: String): Flow<List<Merchant>> =
            merchantDao.observeMerchantsByLedger(ledgerId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getMerchantById(id: String): Merchant? = merchantDao.getMerchantById(id)?.toDomain()

        override suspend fun upsert(merchant: Merchant) {
            merchantDao.upsert(merchant.toEntity())
        }

        override suspend fun update(merchant: Merchant) {
            merchantDao.update(merchant.toEntity())
        }

        override suspend fun deleteById(id: String) {
            merchantDao.deleteById(id)
        }

        override fun observeMerchantsByKeyword(
            ledgerId: String,
            keyword: String,
        ): Flow<List<Merchant>> =
            merchantDao.observeMerchantsByKeyword(ledgerId, keyword).map { entities ->
                entities.map { it.toDomain() }
            }
    }

class TransactionRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val tagDao: TagDao,
    ) : TransactionRepository {
        override fun observeTransactionsByLedger(ledgerId: String): Flow<List<Transaction>> =
            transactionDao.observeTransactionsByLedger(ledgerId).map { entities ->
                val result = ArrayList<Transaction>(entities.size)
                for (entity in entities) {
                    val tagIds = tagDao.getTagIdsByTransaction(entity.id)
                    result.add(entity.toDomain(tagIds))
                }
                result
            }

        override suspend fun getTransactionById(id: String): Transaction? =
            transactionDao
                .getTransactionById(id)
                ?.let { entity ->
                    entity.toDomain(tagDao.getTagIdsByTransaction(entity.id))
                }

        override suspend fun upsert(transaction: Transaction) {
            transactionDao.upsert(transaction.toEntity())
            syncTransactionTags(transaction)
        }

        override suspend fun update(transaction: Transaction) {
            transactionDao.update(transaction.toEntity())
            syncTransactionTags(transaction)
        }

        override suspend fun deleteById(id: String) {
            transactionDao.deleteById(id)
            tagDao.deleteTransactionTagsByTransaction(id)
        }

        override fun observeTransactionsByFilters(
            ledgerId: String,
            startTime: Long?,
            endTime: Long?,
            minAmount: Long?,
            maxAmount: Long?,
            accountId: String?,
            categoryId: String?,
            tagId: String?,
            merchantId: String?,
            keyword: String?,
        ): Flow<List<Transaction>> =
            transactionDao
                .observeTransactionsByFilters(
                    ledgerId = ledgerId,
                    startTime = startTime,
                    endTime = endTime,
                    minAmount = minAmount,
                    maxAmount = maxAmount,
                    accountId = accountId,
                    categoryId = categoryId,
                    tagId = tagId,
                    merchantId = merchantId,
                    keyword = keyword,
                ).map { entities ->
                    val result = ArrayList<Transaction>(entities.size)
                    for (entity in entities) {
                        val tagIds = tagDao.getTagIdsByTransaction(entity.id)
                        result.add(entity.toDomain(tagIds))
                    }
                    result
                }

        private suspend fun syncTransactionTags(transaction: Transaction) {
            tagDao.deleteTransactionTagsByTransaction(transaction.id)
            if (transaction.tagIds.isEmpty()) {
                return
            }
            val crossRefs =
                transaction.tagIds.map { tagId ->
                    TransactionTagCrossRef(
                        transactionId = transaction.id,
                        tagId = tagId,
                    )
                }
            tagDao.upsertTransactionTags(crossRefs)
        }
    }

private fun LedgerEntity.toDomain(): Ledger =
    Ledger(
        id = id,
        name = name,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun Ledger.toEntity(): LedgerEntity =
    LedgerEntity(
        id = id,
        name = name,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDefault = false,
    )

private fun AccountEntity.toDomain(): Account =
    Account(
        id = id,
        ledgerId = ledgerId,
        name = name,
        type = AccountType.valueOf(type),
        currency = currency,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        isActive = isActive,
        creditBillingDay = creditBillingDay,
        creditRepaymentDay = creditRepaymentDay,
        creditLimit = creditLimit,
    )

private fun Account.toEntity(): AccountEntity =
    AccountEntity(
        id = id,
        ledgerId = ledgerId,
        name = name,
        type = type.name,
        currency = currency,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        isActive = isActive,
        creditBillingDay = creditBillingDay,
        creditRepaymentDay = creditRepaymentDay,
        creditLimit = creditLimit,
    )

private fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        ledgerId = ledgerId,
        type = CategoryType.valueOf(type),
        parentId = parentId,
        name = name,
        sort = sort,
        isPinned = isPinned,
    )

private fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        ledgerId = ledgerId,
        type = type.name,
        parentId = parentId,
        name = name,
        sort = sort,
        isPinned = isPinned,
    )

private fun com.countingstar.data.local.CategoryWithChildren.toDomain(): CategoryWithChildren =
    CategoryWithChildren(
        parent = parent.toDomain(),
        children = children.map { it.toDomain() },
    )

private fun TagEntity.toDomain(): Tag =
    Tag(
        id = id,
        ledgerId = ledgerId,
        name = name,
        color = color,
        icon = icon,
    )

private fun Tag.toEntity(): TagEntity =
    TagEntity(
        id = id,
        ledgerId = ledgerId,
        name = name,
        color = color,
        icon = icon,
    )

private fun MerchantEntity.toDomain(): Merchant =
    Merchant(
        id = id,
        ledgerId = ledgerId,
        name = name,
        alias = alias,
    )

private fun Merchant.toEntity(): MerchantEntity =
    MerchantEntity(
        id = id,
        ledgerId = ledgerId,
        name = name,
        alias = alias,
    )

private fun TransactionEntity.toDomain(tagIds: List<String>): Transaction =
    Transaction(
        id = id,
        ledgerId = ledgerId,
        type = TransactionType.valueOf(type),
        amount = amount,
        currency = currency,
        occurredAt = occurredAt,
        note = note,
        accountId = accountId,
        categoryId = categoryId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        tagIds = tagIds,
        merchantId = merchantId,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
    )

private fun Transaction.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        ledgerId = ledgerId,
        type = type.name,
        amount = amount,
        currency = currency,
        occurredAt = occurredAt,
        note = note,
        accountId = accountId,
        categoryId = categoryId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        merchantId = merchantId,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
    )
