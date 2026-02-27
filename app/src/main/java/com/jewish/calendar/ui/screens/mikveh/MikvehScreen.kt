package com.jewish.calendar.ui.screens.mikveh

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

// ── Accessible Mikveh Data ─────────────────────────────────────────────

private data class AccessibleMikveh(
    val city: String,
    val address: String,   // mikveh name / street
    val contact: String,   // contact name + phone(s)
    val region: String
)

private val MIKVEH_REGIONS = listOf(
    "הכל", "צפון", "חיפה", "שרון", "גוש דן", "ירושלים", "שפלה ודרום", "שומרון"
)

private val ACCESSIBLE_MIKVEHS = listOf(
    // ── צפון ──────────────────────────────────────────────────────────────
    AccessibleMikveh("בית שאן",       "אחד העם 7",                    "סמדר: 04-6587480",                              "צפון"),
    AccessibleMikveh("גליל תחתון",    "מצפה נטופה",                   "דנה: 052-4531484",                              "צפון"),
    AccessibleMikveh("עכו",           "הכרם",                         "רמונד: 04-9917554",                             "צפון"),
    AccessibleMikveh("עכו",           "גבעת התמרים",                  "מרים: 04-9913493",                              "צפון"),
    AccessibleMikveh("עמק יזרעאל",    "תל עדשים",                     "גאולה: 058-7977081",                            "צפון"),
    AccessibleMikveh("עפולה",         "גבעת המורה",                   "שושנה: 054-6212270",                            "צפון"),
    AccessibleMikveh("עפולה",         "מעלות (בשיפוצים)",             "רות: 050-6314170",                              "צפון"),
    AccessibleMikveh("צפת",           "ברסלב (פרטי)",                 "יפה: 054-8457552, 077-7877848",                 "צפון"),
    AccessibleMikveh("קרית שמונה",    "מקווה הורדים",                 "רחל: 058-4841815",                              "צפון"),
    AccessibleMikveh("רמת הגולן",     "חיספין",                       "פנינה: 054-4335153",                            "צפון"),
    // ── חיפה ──────────────────────────────────────────────────────────────
    AccessibleMikveh("חיפה",          "רח' בצלאל — מקווה הדר",       "זהבה: 052-7137352",                             "חיפה"),
    AccessibleMikveh("חיפה",          "רח' יד לבנים — חסדי טהרה",   "חנה: 052-7116266",                              "חיפה"),
    AccessibleMikveh("קרית ביאליק",   "קק\"ל 75",                     "חגית: 04-6564843",                              "חיפה"),
    // ── שרון ──────────────────────────────────────────────────────────────
    AccessibleMikveh("אלעד",          "אבטליון",                      "גוליה: 054-8418637",                            "שרון"),
    AccessibleMikveh("הרצליה",        "שלמה המלך 34",                 "לימור: 09-8352246",                             "שרון"),
    AccessibleMikveh("חדרה",          "לבוצ'קין 9",                   "נחמה: 04-6323660",                              "שרון"),
    AccessibleMikveh("מודיעין",       "בוכמן",                        "פרחיה: 054-8590407",                            "שרון"),
    AccessibleMikveh("נתניה",         "גליקסון 17 — מקווה סלע",      "רוחמה: 050-7577574, אביבה: 050-4149332",        "שרון"),
    AccessibleMikveh("פרדס חנה",      "דרך הבנים 103",                "פנינה/איילה: 04-6373440",                       "שרון"),
    AccessibleMikveh("ראש העין",      "הרש\"ש 23",                    "נצחיה: 054-8423737",                            "שרון"),
    AccessibleMikveh("רמלה",          "ברמת דן",                      "עליזה: 050-4447019",                            "שרון"),
    // ── גוש דן ────────────────────────────────────────────────────────────
    AccessibleMikveh("בני ברק",       "שיכון ה' — בארי 7",           "מאירה: 03-5794661, מרגלית: 050-4115307",        "גוש דן"),
    AccessibleMikveh("בת ים",         "הלפר 38",                      "דורית: 052-6551876",                            "גוש דן"),
    AccessibleMikveh("חולון",         "רח' הרב קוק 9",               "אביטל חג'ג: 052-2954916, 050-3323806",          "גוש דן"),
    AccessibleMikveh("סביון",         "הדרום 12",                     "שירלי: 054-2179288",                            "גוש דן"),
    AccessibleMikveh("פתח תקוה",      "עמישב",                        "דורית: 052-6176458",                            "גוש דן"),
    AccessibleMikveh("ראשון לציון",   "מקווה בובה — רח' קפח",        "אורלי: 03-6047934",                             "גוש דן"),
    AccessibleMikveh("רחובות",        "כיכר החשמונאים 4",             "אסתר: 052-4312500",                             "גוש דן"),
    AccessibleMikveh("רמת גן",        "עזריאל 24 / רמת השקמה",       "לימור: 052-6636668",                            "גוש דן"),
    AccessibleMikveh("תל אביב",       "רמת החיל",                     "אילנה פור: 054-7708312",                        "גוש דן"),
    AccessibleMikveh("תל אביב",       "תל כביר",                      "אלינה פור: 054-7708312",                        "גוש דן"),
    // ── ירושלים ───────────────────────────────────────────────────────────
    AccessibleMikveh("אפרת",          "זית שמן 34",                   "הדסה לפידות: 02-9934740",                       "ירושלים"),
    AccessibleMikveh("בית שמש",       "בן זכאי 32",                   "מרגלית: 02-9998472",                            "ירושלים"),
    AccessibleMikveh("בית שמש",       "רמה ג' — יואל 3",             "דבורה גרוס: 054-8410121",                       "ירושלים"),
    AccessibleMikveh("ביתר עלית",     "קדושת הלוי",                   "לאה: 050-4117230",                              "ירושלים"),
    AccessibleMikveh("ירושלים",       "בקעה — גדעון 7",              "אסתר: 02-6717597",                              "ירושלים"),
    AccessibleMikveh("ירושלים",       "מורשה",                        "רבקה: 054-7070738",                             "ירושלים"),
    // ── שפלה ודרום ────────────────────────────────────────────────────────
    AccessibleMikveh("אילת",          "שחמון",                        "נעה: 050-4508499",                              "שפלה ודרום"),
    AccessibleMikveh("אשדוד",         "העצמאות 39",                   "אילנה: 073-2654684",                            "שפלה ודרום"),
    AccessibleMikveh("אשקלון",        "ברנע 7",                       "צביה עיני: 08-6781192, 052-5945588",            "שפלה ודרום"),
    AccessibleMikveh("באר שבע",       "רח' הגאונים",                  "אושרית: 08-6431440",                            "שפלה ודרום"),
    AccessibleMikveh("גן יבנה",       "רח' ארגמן",                    "נינט: 08-6588392",                              "שפלה ודרום"),
    AccessibleMikveh("חוף אשקלון",    "ניצן (ח. טופס 4)",             "08-6775586",                                    "שפלה ודרום"),
    AccessibleMikveh("לכיש",          "אליהב",                        "ענת: 052-3311131",                              "שפלה ודרום"),
    AccessibleMikveh("מרחבים",        "שבי דרון",                     "מעיין: 054-6462719",                            "שפלה ודרום"),
    AccessibleMikveh("נחל שורק",      "נצר חזני",                     "רחל: 054-5684669",                              "שפלה ודרום"),
    AccessibleMikveh("נחל שורק",      "גני טל",                       "אורלי: 050-4124837",                            "שפלה ודרום"),
    AccessibleMikveh("קרית גת",       "יפתח הגלעדי 9 — מקווה תמר",  "דינה: 08-6525490",                              "שפלה ודרום"),
    AccessibleMikveh("קרית גת",       "רח' סטרומה 5 (חדש)",          "08-9957288",                                    "שפלה ודרום"),
    AccessibleMikveh("רמת נגב",       "רתמים",                        "חיה: 054-3129680",                              "שפלה ודרום"),
    AccessibleMikveh("שדות נגב",      "מעגלים",                       "יוכבד: 054-4641725",                            "שפלה ודרום"),
    // ── שומרון ────────────────────────────────────────────────────────────
    AccessibleMikveh("שומרון",        "ברוכין",                       "נעמה: 054-5447429",                             "שומרון"),
)

