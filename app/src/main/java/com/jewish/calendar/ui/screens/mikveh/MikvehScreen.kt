package com.jewish.calendar.ui.screens.mikveh

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.model.CycleStatus
import com.jewish.calendar.model.TevilahRecord
import com.jewish.calendar.ui.theme.*
import com.jewish.calendar.viewmodel.MikvehViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MikvehScreen(
    viewModel: MikvehViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("מצב נוכחי", "מקוואות", "היסטוריה")

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ניהול טהרה",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.showAddCycleDialog() },
                    containerColor = PurityPurple
                ) {
                    Icon(Icons.Default.Add, contentDescription = "הוסף מחזור", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> CycleStatusTab(
                    status = uiState.cycleStatus,
                    isLoading = uiState.isLoading,
                    viewModel = viewModel
                )
                1 -> MikvehMapTab()
                2 -> TevilahHistoryTab(viewModel = viewModel)
            }
        }
    }

    // Dialogs
    if (uiState.showAddCycleDialog) {
        StartCycleDialog(
            onConfirm = { viewModel.startNewCycle() },
            onDismiss = { viewModel.hideAddCycleDialog() }
        )
    }

    if (uiState.showDailyCheckDialog) {
        DailyCheckDialog(
            dayNumber = uiState.cycleStatus?.cleanDayNumber ?: 1,
            onConfirm = { isClean, note ->
                val day = uiState.cycleStatus?.cleanDayNumber ?: 1
                viewModel.addDailyCheck(day, isClean, note)
            },
            onDismiss = { viewModel.hideDailyCheckDialog() }
        )
    }

    if (uiState.showDeleteCycleDialog) {
        DeleteCycleDialog(
            onConfirm = { viewModel.deleteCycle() },
            onDismiss = { viewModel.hideDeleteCycleDialog() }
        )
    }
}

@Composable
private fun CycleStatusTab(
    status: CycleStatus?,
    isLoading: Boolean,
    viewModel: MikvehViewModel
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PurityPurple)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (status == null || status.currentCycle == null) {
            item {
                NoCycleCard(onStart = { viewModel.showAddCycleDialog() })
            }
        } else {
            item {
                CurrentStatusCard(status, onDelete = { viewModel.showDeleteCycleDialog() })
            }

            if (status.isInSevenCleanDays) {
                item {
                    SevenCleanDaysTracker(
                        currentDay = status.cleanDayNumber ?: 1,
                        checks = status.cleanDayChecks,
                        onAddCheck = { viewModel.showDailyCheckDialog() }
                    )
                }
            }

            status.projectedTevilahDate?.let { tevilahDate ->
                item { TevilahCountdownCard(tevilahDate) }
            }

            status.nextVesetDate?.let { vesetDate ->
                item { NextVesetCard(vesetDate) }
            }

            item {
                ActionButtonsRow(
                    status = status,
                    onEndCycle = { viewModel.endCycle() },
                    onRecordTevilah = { viewModel.recordTevilah("") }
                )
            }
        }
    }
}

