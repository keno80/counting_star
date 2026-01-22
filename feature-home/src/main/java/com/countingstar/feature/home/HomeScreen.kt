package com.countingstar.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.countingstar.core.ui.EmptyState
import com.countingstar.core.ui.component.ListHeader
import com.countingstar.core.ui.component.formatAmount
import com.countingstar.core.ui.component.formatDate
import com.countingstar.core.ui.component.formatTime
import com.countingstar.domain.Account
import com.countingstar.domain.Category
import com.countingstar.domain.Transaction
import com.countingstar.domain.TransactionType
import kotlin.math.abs

@Composable
fun homeScreen(
    uiState: HomeUiState,
    onAddTransaction: () -> Unit,
    onStatisticsClick: () -> Unit,
) {
    val transactions = uiState.transactions
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayLimit = 20
    val hasTransactions = transactions.isNotEmpty()
    val displayedTransactions =
        if (expanded) {
            transactions
        } else {
            transactions.take(displayLimit)
        }
    val showMore = transactions.size > displayedTransactions.size

    val listItems =
        if (hasTransactions) {
            buildList {
                add(HomeListItem.Overview)
                add(HomeListItem.Metrics)
                add(HomeListItem.RecentHeader(showMore = showMore))
                var lastDate: String? = null
                displayedTransactions.forEach { transaction ->
                    val currentDate = formatDate(transaction.occurredAt)
                    if (currentDate != lastDate) {
                        add(HomeListItem.DateHeader(currentDate))
                        lastDate = currentDate
                    }
                    add(HomeListItem.TransactionItem(transaction))
                }
            }
        } else {
            emptyList()
        }

    val listState = rememberLazyListState()
    val stickyDate by remember(listItems, listState) {
        derivedStateOf {
            var date: String? = null
            val firstDateIndex = listItems.indexOfFirst { it is HomeListItem.DateHeader }
            val firstIndex = listState.firstVisibleItemIndex
            if (firstDateIndex == -1 || firstIndex < firstDateIndex) {
                return@derivedStateOf null
            }
            for (i in 0..minOf(firstIndex, listItems.lastIndex)) {
                val item = listItems[i]
                if (item is HomeListItem.DateHeader) {
                    date = item.date
                }
            }
            val firstItemIsHeader = listItems.getOrNull(firstIndex) is HomeListItem.DateHeader
            if (firstItemIsHeader && listState.firstVisibleItemScrollOffset == 0) {
                null
            } else {
                date
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        if (hasTransactions) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(
                    listItems,
                    key = { index, item -> item.key(index) },
                ) { _, item ->
                    when (item) {
                        HomeListItem.Overview -> {
                            OverviewCard(
                                summary = uiState.monthSummary,
                                lastMonthSummary = uiState.lastMonthSummary,
                                onClick = onStatisticsClick,
                            )
                        }
                        HomeListItem.Metrics -> {
                            MetricsSection(
                                todaySummary = uiState.todaySummary,
                                monthSummary = uiState.monthSummary,
                            )
                        }
                        is HomeListItem.RecentHeader -> {
                            RecentHeader(
                                showMore = item.showMore,
                                onShowMore = { expanded = true },
                            )
                        }
                        is HomeListItem.DateHeader -> {
                            ListHeader(title = item.date)
                        }
                        is HomeListItem.TransactionItem -> {
                            TransactionListItem(
                                transaction = item.transaction,
                                accountMap = uiState.accountMap,
                                categoryMap = uiState.categoryMap,
                            )
                        }
                    }
                }
            }
            val headerDate = stickyDate
            if (headerDate != null) {
                ListHeader(
                    title = headerDate,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        } else {
            EmptyState(
                message = "暂无流水，记一笔开始记录吧",
                actionLabel = "记一笔",
                onActionClick = onAddTransaction,
            )
        }
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "记一笔",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun homeScreenPreview() {
    homeScreen(
        uiState = HomeUiState(),
        onAddTransaction = {},
        onStatisticsClick = {},
    )
}

private sealed interface HomeListItem {
    data object Overview : HomeListItem

    data object Metrics : HomeListItem

    data class RecentHeader(
        val showMore: Boolean,
    ) : HomeListItem

    data class DateHeader(
        val date: String,
    ) : HomeListItem

    data class TransactionItem(
        val transaction: Transaction,
    ) : HomeListItem
}

private fun HomeListItem.key(index: Int): String =
    when (this) {
        HomeListItem.Overview -> "overview"
        HomeListItem.Metrics -> "metrics"
        is HomeListItem.RecentHeader -> "recent-header"
        is HomeListItem.DateHeader -> "date-$date"
        is HomeListItem.TransactionItem -> transaction.id
    } + "-$index"

@Suppress("ktlint:standard:function-naming")
@Composable
private fun OverviewCard(
    summary: HomeSummaryUi,
    lastMonthSummary: HomeSummaryUi,
    onClick: () -> Unit,
) {
    val delta = summary.balance - lastMonthSummary.balance
    val trendText =
        when {
            delta > 0 -> "较上月 +${formatAmount(delta)}"
            delta < 0 -> "较上月 -${formatAmount(abs(delta))}"
            else -> "较上月持平"
        }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "本月总览",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OverviewSummaryItem(
                    label = "收入",
                    value = formatAmount(summary.income),
                    modifier = Modifier.weight(1f),
                )
                OverviewSummaryItem(
                    label = "支出",
                    value = formatAmount(summary.expense),
                    modifier = Modifier.weight(1f),
                )
                OverviewSummaryItem(
                    label = "结余",
                    value = formatAmount(summary.balance),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = trendText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun MetricsSection(
    todaySummary: HomeSummaryUi,
    monthSummary: HomeSummaryUi,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricsCard(
            title = "今日",
            summary = todaySummary,
            modifier = Modifier.weight(1f),
        )
        MetricsCard(
            title = "本月",
            summary = monthSummary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun MetricsCard(
    title: String,
    summary: HomeSummaryUi,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            SummaryItem(label = "收入", value = formatAmount(summary.income))
            SummaryItem(label = "支出", value = formatAmount(summary.expense))
            SummaryItem(label = "结余", value = formatAmount(summary.balance))
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun SummaryItem(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun OverviewSummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun RecentHeader(
    showMore: Boolean,
    onShowMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "最近流水",
            style = MaterialTheme.typography.titleMedium,
        )
        if (showMore) {
            TextButton(onClick = onShowMore) {
                Text(text = "查看更多")
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun TransactionListItem(
    transaction: Transaction,
    accountMap: Map<String, Account>,
    categoryMap: Map<String, Category>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = transactionTypeLabel(transaction.type))
            Text(text = transactionAmountText(transaction))
        }
        Text(
            text =
                if (transaction.type == TransactionType.TRANSFER) {
                    "转账 · ${transactionDirectionText(transaction, accountMap)}"
                } else {
                    "${transactionCategoryText(transaction, categoryMap)} · ${transactionDirectionText(transaction, accountMap)}"
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val note = transaction.note
        Text(
            text =
                if (note.isNotBlank()) {
                    "${formatTime(transaction.occurredAt)} · ${noteSummary(note)}"
                } else {
                    formatTime(transaction.occurredAt)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun transactionTypeLabel(type: TransactionType): String =
    when (type) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
        TransactionType.TRANSFER -> "转账"
    }

private fun transactionAmountText(transaction: Transaction): String {
    val amount = formatAmount(transaction.amount)
    return when (transaction.type) {
        TransactionType.INCOME -> "+$amount"
        TransactionType.EXPENSE -> "-$amount"
        TransactionType.TRANSFER -> amount
    }
}

private fun transactionDirectionText(
    transaction: Transaction,
    accountMap: Map<String, Account>,
): String {
    val accountName = { id: String? ->
        id?.let { accountMap[it]?.name } ?: "未选择"
    }
    return when (transaction.type) {
        TransactionType.TRANSFER ->
            "${accountName(transaction.fromAccountId)} → ${accountName(transaction.toAccountId)}"
        TransactionType.INCOME,
        TransactionType.EXPENSE,
        -> accountName(transaction.accountId)
    }
}

private fun transactionCategoryText(
    transaction: Transaction,
    categoryMap: Map<String, Category>,
): String = transaction.categoryId?.let { categoryMap[it]?.name } ?: "未选择"

private fun noteSummary(
    note: String,
    limit: Int = 20,
): String {
    val content = note.trim()
    return if (content.length > limit) {
        "${content.take(limit)}…"
    } else {
        content
    }
}
