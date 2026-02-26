package com.jewish.calendar.ui.screens.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jewish.calendar.data.StudyItem
import com.jewish.calendar.ui.theme.ShabbatBlue
import com.jewish.calendar.viewmodel.DailyStudyUiState

@Composable
fun DailyStudySection(
    state: DailyStudyUiState,
    onItemClick: (StudyItem) -> Unit,
    onDialogDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = ShabbatBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "לימוד יומי",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ShabbatBlue
            )
        }

        when {
            state.isLoading -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            state.error != null -> {
                Text(
                    state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    textAlign = TextAlign.End
                )
            }
            state.items.isEmpty() -> { /* nothing */ }
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        state.items.forEachIndexed { idx, item ->
                            StudyItemRow(item = item, onClick = { onItemClick(item) })
                            if (idx < state.items.lastIndex) {
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for selected item
    state.selectedItem?.let { item ->
        StudyTextDialog(
            item = item,
            text = state.selectedText,
            isLoading = state.isLoadingText,
            onDismiss = onDialogDismiss
        )
    }
}

@Composable
fun StudyItemRow(item: StudyItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = ShabbatBlue,
            modifier = Modifier.size(18.dp)
        )
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.titleHe,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            )
            if (item.subtitleHe.isNotBlank()) {
                Text(
                    text = item.subtitleHe,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTextDialog(
    item: StudyItem,
    text: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    item.titleHe,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                if (item.subtitleHe.isNotBlank()) {
                    Text(
                        item.subtitleHe,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 400.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = text ?: "לא נמצא תוכן",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            lineHeight = 28.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("סגור") }
        }
    )
}
