package com.jewish.calendar.ui.screens.zmanim

import android.annotation.SuppressLint
import android.Manifest
import android.location.Geocoder
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.LocationServices
import com.jewish.calendar.model.ZmanimModel
import com.jewish.calendar.ui.screens.common.DailyStudySection
import com.jewish.calendar.ui.theme.*
import com.jewish.calendar.viewmodel.DailyStudyViewModel
import com.jewish.calendar.viewmodel.ZmanimViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ── Resolve city name via reverse geocoding ───────────────────────────

private suspend fun resolveCityName(
    context: android.content.Context,
    location: android.location.Location
): String = withContext(Dispatchers.IO) {
    try {
        Geocoder(context, Locale("he"))
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.let { addr -> addr.locality ?: addr.subAdminArea ?: addr.adminArea }
            ?: "מיקומי הנוכחי"
    } catch (_: Exception) { "מיקומי הנוכחי" }
}

// ── ZmanimItem data class ─────────────────────────────────────────────

data class ZmanimItem(
    val key: String,                          // unique identifier for alarm
    val label: String,                        // Hebrew display name
    val time: String,                         // formatted HH:mm
    val date: Date?,                          // actual Date for alarm scheduling
    val isHighlighted: Boolean = false,
    val highlightColor: Color = Color.Unspecified,
    val hasAlarm: Boolean = false
)

// ── Screen ────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ZmanimScreen(
    viewModel: ZmanimViewModel = hiltViewModel(),
    dailyStudyViewModel: DailyStudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val studyState by dailyStudyViewModel.uiState.collectAsState()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show alarm feedback messages
    LaunchedEffect(uiState.alarmMessage) {
        uiState.alarmMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAlarmMessage()
        }
    }

    // Auto-load real GPS location whenever permission status changes to granted
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val cityName = resolveCityName(context, location)
                    viewModel.onLocationGranted(location, cityName)
                }
            } catch (_: Exception) { /* use default Jerusalem zmanim */ }
        } else {
            viewModel.onLocationDenied()
        }
    }

    // Alarm confirmation dialog state
    var pendingAlarmItem by remember { mutableStateOf<ZmanimItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "זמני היום",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (locationPermission.status.isGranted) {
                            scope.launch {
                                try {
                                    val location = fusedLocationClient.lastLocation.await()
                                    if (location != null) {
                                        val cityName = resolveCityName(context, location)
                                        viewModel.onLocationGranted(location, cityName)
                                    } else {
                                        viewModel.refreshZmanim()
                                    }
                                } catch (_: Exception) { viewModel.refreshZmanim() }
                            }
                        } else {
                            viewModel.refreshZmanim()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "רענן")
                    }
                    IconButton(onClick = { locationPermission.launchPermissionRequest() }) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "מיקום",
                            tint = if (locationPermission.status.isGranted) OmerGreen
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                uiState.zmanim?.let { zmanim ->
                    val activeKeys = uiState.activeAlarmKeys

                    LocationHeader(zmanim)

                    if (zmanim.isShabbat || zmanim.isYomTov) ShabbatBar(zmanim)
                    if (zmanim.isErevShabbat) ErevShabbatBar(zmanim)

                    ZmanimSection(
                        title = "בוקר",
                        icon = Icons.Default.WbSunny,
                        color = Gold60,
                        items = buildMorningZmanim(zmanim, activeKeys),
                        onItemClick = { pendingAlarmItem = it }
                    )
                    ZmanimSection(
                        title = "צהריים",
                        icon = Icons.Default.LightMode,
                        color = MaterialTheme.colorScheme.primary,
                        items = buildAfternoonZmanim(zmanim, activeKeys),
                        onItemClick = { pendingAlarmItem = it }
                    )
                    ZmanimSection(
                        title = "ערב",
                        icon = Icons.Default.NightsStay,
                        color = ShabbatBlue,
                        items = buildEveningZmanim(zmanim, activeKeys),
                        onItemClick = { pendingAlarmItem = it }
                    )

                    DailyStudySection(
                        state = studyState,
                        onItemClick = { dailyStudyViewModel.openItem(it) },
                        onDialogDismiss = { dailyStudyViewModel.closeDialog() }
                    )

                    if (!locationPermission.status.isGranted) {
                        LocationNote { locationPermission.launchPermissionRequest() }
                    }
                }

                uiState.error?.let { ErrorCard(it) }
            }
        }
    }

    // Alarm confirmation dialog
    pendingAlarmItem?.let { item ->
        AlarmConfirmDialog(
            item = item,
            onConfirm = { repeatDaily ->
                viewModel.toggleAlarm(item.key, item.label, item.date?.time, repeatDaily)
                pendingAlarmItem = null
            },
            onDismiss = { pendingAlarmItem = null }
        )
    }
}

