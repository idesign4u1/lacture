package com.jewish.calendar.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.data.CalendarEvent
import com.jewish.calendar.model.HebrewDateModel
import com.jewish.calendar.ui.screens.common.DailyStudySection
import com.jewish.calendar.ui.theme.*
import com.jewish.calendar.viewmodel.CalendarViewModel
import com.jewish.calendar.viewmodel.DailyStudyUiState
import com.jewish.calendar.viewmodel.DailyStudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    dailyStudyViewModel: DailyStudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val studyState by dailyStudyViewModel.uiState.collectAsState()

    // Reload study items whenever selected date changes
    LaunchedEffect(uiState.selectedDate) {
        uiState.selectedDate?.let { dailyStudyViewModel.loadStudyForDate(it.gregorianDate) }
    }

    Scaffold(
        topBar = {
            CalendarTopBar(
                year = uiState.displayYear,
                month = uiState.displayMonth,
                onPreviousMonth = { viewModel.navigateMonth(false) },
                onNextMonth = { viewModel.navigateMonth(true) },
                onTodayClick = { viewModel.goToToday() }
            )
        },
        floatingActionButton = {
            if (uiState.selectedDate != null) {
                FloatingActionButton(
                    onClick = { viewModel.showAddEventDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "הוסף אירוע", tint = Color.White)
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
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // Hebrew date header for today
                uiState.today?.let { today ->
                    TodayHebrewHeader(today, viewModel)
                }

                // Day of week headers
                DayOfWeekHeader()

                // Calendar grid
                CalendarGrid(
                    days = uiState.currentMonthDays,
                    today = uiState.today,
                    selectedDate = uiState.selectedDate,
                    displayYear = uiState.displayYear,
                    displayMonth = uiState.displayMonth,
                    onDayClick = { viewModel.selectDate(it) }
                )

                // Selected day details
                uiState.selectedDate?.let { selected ->
                    SelectedDayDetails(
                        dateModel = selected,
                        events = uiState.selectedDayEvents,
                        studyState = studyState,
                        onStudyItemClick = { dailyStudyViewModel.openItem(it) },
                        onStudyDialogDismiss = { dailyStudyViewModel.closeDialog() },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Add event dialog
    if (uiState.showAddEventDialog) {
        uiState.selectedDate?.let { selectedDate ->
            AddEventDialog(
                selectedDate = selectedDate,
                onConfirm = { title, desc -> viewModel.addEvent(title, desc) },
                onDismiss = { viewModel.hideAddEventDialog() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    year: Int,
    month: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    val monthNames = listOf(
        "ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני",
        "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"
    )

    TopAppBar(
        title = {
            Text(
                text = "${monthNames[month - 1]} $year",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            TextButton(onClick = onTodayClick) {
                Text("היום", color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "חודש קודם")
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "חודש הבא")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun TodayHebrewHeader(
    today: HebrewDateModel,
    viewModel: CalendarViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hebrew date
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = today.hebrewDateString,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (today.parshaName?.isNotEmpty() == true) {
                        Text(
                            text = "פרשת ${today.parshaName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Special day badges
                Column(horizontalAlignment = Alignment.Start) {
                    if (today.isShabbat) {
                        HolidayBadge("שבת קודש", ShabbatBlue)
                    }
                    today.holidayName?.let {
                        HolidayBadge(it, if (today.isFastDay) FastDayGray else HolidayRed)
                    }
                    // Show Rosh Chodesh badge only when holidayName doesn't already include it
                    if (today.isRoshChodesh && today.holidayName == null) {
                        HolidayBadge("ראש חודש", OmerGreen)
                    }
                }
            }

            // Omer count
            today.omerCount?.let { count ->
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = viewModel.getOmerText(count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmerGreen,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HolidayBadge(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        days.forEachIndexed { index, day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (index == 6) ShabbatBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
    Divider()
}

@Composable
private fun CalendarGrid(
    days: List<HebrewDateModel>,
    today: HebrewDateModel?,
    selectedDate: HebrewDateModel?,
    displayYear: Int,
    displayMonth: Int,
    onDayClick: (HebrewDateModel) -> Unit
) {
    if (days.isEmpty()) return

    val firstDayCal = Calendar.getInstance().apply {
        set(displayYear, displayMonth - 1, 1)
    }
    val firstDayOfWeek = (firstDayCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)

    val todayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = todayFormatter.format(today?.gregorianDate ?: Date())

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.padding(horizontal = 4.dp),
        userScrollEnabled = false
    ) {
        items(firstDayOfWeek) {
            Box(modifier = Modifier.aspectRatio(1f))
        }

        items(days) { dayModel ->
            val dateStr = todayFormatter.format(dayModel.gregorianDate)
            val isToday = dateStr == todayStr
            val isSelected = selectedDate?.let {
                todayFormatter.format(it.gregorianDate) == dateStr
            } ?: false

            DayCell(
                dayModel = dayModel,
                isToday = isToday,
                isSelected = isSelected,
                onClick = { onDayClick(dayModel) }
            )
        }
    }
}

@Composable
private fun DayCell(
    dayModel: HebrewDateModel,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cal = Calendar.getInstance().apply { time = dayModel.gregorianDate }
    val gregorianDay = cal.get(Calendar.DAY_OF_MONTH)
    val isShabbat = dayModel.isShabbat

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isShabbat -> ShabbatBlue
        dayModel.isYomTov -> HolidayRed
        dayModel.isFastDay -> FastDayGray
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = gregorianDay.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            Text(
                text = com.jewish.calendar.model.HebrewNumbers.toGematria(dayModel.hebrewDay),
                fontSize = 8.sp,
                color = textColor.copy(alpha = 0.7f),
                lineHeight = 9.sp
            )
            val hasDot = dayModel.isHoliday || dayModel.isRoshChodesh || dayModel.additionalEvents.isNotEmpty()
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                dayModel.isHoliday || dayModel.isRoshChodesh -> HolidayRed
                                else -> FastDayGray
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun SelectedDayDetails(
    dateModel: HebrewDateModel,
    events: List<CalendarEvent>,
    studyState: DailyStudyUiState,
    onStudyItemClick: (com.jewish.calendar.data.StudyItem) -> Unit,
    onStudyDialogDismiss: () -> Unit,
    viewModel: CalendarViewModel
) {
    val cal = Calendar.getInstance().apply { time = dateModel.gregorianDate }
    val gregFormatter = SimpleDateFormat("EEEE, d בMMMM yyyy", Locale("he"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = gregFormatter.format(dateModel.gregorianDate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = dateModel.hebrewDateString,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Special day info
        if (dateModel.isShabbat) {
            InfoChip("שבת קודש", Icons.Default.Star, ShabbatBlue)
        }
        dateModel.holidayName?.let {
            InfoChip(
                it,
                Icons.Default.Celebration,
                if (dateModel.isFastDay) FastDayGray else HolidayRed
            )
        }
        if (dateModel.isRoshChodesh && dateModel.holidayName == null) {
            InfoChip("ראש חודש ${dateModel.hebrewMonthName}", Icons.Default.NightsStay, OmerGreen)
        }
        dateModel.parshaName?.let {
            InfoChip("פרשת $it", Icons.Default.MenuBook, MaterialTheme.colorScheme.primary)
        }
        dateModel.omerCount?.let { count ->
            InfoChip(viewModel.getOmerText(count), Icons.Default.Grain, OmerGreen)
        }
        // Yahrzeits and special events
        dateModel.additionalEvents.forEach { event ->
            val isYahrzeit = event.startsWith("יארצייט")
            InfoChip(
                text = event,
                icon = if (isYahrzeit) Icons.Default.Person else Icons.Default.Star,
                color = if (isYahrzeit) FastDayGray else Gold60
            )
        }

        // Events section
        if (events.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "אירועים ומשימות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            events.forEach { event ->
                EventItem(event = event, onDelete = { viewModel.deleteEvent(event) })
            }
        }

        // Daily study section
        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        DailyStudySection(
            state = studyState,
            onItemClick = onStudyItemClick,
            onDialogDismiss = onStudyDialogDismiss
        )
    }
}

@Composable
private fun EventItem(event: CalendarEvent, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "מחק",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.End
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Circle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(8.dp)
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("מחיקת אירוע") },
            text = { Text("למחוק את \"${event.title}\"?") },
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

@Composable
private fun InfoChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AddEventDialog(
    selectedDate: HebrewDateModel,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val gregFormatter = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "הוסף אירוע - ${gregFormatter.format(selectedDate.gregorianDate)}",
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("כותרת האירוע") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("תיאור (אופציונלי)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), description.trim()) },
                enabled = title.isNotBlank()
            ) {
                Text("הוסף")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}