@Composable
private fun NoCycleCard(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PurityPink.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💧", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "אין מחזור פעיל",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PurityPurple
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "לחצי על + כדי להתחיל מחזור חדש",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = PurityPurple)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("התחל מחזור חדש")
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(status: CycleStatus, onDelete: () -> Unit) {
    val dateFormatter = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                status.isInSevenCleanDays -> CleanGreen.copy(alpha = 0.1f)
                status.isInPeriod -> PurityPink.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "מחק מחזור",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                val statusText = when {
                    status.isInSevenCleanDays -> "שבעה נקיים - יום ${status.cleanDayNumber}"
                    status.isInPeriod -> "ימי נידה - יום ${status.dayOfPeriod}"
                    else -> "מחזור פעיל"
                }
                val statusColor = when {
                    status.isInSevenCleanDays -> CleanGreen
                    status.isInPeriod -> PurityPurple
                    else -> MaterialTheme.colorScheme.primary
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    status.currentCycle?.let { cycle ->
                        Text(
                            text = "תחילת מחזור: ${dateFormatter.format(Date(cycle.startDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                status.isInSevenCleanDays -> CleanGreen
                                status.isInPeriod -> PurityPurple
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            status.isInSevenCleanDays -> "✓"
                            status.isInPeriod -> "${status.dayOfPeriod}"
                            else -> "?"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SevenCleanDaysTracker(
    currentDay: Int,
    checks: List<com.jewish.calendar.model.CleanDayCheck>,
    onAddCheck: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CleanGreen.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanGreen),
                    modifier = Modifier.size(width = 120.dp, height = 36.dp)
                ) {
                    Text("הוסף בדיקה", fontSize = 12.sp)
                }
                Text(
                    text = "שבעה נקיים",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CleanGreen
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..7).forEach { day ->
                    CleanDayDot(
                        dayNumber = day,
                        isCurrent = day == currentDay,
                        isPast = day < currentDay,
                        isChecked = checks.any { it.dayNumber == day && it.isClean }
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanDayDot(
    dayNumber: Int,
    isCurrent: Boolean,
    isPast: Boolean,
    isChecked: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isChecked -> CleanGreen
                        isCurrent -> CleanGreen.copy(alpha = 0.3f)
                        isPast -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent || isChecked) CleanGreen else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = dayNumber.toString(),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) CleanGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        Text(
            text = "יום $dayNumber",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TevilahCountdownCard(tevilahDate: Date) {
    val dateFormatter = SimpleDateFormat("EEEE, d/M", Locale("he"))
    val daysLeft = ((tevilahDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PurityPurple.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = "יום הטבילה",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PurityPurple
                )
                Text(
                    text = dateFormatter.format(tevilahDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (daysLeft >= 0) {
                    Text(
                        text = if (daysLeft == 0) "הלילה!" else "עוד $daysLeft ימים",
                        style = MaterialTheme.typography.bodySmall,
                        color = PurityPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("💧", fontSize = 36.sp)
        }
    }
}

@Composable
private fun NextVesetCard(vesetDate: Date) {
    val dateFormatter = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormatter.format(vesetDate),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "וסת צפוי (בערך)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    status: CycleStatus,
    onEndCycle: () -> Unit,
    onRecordTevilah: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (status.isInPeriod && status.currentCycle?.endDate == null) {
            OutlinedButton(
                onClick = onEndCycle,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, PurityPurple)
            ) {
                Text("סיום ימי נידה", color = PurityPurple)
            }
        }
        if (status.isInSevenCleanDays) {
            Button(
                onClick = onRecordTevilah,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PurityPurple)
            ) {
                Text("רשום טבילה")
            }
        }
    }
}

// --- Mikveh Map Tab (replaced with Google Maps link) ---

@Composable
private fun MikvehMapTab() {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("💧", fontSize = 56.sp)
            Text(
                text = "חיפוש מקוואות בסביבה",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "לחצי כדי לחפש מקוואות קרובות דרך Google Maps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val mapsUri = Uri.parse("geo:0,0?q=מקווה")
                    val intent = Intent(Intent.ACTION_VIEW, mapsUri)
                    intent.setPackage("com.google.android.apps.maps")
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        // Fallback to browser
                        val webIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://maps.google.com/?q=מקווה")
                        )
                        context.startActivity(webIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurityPurple)
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("פתח Google Maps", color = Color.White)
            }
        }
    }
}

// --- Tevilah History Tab ---

@Composable
private fun TevilahHistoryTab(viewModel: MikvehViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

    if (uiState.tevilahHistory.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💧", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "אין היסטוריית טבילות עדיין",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.tevilahHistory) { record ->
                TevilahHistoryItem(
                    record = record,
                    dateFormatter = dateFormatter,
                    onDelete = { viewModel.deleteTevilah(record) }
                )
            }
        }
    }
}

@Composable
private fun TevilahHistoryItem(
    record: TevilahRecord,
    dateFormatter: SimpleDateFormat,
    onDelete: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "מחק",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormatter.format(Date(record.date)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PurityPurple,
                    textAlign = TextAlign.End
                )
                if (record.mikvehName.isNotEmpty()) {
                    Text(
                        text = record.mikvehName,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("💧", fontSize = 20.sp)
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("מחיקת רשומה") },
            text = { Text("למחוק את רשומת הטבילה מתאריך ${dateFormatter.format(Date(record.date))}?") },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("מחק") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("ביטול") }
            }
        )
    }
}

// --- Dialogs ---

@Composable
private fun StartCycleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("התחלת מחזור חדש", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
        text = {
            Text(
                "האם להתחיל מחזור חדש מהיום?",
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PurityPurple)
            ) {
                Text("כן, התחל")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}

@Composable
private fun DeleteCycleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("מחיקת מחזור", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text(
                "האם למחוק את המחזור הנוכחי? פעולה זו לא ניתנת לביטול.",
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("מחק")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}

@Composable
private fun DailyCheckDialog(
    dayNumber: Int,
    onConfirm: (Boolean, String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "בדיקת יום $dayNumber",
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("הערה (אופציונלי)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(true, note) },
                colors = ButtonDefaults.buttonColors(containerColor = CleanGreen)
            ) {
                Text("בדיקה נקייה ✓")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(false, note) }) {
                    Text("לא נקייה", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text("ביטול") }
            }
        }
    )
}