// ── Alarm confirmation dialog ─────────────────────────────────────────

@Composable
private fun AlarmConfirmDialog(
    item: ZmanimItem,
    onConfirm: (repeatDaily: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var repeatDaily by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (item.hasAlarm) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                contentDescription = null,
                tint = if (item.hasAlarm) MaterialTheme.colorScheme.error else Gold60,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (item.hasAlarm) "ביטול התרעה" else "הגדרת התרעה",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (item.hasAlarm)
                        "האם לבטל את ההתרעה עבור\n${item.label} (${item.time})?"
                    else
                        "האם להגדיר התרעה עבור\n${item.label} (${item.time})?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // Show "repeat daily" toggle only when setting a new alarm
                if (!item.hasAlarm) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "חזור כל יום",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = repeatDaily,
                            onCheckedChange = { repeatDaily = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(repeatDaily) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.hasAlarm) MaterialTheme.colorScheme.error else Gold60
                )
            ) {
                Text(if (item.hasAlarm) "בטל התרעה" else "הגדר התרעה")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("לא עכשיו") }
        }
    )
}

// ── Composables ───────────────────────────────────────────────────────

@Composable
private fun LocationHeader(zmanim: ZmanimModel) {
    val dateFormatter = SimpleDateFormat("EEEE, d בMMMM", Locale("he"))
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = zmanim.cityName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = dateFormatter.format(zmanim.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ShabbatBar(zmanim: ZmanimModel) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color = ShabbatBlue,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = ShabbatGold)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (zmanim.isShabbat) "שבת שלום!" else "יום טוב!",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Star, contentDescription = null, tint = ShabbatGold)
        }
    }
}

