package com.countingstar.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class TagItem(
    val id: String,
    val name: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun TagGroup(
    tags: List<TagItem>,
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val selected = selectedTagIds.contains(tag.id)
            FilterChip(
                selected = selected,
                onClick = { onTagToggle(tag.id) },
                label = { Text(tag.name) },
            )
        }
    }
}
