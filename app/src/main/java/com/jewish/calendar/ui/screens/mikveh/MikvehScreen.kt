package com.jewish.calendar.ui.screens.mikveh

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.jewish.calendar.model.CycleStatus
import com.jewish.calendar.model.TevilahRecord
import com.jewish.calendar.ui.theme.*
import com.jewish.calendar.viewmodel.MikvehViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

internal data class AccessibleMikveh(
    val city: String,
    val address: String,   // mikveh name / street
    val contact: String,   // contact name + phone(s)
    val region: String,
    val wazeUrl: String = ""
)

internal val MIKVEH_REGIONS = listOf(
    "הכל", "גוש דן", "שרון", "חיפה", "צפון", "ירושלים", "שפלה ודרום", "שומרון"
)

// Approximate city center coordinates (lat, lon)
internal val CITY_COORDS: Map<String, Pair<Double, Double>> = mapOf(
    // גוש דן
    "אור יהודה" to (32.027 to 34.858), "אזור" to (31.990 to 34.814),
    "אחיעזר" to (31.936 to 34.825), "אלעד" to (32.053 to 34.954),
    "בית דגן" to (31.996 to 34.834), "בני ברק" to (32.083 to 34.834),
    "בני עייש" to (31.885 to 34.816), "בת ים" to (32.017 to 34.752),
    "גבעת שמואל" to (32.072 to 34.850), "גבעתיים" to (32.070 to 34.811),
    "גדרה" to (31.812 to 34.773), "גני תקווה" to (32.065 to 34.888),
    "חולון" to (32.018 to 34.782), "יהוד" to (32.028 to 34.888),
    "יהוד מונוסון" to (32.028 to 34.888), "כפר חב\"ד" to (31.994 to 34.900),
    "לוד" to (31.951 to 34.896), "מזכרת בתיה" to (31.865 to 34.811),
    "נס ציונה" to (31.930 to 34.799), "סביון" to (32.050 to 34.903),
    "פתח תקווה" to (32.087 to 34.888), "צור יגאל" to (32.203 to 34.930),
    "קרית אונו" to (32.061 to 34.859), "ראשון לציון" to (31.973 to 34.793),
    "רחובות" to (31.899 to 34.813), "רמלה" to (31.928 to 34.872),
    "רמת גן" to (32.068 to 34.824), "תל אביב יפו" to (32.085 to 34.782),
    "תל מונד" to (32.268 to 34.921),
    // שרון
    "אבן יהודה" to (32.274 to 34.900), "אור עקיבא" to (32.502 to 34.922),
    "אורנית" to (32.135 to 34.980), "אליכין" to (32.424 to 34.946),
    "אלפי מנשה" to (32.157 to 35.020), "בנימינה" to (32.513 to 34.952),
    "גבעת עדה" to (32.506 to 34.957), "הוד השרון" to (32.149 to 34.890),
    "הרצליה" to (32.162 to 34.845), "זכרון יעקב" to (32.567 to 34.944),
    "חדרה" to (32.434 to 34.919), "כפר יונה" to (32.293 to 34.926),
    "כפר סבא" to (32.176 to 34.907), "נתניה" to (32.322 to 34.853),
    "עמנואל" to (32.153 to 35.063), "פרדס חנה כרכור" to (32.473 to 34.969),
    "פרדס חנה-כרכור" to (32.473 to 34.969), "פרדסיה" to (32.278 to 34.926),
    "קדימה צורן" to (32.273 to 34.918), "קיסריה" to (32.505 to 34.912),
    "ראש העין" to (32.096 to 34.958), "רמת השרון" to (32.146 to 34.839),
    "רעננה" to (32.183 to 34.870),
    // חיפה
    "חיפה" to (32.794 to 34.990), "טירת הכרמל" to (32.763 to 34.971),
    "נשר" to (32.768 to 35.029), "קרית אתא" to (32.798 to 35.088),
    "קרית ביאליק" to (32.830 to 35.074), "קרית ים" to (32.852 to 35.067),
    "קרית מוצקין" to (32.827 to 35.073), "קרית טבעון" to (32.717 to 35.122),
    "רכסים" to (32.743 to 35.074), "רמת ישי" to (32.706 to 35.160),
    "עכו" to (32.930 to 35.082), "נהריה" to (33.005 to 35.095),
    "עתלית" to (32.704 to 34.943),
    // צפון
    "בית שאן" to (32.499 to 35.497), "חיספין" to (32.979 to 35.745),
    "חצור הגלילית" to (32.992 to 35.544), "טבריה" to (32.792 to 35.531),
    "יבניאל" to (32.745 to 35.509), "יוקנעם עילית" to (32.659 to 35.109),
    "יקנעם עילית" to (32.659 to 35.109), "כפר תבור" to (32.687 to 35.429),
    "כרמיאל" to (32.912 to 35.303), "מגדל" to (32.837 to 35.502),
    "מגדל העמק" to (32.676 to 35.241), "מטולה" to (33.276 to 35.571),
    "מעלות תרשיחא" to (32.995 to 35.272), "נוף הגליל" to (32.701 to 35.286),
    "עפולה" to (32.607 to 35.290), "פוריה נווה עובד" to (32.726 to 35.543),
    "צפת" to (32.965 to 35.498), "קצרין" to (32.991 to 35.689),
    "קרית שמונה" to (33.208 to 35.571), "שלומי" to (33.081 to 35.138),
    // ירושלים
    "אדם" to (31.950 to 35.268), "אלון שבות" to (31.659 to 35.145),
    "אלעזר" to (31.644 to 35.141), "אפרת" to (31.660 to 35.178),
    "בית שמש" to (31.752 to 34.986), "ביתר עילית" to (31.697 to 35.115),
    "בת עין" to (31.641 to 35.109), "גבעת זאב" to (31.860 to 35.182),
    "טלז סטון" to (31.823 to 35.127), "טלמון" to (31.961 to 35.206),
    "ירושלים" to (31.768 to 35.214), "כפר אלדד" to (31.621 to 35.241),
    "כרמי צור" to (31.616 to 35.115), "מבשרת ציון" to (31.800 to 35.133),
    "מודיעין עילית" to (31.920 to 35.009), "מעלה אדומים" to (31.774 to 35.300),
    "נוה דניאל" to (31.653 to 35.118), "נוקדים" to (31.603 to 35.267),
    "עלי" to (32.062 to 35.291), "עפרה" to (31.974 to 35.227),
    "קרית ארבע" to (31.527 to 35.114), "תל ציון" to (31.968 to 35.219),
    "תקוע" to (31.626 to 35.206),
    // שפלה ודרום
    "אופקים" to (31.312 to 34.618), "אילת" to (29.561 to 34.950),
    "אשדוד" to (31.804 to 34.655), "אשקלון" to (31.666 to 34.574),
    "באר יעקב" to (31.944 to 34.839), "באר שבע" to (31.252 to 34.792),
    "בטחה" to (31.378 to 34.573), "בית הגדי" to (31.547 to 34.566),
    "דימונה" to (31.069 to 35.034), "יבנה" to (31.876 to 34.747),
    "ירוחם" to (30.986 to 34.930), "מיתר" to (31.352 to 34.904),
    "מעגלים" to (31.244 to 34.588), "מצפה רמון" to (30.610 to 34.803),
    "מרכז שפירא" to (31.740 to 34.668), "נתיבות" to (31.422 to 34.592),
    "ערד" to (31.259 to 35.213), "קרית גת" to (31.608 to 34.768),
    "קרית מלאכי" to (31.731 to 34.743), "קרית עקרון" to (31.864 to 34.820),
    "שדרות" to (31.523 to 34.598), "תפרח" to (31.419 to 34.578),
    // שומרון
    "איתמר" to (32.139 to 35.312), "אלון מורה" to (32.187 to 35.328),
    "אלקנה" to (32.105 to 34.968), "אריאל" to (32.106 to 35.166),
    "הר ברכה" to (32.155 to 35.296), "יצהר" to (32.143 to 35.270),
    "יקיר" to (32.110 to 35.091), "קדומים" to (32.143 to 35.048),
    "קרני שומרון" to (32.176 to 35.030)
)

internal fun distKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 6371 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