private val PHONE_REGEX = Regex("""0\d{1,2}-\d{7}""")

// --- Mikveh Map Tab — Accessible Mikvehs with Region Filter ---

@Composable
private fun MikvehMapTab() {
    var selectedRegion by remember { mutableStateOf("הכל") }

    val filtered = remember(selectedRegion) {
        if (selectedRegion == "הכל") ACCESSIBLE_MIKVEHS
        else ACCESSIBLE_MIKVEHS.filter { it.region == selectedRegion }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "מקוואות מונגשות ♿ עם מעלון",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PurityPurple
            )
        }

        // ── Region filter chips ──────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(MIKVEH_REGIONS) { region ->
                FilterChip(
                    selected = selectedRegion == region,
                    onClick = { selectedRegion = region },
                    label = { Text(region, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurityPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // ── Count line ───────────────────────────────────────────────────
        Text(
            text = "${filtered.size} מקוואות",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
        Divider()

        // ── List ─────────────────────────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { mikveh ->
                AccessibleMikvehCard(mikveh)
            }
        }
    }
}

@Composable
private fun AccessibleMikvehCard(mikveh: AccessibleMikveh) {
    val context = LocalContext.current
    val firstPhone = PHONE_REGEX.find(mikveh.contact)?.value

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // City + region badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Region badge (left/start in RTL = visually right)
                Surface(
                    color = PurityPurple.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = mikveh.region,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PurityPurple,
                        fontWeight = FontWeight.Medium
                    )
                }
                // City name (right/end in RTL = visually left)
                Text(
                    text = mikveh.city,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(4.dp))

            // Address
            Text(
                text = mikveh.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Contact + call button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call button
                if (firstPhone != null) {
                    IconButton(
                        onClick = {
                            val dialUri = Uri.parse("tel:${firstPhone.replace("-", "")}")
                            context.startActivity(Intent(Intent.ACTION_DIAL, dialUri))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurityPurple.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "התקשרי",
                            tint = PurityPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Contact text
                Text(
                    text = mikveh.contact,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
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
