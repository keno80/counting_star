package com.countingstar.domain

import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class BalanceCheckParams(
    val ledgerId: String,
)

data class AccountBalanceStatus(
    val accountId: String,
    val expectedBalance: Long,
    val actualBalance: Long,
    val delta: Long,
)

data class BalanceCheckResult(
    val ledgerId: String,
    val items: List<AccountBalanceStatus>,
    val hasMismatch: Boolean,
)

class CheckBalanceConsistencyUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository,
    ) {
        suspend operator fun invoke(params: BalanceCheckParams): BalanceCheckResult {
            val accounts = accountRepository.observeAccountsByLedger(params.ledgerId).first()
            val transactions = transactionRepository.observeTransactionsByLedger(params.ledgerId).first()
            val deltas = computeDeltas(transactions)
            val items =
                accounts.map { account ->
                    val expected = account.initialBalance + (deltas[account.id] ?: 0L)
                    val delta = expected - account.currentBalance
                    AccountBalanceStatus(
                        accountId = account.id,
                        expectedBalance = expected,
                        actualBalance = account.currentBalance,
                        delta = delta,
                    )
                }
            return BalanceCheckResult(
                ledgerId = params.ledgerId,
                items = items,
                hasMismatch = items.any { it.delta != 0L },
            )
        }
    }

data class RecalculateBalancesParams(
    val ledgerId: String,
)

data class RecalculateBalancesResult(
    val ledgerId: String,
    val items: List<AccountBalanceStatus>,
    val updatedCount: Int,
)

class RecalculateBalancesUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository,
    ) {
        suspend operator fun invoke(params: RecalculateBalancesParams): RecalculateBalancesResult {
            val accounts = accountRepository.observeAccountsByLedger(params.ledgerId).first()
            val transactions = transactionRepository.observeTransactionsByLedger(params.ledgerId).first()
            val deltas = computeDeltas(transactions)
            val items = ArrayList<AccountBalanceStatus>(accounts.size)
            var updatedCount = 0

            for (account in accounts) {
                val expected = account.initialBalance + (deltas[account.id] ?: 0L)
                val delta = expected - account.currentBalance
                if (delta != 0L) {
                    accountRepository.updateBalance(account.id, expected)
                    updatedCount += 1
                }
                items.add(
                    AccountBalanceStatus(
                        accountId = account.id,
                        expectedBalance = expected,
                        actualBalance = account.currentBalance,
                        delta = delta,
                    ),
                )
            }

            return RecalculateBalancesResult(
                ledgerId = params.ledgerId,
                items = items,
                updatedCount = updatedCount,
            )
        }
    }

private fun computeDeltas(transactions: List<Transaction>): Map<String, Long> {
    val deltas = LinkedHashMap<String, Long>()
    for (transaction in transactions) {
        if (transaction.isDeleted) {
            continue
        }
        when (transaction.type) {
            TransactionType.INCOME -> addDelta(deltas, transaction.accountId, transaction.amount)
            TransactionType.EXPENSE -> addDelta(deltas, transaction.accountId, -transaction.amount)
            TransactionType.TRANSFER -> {
                addDelta(deltas, transaction.fromAccountId, -transaction.amount)
                addDelta(deltas, transaction.toAccountId, transaction.amount)
            }
        }
    }
    return deltas
}

private fun addDelta(
    deltas: MutableMap<String, Long>,
    accountId: String?,
    amount: Long,
) {
    if (accountId.isNullOrBlank()) {
        return
    }
    val current = deltas[accountId] ?: 0L
    deltas[accountId] = current + amount
}
