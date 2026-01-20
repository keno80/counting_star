package com.countingstar.domain

import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class BackupPayload(
    val version: Int,
    val createdAt: Long,
    val ledgers: List<Ledger>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val tags: List<Tag>,
    val merchants: List<Merchant>,
    val transactions: List<Transaction>,
)

data class CreateBackupParams(
    val createdAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
)

class CreateBackupUseCase
    @Inject
    constructor(
        private val ledgerRepository: LedgerRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository,
        private val merchantRepository: MerchantRepository,
        private val transactionRepository: TransactionRepository,
    ) {
        suspend operator fun invoke(params: CreateBackupParams = CreateBackupParams()): BackupPayload {
            val ledgers = ledgerRepository.observeLedgers().first()
            val accounts = ArrayList<Account>()
            val categories = ArrayList<Category>()
            val tags = ArrayList<Tag>()
            val merchants = ArrayList<Merchant>()
            val transactions = ArrayList<Transaction>()

            for (ledger in ledgers) {
                val ledgerId = ledger.id
                accounts.addAll(accountRepository.observeAccountsByLedger(ledgerId).first())
                categories.addAll(categoryRepository.observeCategories(ledgerId, CategoryType.INCOME).first())
                categories.addAll(categoryRepository.observeCategories(ledgerId, CategoryType.EXPENSE).first())
                tags.addAll(tagRepository.observeTagsByLedger(ledgerId).first())
                merchants.addAll(merchantRepository.observeMerchantsByLedger(ledgerId).first())
                transactions.addAll(transactionRepository.observeTransactionsByLedger(ledgerId).first())
            }

            return BackupPayload(
                version = params.version,
                createdAt = params.createdAt,
                ledgers = ledgers,
                accounts = accounts,
                categories = categories,
                tags = tags,
                merchants = merchants,
                transactions = transactions,
            )
        }
    }

data class RestoreBackupParams(
    val payload: BackupPayload,
    val overwrite: Boolean = true,
)

data class RestoreSummary(
    val ledgers: Int,
    val accounts: Int,
    val categories: Int,
    val tags: Int,
    val merchants: Int,
    val transactions: Int,
)

class RestoreBackupUseCase
    @Inject
    constructor(
        private val ledgerRepository: LedgerRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository,
        private val merchantRepository: MerchantRepository,
        private val transactionRepository: TransactionRepository,
    ) {
        suspend operator fun invoke(params: RestoreBackupParams): RestoreSummary {
            if (params.overwrite) {
                clearExistingData()
            }

            for (ledger in params.payload.ledgers) {
                ledgerRepository.upsert(ledger)
            }
            for (account in params.payload.accounts) {
                accountRepository.upsert(account)
            }
            for (category in params.payload.categories) {
                categoryRepository.upsert(category)
            }
            for (tag in params.payload.tags) {
                tagRepository.upsert(tag)
            }
            for (merchant in params.payload.merchants) {
                merchantRepository.upsert(merchant)
            }
            for (transaction in params.payload.transactions) {
                transactionRepository.upsert(transaction)
            }

            return RestoreSummary(
                ledgers = params.payload.ledgers.size,
                accounts = params.payload.accounts.size,
                categories = params.payload.categories.size,
                tags = params.payload.tags.size,
                merchants = params.payload.merchants.size,
                transactions = params.payload.transactions.size,
            )
        }

        private suspend fun clearExistingData() {
            val ledgers = ledgerRepository.observeLedgers().first()
            for (ledger in ledgers) {
                val ledgerId = ledger.id
                val transactions = transactionRepository.observeTransactionsByLedger(ledgerId).first()
                for (transaction in transactions) {
                    transactionRepository.deleteById(transaction.id)
                }
                val merchants = merchantRepository.observeMerchantsByLedger(ledgerId).first()
                for (merchant in merchants) {
                    merchantRepository.deleteById(merchant.id)
                }
                val tags = tagRepository.observeTagsByLedger(ledgerId).first()
                for (tag in tags) {
                    tagRepository.deleteById(tag.id)
                }
                val categories =
                    categoryRepository.observeCategories(ledgerId, CategoryType.INCOME).first() +
                        categoryRepository.observeCategories(ledgerId, CategoryType.EXPENSE).first()
                for (category in categories) {
                    categoryRepository.deleteById(category.id)
                }
                val accounts = accountRepository.observeAccountsByLedger(ledgerId).first()
                for (account in accounts) {
                    accountRepository.deleteById(account.id)
                }
            }
            for (ledger in ledgers) {
                ledgerRepository.deleteById(ledger.id)
            }
        }
    }
