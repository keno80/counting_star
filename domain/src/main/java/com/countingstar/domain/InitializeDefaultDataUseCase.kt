package com.countingstar.domain

import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

data class InitializeDefaultDataResult(
    val ledgerId: String,
    val accountId: String,
    val createdLedger: Boolean,
    val createdAccount: Boolean,
    val createdCategoryCount: Int,
    val createdTagCount: Int,
)

class InitializeDefaultDataUseCase
    @Inject
    constructor(
        private val ledgerRepository: LedgerRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository,
        private val preferenceRepository: PreferenceRepository,
    ) {
        suspend operator fun invoke(): InitializeDefaultDataResult {
            val now = System.currentTimeMillis()
            val ledgers = ledgerRepository.observeLedgers().first()
            val preferredLedgerId = preferenceRepository.getDefaultLedgerId()
            val defaultLedgerIdFromDb = ledgerRepository.observeDefaultLedger().first()?.id
            var ledgerId =
                preferredLedgerId
                    ?: defaultLedgerIdFromDb
                    ?: ledgers.firstOrNull()?.id
            var createdLedger = false
            if (ledgerId.isNullOrBlank()) {
                ledgerId = UUID.randomUUID().toString()
                ledgerRepository.upsert(
                    Ledger(
                        id = ledgerId,
                        name = "默认账本",
                        isArchived = false,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                ledgerRepository.clearDefault()
                ledgerRepository.setDefaultLedger(ledgerId)
                preferenceRepository.setDefaultLedgerId(ledgerId)
                createdLedger = true
            } else {
                if (defaultLedgerIdFromDb != ledgerId) {
                    ledgerRepository.clearDefault()
                    ledgerRepository.setDefaultLedger(ledgerId)
                }
                if (preferredLedgerId.isNullOrBlank()) {
                    preferenceRepository.setDefaultLedgerId(ledgerId)
                }
            }

            val accounts = accountRepository.observeAccountsByLedger(ledgerId).first()
            val preferredAccountId = preferenceRepository.getDefaultAccountId()
            var accountId = preferredAccountId
            var createdAccount = false
            if (accountId.isNullOrBlank() || accounts.none { it.id == accountId }) {
                if (accounts.isEmpty()) {
                    accountId = UUID.randomUUID().toString()
                    accountRepository.upsert(
                        Account(
                            id = accountId,
                            ledgerId = ledgerId,
                            name = "现金",
                            type = AccountType.CASH,
                            currency = "CNY",
                            initialBalance = 0L,
                            currentBalance = 0L,
                            isActive = true,
                        ),
                    )
                    createdAccount = true
                } else {
                    accountId = accounts.first().id
                }
                preferenceRepository.setDefaultAccountId(accountId)
            }

            var createdCategoryCount = 0
            val incomeCategories =
                categoryRepository.observeCategories(ledgerId, CategoryType.INCOME).first()
            if (incomeCategories.isEmpty()) {
                createdCategoryCount +=
                    upsertCategoryGroups(
                        ledgerId,
                        CategoryType.INCOME,
                        incomeGroups(),
                    )
            }
            val expenseCategories =
                categoryRepository.observeCategories(ledgerId, CategoryType.EXPENSE).first()
            if (expenseCategories.isEmpty()) {
                createdCategoryCount +=
                    upsertCategoryGroups(
                        ledgerId,
                        CategoryType.EXPENSE,
                        expenseGroups(),
                    )
            }

            var createdTagCount = 0
            val tags = tagRepository.observeTagsByLedger(ledgerId).first()
            if (tags.isEmpty()) {
                for (name in defaultTagNames()) {
                    tagRepository.upsert(
                        Tag(
                            id = UUID.randomUUID().toString(),
                            ledgerId = ledgerId,
                            name = name,
                        ),
                    )
                    createdTagCount += 1
                }
            }

            return InitializeDefaultDataResult(
                ledgerId = ledgerId,
                accountId = accountId,
                createdLedger = createdLedger,
                createdAccount = createdAccount,
                createdCategoryCount = createdCategoryCount,
                createdTagCount = createdTagCount,
            )
        }

        private suspend fun upsertCategoryGroups(
            ledgerId: String,
            type: CategoryType,
            groups: List<CategoryGroup>,
        ): Int {
            var count = 0
            groups.forEachIndexed { parentIndex, group ->
                val parentId = UUID.randomUUID().toString()
                categoryRepository.upsert(
                    Category(
                        id = parentId,
                        ledgerId = ledgerId,
                        type = type,
                        parentId = null,
                        name = group.name,
                        sort = parentIndex + 1,
                        isPinned = false,
                    ),
                )
                count += 1
                group.children.forEachIndexed { childIndex, child ->
                    categoryRepository.upsert(
                        Category(
                            id = UUID.randomUUID().toString(),
                            ledgerId = ledgerId,
                            type = type,
                            parentId = parentId,
                            name = child,
                            sort = childIndex + 1,
                            isPinned = false,
                        ),
                    )
                    count += 1
                }
            }
            return count
        }

        private fun incomeGroups(): List<CategoryGroup> =
            listOf(
                CategoryGroup("工作", listOf("工资", "奖金")),
                CategoryGroup("投资", listOf("利息", "理财")),
                CategoryGroup("其他收入", listOf("其他")),
            )

        private fun expenseGroups(): List<CategoryGroup> =
            listOf(
                CategoryGroup("餐饮", listOf("三餐", "零食")),
                CategoryGroup("交通", listOf("公交", "打车")),
                CategoryGroup("购物", listOf("日用品", "服饰")),
                CategoryGroup("居住", listOf("房租", "水电")),
                CategoryGroup("医疗", listOf("看病")),
                CategoryGroup("娱乐", listOf("游戏")),
                CategoryGroup("其他支出", listOf("其他")),
            )

        private fun defaultTagNames(): List<String> =
            listOf(
                "日常",
                "旅行",
                "重要",
            )

        private data class CategoryGroup(
            val name: String,
            val children: List<String>,
        )
    }