@Composable
private fun ErevShabbatBar(zmanim: ZmanimModel) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    zmanim.candleLighting?.let { candleTime ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            color = Gold60.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "הדלקת נרות: ${timeFormatter.format(candleTime)}",
                    fontWeight = FontWeight.Bold, color = Gold80, fontSize = 16.sp
                )
                Text("🕯️", fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun ZmanimSection(
    title: String,
    icon: ImageVector,
    color: Color,
    items: List<ZmanimItem>,
    onItemClick: (ZmanimItem) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    ZmanimRow(item = item, onClick = { onItemClick(item) })
                    if (index < items.lastIndex) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZmanimRow(item: ZmanimItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (item.isHighlighted) item.highlightColor.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bell icon — shows alarm status
        Icon(
            imageVector = if (item.hasAlarm) Icons.Default.Notifications else Icons.Default.NotificationsNone,
            contentDescription = if (item.hasAlarm) "התרעה פעילה" else "הגדר התרעה",
            tint = if (item.hasAlarm) Gold60 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(8.dp))

        // Time — left side in RTL layout
        Text(
            text = item.time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (item.isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (item.isHighlighted) item.highlightColor else MaterialTheme.colorScheme.onSurface,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )

        Spacer(Modifier.weight(1f))

        // Label — right side in RTL layout
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isHighlighted) item.highlightColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LocationNote(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onRequestPermission() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "הזמנים מחושבים לירושלים. לחץ להפעלת GPS לזמנים מדויקים",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = HolidayRed.copy(alpha = 0.1f))
    ) {
        Text(text = error, modifier = Modifier.padding(16.dp), color = HolidayRed, textAlign = TextAlign.Center)
    }
}

// ── Zmanim builders ───────────────────────────────────────────────────

private fun buildMorningZmanim(zmanim: ZmanimModel, activeKeys: Set<String>): List<ZmanimItem> {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return listOfNotNull(
        zmanim.alotHashachar?.let {
            ZmanimItem("alotHashachar", "עלות השחר", fmt.format(it), it,
                hasAlarm = "alotHashachar" in activeKeys)
        },
        zmanim.sunrise?.let {
            ZmanimItem("sunrise", "הנץ החמה", fmt.format(it), it,
                isHighlighted = true, highlightColor = Gold60,
                hasAlarm = "sunrise" in activeKeys)
        },
        zmanim.sofZmanKriatShmaGRA?.let {
            ZmanimItem("sofZmanShmaGRA", "סוף ז\"ק שמע (גר\"א)", fmt.format(it), it,
                hasAlarm = "sofZmanShmaGRA" in activeKeys)
        },
        zmanim.sofZmanKriatShmaMGA?.let {
            ZmanimItem("sofZmanShmaMGA", "סוף ז\"ק שמע (מג\"א)", fmt.format(it), it,
                hasAlarm = "sofZmanShmaMGA" in activeKeys)
        },
        zmanim.sofZmanTfilaGRA?.let {
            ZmanimItem("sofZmanTfilaGRA", "סוף זמן תפילה (גר\"א)", fmt.format(it), it,
                hasAlarm = "sofZmanTfilaGRA" in activeKeys)
        },
        zmanim.sofZmanTfilaMGA?.let {
            ZmanimItem("sofZmanTfilaMGA", "סוף זמן תפילה (מג\"א)", fmt.format(it), it,
                hasAlarm = "sofZmanTfilaMGA" in activeKeys)
        }
    )
}

private fun buildAfternoonZmanim(zmanim: ZmanimModel, activeKeys: Set<String>): List<ZmanimItem> {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return listOfNotNull(
        zmanim.chatzot?.let {
            ZmanimItem("chatzot", "חצות היום", fmt.format(it), it,
                isHighlighted = true, highlightColor = Blue60,
                hasAlarm = "chatzot" in activeKeys)
        },
        zmanim.minchaGedola?.let {
            ZmanimItem("minchaGedola", "מנחה גדולה", fmt.format(it), it,
                hasAlarm = "minchaGedola" in activeKeys)
        },
        zmanim.minchaKetana?.let {
            ZmanimItem("minchaKetana", "מנחה קטנה", fmt.format(it), it,
                hasAlarm = "minchaKetana" in activeKeys)
        },
        zmanim.plagHamincha?.let {
            ZmanimItem("plagHamincha", "פלג המנחה", fmt.format(it), it,
                hasAlarm = "plagHamincha" in activeKeys)
        }
    )
}

private fun buildEveningZmanim(zmanim: ZmanimModel, activeKeys: Set<String>): List<ZmanimItem> {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return listOfNotNull(
        zmanim.candleLighting?.let {
            ZmanimItem("candleLighting", "הדלקת נרות", fmt.format(it), it,
                isHighlighted = true, highlightColor = Gold60,
                hasAlarm = "candleLighting" in activeKeys)
        },
        zmanim.sunset?.let {
            ZmanimItem("sunset", "שקיעת החמה", fmt.format(it), it,
                isHighlighted = true, highlightColor = HolidayRed.copy(0.8f),
                hasAlarm = "sunset" in activeKeys)
        },
        zmanim.tzaitHakochavim?.let {
            ZmanimItem("tzaitHakochavim", "צאת הכוכבים", fmt.format(it), it,
                hasAlarm = "tzaitHakochavim" in activeKeys)
        },
        zmanim.tzaitHakochavimRT?.let {
            ZmanimItem("tzaitHakochavimRT", "צאת הכוכבים (ר\"ת)", fmt.format(it), it,
                hasAlarm = "tzaitHakochavimRT" in activeKeys)
        }
    )
}