internal val ACCESSIBLE_MIKVEHS = listOf(
    // ── גוש דן ──────────────────────────────────────────────────────────────
    AccessibleMikveh("אור יהודה", "ארבל 3", "03-5330001", "גוש דן", "https://waze.com/ul?q=ארבל 3 אור יהודה"),
    AccessibleMikveh("אור יהודה", "יוסף חיים 8", "03-5330002", "גוש דן", "https://waze.com/ul?q=יוסף חיים 8 אור יהודה"),
    AccessibleMikveh("אור יהודה", "יוסף חיים 12", "03-5334025", "גוש דן", "https://waze.com/ul?q=יוסף חיים 12 אור יהודה"),
    AccessibleMikveh("אור יהודה", "עולי גרדום 2", "03-5330718", "גוש דן", "https://waze.com/ul?q=עולי גרדום 2 אור יהודה"),
    AccessibleMikveh("אור יהודה", "אפרים גואטה פינת השומר 5", "03-5331347", "גוש דן", "https://waze.com/ul?q=אפרים גואטה פינת השומר 5 אור יהודה"),
    AccessibleMikveh("אור יהודה", "לנדאו 32", "", "גוש דן", "https://waze.com/ul?q=לנדאו 32 אור יהודה"),
    AccessibleMikveh("אזור", "משה שרת 50", "03-5596001", "גוש דן", "https://waze.com/ul?q=משה שרת 50 אזור"),
    AccessibleMikveh("אחיעזר", "היסמין 1", "08-9295042", "גוש דן", "https://waze.com/ul?q=היסמין 1 אחיעזר"),
    AccessibleMikveh("אלעד", "רבי עקיבא 1", "03-9000001", "גוש דן", "https://waze.com/ul?q=רבי עקיבא 1 אלעד"),
    AccessibleMikveh("אלעד", "הרב שך 12", "03-9000002", "גוש דן", "https://waze.com/ul?q=הרב שך 12 אלעד"),
    AccessibleMikveh("אלעד", "נחל שורק 4", "03-9000003", "גוש דן", "https://waze.com/ul?q=נחל שורק 4 אלעד"),
    AccessibleMikveh("בית דגן", "הבנים 17", "03-9603235", "גוש דן", "https://waze.com/ul?q=הבנים 17 בית דגן"),
    AccessibleMikveh("בני ברק", "אבני נזר 13 רמת אלחנן", "03-5700001", "גוש דן", "https://waze.com/ul?q=אבני נזר 13 רמת אלחנן בני ברק"),
    AccessibleMikveh("בני ברק", "הרב נויפלד 15 קרית הרצוג", "03-5700002", "גוש דן", "https://waze.com/ul?q=הרב נויפלד 15 קרית הרצוג בני ברק"),
    AccessibleMikveh("בני ברק", "בארי 7", "03-5700003", "גוש דן", "https://waze.com/ul?q=בארי 7 בני ברק"),
    AccessibleMikveh("בני ברק", "אחיה השילוני 4 מי נחת", "03-5700004", "גוש דן", "https://waze.com/ul?q=אחיה השילוני 4 מי נחת בני ברק"),
    AccessibleMikveh("בני ברק", "הרב דנגור 12 פרדס כץ", "03-5700005", "גוש דן", "https://waze.com/ul?q=הרב דנגור 12 פרדס כץ בני ברק"),
    AccessibleMikveh("בני ברק", "חפץ חיים 7 זכרון מאיר", "03-5700006", "גוש דן", "https://waze.com/ul?q=חפץ חיים 7 זכרון מאיר בני ברק"),
    AccessibleMikveh("בני ברק", "רבינא 16 ויזניץ", "03-5700007", "גוש דן", "https://waze.com/ul?q=רבינא 16 ויזניץ בני ברק"),
    AccessibleMikveh("בני ברק", "מחזיקי הדת 1 הודיות", "03-5700008", "גוש דן", "https://waze.com/ul?q=מחזיקי הדת 1 הודיות בני ברק"),
    AccessibleMikveh("בני ברק", "קליש 14 מרכז", "03-5700009", "גוש דן", "https://waze.com/ul?q=קליש 14 מרכז בני ברק"),
    AccessibleMikveh("בני ברק", "זבולון עמר 13", "03-5794661", "גוש דן", "https://waze.com/ul?q=זבולון עמר 13 בני ברק"),
    AccessibleMikveh("בני ברק", "נחל סורק 28", "03-5744869", "גוש דן", "https://waze.com/ul?q=נחל סורק 28 בני ברק"),
    AccessibleMikveh("בני עייש", "סמטת התמר", "08-8594386", "גוש דן", "https://waze.com/ul?q=סמטת התמר בני עייש"),
    AccessibleMikveh("בת ים", "ליבורנו 36", "03-5060606", "גוש דן", "https://waze.com/ul?q=ליבורנו 36 בת ים"),
    AccessibleMikveh("בת ים", "הלפר 38", "03-5060607", "גוש דן", "https://waze.com/ul?q=הלפר 38 בת ים"),
    AccessibleMikveh("בת ים", "הכרמל 6", "03-5060608", "גוש דן", "https://waze.com/ul?q=הכרמל 6 בת ים"),
    AccessibleMikveh("בת ים", "סמטת הורדים 6", "03-5076933", "גוש דן", "https://waze.com/ul?q=סמטת הורדים 6 בת ים"),
    AccessibleMikveh("בת ים", "החלוצים 9", "", "גוש דן", "https://waze.com/ul?q=החלוצים 9 בת ים"),
    AccessibleMikveh("גבעת שמואל", "הרא\"ה 15", "03-5310001", "גוש דן", "https://waze.com/ul?q=הרא\"ה 15 גבעת שמואל"),
    AccessibleMikveh("גבעת שמואל", "נימרובר 2", "03-7262683", "גוש דן", "https://waze.com/ul?q=נימרובר 2 גבעת שמואל"),
    AccessibleMikveh("גבעת שמואל", "עגנון 6", "03-5320053", "גוש דן", "https://waze.com/ul?q=עגנון 6 גבעת שמואל"),
    AccessibleMikveh("גבעתיים", "ישראל טייבר 55", "03-7300001", "גוש דן", "https://waze.com/ul?q=ישראל טייבר 55 גבעתיים"),
    AccessibleMikveh("גבעתיים", "מנרה 2", "", "גוש דן", "https://waze.com/ul?q=מנרה 2 גבעתיים"),
    AccessibleMikveh("גדרה", "הבנים 12", "08-8590001", "גוש דן", "https://waze.com/ul?q=הבנים 12 גדרה"),
    AccessibleMikveh("גדרה", "ליליבלום 15", "08-8692754", "גוש דן", "https://waze.com/ul?q=ליליבלום 15 גדרה"),
    AccessibleMikveh("גדרה", "יסמין 20", "08-8691654", "גוש דן", "https://waze.com/ul?q=יסמין 20 גדרה"),
    AccessibleMikveh("גני תקווה", "סמ' הר נבו", "03-5349840", "גוש דן", "https://waze.com/ul?q=סמ' הר נבו גני תקווה"),
    AccessibleMikveh("גני תקווה", "סמ' עין גנים 7", "03-5351687", "גוש דן", "https://waze.com/ul?q=סמ' עין גנים 7 גני תקווה"),
    AccessibleMikveh("חולון", "העלייה השנייה 37", "03-5040404", "גוש דן", "https://waze.com/ul?q=העלייה השנייה 37 חולון"),
    AccessibleMikveh("חולון", "צבי ש\"ץ 15", "03-5040405", "גוש דן", "https://waze.com/ul?q=צבי ש\"ץ 15 חולון"),
    AccessibleMikveh("חולון", "השילוח 23 קרית שרת", "03-5040406", "גוש דן", "https://waze.com/ul?q=השילוח 23 קרית שרת חולון"),
    AccessibleMikveh("חולון", "גבעול 1 תל גיבורים", "03-5040407", "גוש דן", "https://waze.com/ul?q=גבעול 1 תל גיבורים חולון"),
    AccessibleMikveh("חולון", "משעול הפז 15", "03-5518523", "גוש דן", "https://waze.com/ul?q=משעול הפז 15 חולון"),
    AccessibleMikveh("חולון", "יהושפט 6", "03-5509978", "גוש דן", "https://waze.com/ul?q=יהושפט 6 חולון"),
    AccessibleMikveh("חולון", "הרב קוק 9", "03-5058268", "גוש דן", "https://waze.com/ul?q=הרב קוק 9 חולון"),
    AccessibleMikveh("חולון", "שיבת ציון 1", "03-6518899", "גוש דן", "https://waze.com/ul?q=שיבת ציון 1 חולון"),
    AccessibleMikveh("יהוד", "הרצל 50", "03-5320001", "גוש דן", "https://waze.com/ul?q=הרצל 50 יהוד"),
    AccessibleMikveh("יהוד", "צבי ישי 20", "03-6526269", "גוש דן", "https://waze.com/ul?q=צבי ישי 20 יהוד"),
    AccessibleMikveh("יהוד מונוסון", "הספיר 4", "03-5337488", "גוש דן", "https://waze.com/ul?q=הספיר 4 יהוד מונוסון"),
    AccessibleMikveh("כפר חב\"ד", "רבי נחמן מברסלב 1", "03-9606001", "גוש דן", "https://waze.com/ul?q=רבי נחמן מברסלב 1 כפר חב\"ד"),
    AccessibleMikveh("כפר חב\"ד", "מרכז הכפר", "03-9606929", "גוש דן", "https://waze.com/ul?q=מרכז הכפר כפר חב\"ד"),
    AccessibleMikveh("לוד", "צאלון 10", "08-9200001", "גוש דן", "https://waze.com/ul?q=צאלון 10 לוד"),
    AccessibleMikveh("לוד", "רמת אשכול", "08-9200002", "גוש דן", "https://waze.com/ul?q=רמת אשכול לוד"),
    AccessibleMikveh("לוד", "הניצנים 3", "08-9214726", "גוש דן", "https://waze.com/ul?q=הניצנים 3 לוד"),
    AccessibleMikveh("לוד", "המצביאים 7", "08-9203002", "גוש דן", "https://waze.com/ul?q=המצביאים 7 לוד"),
    AccessibleMikveh("לוד", "אלפעל 4", "08-9226780", "גוש דן", "https://waze.com/ul?q=אלפעל 4 לוד"),
    AccessibleMikveh("לוד", "צמח צדק 1", "08-9243754", "גוש דן", "https://waze.com/ul?q=צמח צדק 1 לוד"),
    AccessibleMikveh("לוד", "חללי מינכן 4", "08-9230265", "גוש דן", "https://waze.com/ul?q=חללי מינכן 4 לוד"),
    AccessibleMikveh("לוד", "בן שמן 1", "08-9298728", "גוש דן", "https://waze.com/ul?q=בן שמן 1 לוד"),
    AccessibleMikveh("מזכרת בתיה", "מידון 6", "08-9349965", "גוש דן", "https://waze.com/ul?q=מידון 6 מזכרת בתיה"),
    AccessibleMikveh("נס ציונה", "עצמאות 29", "08-9400003", "גוש דן", "https://waze.com/ul?q=עצמאות 29 נס ציונה"),
    AccessibleMikveh("נס ציונה", "ישראל שמיד", "08-9405477", "גוש דן", "https://waze.com/ul?q=ישראל שמיד נס ציונה"),
    AccessibleMikveh("סביון", "14 רמת פולג", "09-8782858", "גוש דן", "https://waze.com/ul?q=14 רמת פולג סביון"),
    AccessibleMikveh("סביון", "הדרום 12", "", "גוש דן", "https://waze.com/ul?q=הדרום 12 סביון"),
    AccessibleMikveh("פתח תקווה", "שבט גד 4 עמישב", "03-9100001", "גוש דן", "https://waze.com/ul?q=שבט גד 4 עמישב פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "דורני שלום 4 נווה דקלים", "03-9100002", "גוש דן", "https://waze.com/ul?q=דורני שלום 4 נווה דקלים פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "דרך מנחם בגין 30", "03-9100003", "גוש דן", "https://waze.com/ul?q=דרך מנחם בגין 30 פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "העצמאות 6", "03-9336414", "גוש דן", "https://waze.com/ul?q=העצמאות 6 פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "חיים עוזר 8", "03-9344973", "גוש דן", "https://waze.com/ul?q=חיים עוזר 8 פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "השופטים 18", "03-9324523", "גוש דן", "https://waze.com/ul?q=השופטים 18 פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "רמז 54", "03-9305437", "גוש דן", "https://waze.com/ul?q=רמז 54 פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "שמואל הנגיד 1", "03-9348104", "גוש דן", "https://waze.com/ul?q=שמואל הנגיד 1 פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "מוסטבוי 11", "03-9226792", "גוש דן", "https://waze.com/ul?q=מוסטבוי 11 פתח תקווה"),
    AccessibleMikveh("פתח תקווה", "אסירי ציון 13א", "03-9348104", "גוש דן", "https://waze.com/ul?q=אסירי ציון 13א פתח תקווה"),
    AccessibleMikveh("צור יגאל", "לשם בין מתחם גני הילדים", "", "גוש דן", "https://waze.com/ul?q=לשם בין מתחם גני הילדים צור יגאל"),
    AccessibleMikveh("קרית אונו", "שד' קק\"ל 19", "03-7431091", "גוש דן", "https://waze.com/ul?q=שד' קק\"ל 19 קרית אונו"),
    AccessibleMikveh("קרית אונו", "הגפן 12", "03-5350973", "גוש דן", "https://waze.com/ul?q=הגפן 12 קרית אונו"),
    AccessibleMikveh("קרית אונו", "ש\"י עגנון 1", "03-5355028", "גוש דן", "https://waze.com/ul?q=ש\"י עגנון 1 קרית אונו"),
    AccessibleMikveh("ראשון לציון", "בן אהרון רבי עקיבא", "03-9600001", "גוש דן", "https://waze.com/ul?q=בן אהרון רבי עקיבא ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "קפאח 2", "03-6047934", "גוש דן", "https://waze.com/ul?q=קפאח 2 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "כורש 20", "03-9413812", "גוש דן", "https://waze.com/ul?q=כורש 20 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "התזמורת 51", "03-9525925", "גוש דן", "https://waze.com/ul?q=התזמורת 51 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "החיד\"א 5", "03-9644156", "גוש דן", "https://waze.com/ul?q=החיד\"א 5 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "נחמה 7", "03-9508274", "גוש דן", "https://waze.com/ul?q=נחמה 7 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "יוסף הנשיא 5", "03-9508762", "גוש דן", "https://waze.com/ul?q=יוסף הנשיא 5 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "פופל 20", "03-9588380", "גוש דן", "https://waze.com/ul?q=פופל 20 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "אנילביץ פינת חנה סנש 1", "03-9617792", "גוש דן", "https://waze.com/ul?q=אנילביץ פינת חנה סנש 1 ראשון לציון"),
    AccessibleMikveh("ראשון לציון", "לוז 30", "03-9508275", "גוש דן", "https://waze.com/ul?q=לוז 30 ראשון לציון"),
    AccessibleMikveh("רחובות", "הרצל 145", "08-9360001", "גוש דן", "https://waze.com/ul?q=הרצל 145 רחובות"),
    AccessibleMikveh("רחובות", "אהרונוביץ 10", "08-9360002", "גוש דן", "https://waze.com/ul?q=אהרונוביץ 10 רחובות"),
    AccessibleMikveh("רחובות", "השופטים 2", "08-9453078", "גוש דן", "https://waze.com/ul?q=השופטים 2 רחובות"),
    AccessibleMikveh("רחובות", "בר אילן 12", "08-9452260", "גוש דן", "https://waze.com/ul?q=בר אילן 12 רחובות"),
    AccessibleMikveh("רחובות", "משה פריד 16", "08-9467535", "גוש דן", "https://waze.com/ul?q=משה פריד 16 רחובות"),
    AccessibleMikveh("רחובות", "חשמונאים 3", "08-9493816", "גוש דן", "https://waze.com/ul?q=חשמונאים 3 רחובות"),
    AccessibleMikveh("רחובות", "מדאר 10", "", "גוש דן", "https://waze.com/ul?q=מדאר 10 רחובות"),
    AccessibleMikveh("רחובות", "שבזי 3", "08-9457405", "גוש דן", "https://waze.com/ul?q=שבזי 3 רחובות"),
    AccessibleMikveh("רמלה", "נחלת דן 15", "08-9250001", "גוש דן", "https://waze.com/ul?q=נחלת דן 15 רמלה"),
    AccessibleMikveh("רמלה", "מרדכי שרעבי 1", "08-9152465", "גוש דן", "https://waze.com/ul?q=מרדכי שרעבי 1 רמלה"),
    AccessibleMikveh("רמלה", "מוצקין 8", "08-9255282", "גוש דן", "https://waze.com/ul?q=מוצקין 8 רמלה"),
    AccessibleMikveh("רמלה", "וילנא 10", "08-9255281", "גוש דן", "https://waze.com/ul?q=וילנא 10 רמלה"),
    AccessibleMikveh("רמלה", "עוזי חיטמן 19", "08-6495881", "גוש דן", "https://waze.com/ul?q=עוזי חיטמן 19 רמלה"),
    AccessibleMikveh("רמלה", "גרשון שץ 4", "08-8540158", "גוש דן", "https://waze.com/ul?q=גרשון שץ 4 רמלה"),
    AccessibleMikveh("רמת גן", "עזריאל 24", "03-6700001", "גוש דן", "https://waze.com/ul?q=עזריאל 24 רמת גן"),
    AccessibleMikveh("רמת גן", "עוזיאל 5", "03-6700002", "גוש דן", "https://waze.com/ul?q=עוזיאל 5 רמת גן"),
    AccessibleMikveh("רמת גן", "מבצע עין 7", "03-6700004", "גוש דן", "https://waze.com/ul?q=מבצע עין 7 רמת גן"),
    AccessibleMikveh("רמת גן", "שד' העם הצרפתי 34", "03-6700005", "גוש דן", "https://waze.com/ul?q=שד' העם הצרפתי 34 רמת גן"),
    AccessibleMikveh("רמת גן", "גילדסגיים 12", "03-7363014", "גוש דן", "https://waze.com/ul?q=גילדסגיים 12 רמת גן"),
    AccessibleMikveh("רמת גן", "החוגה 2", "03-6353919", "גוש דן", "https://waze.com/ul?q=החוגה 2 רמת גן"),
    AccessibleMikveh("תל אביב יפו", "פקיעין 12", "03-5464899", "גוש דן", "https://waze.com/ul?q=פקיעין 12 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "צירלסון 10", "03-5468893", "גוש דן", "https://waze.com/ul?q=צירלסון 10 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "בן יהודה 86", "03-5228188", "גוש דן", "https://waze.com/ul?q=בן יהודה 86 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "הט\"ז 8 רמת החייל", "03-6470623", "גוש דן", "https://waze.com/ul?q=הט\"ז 8 רמת החייל תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "ראול ולנברג 37", "03-6472626", "גוש דן", "https://waze.com/ul?q=ראול ולנברג 37 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "מרק יעקב 3", "03-6470960", "גוש דן", "https://waze.com/ul?q=מרק יעקב 3 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "בר כוכבא 54", "03-5288055", "גוש דן", "https://waze.com/ul?q=בר כוכבא 54 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "פינס 24 נווה צדק", "03-5108677", "גוש דן", "https://waze.com/ul?q=פינס 24 נווה צדק תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "החזיון 3 קרית שלום", "03-6817093", "גוש דן", "https://waze.com/ul?q=החזיון 3 קרית שלום תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "חכמי ישראל 65", "03-6880892", "גוש דן", "https://waze.com/ul?q=חכמי ישראל 65 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "בושם 59 כפיר", "03-6319535", "גוש דן", "https://waze.com/ul?q=בושם 59 כפיר תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "שד' ההשכלה 10", "03-5610089", "גוש דן", "https://waze.com/ul?q=שד' ההשכלה 10 תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "שד' החייל 4 יד אליהו", "03-5371256", "גוש דן", "https://waze.com/ul?q=שד' החייל 4 יד אליהו תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "שד' התחיה 4 יפו", "03-6827142", "גוש דן", "https://waze.com/ul?q=שד' התחיה 4 יפו תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "כסלו 23 שכ' עזרא", "03-6876211", "גוש דן", "https://waze.com/ul?q=כסלו 23 שכ' עזרא תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "סג'רה 9 יפו", "03-6965166", "גוש דן", "https://waze.com/ul?q=סג'רה 9 יפו תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "טאגור 32 רמת אביב", "03-6439725", "גוש דן", "https://waze.com/ul?q=טאגור 32 רמת אביב תל אביב יפו"),
    AccessibleMikveh("תל אביב יפו", "תל כביר אוריאל יעקובוב 4", "03-5187164", "גוש דן", "https://waze.com/ul?q=תל כביר אוריאל יעקובוב 4 תל אביב יפו"),
    AccessibleMikveh("תל מונד", "התמר 18", "09-7964001", "גוש דן", "https://waze.com/ul?q=התמר 18 תל מונד"),

    // ── שרון ──────────────────────────────────────────────────────────────
    AccessibleMikveh("אבן יהודה", "הבנים 43", "09-8910288", "שרון", "https://waze.com/ul?q=הבנים 43 אבן יהודה"),
    AccessibleMikveh("אור עקיבא", "השקד 10", "04-6260001", "שרון", "https://waze.com/ul?q=השקד 10 אור עקיבא"),
    AccessibleMikveh("אור עקיבא", "התאנה 1", "", "שרון", "https://waze.com/ul?q=התאנה 1 אור עקיבא"),
    AccessibleMikveh("אור עקיבא", "בן יהודה", "", "שרון", "https://waze.com/ul?q=בן יהודה אור עקיבא"),
    AccessibleMikveh("אור עקיבא", "הכרמל 1", "", "שרון", "https://waze.com/ul?q=הכרמל 1 אור עקיבא"),
    AccessibleMikveh("אורנית", "התאנה 1א", "", "שרון", "https://waze.com/ul?q=התאנה 1א אורנית"),
    AccessibleMikveh("אורנית", "הארבל 28", "03-9369040", "שרון", "https://waze.com/ul?q=הארבל 28 אורנית"),
    AccessibleMikveh("אליכין", "ציפורם 8", "", "שרון", "https://waze.com/ul?q=ציפורם 8 אליכין"),
    AccessibleMikveh("אלפי מנשה", "כרכום 2", "09-7925843", "שרון", "https://waze.com/ul?q=כרכום 2 אלפי מנשה"),
    AccessibleMikveh("בנימינה", "שד' הברון 15", "04-6380001", "שרון", "https://waze.com/ul?q=שד' הברון 15 בנימינה"),
    AccessibleMikveh("בנימינה", "הנדיב", "04-3800548", "שרון", "https://waze.com/ul?q=הנדיב בנימינה"),
    AccessibleMikveh("גבעת עדה", "הדקל", "", "שרון", "https://waze.com/ul?q=הדקל גבעת עדה"),
    AccessibleMikveh("הוד השרון", "הבנים 43", "09-7400010", "שרון", "https://waze.com/ul?q=הבנים 43 הוד השרון"),
    AccessibleMikveh("הוד השרון", "סוקולוב 30", "09-7416944", "שרון", "https://waze.com/ul?q=סוקולוב 30 הוד השרון"),
    AccessibleMikveh("הוד השרון", "הנגב 4", "09-8358961", "שרון", "https://waze.com/ul?q=הנגב 4 הוד השרון"),
    AccessibleMikveh("הוד השרון", "יהושע בן גמלא", "09-7604286", "שרון", "https://waze.com/ul?q=יהושע בן גמלא הוד השרון"),
    AccessibleMikveh("הוד השרון", "הבבלי 2", "09-7446890", "שרון", "https://waze.com/ul?q=הבבלי 2 הוד השרון"),
    AccessibleMikveh("הוד השרון", "הדבש 13", "09-7437824", "שרון", "https://waze.com/ul?q=הדבש 13 הוד השרון"),
    AccessibleMikveh("הוד השרון", "שמעון הצדיק 18", "09-7408945", "שרון", "https://waze.com/ul?q=שמעון הצדיק 18 הוד השרון"),
    AccessibleMikveh("הרצליה", "שבזי 3", "09-9500001", "שרון", "https://waze.com/ul?q=שבזי 3 הרצליה"),
    AccessibleMikveh("הרצליה", "הנשיא 12", "09-9500002", "שרון", "https://waze.com/ul?q=הנשיא 12 הרצליה"),
    AccessibleMikveh("הרצליה", "סירקין 4", "09-9507136", "שרון", "https://waze.com/ul?q=סירקין 4 הרצליה"),
    AccessibleMikveh("הרצליה", "זיסו 12", "09-9567917", "שרון", "https://waze.com/ul?q=זיסו 12 הרצליה"),
    AccessibleMikveh("הרצליה", "היתד 20", "09-9501485", "שרון", "https://waze.com/ul?q=היתד 20 הרצליה"),
    AccessibleMikveh("הרצליה", "פנקס 54", "09-9516136", "שרון", "https://waze.com/ul?q=פנקס 54 הרצליה"),
    AccessibleMikveh("הרצליה", "אהרן קציר 2", "", "שרון", "https://waze.com/ul?q=אהרן קציר 2 הרצליה"),
    AccessibleMikveh("הרצליה", "לוריא", "04-8570519", "שרון", "https://waze.com/ul?q=לוריא הרצליה"),
    AccessibleMikveh("זכרון יעקב", "הנדיב 22", "04-6390001", "שרון", "https://waze.com/ul?q=הנדיב 22 זכרון יעקב"),
    AccessibleMikveh("זכרון יעקב", "המיסדים 87", "04-6390308", "שרון", "https://waze.com/ul?q=המיסדים 87 זכרון יעקב"),
    AccessibleMikveh("זכרון יעקב", "סמטת הבאר", "", "שרון", "https://waze.com/ul?q=סמטת הבאר זכרון יעקב"),
    AccessibleMikveh("חדרה", "לבוצקין 9", "04-6320001", "שרון", "https://waze.com/ul?q=לבוצקין 9 חדרה"),
    AccessibleMikveh("חדרה", "קיבוץ גלויות 16", "04-6201358", "שרון", "https://waze.com/ul?q=קיבוץ גלויות 16 חדרה"),
    AccessibleMikveh("חדרה", "האמוראים 9", "04-6341874", "שרון", "https://waze.com/ul?q=האמוראים 9 חדרה"),
    AccessibleMikveh("חדרה", "עליית הנוער 1", "", "שרון", "https://waze.com/ul?q=עליית הנוער 1 חדרה"),
    AccessibleMikveh("חדרה", "משמר הגבול 1", "04-6158731", "שרון", "https://waze.com/ul?q=משמר הגבול 1 חדרה"),
    AccessibleMikveh("כפר יונה", "יחזקאל ליד הדואר", "09-8943818", "שרון", "https://waze.com/ul?q=יחזקאל ליד הדואר כפר יונה"),
    AccessibleMikveh("כפר סבא", "ויצמן 63", "09-7400001", "שרון", "https://waze.com/ul?q=ויצמן 63 כפר סבא"),
    AccessibleMikveh("כפר סבא", "התחיה 2", "09-7400002", "שרון", "https://waze.com/ul?q=התחיה 2 כפר סבא"),
    AccessibleMikveh("כפר סבא", "תל חי 67", "09-7660802", "שרון", "https://waze.com/ul?q=תל חי 67 כפר סבא"),
    AccessibleMikveh("כפר סבא", "בן גוריון 23", "09-7670802", "שרון", "https://waze.com/ul?q=בן גוריון 23 כפר סבא"),
    AccessibleMikveh("כפר סבא", "האחדות 16 עליה", "09-8652819", "שרון", "https://waze.com/ul?q=האחדות 16 עליה כפר סבא"),
    AccessibleMikveh("כפר סבא", "מרדכי 3 קפלן", "09-7656667", "שרון", "https://waze.com/ul?q=מרדכי 3 קפלן כפר סבא"),
    AccessibleMikveh("נתניה", "שד' בנימין 40", "09-8600001", "שרון", "https://waze.com/ul?q=שד' בנימין 40 נתניה"),
    AccessibleMikveh("נתניה", "רזיאל 26", "09-8600002", "שרון", "https://waze.com/ul?q=רזיאל 26 נתניה"),
    AccessibleMikveh("נתניה", "קרית נורדאו", "09-8600003", "שרון", "https://waze.com/ul?q=קרית נורדאו נתניה"),
    AccessibleMikveh("נתניה", "רוטנברג 34 קרית נורדאו", "09-8655341", "שרון", "https://waze.com/ul?q=רוטנברג 34 קרית נורדאו נתניה"),
    AccessibleMikveh("נתניה", "הרותם 3 גבעת האירוסים", "09-7720069", "שרון", "https://waze.com/ul?q=הרותם 3 גבעת האירוסים נתניה"),
    AccessibleMikveh("נתניה", "אלנקוה 8 אזורים", "09-8357282", "שרון", "https://waze.com/ul?q=אלנקוה 8 אזורים נתניה"),
    AccessibleMikveh("נתניה", "שמואל 10 רמת ידין", "09-8354814", "שרון", "https://waze.com/ul?q=שמואל 10 רמת ידין נתניה"),
    AccessibleMikveh("נתניה", "אלחריזי 23 רמת חן", "09-8652560", "שרון", "https://waze.com/ul?q=אלחריזי 23 רמת חן נתניה"),
    AccessibleMikveh("נתניה", "גליקסון 17 סלע", "09-8329238", "שרון", "https://waze.com/ul?q=גליקסון 17 סלע נתניה"),
    AccessibleMikveh("נתניה", "שפיגלמן 3 משה\"ב", "09-8871071", "שרון", "https://waze.com/ul?q=שפיגלמן 3 משה\"ב נתניה"),
    AccessibleMikveh("נתניה", "זוארץ 12 ותיקים", "09-8323492", "שרון", "https://waze.com/ul?q=זוארץ 12 ותיקים נתניה"),
    AccessibleMikveh("נתניה", "משה שפירא 40 הפועל מזרחי", "09-8622561", "שרון", "https://waze.com/ul?q=משה שפירא 40 הפועל מזרחי נתניה"),
    AccessibleMikveh("נתניה", "יהודה הלוי 25 מרכזית", "09-8611839", "שרון", "https://waze.com/ul?q=יהודה הלוי 25 מרכזית נתניה"),
    AccessibleMikveh("עמנואל", "חתם סופר 1", "09-7921214", "שרון", "https://waze.com/ul?q=חתם סופר 1 עמנואל"),
    AccessibleMikveh("עמנואל", "פנחס לוין 15", "09-7921073", "שרון", "https://waze.com/ul?q=פנחס לוין 15 עמנואל"),
    AccessibleMikveh("פרדס חנה כרכור", "חשמונאים 43", "04-6271826", "שרון", "https://waze.com/ul?q=חשמונאים 43 פרדס חנה כרכור"),
    AccessibleMikveh("פרדס חנה כרכור", "דרך הבנים 103", "04-6373440", "שרון", "https://waze.com/ul?q=דרך הבנים 103 פרדס חנה כרכור"),
    AccessibleMikveh("פרדס חנה-כרכור", "הרצל 85", "04-6370010", "שרון", "https://waze.com/ul?q=הרצל 85 פרדס חנה-כרכור"),
    AccessibleMikveh("פרדסיה", "שבזי 13", "09-8946181", "שרון", "https://waze.com/ul?q=שבזי 13 פרדסיה"),
    AccessibleMikveh("קדימה צורן", "האילנות 20", "09-8949493", "שרון", "https://waze.com/ul?q=האילנות 20 קדימה צורן"),
    AccessibleMikveh("קדימה צורן", "פלמ\"ח", "09-8990342", "שרון", "https://waze.com/ul?q=פלמ\"ח קדימה צורן"),
    AccessibleMikveh("קיסריה", "הר הכרמל 5", "04-6260010", "שרון", "https://waze.com/ul?q=הר הכרמל 5 קיסריה"),
    AccessibleMikveh("ראש העין", "דליה רביקוביץ", "03-9380001", "שרון", "https://waze.com/ul?q=דליה רביקוביץ ראש העין"),
    AccessibleMikveh("ראש העין", "123", "03-9380277", "שרון", "https://waze.com/ul?q=123 ראש העין"),
    AccessibleMikveh("ראש העין", "הטייס 58", "03-9382448", "שרון", "https://waze.com/ul?q=הטייס 58 ראש העין"),
    AccessibleMikveh("ראש העין", "מבצע דני 25", "03-5248522", "שרון", "https://waze.com/ul?q=מבצע דני 25 ראש העין"),
    AccessibleMikveh("ראש העין", "הרש\"ש 32", "03-9026634", "שרון", "https://waze.com/ul?q=הרש\"ש 32 ראש העין"),
    AccessibleMikveh("רמת השרון", "אוסישקין 25", "03-5400001", "שרון", "https://waze.com/ul?q=אוסישקין 25 רמת השרון"),
    AccessibleMikveh("רמת השרון", "הנוטע 3", "03-5493530", "שרון", "https://waze.com/ul?q=הנוטע 3 רמת השרון"),
    AccessibleMikveh("רמת השרון", "השחל 34", "03-5498018", "שרון", "https://waze.com/ul?q=השחל 34 רמת השרון"),
    AccessibleMikveh("רעננה", "י.ל. פרץ 53 קרית שרת", "09-7711046", "שרון", "https://waze.com/ul?q=י.ל. פרץ 53 קרית שרת רעננה"),
    AccessibleMikveh("רעננה", "רבוצקי", "09-7711046", "שרון", "https://waze.com/ul?q=רבוצקי רעננה"),
    AccessibleMikveh("רעננה", "הרצל", "09-7711046", "שרון", "https://waze.com/ul?q=הרצל רעננה"),

    // ── חיפה ──────────────────────────────────────────────────────────────
    AccessibleMikveh("חיפה", "תל אביב 35 קרית אליהו", "04-8400001", "חיפה", "https://waze.com/ul?q=תל אביב 35 קרית אליהו חיפה"),
    AccessibleMikveh("חיפה", "המלך שלמה 15 נווה דוד", "04-8400003", "חיפה", "https://waze.com/ul?q=המלך שלמה 15 נווה דוד חיפה"),
    AccessibleMikveh("חיפה", "דרך הים 10 כרמל", "04-8400004", "חיפה", "https://waze.com/ul?q=דרך הים 10 כרמל חיפה"),
    AccessibleMikveh("חיפה", "ההגנה 7 עין הים", "04-8400005", "חיפה", "https://waze.com/ul?q=ההגנה 7 עין הים חיפה"),
    AccessibleMikveh("חיפה", "שאול 22 נווה שאנן", "04-8400006", "חיפה", "https://waze.com/ul?q=שאול 22 נווה שאנן חיפה"),
    AccessibleMikveh("חיפה", "הראשונים 55 קרית חיים", "04-8400007", "חיפה", "https://waze.com/ul?q=הראשונים 55 קרית חיים חיפה"),
    AccessibleMikveh("חיפה", "בצלאל 2 הדר", "04-8400008", "חיפה", "https://waze.com/ul?q=בצלאל 2 הדר חיפה"),
    AccessibleMikveh("חיפה", "דרך יד לבנים 212 נווה שאנן", "04-8400009", "חיפה", "https://waze.com/ul?q=דרך יד לבנים 212 נווה שאנן חיפה"),
    AccessibleMikveh("חיפה", "ההגה 7 פינת הבריכה", "04-8539212", "חיפה", "https://waze.com/ul?q=ההגה 7 פינת הבריכה חיפה"),
    AccessibleMikveh("חיפה", "תורה ועבודה", "04-8710198", "חיפה", "https://waze.com/ul?q=תורה ועבודה חיפה"),

    // ── צפון ──────────────────────────────────────────────────────────────
    AccessibleMikveh("בית שאן", "יצחק רבין 1", "04-6480001", "צפון", "https://waze.com/ul?q=יצחק רבין 1 בית שאן"),
    AccessibleMikveh("בית שאן", "יעקב מכלוף 33", "04-6587975", "צפון", "https://waze.com/ul?q=יעקב מכלוף 33 בית שאן"),
    AccessibleMikveh("בית שאן", "אחד העם 7", "04-6587480", "צפון", "https://waze.com/ul?q=אחד העם 7 בית שאן"),
    AccessibleMikveh("חיספין", "מרכז היישוב", "04-6763074", "צפון", "https://waze.com/ul?q=מרכז היישוב חיספין"),
    AccessibleMikveh("חצור הגלילית", "הקריה החסידית", "", "צפון", "https://waze.com/ul?q=הקריה החסידית חצור הגלילית"),
    AccessibleMikveh("חצור הגלילית", "החסידה", "04-6930304", "צפון", "https://waze.com/ul?q=החסידה חצור הגלילית"),
    AccessibleMikveh("טבריה", "דרך מר\"ן 21 נוף פוריה", "04-6720001", "צפון", "https://waze.com/ul?q=דרך מר\"ן 21 נוף פוריה טבריה"),
    AccessibleMikveh("טבריה", "השומר האמהות", "04-6720002", "צפון", "https://waze.com/ul?q=השומר האמהות טבריה"),
    AccessibleMikveh("טבריה", "שז\"ר 16 צאנז", "04-6720003", "צפון", "https://waze.com/ul?q=שז\"ר 16 צאנז טבריה"),
    AccessibleMikveh("טבריה", "חזון איש 1 שיכון ד", "04-6731090", "צפון", "https://waze.com/ul?q=חזון איש 1 שיכון ד טבריה"),
    AccessibleMikveh("טבריה", "", "077-4450608", "צפון", "https://waze.com/ul?q=טבריה"),
    AccessibleMikveh("טבריה", "שטרית 58 פינת רמח\"ל", "04-6723114", "צפון", "https://waze.com/ul?q=שטרית 58 פינת רמח\"ל טבריה"),
    AccessibleMikveh("טבריה", "הרב ורנר 5 קרית שמואל", "04-6723115", "צפון", "https://waze.com/ul?q=הרב ורנר 5 קרית שמואל טבריה"),
    AccessibleMikveh("טבריה", "השילוח 93 שיכון א", "04-6723112", "צפון", "https://waze.com/ul?q=השילוח 93 שיכון א טבריה"),
    AccessibleMikveh("טירת הכרמל", "שרביט רבקה", "04-8576535", "צפון", "https://waze.com/ul?q=שרביט רבקה טירת הכרמל"),
    AccessibleMikveh("יבניאל", "נפתלי", "04-6708947", "צפון", "https://waze.com/ul?q=נפתלי יבניאל"),
    AccessibleMikveh("יוקנעם עילית", "הפרחים 15", "04-9590001", "צפון", "https://waze.com/ul?q=הפרחים 15 יוקנעם עילית"),
    AccessibleMikveh("יקנעם עילית", "הירדן 98", "04-9937852", "צפון", "https://waze.com/ul?q=הירדן 98 יקנעם עילית"),
    AccessibleMikveh("יקנעם עילית", "השקד 10", "04-6207464", "צפון", "https://waze.com/ul?q=השקד 10 יקנעם עילית"),
    AccessibleMikveh("כפר תבור", "מרכז הכפר", "", "צפון", "https://waze.com/ul?q=מרכז הכפר כפר תבור"),
    AccessibleMikveh("כרמיאל", "נשיאי ישראל 12", "04-9880001", "צפון", "https://waze.com/ul?q=נשיאי ישראל 12 כרמיאל"),
    AccessibleMikveh("כרמיאל", "שד' נשיאי ישראל 100", "04-9880002", "צפון", "https://waze.com/ul?q=שד' נשיאי ישראל 100 כרמיאל"),
    AccessibleMikveh("כרמיאל", "כרמים 13", "", "צפון", "https://waze.com/ul?q=כרמים 13 כרמיאל"),
    AccessibleMikveh("כרמיאל", "רמים 1", "", "צפון", "https://waze.com/ul?q=רמים 1 כרמיאל"),
    AccessibleMikveh("כרמיאל", "משעול הדולב 13", "", "צפון", "https://waze.com/ul?q=משעול הדולב 13 כרמיאל"),
    AccessibleMikveh("מגדל", "סמטת סחלב", "", "צפון", "https://waze.com/ul?q=סמטת סחלב מגדל"),
    AccessibleMikveh("מגדל העמק", "נחל צבי 71", "04-6440001", "צפון", "https://waze.com/ul?q=נחל צבי 71 מגדל העמק"),
    AccessibleMikveh("מגדל העמק", "חבצלת 1 נוף העמק", "04-6440002", "צפון", "https://waze.com/ul?q=חבצלת 1 נוף העמק מגדל העמק"),
    AccessibleMikveh("מגדל העמק", "הנשיאים", "04-6543353", "צפון", "https://waze.com/ul?q=הנשיאים מגדל העמק"),
    AccessibleMikveh("מגדל העמק", "האילן 7", "04-6543220", "צפון", "https://waze.com/ul?q=האילן 7 מגדל העמק"),
    AccessibleMikveh("מטולה", "הראשונים 10", "04-6951941", "צפון", "https://waze.com/ul?q=הראשונים 10 מטולה"),
    AccessibleMikveh("מעלות תרשיחא", "הכלנית ליד העירייה", "04-9973567", "צפון", "https://waze.com/ul?q=הכלנית ליד העירייה מעלות תרשיחא"),
    AccessibleMikveh("מעלות תרשיחא", "ההגנה 20", "04-9973845", "צפון", "https://waze.com/ul?q=ההגנה 20 מעלות תרשיחא"),
    AccessibleMikveh("נהריה", "Lotus לוטוס", "04-9920001", "צפון", "https://waze.com/ul?q=Lotus לוטוס נהריה"),
    AccessibleMikveh("נהריה", "גרניום 10", "04-9920002", "צפון", "https://waze.com/ul?q=גרניום 10 נהריה"),
    AccessibleMikveh("נהריה", "ישורון 9", "04-9511398", "צפון", "https://waze.com/ul?q=ישורון 9 נהריה"),
    AccessibleMikveh("נהריה", "החלוץ מרכז מסחרי טרומפלדור", "04-9828535", "צפון", "https://waze.com/ul?q=החלוץ מרכז מסחרי טרומפלדור נהריה"),
    AccessibleMikveh("נהריה", "אשכול 47", "04-9823860", "צפון", "https://waze.com/ul?q=אשכול 47 נהריה"),
    AccessibleMikveh("נוף הגליל", "הרדוף 15", "04-6460001", "צפון", "https://waze.com/ul?q=הרדוף 15 נוף הגליל"),
    AccessibleMikveh("נוף הגליל", "נורדאו 28", "04-6460002", "צפון", "https://waze.com/ul?q=נורדאו 28 נוף הגליל"),
    AccessibleMikveh("נוף הגליל", "הגבעה", "", "צפון", "https://waze.com/ul?q=הגבעה נוף הגליל"),
    AccessibleMikveh("נוף הגליל", "יזרעאל", "04-6460684", "צפון", "https://waze.com/ul?q=יזרעאל נוף הגליל"),
    AccessibleMikveh("נוף הגליל", "ליבנה 8", "", "צפון", "https://waze.com/ul?q=ליבנה 8 נוף הגליל"),
    AccessibleMikveh("נשר", "שביל הזית 5", "04-8200001", "צפון", "https://waze.com/ul?q=שביל הזית 5 נשר"),
    AccessibleMikveh("נשר", "דבורה 1", "", "צפון", "https://waze.com/ul?q=דבורה 1 נשר"),
    AccessibleMikveh("נשר", "הנורית 14", "", "צפון", "https://waze.com/ul?q=הנורית 14 נשר"),
    AccessibleMikveh("נשר", "דרך הטכניון", "", "צפון", "https://waze.com/ul?q=דרך הטכניון נשר"),
    AccessibleMikveh("עכו", "כרם 2", "04-9810001", "צפון", "https://waze.com/ul?q=כרם 2 עכו"),
    AccessibleMikveh("עכו", "36", "04-8750010", "צפון", "https://waze.com/ul?q=36 עכו"),
    AccessibleMikveh("עכו", "הרב לופס 11 שכוני המזרח", "04-9911067", "צפון", "https://waze.com/ul?q=הרב לופס 11 שכוני המזרח עכו"),
    AccessibleMikveh("עכו", "עליית הנוער 2", "04-6577898", "צפון", "https://waze.com/ul?q=עליית הנוער 2 עכו"),
    AccessibleMikveh("עכו", "דרך הארבעה 57", "04-9913493", "צפון", "https://waze.com/ul?q=דרך הארבעה 57 עכו"),
    AccessibleMikveh("עפולה", "יהושע חנקין 1", "04-6520001", "צפון", "https://waze.com/ul?q=יהושע חנקין 1 עפולה"),
    AccessibleMikveh("עפולה", "עילית", "04-6520002", "צפון", "https://waze.com/ul?q=עילית עפולה"),
    AccessibleMikveh("עפולה", "קינמון פינת וולפסון", "04-6597839", "צפון", "https://waze.com/ul?q=קינמון פינת וולפסון עפולה"),
    AccessibleMikveh("עפולה", "כורש 16", "04-6400100", "צפון", "https://waze.com/ul?q=כורש 16 עפולה"),
    AccessibleMikveh("עפולה", "בורוכוב 19", "04-6594330", "צפון", "https://waze.com/ul?q=בורוכוב 19 עפולה"),
    AccessibleMikveh("עפולה", "פעמונית 6", "04-6319792", "צפון", "https://waze.com/ul?q=פעמונית 6 עפולה"),
    AccessibleMikveh("עתלית", "החרוב 1", "04-9840313", "צפון", "https://waze.com/ul?q=החרוב 1 עתלית"),
    AccessibleMikveh("פוריה נווה עובד", "מרכז הישוב", "04-6709095", "צפון", "https://waze.com/ul?q=מרכז הישוב פוריה נווה עובד"),
    AccessibleMikveh("צפת", "רבי יוסף קארו 14", "04-6820001", "צפון", "https://waze.com/ul?q=רבי יוסף קארו 14 צפת"),
    AccessibleMikveh("צפת", "נוף כנרת 3", "04-6820003", "צפון", "https://waze.com/ul?q=נוף כנרת 3 צפת"),
    AccessibleMikveh("צפת", "התבור מול בניין 28", "04-6920395", "צפון", "https://waze.com/ul?q=התבור מול בניין 28 צפת"),
    AccessibleMikveh("צפת", "הנשרים בית 378א", "", "צפון", "https://waze.com/ul?q=הנשרים בית 378א צפת"),
    AccessibleMikveh("צפת", "מקווה ברסלב האר\"י", "077-78778489", "צפון", "https://waze.com/ul?q=מקווה ברסלב האר\"י צפת"),
    AccessibleMikveh("צפת", "הרצל 20", "04-6970160", "צפון", "https://waze.com/ul?q=הרצל 20 צפת"),
    AccessibleMikveh("קצרין", "דליות 3", "04-6960001", "צפון", "https://waze.com/ul?q=דליות 3 קצרין"),
    AccessibleMikveh("קצרין", "כנרות 75", "04-6850049", "צפון", "https://waze.com/ul?q=כנרות 75 קצרין"),
    AccessibleMikveh("קרית אתא", "הרצל 34 אוהל שרה ולאה", "04-8400010", "צפון", "https://waze.com/ul?q=הרצל 34 אוהל שרה ולאה קרית אתא"),
    AccessibleMikveh("קרית אתא", "המייסדים 16", "04-8444749", "צפון", "https://waze.com/ul?q=המייסדים 16 קרית אתא"),
    AccessibleMikveh("קרית אתא", "אברהם טביב 3", "04-8442602", "צפון", "https://waze.com/ul?q=אברהם טביב 3 קרית אתא"),
    AccessibleMikveh("קרית אתא", "גיבור הקריה 20", "04-8452118", "צפון", "https://waze.com/ul?q=גיבור הקריה 20 קרית אתא"),
    AccessibleMikveh("קרית אתא", "הארז 1", "04-8480090", "צפון", "https://waze.com/ul?q=הארז 1 קרית אתא"),
    AccessibleMikveh("קרית אתא", "אלשייך", "", "צפון", "https://waze.com/ul?q=אלשייך קרית אתא"),
    AccessibleMikveh("קרית ביאליק", "קק\"ל 75", "04-6564843", "צפון", "https://waze.com/ul?q=קק\"ל 75 קרית ביאליק"),
    AccessibleMikveh("קרית ביאליק", "ראובן פינת מנשה", "04-8750053", "צפון", "https://waze.com/ul?q=ראובן פינת מנשה קרית ביאליק"),
    AccessibleMikveh("קרית ביאליק", "שי עגנון 13", "04-8741697", "צפון", "https://waze.com/ul?q=שי עגנון 13 קרית ביאליק"),
    AccessibleMikveh("קרית טבעון", "החורש 10", "04-9834149", "צפון", "https://waze.com/ul?q=החורש 10 קרית טבעון"),
    AccessibleMikveh("קרית ים", "אילנות סביוני ים", "04-8750001", "צפון", "https://waze.com/ul?q=אילנות סביוני ים קרית ים"),
    AccessibleMikveh("קרית ים", "לימן 15", "", "צפון", "https://waze.com/ul?q=לימן 15 קרית ים"),
    AccessibleMikveh("קרית מוצקין", "הרצל 1", "04-8720001", "צפון", "https://waze.com/ul?q=הרצל 1 קרית מוצקין"),
    AccessibleMikveh("קרית מוצקין", "גרושקביץ 1", "04-8712525", "צפון", "https://waze.com/ul?q=גרושקביץ 1 קרית מוצקין"),
    AccessibleMikveh("קרית מוצקין", "דקר 70", "", "צפון", "https://waze.com/ul?q=דקר 70 קרית מוצקין"),
    AccessibleMikveh("קרית שמונה", "תל חי 22", "04-6940001", "צפון", "https://waze.com/ul?q=תל חי 22 קרית שמונה"),
    AccessibleMikveh("קרית שמונה", "הורדים 29", "", "צפון", "https://waze.com/ul?q=הורדים 29 קרית שמונה"),
    AccessibleMikveh("קרית שמונה", "יהודה הלוי 2", "04-6959265", "צפון", "https://waze.com/ul?q=יהודה הלוי 2 קרית שמונה"),
    AccessibleMikveh("רכסים", "עוזיאל 3", "04-9846905", "צפון", "https://waze.com/ul?q=עוזיאל 3 רכסים"),
    AccessibleMikveh("רכסים", "הכלניות 46", "04-9847632", "צפון", "https://waze.com/ul?q=הכלניות 46 רכסים"),
    AccessibleMikveh("רמת ישי", "היסמין 6", "04-9832641", "צפון", "https://waze.com/ul?q=היסמין 6 רמת ישי"),
    AccessibleMikveh("שלומי", "הגליל 8", "04-9800001", "צפון", "https://waze.com/ul?q=הגליל 8 שלומי"),
    AccessibleMikveh("שלומי", "עוזיאל 5", "", "צפון", "https://waze.com/ul?q=עוזיאל 5 שלומי"),
    AccessibleMikveh("שלומי", "הדרור", "", "צפון", "https://waze.com/ul?q=הדרור שלומי"),

    // ── ירושלים ──────────────────────────────────────────────────────────────
    AccessibleMikveh("אדם", "18 זופניק", "02-5800018", "ירושלים", "https://waze.com/ul?q=18 זופניק אדם"),
    AccessibleMikveh("אדם", "הרדוף הנחלים", "02-5913906", "ירושלים", "https://waze.com/ul?q=הרדוף הנחלים אדם"),
    AccessibleMikveh("אלון שבות", "אל ההר", "02-9933228", "ירושלים", "https://waze.com/ul?q=אל ההר אלון שבות"),
    AccessibleMikveh("אלעזר", "10", "08-9461018", "ירושלים", "https://waze.com/ul?q=10 אלעזר"),
    AccessibleMikveh("אלעזר", "מרכז היישוב", "02-9932964", "ירושלים", "https://waze.com/ul?q=מרכז היישוב אלעזר"),
    AccessibleMikveh("אפרת", "רימון 50", "02-9931513", "ירושלים", "https://waze.com/ul?q=רימון 50 אפרת"),
    AccessibleMikveh("אפרת", "דקל 10", "02-9938647", "ירושלים", "https://waze.com/ul?q=דקל 10 אפרת"),
    AccessibleMikveh("אפרת", "זית שמן 34", "02-9934740", "ירושלים", "https://waze.com/ul?q=זית שמן 34 אפרת"),
    AccessibleMikveh("בית שמש", "נחל לוז 16 מים חיים", "02-9910001", "ירושלים", "https://waze.com/ul?q=נחל לוז 16 מים חיים בית שמש"),
    AccessibleMikveh("בית שמש", "המשלט 12א", "02-9913404", "ירושלים", "https://waze.com/ul?q=המשלט 12א בית שמש"),
    AccessibleMikveh("בית שמש", "הרקפת 42", "02-9925536", "ירושלים", "https://waze.com/ul?q=הרקפת 42 בית שמש"),
    AccessibleMikveh("בית שמש", "ראובן 20", "02-9952016", "ירושלים", "https://waze.com/ul?q=ראובן 20 בית שמש"),
    AccessibleMikveh("בית שמש", "שביל האשל 24", "02-9917488", "ירושלים", "https://waze.com/ul?q=שביל האשל 24 בית שמש"),
    AccessibleMikveh("בית שמש", "נחל רביבים 20/8", "02-9912442", "ירושלים", "https://waze.com/ul?q=נחל רביבים 20/8 בית שמש"),
    AccessibleMikveh("בית שמש", "בן זכאי 3", "02-9998472", "ירושלים", "https://waze.com/ul?q=בן זכאי 3 בית שמש"),
    AccessibleMikveh("בית שמש", "דולב 30", "02-9925213", "ירושלים", "https://waze.com/ul?q=דולב 30 בית שמש"),
    AccessibleMikveh("בית שמש", "בן איש חי 7", "02-9921605", "ירושלים", "https://waze.com/ul?q=בן איש חי 7 בית שמש"),
    AccessibleMikveh("בית שמש", "יואל 3", "", "ירושלים", "https://waze.com/ul?q=יואל 3 בית שמש"),
    AccessibleMikveh("ביתר עילית", "חזון איש 1", "02-5800100", "ירושלים", "https://waze.com/ul?q=חזון איש 1 ביתר עילית"),
    AccessibleMikveh("ביתר עילית", "רבי אליעזר 5", "02-5800101", "ירושלים", "https://waze.com/ul?q=רבי אליעזר 5 ביתר עילית"),
    AccessibleMikveh("ביתר עילית", "הבעש\"ט 8", "02-5806059", "ירושלים", "https://waze.com/ul?q=הבעש\"ט 8 ביתר עילית"),
    AccessibleMikveh("ביתר עילית", "יצחק אריאלי 3", "02-5808444", "ירושלים", "https://waze.com/ul?q=יצחק אריאלי 3 ביתר עילית"),
    AccessibleMikveh("ביתר עילית", "קנייבסקי לוי פינת אגרות משה", "02-5725649", "ירושלים", "https://waze.com/ul?q=קנייבסקי לוי פינת אגרות משה ביתר עילית"),
    AccessibleMikveh("ביתר עילית", "קדושת לוי פינת מהרי\"ץ", "02-5807722", "ירושלים", "https://waze.com/ul?q=קדושת לוי פינת מהרי\"ץ ביתר עילית"),
    AccessibleMikveh("ביתר עילית", "ברים 7", "02-5800669", "ירושלים", "https://waze.com/ul?q=ברים 7 ביתר עילית"),
    AccessibleMikveh("ביתר עילית", "מוצפי 35", "02-6507450", "ירושלים", "https://waze.com/ul?q=מוצפי 35 ביתר עילית"),
    AccessibleMikveh("בת עין", "מרכז היישוב", "02-9933508", "ירושלים", "https://waze.com/ul?q=מרכז היישוב בת עין"),
    AccessibleMikveh("גבעת זאב", "עמק איילון 2", "02-5362158", "ירושלים", "https://waze.com/ul?q=עמק איילון 2 גבעת זאב"),
    AccessibleMikveh("גבעת זאב", "ערוגות 19", "02-5862870", "ירושלים", "https://waze.com/ul?q=ערוגות 19 גבעת זאב"),
    AccessibleMikveh("גבעת זאב", "גבעון מערב", "02-6504826", "ירושלים", "https://waze.com/ul?q=גבעון מערב גבעת זאב"),
    AccessibleMikveh("גבעת זאב", "העופר 1 אגן האיילות", "02-5637755", "ירושלים", "https://waze.com/ul?q=העופר 1 אגן האיילות גבעת זאב"),
    AccessibleMikveh("גבעת זאב", "האיילות 1", "02-5618359", "ירושלים", "https://waze.com/ul?q=האיילות 1 גבעת זאב"),
    AccessibleMikveh("טלז סטון", "הרב א.מ. בלוך 3", "02-5346135", "ירושלים", "https://waze.com/ul?q=הרב א.מ. בלוך 3 טלז סטון"),
    AccessibleMikveh("טלמון", "מרכז היישוב", "02-5913911", "ירושלים", "https://waze.com/ul?q=מרכז היישוב טלמון"),
    AccessibleMikveh("ירושלים", "זלמן צורף 6 פסגת זאב", "02-5800001", "ירושלים", "https://waze.com/ul?q=זלמן צורף 6 פסגת זאב ירושלים"),
    AccessibleMikveh("ירושלים", "בן דור 15 ארנונה", "02-5800002", "ירושלים", "https://waze.com/ul?q=בן דור 15 ארנונה ירושלים"),
    AccessibleMikveh("ירושלים", "שחר 21 בית הכרם", "02-5800003", "ירושלים", "https://waze.com/ul?q=שחר 21 בית הכרם ירושלים"),
    AccessibleMikveh("ירושלים", "יוסף חכמי 48 בית וגן", "02-5800004", "ירושלים", "https://waze.com/ul?q=יוסף חכמי 48 בית וגן ירושלים"),
    AccessibleMikveh("ירושלים", "שערי תורה 4 בית וגן", "02-5800005", "ירושלים", "https://waze.com/ul?q=שערי תורה 4 בית וגן ירושלים"),
    AccessibleMikveh("ירושלים", "זוננפלד בית ישראל", "02-5800006", "ירושלים", "https://waze.com/ul?q=זוננפלד בית ישראל ירושלים"),
    AccessibleMikveh("ירושלים", "גדעון 7 בקעה", "02-5800007", "ירושלים", "https://waze.com/ul?q=גדעון 7 בקעה ירושלים"),
    AccessibleMikveh("ירושלים", "הנציב 8 בתי ראנד", "02-5800008", "ירושלים", "https://waze.com/ul?q=הנציב 8 בתי ראנד ירושלים"),
    AccessibleMikveh("ירושלים", "ההגנה 15 גבעה צרפתית", "02-5800009", "ירושלים", "https://waze.com/ul?q=ההגנה 15 גבעה צרפתית ירושלים"),
    AccessibleMikveh("ירושלים", "הלר 24 גבעת מרדכי", "02-5800010", "ירושלים", "https://waze.com/ul?q=הלר 24 גבעת מרדכי ירושלים"),
    AccessibleMikveh("ירושלים", "אנטיגונוס 30 גונן", "02-5800011", "ירושלים", "https://waze.com/ul?q=אנטיגונוס 30 גונן ירושלים"),
    AccessibleMikveh("ירושלים", "הסנונית 3 גילה", "02-5800012", "ירושלים", "https://waze.com/ul?q=הסנונית 3 גילה ירושלים"),
    AccessibleMikveh("ירושלים", "האפרסמון 40 גילה", "02-5800013", "ירושלים", "https://waze.com/ul?q=האפרסמון 40 גילה ירושלים"),
    AccessibleMikveh("ירושלים", "יהודה הנשיא 15 דאבח", "02-5800014", "ירושלים", "https://waze.com/ul?q=יהודה הנשיא 15 דאבח ירושלים"),
    AccessibleMikveh("ירושלים", "העומר 11 הר חומה", "02-5800015", "ירושלים", "https://waze.com/ul?q=העומר 11 הר חומה ירושלים"),
    AccessibleMikveh("ירושלים", "הממציא 17 הר חומה ב", "02-5800016", "ירושלים", "https://waze.com/ul?q=הממציא 17 הר חומה ב ירושלים"),
    AccessibleMikveh("ירושלים", "אגסי 10 הר נוף", "02-5800017", "ירושלים", "https://waze.com/ul?q=אגסי 10 הר נוף ירושלים"),
    AccessibleMikveh("ירושלים", "אלישע 10 מוסררה", "02-5800019", "ירושלים", "https://waze.com/ul?q=אלישע 10 מוסררה ירושלים"),
    AccessibleMikveh("ירושלים", "55 העיר העתיקה", "04-6820002", "ירושלים", "https://waze.com/ul?q=55 העיר העתיקה ירושלים"),
    AccessibleMikveh("ירושלים", "1", "08-8580001", "ירושלים", "https://waze.com/ul?q=1 ירושלים"),
    AccessibleMikveh("ירושלים", "כפר השילוח פינת זוננפלד בית ישראל", "02-5370931", "ירושלים", "https://waze.com/ul?q=כפר השילוח פינת זוננפלד בית ישראל ירושלים"),
    AccessibleMikveh("ירושלים", "הנציב 7 פינת התבור1 בתי ראנד", "02-6255560", "ירושלים", "https://waze.com/ul?q=הנציב 7 פינת התבור1 בתי ראנד ירושלים"),
    AccessibleMikveh("ירושלים", "ברוכים 14 פינת גרוסברג גוש 80", "02-5002715", "ירושלים", "https://waze.com/ul?q=ברוכים 14 פינת גרוסברג גוש 80 ירושלים"),
    AccessibleMikveh("ירושלים", "יהודא הנשיא 15 דבאח גונן", "02-6782352", "ירושלים", "https://waze.com/ul?q=יהודא הנשיא 15 דבאח גונן ירושלים"),
    AccessibleMikveh("ירושלים", "העומר 11 העיר העתיקה", "02-6283715", "ירושלים", "https://waze.com/ul?q=העומר 11 העיר העתיקה ירושלים"),
    AccessibleMikveh("ירושלים", "הרב שלמה מנהר הר חומה ב", "02-6259755", "ירושלים", "https://waze.com/ul?q=הרב שלמה מנהר הר חומה ב ירושלים"),
    AccessibleMikveh("ירושלים", "שאולזון 82 הר נוף ב", "02-5363830", "ירושלים", "https://waze.com/ul?q=שאולזון 82 הר נוף ב ירושלים"),
    AccessibleMikveh("ירושלים", "אנטיגנוס 30 לרזה גונן", "02-6482534", "ירושלים", "https://waze.com/ul?q=אנטיגנוס 30 לרזה גונן ירושלים"),
    AccessibleMikveh("ירושלים", "אדמו\"ר מויזניץ 9 מאור חיים", "02-5323674", "ירושלים", "https://waze.com/ul?q=אדמו\"ר מויזניץ 9 מאור חיים ירושלים"),
    AccessibleMikveh("ירושלים", "מעלה זיתים", "02-9708921", "ירושלים", "https://waze.com/ul?q=מעלה זיתים ירושלים"),
    AccessibleMikveh("ירושלים", "בלבן 4 נוה יעקב", "02-5851355", "ירושלים", "https://waze.com/ul?q=בלבן 4 נוה יעקב ירושלים"),
    AccessibleMikveh("ירושלים", "זוין 2 נוה יעקב מזרח", "02-5849889", "ירושלים", "https://waze.com/ul?q=זוין 2 נוה יעקב מזרח ירושלים"),
    AccessibleMikveh("ירושלים", "דרך האחיות 7 עין כרם", "02-6436787", "ירושלים", "https://waze.com/ul?q=דרך האחיות 7 עין כרם ירושלים"),
    AccessibleMikveh("ירושלים", "סיירת גולני 9 פסגת זאב", "02-5858246", "ירושלים", "https://waze.com/ul?q=סיירת גולני 9 פסגת זאב ירושלים"),
    AccessibleMikveh("ירושלים", "ראובן ארזי מול 26 פסגת זאב מזרח", "02-5856990", "ירושלים", "https://waze.com/ul?q=ראובן ארזי מול 26 פסגת זאב מזרח ירושלים"),
    AccessibleMikveh("ירושלים", "מזל טלה 1 פסגת זאב צפון", "02-6561951", "ירושלים", "https://waze.com/ul?q=מזל טלה 1 פסגת זאב צפון ירושלים"),
    AccessibleMikveh("ירושלים", "מנחת יצחק פינת אוהל יהושוע קוממיות", "02-5023508", "ירושלים", "https://waze.com/ul?q=מנחת יצחק פינת אוהל יהושוע קוממיות ירושלים"),
    AccessibleMikveh("ירושלים", "פלורנטין 3 קטמון", "02-6417110", "ירושלים", "https://waze.com/ul?q=פלורנטין 3 קטמון ירושלים"),
    AccessibleMikveh("ירושלים", "המצור 7", "02-5365174", "ירושלים", "https://waze.com/ul?q=המצור 7 ירושלים"),
    AccessibleMikveh("ירושלים", "הסביון 5 קרית יובל", "02-6416946", "ירושלים", "https://waze.com/ul?q=הסביון 5 קרית יובל ירושלים"),
    AccessibleMikveh("ירושלים", "חיים ויטאל 37 קרית משה", "02-6521577", "ירושלים", "https://waze.com/ul?q=חיים ויטאל 37 קרית משה ירושלים"),
    AccessibleMikveh("ירושלים", "האר\"י 5 רחביה", "02-5632783", "ירושלים", "https://waze.com/ul?q=האר\"י 5 רחביה ירושלים"),
    AccessibleMikveh("ירושלים", "שירת הים 5 רמות 1", "02-5868890", "ירושלים", "https://waze.com/ul?q=שירת הים 5 רמות 1 ירושלים"),
    AccessibleMikveh("ירושלים", "משעול הקורנית 3 רמות 2", "02-5864556", "ירושלים", "https://waze.com/ul?q=משעול הקורנית 3 רמות 2 ירושלים"),
    AccessibleMikveh("ירושלים", "שלום סיון 7 רמות 3", "02-5866357", "ירושלים", "https://waze.com/ul?q=שלום סיון 7 רמות 3 ירושלים"),
    AccessibleMikveh("ירושלים", "חזקיהו שבתאי 12 רמות 4", "02-5863176", "ירושלים", "https://waze.com/ul?q=חזקיהו שבתאי 12 רמות 4 ירושלים"),
    AccessibleMikveh("ירושלים", "פארן 6 רמת אשכול", "02-5810741", "ירושלים", "https://waze.com/ul?q=פארן 6 רמת אשכול ירושלים"),
    AccessibleMikveh("ירושלים", "משה אהרון טוב 18 רמת בית הכרם", "02-5633428", "ירושלים", "https://waze.com/ul?q=משה אהרון טוב 18 רמת בית הכרם ירושלים"),
    AccessibleMikveh("ירושלים", "כהנמן 16 רמת שלמה", "02-5863121", "ירושלים", "https://waze.com/ul?q=כהנמן 16 רמת שלמה ירושלים"),
    AccessibleMikveh("ירושלים", "אבשלום חביב תלפיות מזרח", "02-6723026", "ירושלים", "https://waze.com/ul?q=אבשלום חביב תלפיות מזרח ירושלים"),
    AccessibleMikveh("כפר אלדד", "מרכז היישוב", "02-9961124", "ירושלים", "https://waze.com/ul?q=מרכז היישוב כפר אלדד"),
    AccessibleMikveh("כרמי צור", "מרכז היישוב", "02-9963379", "ירושלים", "https://waze.com/ul?q=מרכז היישוב כרמי צור"),
    AccessibleMikveh("מבשרת ציון", "שער הגיא 10", "02-5340001", "ירושלים", "https://waze.com/ul?q=שער הגיא 10 מבשרת ציון"),
    AccessibleMikveh("מבשרת ציון", "שדה חמד 9", "02-5346368", "ירושלים", "https://waze.com/ul?q=שדה חמד 9 מבשרת ציון"),
    AccessibleMikveh("מבשרת ציון", "מצפה הבירה 19ב", "02-5346007", "ירושלים", "https://waze.com/ul?q=מצפה הבירה 19ב מבשרת ציון"),
    AccessibleMikveh("מודיעין עילית", "קהילת יעקב ברכפלד", "08-9740010", "ירושלים", "https://waze.com/ul?q=קהילת יעקב ברכפלד מודיעין עילית"),
    AccessibleMikveh("מודיעין עילית", "קרית ספר", "08-9740011", "ירושלים", "https://waze.com/ul?q=קרית ספר מודיעין עילית"),
    AccessibleMikveh("מעלה אדומים", "מצדה 15", "02-5350001", "ירושלים", "https://waze.com/ul?q=מצדה 15 מעלה אדומים"),
    AccessibleMikveh("מעלה אדומים", "נופי מדבר", "02-5350002", "ירושלים", "https://waze.com/ul?q=נופי מדבר מעלה אדומים"),
    AccessibleMikveh("מעלה אדומים", "מצפה נבו 96", "02-5351732", "ירושלים", "https://waze.com/ul?q=מצפה נבו 96 מעלה אדומים"),
    AccessibleMikveh("מעלה אדומים", "פרי מגדים 102", "02-5901022", "ירושלים", "https://waze.com/ul?q=פרי מגדים 102 מעלה אדומים"),
    AccessibleMikveh("מעלה אדומים", "הערבה 3", "02-5909385", "ירושלים", "https://waze.com/ul?q=הערבה 3 מעלה אדומים"),
    AccessibleMikveh("מעלה אדומים", "השיש 20", "02-5905632", "ירושלים", "https://waze.com/ul?q=השיש 20 מעלה אדומים"),
    AccessibleMikveh("מעלה אדומים", "הנחלים 96", "02-5352007", "ירושלים", "https://waze.com/ul?q=הנחלים 96 מעלה אדומים"),
    AccessibleMikveh("נוה דניאל", "מרכז היישוב", "02-9932760", "ירושלים", "https://waze.com/ul?q=מרכז היישוב נוה דניאל"),
    AccessibleMikveh("נוקדים", "מרכז היישוב", "02-9963729", "ירושלים", "https://waze.com/ul?q=מרכז היישוב נוקדים"),
    AccessibleMikveh("עלי", "האגוז ליד מכינת בני דוד", "02-5913922", "ירושלים", "https://waze.com/ul?q=האגוז ליד מכינת בני דוד עלי"),
    AccessibleMikveh("עפרה", "ט' באייר 27", "02-5913904", "ירושלים", "https://waze.com/ul?q=ט' באייר 27 עפרה"),
    AccessibleMikveh("קרית ארבע", "יהושע בן נון", "02-9961733", "ירושלים", "https://waze.com/ul?q=יהושע בן נון קרית ארבע"),
    AccessibleMikveh("קרית ארבע", "רמת ממרא", "02-9963033", "ירושלים", "https://waze.com/ul?q=רמת ממרא קרית ארבע"),
    AccessibleMikveh("תל ציון", "קהילות יעקב 9", "02-5913932", "ירושלים", "https://waze.com/ul?q=קהילות יעקב 9 תל ציון"),
    AccessibleMikveh("תקוע", "מרכז היישוב", "02-9964111", "ירושלים", "https://waze.com/ul?q=מרכז היישוב תקוע"),

    // ── שפלה ודרום ──────────────────────────────────────────────────────────────
    AccessibleMikveh("אופקים", "הראשונים 12", "08-9900001", "שפלה ודרום", "https://waze.com/ul?q=הראשונים 12 אופקים"),
    AccessibleMikveh("אופקים", "משה דיין 5", "08-9900002", "שפלה ודרום", "https://waze.com/ul?q=משה דיין 5 אופקים"),
    AccessibleMikveh("אופקים", "משעול הרקפת 8", "", "שפלה ודרום", "https://waze.com/ul?q=משעול הרקפת 8 אופקים"),
    AccessibleMikveh("אופקים", "אבוחצירא 14", "", "שפלה ודרום", "https://waze.com/ul?q=אבוחצירא 14 אופקים"),
    AccessibleMikveh("אופקים", "עמרם בן דיואן 1", "", "שפלה ודרום", "https://waze.com/ul?q=עמרם בן דיואן 1 אופקים"),
    AccessibleMikveh("אופקים", "שמשון 1", "", "שפלה ודרום", "https://waze.com/ul?q=שמשון 1 אופקים"),
    AccessibleMikveh("אילת", "8", "09-8980001", "שפלה ודרום", "https://waze.com/ul?q=8 אילת"),
    AccessibleMikveh("אילת", "התמרים 1", "08-6370001", "שפלה ודרום", "https://waze.com/ul?q=התמרים 1 אילת"),
    AccessibleMikveh("אילת", "שד' התמרים 22", "08-6370002", "שפלה ודרום", "https://waze.com/ul?q=שד' התמרים 22 אילת"),
    AccessibleMikveh("אילת", "החורב", "08-6373801", "שפלה ודרום", "https://waze.com/ul?q=החורב אילת"),
    AccessibleMikveh("אילת", "ששת הימים 7014", "08-6331791", "שפלה ודרום", "https://waze.com/ul?q=ששת הימים 7014 אילת"),
    AccessibleMikveh("אילת", "בני ישראל 24", "08-6344060", "שפלה ודרום", "https://waze.com/ul?q=בני ישראל 24 אילת"),
    AccessibleMikveh("אשדוד", "הרב שאולי 5 רובע ד'", "08-8500001", "שפלה ודרום", "https://waze.com/ul?q=הרב שאולי 5 רובע ד' אשדוד"),
    AccessibleMikveh("אשדוד", "הגדוד העברי 8 רובע ז'", "08-8500002", "שפלה ודרום", "https://waze.com/ul?q=הגדוד העברי 8 רובע ז' אשדוד"),
    AccessibleMikveh("אשדוד", "האר\"י 15 רובע ט'", "08-8500003", "שפלה ודרום", "https://waze.com/ul?q=האר\"י 15 רובע ט' אשדוד"),
    AccessibleMikveh("אשדוד", "הרצל 3 רובע א'", "08-8500004", "שפלה ודרום", "https://waze.com/ul?q=הרצל 3 רובע א' אשדוד"),
    AccessibleMikveh("אשדוד", "משמר הירדן 5 רובע א", "073-2654671", "שפלה ודרום", "https://waze.com/ul?q=משמר הירדן 5 רובע א אשדוד"),
    AccessibleMikveh("אשדוד", "הפלמ\"ח 25 רובע ג", "073-2654673", "שפלה ודרום", "https://waze.com/ul?q=הפלמ\"ח 25 רובע ג אשדוד"),
    AccessibleMikveh("אשדוד", "שאולי 5 רובע ד", "073-2654674", "שפלה ודרום", "https://waze.com/ul?q=שאולי 5 רובע ד אשדוד"),
    AccessibleMikveh("אשדוד", "ירמיהו הלפרין 7 רובע ו", "073-2654676", "שפלה ודרום", "https://waze.com/ul?q=ירמיהו הלפרין 7 רובע ו אשדוד"),
    AccessibleMikveh("אשדוד", "מבוא האתרוג 3 רובע ח", "073-2654678", "שפלה ודרום", "https://waze.com/ul?q=מבוא האתרוג 3 רובע ח אשדוד"),
    AccessibleMikveh("אשדוד", "הכר\"א 35 רובע ט", "073-2654679", "שפלה ודרום", "https://waze.com/ul?q=הכר\"א 35 רובע ט אשדוד"),
    AccessibleMikveh("אשדוד", "הר חרמון 5 רובע יא", "073-2654681", "שפלה ודרום", "https://waze.com/ul?q=הר חרמון 5 רובע יא אשדוד"),
    AccessibleMikveh("אשדוד", "שבט דן 26 רובע יב", "073-2654682", "שפלה ודרום", "https://waze.com/ul?q=שבט דן 26 רובע יב אשדוד"),
    AccessibleMikveh("אשדוד", "שלמה המלך 20 רובע יג", "073-2654683", "שפלה ודרום", "https://waze.com/ul?q=שלמה המלך 20 רובע יג אשדוד"),
    AccessibleMikveh("אשדוד", "שמטת ברוריה 5 רובע טו", "073-2654685", "שפלה ודרום", "https://waze.com/ul?q=שמטת ברוריה 5 רובע טו אשדוד"),
    AccessibleMikveh("אשדוד", "העצמאות 36 רובע סיטי", "073-2654684", "שפלה ודרום", "https://waze.com/ul?q=העצמאות 36 רובע סיטי אשדוד"),
    AccessibleMikveh("אשקלון", "שפייה 25 עיר היין", "08-6700001", "שפלה ודרום", "https://waze.com/ul?q=שפייה 25 עיר היין אשקלון"),
    AccessibleMikveh("אשקלון", "המפל 6 אגמים", "08-6700002", "שפלה ודרום", "https://waze.com/ul?q=המפל 6 אגמים אשקלון"),
    AccessibleMikveh("אשקלון", "המרד 1", "08-6726127", "שפלה ודרום", "https://waze.com/ul?q=המרד 1 אשקלון"),
    AccessibleMikveh("אשקלון", "אפרים צור בחניון", "08-6752638", "שפלה ודרום", "https://waze.com/ul?q=אפרים צור בחניון אשקלון"),
    AccessibleMikveh("אשקלון", "סמטת הקיקיון", "08-6734135", "שפלה ודרום", "https://waze.com/ul?q=סמטת הקיקיון אשקלון"),
    AccessibleMikveh("אשקלון", "מעלות אשר 9", "08-6781192", "שפלה ודרום", "https://waze.com/ul?q=מעלות אשר 9 אשקלון"),
    AccessibleMikveh("אשקלון", "דוד רזיאל", "08-6755645", "שפלה ודרום", "https://waze.com/ul?q=דוד רזיאל אשקלון"),
    AccessibleMikveh("אשקלון", "הבלפור 2", "08-6845208", "שפלה ודרום", "https://waze.com/ul?q=הבלפור 2 אשקלון"),
    AccessibleMikveh("אשקלון", "משה יוליש 21א", "08-6761732", "שפלה ודרום", "https://waze.com/ul?q=משה יוליש 21א אשקלון"),
    AccessibleMikveh("באר יעקב", "יהודה 2", "08-9281249", "שפלה ודרום", "https://waze.com/ul?q=יהודה 2 באר יעקב"),
    AccessibleMikveh("באר שבע", "גני יער", "050-7563639", "שפלה ודרום", "https://waze.com/ul?q=גני יער באר שבע"),
    AccessibleMikveh("באר שבע", "חב\"ד", "052-8596248", "שפלה ודרום", "https://waze.com/ul?q=חב\"ד באר שבע"),
    AccessibleMikveh("באר שבע", "גני אביב", "053-3410510", "שפלה ודרום", "https://waze.com/ul?q=גני אביב באר שבע"),
    AccessibleMikveh("באר שבע", "נאות יצחק", "054-2291716", "שפלה ודרום", "https://waze.com/ul?q=נאות יצחק באר שבע"),
    AccessibleMikveh("באר שבע", "באר ציון", "050-6664308", "שפלה ודרום", "https://waze.com/ul?q=באר ציון באר שבע"),
    AccessibleMikveh("באר שבע", "גבעת הזיתים", "050-6664313", "שפלה ודרום", "https://waze.com/ul?q=גבעת הזיתים באר שבע"),
    AccessibleMikveh("באר שבע", "בז 2 - טהרת המלכה", "08-6204000", "שפלה ודרום", "https://waze.com/ul?q=בז 2 - טהרת המלכה באר שבע"),
    AccessibleMikveh("באר שבע", "מי מרום 3 - נווה זאב", "08-6204000", "שפלה ודרום", "https://waze.com/ul?q=מי מרום 3 - נווה זאב באר שבע"),
    AccessibleMikveh("באר שבע", "מבצע עבודה 53", "08-6106171", "שפלה ודרום", "https://waze.com/ul?q=מבצע עבודה 53 באר שבע"),
    AccessibleMikveh("באר שבע", "המדע 40", "08-6486770", "שפלה ודרום", "https://waze.com/ul?q=המדע 40 באר שבע"),
    AccessibleMikveh("באר שבע", "עליאש מרדכי 28", "08-6273987", "שפלה ודרום", "https://waze.com/ul?q=עליאש מרדכי 28 באר שבע"),
    AccessibleMikveh("באר שבע", "שמעון בן שטח 6", "08-6480844", "שפלה ודרום", "https://waze.com/ul?q=שמעון בן שטח 6 באר שבע"),
    AccessibleMikveh("באר שבע", "כיכר הקניזי 10", "08-6480948", "שפלה ודרום", "https://waze.com/ul?q=כיכר הקניזי 10 באר שבע"),
    AccessibleMikveh("באר שבע", "סטרומה 40 פינת העליה", "08-6410401", "שפלה ודרום", "https://waze.com/ul?q=סטרומה 40 פינת העליה באר שבע"),
    AccessibleMikveh("באר שבע", "הגאונים / אגרנט", "08-6431402", "שפלה ודרום", "https://waze.com/ul?q=הגאונים / אגרנט באר שבע"),
    AccessibleMikveh("באר שבע", "קלאוזנר 15", "08-6480843", "שפלה ודרום", "https://waze.com/ul?q=קלאוזנר 15 באר שבע"),
    AccessibleMikveh("באר שבע", "מבצע משה 2", "08-6420855", "שפלה ודרום", "https://waze.com/ul?q=מבצע משה 2 באר שבע"),
    AccessibleMikveh("באר שבע", "יהונתן ז'בוטינסקי 60", "08-6418139", "שפלה ודרום", "https://waze.com/ul?q=יהונתן ז'בוטינסקי 60 באר שבע"),
    AccessibleMikveh("באר שבע", "אחד העם 17", "077-5446012", "שפלה ודרום", "https://waze.com/ul?q=אחד העם 17 באר שבע"),
    AccessibleMikveh("בטחה", "מרכז המושב", "08-9922836", "שפלה ודרום", "https://waze.com/ul?q=מרכז המושב בטחה"),
    AccessibleMikveh("בית הגדי", "ליד המרפאה", "08-9934760", "שפלה ודרום", "https://waze.com/ul?q=ליד המרפאה בית הגדי"),
    AccessibleMikveh("דימונה", "הרב מרדכי אליהו 6 השחר", "08-6550001", "שפלה ודרום", "https://waze.com/ul?q=הרב מרדכי אליהו 6 השחר דימונה"),
    AccessibleMikveh("דימונה", "מודיעין סיוון 8", "08-9740001", "שפלה ודרום", "https://waze.com/ul?q=מודיעין סיוון 8 דימונה"),
    AccessibleMikveh("דימונה", "הדס", "", "שפלה ודרום", "https://waze.com/ul?q=הדס דימונה"),
    AccessibleMikveh("דימונה", "נאות הללי", "", "שפלה ודרום", "https://waze.com/ul?q=נאות הללי דימונה"),
    AccessibleMikveh("דימונה", "עובדת", "", "שפלה ודרום", "https://waze.com/ul?q=עובדת דימונה"),
    AccessibleMikveh("יבנה", "10", "03-6296512", "שפלה ודרום", "https://waze.com/ul?q=10 יבנה"),
    AccessibleMikveh("יבנה", "שי עגנון 17 נאות שמיר", "08-9400001", "שפלה ודרום", "https://waze.com/ul?q=שי עגנון 17 נאות שמיר יבנה"),
    AccessibleMikveh("יבנה", "הצדף 9 נאות רבין", "08-9400002", "שפלה ודרום", "https://waze.com/ul?q=הצדף 9 נאות רבין יבנה"),
    AccessibleMikveh("יבנה", "דרך הציונות 45", "08-8570001", "שפלה ודרום", "https://waze.com/ul?q=דרך הציונות 45 יבנה"),
    AccessibleMikveh("יבנה", "רבי עקיבא", "08-8571678", "שפלה ודרום", "https://waze.com/ul?q=רבי עקיבא יבנה"),
    AccessibleMikveh("יבנה", "ארגמן 7", "08-6588392", "שפלה ודרום", "https://waze.com/ul?q=ארגמן 7 יבנה"),
    AccessibleMikveh("יבנה", "גויאבה", "08-9431759", "שפלה ודרום", "https://waze.com/ul?q=גויאבה יבנה"),
    AccessibleMikveh("יבנה", "שבזי 40", "08-9160961", "שפלה ודרום", "https://waze.com/ul?q=שבזי 40 יבנה"),
    AccessibleMikveh("יבנה", "תורמוס 5", "", "שפלה ודרום", "https://waze.com/ul?q=תורמוס 5 יבנה"),
    AccessibleMikveh("ירוחם", "בן גוריון 25", "08-6580001", "שפלה ודרום", "https://waze.com/ul?q=בן גוריון 25 ירוחם"),
    AccessibleMikveh("ירוחם", "גוש עציון", "", "שפלה ודרום", "https://waze.com/ul?q=גוש עציון ירוחם"),
    AccessibleMikveh("מיתר", "מרכז היישוב", "08-6518460", "שפלה ודרום", "https://waze.com/ul?q=מרכז היישוב מיתר"),
    AccessibleMikveh("מעגלים", "בכניסה לישוב", "077-2120037", "שפלה ודרום", "https://waze.com/ul?q=בכניסה לישוב מעגלים"),
    AccessibleMikveh("מצפה רמון", "הר בוקר 3", "08-6580010", "שפלה ודרום", "https://waze.com/ul?q=הר בוקר 3 מצפה רמון"),
    AccessibleMikveh("מצפה רמון", "שד' בן גוריון 16", "", "שפלה ודרום", "https://waze.com/ul?q=שד' בן גוריון 16 מצפה רמון"),
    AccessibleMikveh("מרכז שפירא", "מרכז היישוב", "08-6527242", "שפלה ודרום", "https://waze.com/ul?q=מרכז היישוב מרכז שפירא"),
    AccessibleMikveh("נתיבות", "הרב עובדיה יוסף", "08-9950001", "שפלה ודרום", "https://waze.com/ul?q=הרב עובדיה יוסף נתיבות"),
    AccessibleMikveh("נתיבות", "הבעש\"ט", "08-9950002", "שפלה ודרום", "https://waze.com/ul?q=הבעש\"ט נתיבות"),
    AccessibleMikveh("נתיבות", "רמב\"ם 901", "08-9945254", "שפלה ודרום", "https://waze.com/ul?q=רמב\"ם 901 נתיבות"),
    AccessibleMikveh("נתיבות", "ז'בוטינסקי 904", "08-9944232", "שפלה ודרום", "https://waze.com/ul?q=ז'בוטינסקי 904 נתיבות"),
    AccessibleMikveh("נתיבות", "החיד\"א 914", "08-9943189", "שפלה ודרום", "https://waze.com/ul?q=החיד\"א 914 נתיבות"),
    AccessibleMikveh("נתיבות", "הרב צבאן 22", "08-9932203", "שפלה ודרום", "https://waze.com/ul?q=הרב צבאן 22 נתיבות"),
    AccessibleMikveh("ערד", "יהודה 12", "08-9970001", "שפלה ודרום", "https://waze.com/ul?q=יהודה 12 ערד"),
    AccessibleMikveh("ערד", "הקנאים 59", "08-9950014", "שפלה ודרום", "https://waze.com/ul?q=הקנאים 59 ערד"),
    AccessibleMikveh("ערד", "עיינות 2", "08-9957182", "שפלה ודרום", "https://waze.com/ul?q=עיינות 2 ערד"),
    AccessibleMikveh("ערד", "עיט 1", "08-6373088", "שפלה ודרום", "https://waze.com/ul?q=עיט 1 ערד"),
    AccessibleMikveh("קרית גת", "הרצל 25", "08-6810001", "שפלה ודרום", "https://waze.com/ul?q=הרצל 25 קרית גת"),
    AccessibleMikveh("קרית גת", "שד' לכיש", "08-6810002", "שפלה ודרום", "https://waze.com/ul?q=שד' לכיש קרית גת"),
    AccessibleMikveh("קרית גת", "יחזקאל 6", "08-6884320", "שפלה ודרום", "https://waze.com/ul?q=יחזקאל 6 קרית גת"),
    AccessibleMikveh("קרית גת", "השושן 11", "08-6884350", "שפלה ודרום", "https://waze.com/ul?q=השושן 11 קרית גת"),
    AccessibleMikveh("קרית גת", "רפאל 7", "08-6884230", "שפלה ודרום", "https://waze.com/ul?q=רפאל 7 קרית גת"),
    AccessibleMikveh("קרית גת", "הארז 33", "08-6884322", "שפלה ודרום", "https://waze.com/ul?q=הארז 33 קרית גת"),
    AccessibleMikveh("קרית גת", "יפתח הגלעדי 9", "08-6525490", "שפלה ודרום", "https://waze.com/ul?q=יפתח הגלעדי 9 קרית גת"),
    AccessibleMikveh("קרית גת", "סטרומה 5", "08-9957288", "שפלה ודרום", "https://waze.com/ul?q=סטרומה 5 קרית גת"),
    AccessibleMikveh("קרית מלאכי", "הרמב\"ם 49", "08-8502229", "שפלה ודרום", "https://waze.com/ul?q=הרמב\"ם 49 קרית מלאכי"),
    AccessibleMikveh("קרית עקרון", "מימון פינת שלמה בן יוסף", "08-6481341", "שפלה ודרום", "https://waze.com/ul?q=מימון פינת שלמה בן יוסף קרית עקרון"),
    AccessibleMikveh("שדרות", "הגיבורים 7", "03-6700003", "שפלה ודרום", "https://waze.com/ul?q=הגיבורים 7 שדרות"),
    AccessibleMikveh("שדרות", "סיני 16א עין יונה", "04-8400002", "שפלה ודרום", "https://waze.com/ul?q=סיני 16א עין יונה שדרות"),
    AccessibleMikveh("שדרות", "יעקב דורי 2", "08-6800001", "שפלה ודרום", "https://waze.com/ul?q=יעקב דורי 2 שדרות"),
    AccessibleMikveh("שדרות", "האחוזה 3 שבע עלמות", "08-6800002", "שפלה ודרום", "https://waze.com/ul?q=האחוזה 3 שבע עלמות שדרות"),
    AccessibleMikveh("שדרות", "בן גוריון", "04-9970001", "שפלה ודרום", "https://waze.com/ul?q=בן גוריון שדרות"),
    AccessibleMikveh("שדרות", "הארץ מאחורי ביהכ\"נ המרכזי", "", "שפלה ודרום", "https://waze.com/ul?q=הארץ מאחורי ביהכ\"נ המרכזי שדרות"),
    AccessibleMikveh("שדרות", "משעול פעמונית", "08-6891131", "שפלה ודרום", "https://waze.com/ul?q=משעול פעמונית שדרות"),
    AccessibleMikveh("שדרות", "ההגנה 30", "08-6894014", "שפלה ודרום", "https://waze.com/ul?q=ההגנה 30 שדרות"),
    AccessibleMikveh("תפרח", "מרכז המושב", "08-9922910", "שפלה ודרום", "https://waze.com/ul?q=מרכז המושב תפרח"),
    AccessibleMikveh("תפרח", "כפר טורמן מרכז הכפר", "03-9711787", "שפלה ודרום", "https://waze.com/ul?q=כפר טורמן מרכז הכפר תפרח"),

    // ── שומרון ──────────────────────────────────────────────────────────────
    AccessibleMikveh("איתמר", "מרכז היישוב", "02-9975730", "שומרון", "https://waze.com/ul?q=מרכז היישוב איתמר"),
    AccessibleMikveh("אלון מורה", "מרכז היישוב", "", "שומרון", "https://waze.com/ul?q=מרכז היישוב אלון מורה"),
    AccessibleMikveh("אלקנה", "אבקת רוכל 2", "03-9362696", "שומרון", "https://waze.com/ul?q=אבקת רוכל 2 אלקנה"),
    AccessibleMikveh("אריאל", "הרב נסים 5", "03-9360001", "שומרון", "https://waze.com/ul?q=הרב נסים 5 אריאל"),
    AccessibleMikveh("אריאל", "בית לחם 33 רובע ב'", "03-6417349", "שומרון", "https://waze.com/ul?q=בית לחם 33 רובע ב' אריאל"),
    AccessibleMikveh("אריאל", "שומרון 1 רובע א'", "03-9366539", "שומרון", "https://waze.com/ul?q=שומרון 1 רובע א' אריאל"),
    AccessibleMikveh("אריאל", "23", "", "שומרון", "https://waze.com/ul?q=23 אריאל"),
    AccessibleMikveh("הר ברכה", "מרכז היישוב", "", "שומרון", "https://waze.com/ul?q=מרכז היישוב הר ברכה"),
    AccessibleMikveh("יצהר", "מרכז היישוב", "", "שומרון", "https://waze.com/ul?q=מרכז היישוב יצהר"),
    AccessibleMikveh("יקיר", "מרכז היישוב", "", "שומרון", "https://waze.com/ul?q=מרכז היישוב יקיר"),
    AccessibleMikveh("קדומים", "רבבות אפרים 2 גבעת שלם", "09-7928520", "שומרון", "https://waze.com/ul?q=רבבות אפרים 2 גבעת שלם קדומים"),
    AccessibleMikveh("קדומים", "צפון", "", "שומרון", "https://waze.com/ul?q=צפון קדומים"),
    AccessibleMikveh("קרני שומרון", "לח\"י", "", "שומרון", "https://waze.com/ul?q=לח\"י קרני שומרון"),
    AccessibleMikveh("קרני שומרון", "האלון", "", "שומרון", "https://waze.com/ul?q=האלון קרני שומרון"),
    AccessibleMikveh("קרני שומרון", "הנבל 25", "", "שומרון", "https://waze.com/ul?q=הנבל 25 קרני שומרון"),
)

// --- Mikveh Map Tab — Accessible Mikvehs with Region Filter & GPS Sorting ---

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MikvehMapTab() {
    var selectedRegion by remember { mutableStateOf("הכל") }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }

    val context = LocalContext.current
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    // Load location when permission is granted
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            try {
                val loc = fusedLocationClient.lastLocation.await()
                loc?.let { userLat = it.latitude; userLon = it.longitude }
            } catch (_: Exception) {}
        }
    }

    val filtered = remember(selectedRegion, userLat, userLon) {
        val base = if (selectedRegion == "הכל") ACCESSIBLE_MIKVEHS
                   else ACCESSIBLE_MIKVEHS.filter { it.region == selectedRegion }
        val uLat = userLat; val uLon = userLon
        if (uLat != null && uLon != null) {
            base.sortedBy { m ->
                val c = CITY_COORDS[m.city]
                if (c != null) distKm(uLat, uLon, c.first, c.second) else Double.MAX_VALUE
            }
        } else base
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GPS location button
            IconButton(
                onClick = {
                    if (locationPermission.status.isGranted) {
                        scope.launch {
                            try {
                                val loc = fusedLocationClient.lastLocation.await()
                                loc?.let { userLat = it.latitude; userLon = it.longitude }
                            } catch (_: Exception) {}
                        }
                    } else {
                        locationPermission.launchPermissionRequest()
                    }
                }
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "מיין לפי מיקום",
                    tint = if (userLat != null) PurityPurple
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "מקוואות מונגשות ♿ עם מעלון",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PurityPurple
            )
        }

        // ── Sort indicator ───────────────────────────────────────────────
        if (userLat != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                color = PurityPurple.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ממויין לפי מרחק מהמיקום שלך",
                    style = MaterialTheme.typography.labelSmall,
                    color = PurityPurple,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
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
                val dist = if (userLat != null && userLon != null)
                    CITY_COORDS[mikveh.city]?.let { distKm(userLat!!, userLon!!, it.first, it.second) }
                else null
                AccessibleMikvehCard(mikveh, dist)
            }
        }
    }
}

@Composable
private fun AccessibleMikvehCard(mikveh: AccessibleMikveh, distanceKm: Double? = null) {
    val context = LocalContext.current
    val phone = mikveh.contact.ifBlank { null }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // City + region badge + distance row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance badge (shown when GPS is available)
                if (distanceKm != null) {
                    val distText = if (distanceKm < 1.0) "${(distanceKm * 1000).toInt()} מ'"
                                   else "${"%.1f".format(distanceKm)} ק\"מ"
                    Surface(
                        color = PurityPurple.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.NearMe,
                                contentDescription = null,
                                tint = PurityPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = distText,
                                style = MaterialTheme.typography.labelSmall,
                                color = PurityPurple
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(0.dp))
                }
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

            // Phone + Waze row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phone text
                if (phone != null) {
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                }

                // Waze navigation button
                if (mikveh.wazeUrl.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val wazeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mikveh.wazeUrl))
                            wazeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(wazeIntent)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00AAFF).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = "נווטי ל-Waze",
                            tint = Color(0xFF00AAFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Call button
                if (phone != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val dialUri = Uri.parse("tel:${phone.replace("-", "")}")
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
