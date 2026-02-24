package com.jewish.calendar.ui.screens.zmanim

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import android.Manifest
import com.jewish.calendar.model.ZmanimModel
import com.jewish.calendar.ui.theme.*
import com.jewish.calendar.viewmodel.ZmanimViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ZmanimScreen(
    viewModel: ZmanimViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

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
                    IconButton(onClick = { viewModel.refreshZmanim() }) {
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
        }
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
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                uiState.zmanim?.let { zmanim ->
                    // Location header
                    LocationHeader(zmanim)

                    // Shabbat / Yom Tov special bar
                    if (zmanim.isShabbat || zmanim.isYomTov) {
                        ShabbatBar(zmanim)
                    }
                    if (zmanim.isErevShabbat) {
                        ErevShabbatBar(zmanim)
                    }

                    // Zmanim sections
                    ZmanimSection(
                        title = "בוקר",
                        icon = Icons.Default.WbSunny,
                        color = Gold60,
                        items = buildMorningZmanim(zmanim)
                    )
                    ZmanimSection(
                        title = "צהריים",
                        icon = Icons.Default.LightMode,
                        color = MaterialTheme.colorScheme.primary,
                        items = buildAfternoonZmanim(zmanim)
                    )
                    ZmanimSection(
                        title = "ערב",
                        icon = Icons.Default.NightsStay,
                        color = ShabbatBlue,
                        items = buildEveningZmanim(zmanim)
                    )

                    // Location note
                    if (!uiState.hasLocationPermission) {
                        LocationNote { locationPermission.launchPermissionRequest() }
                    }
                }

                uiState.error?.let { error ->
                    ErrorCard(error)
                }
            }
        }
    }
}

@Composable
private fun LocationHeader(zmanim: ZmanimModel) {
    val dateFormatter = SimpleDateFormat("EEEE, d בMMMM", Locale("he"))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
                    fontWeight = FontWeight.Bold,
                    color = Gold80,
                    fontSize = 16.sp
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
    items: List<ZmanimItem>
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    ZmanimRow(item = item)
                    if (index < items.lastIndex) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

data class ZmanimItem(
    val label: String,
    val time: String,
    val isHighlighted: Boolean = false,
    val highlightColor: Color = Color.Unspecified
)

@Composable
private fun ZmanimRow(item: ZmanimItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (item.isHighlighted) item.highlightColor.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time (LTR direction)
        Text(
            text = item.time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (item.isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (item.isHighlighted) item.highlightColor else MaterialTheme.colorScheme.onSurface,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        // Label (RTL direction)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onRequestPermission() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = HolidayRed.copy(alpha = 0.1f))
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(16.dp),
            color = HolidayRed,
            textAlign = TextAlign.Center
        )
    }
}

// Helper functions to build zmanim items
private fun buildMorningZmanim(zmanim: ZmanimModel): List<ZmanimItem> {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return listOfNotNull(
        zmanim.alotHashachar?.let { ZmanimItem("עלות השחר", fmt.format(it)) },
        zmanim.sunrise?.let { ZmanimItem("הנץ החמה", fmt.format(it), isHighlighted = true, highlightColor = Gold60) },
        zmanim.sofZmanKriatShmaGRA?.let { ZmanimItem("סוף ז\"ק שמע (גר\"א)", fmt.format(it)) },
        zmanim.sofZmanKriatShmaMGA?.let { ZmanimItem("סוף ז\"ק שמע (מג\"א)", fmt.format(it)) },
        zmanim.sofZmanTfilaGRA?.let { ZmanimItem("סוף זמן תפילה (גר\"א)", fmt.format(it)) },
        zmanim.sofZmanTfilaMGA?.let { ZmanimItem("סוף זמן תפילה (מג\"א)", fmt.format(it)) }
    )
}

private fun buildAfternoonZmanim(zmanim: ZmanimModel): List<ZmanimItem> {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return listOfNotNull(
        zmanim.chatzot?.let { ZmanimItem("חצות היום", fmt.format(it), isHighlighted = true, highlightColor = Blue60) },
        zmanim.minchaGedola?.let { ZmanimItem("מנחה גדולה", fmt.format(it)) },
        zmanim.minchaKetana?.let { ZmanimItem("מנחה קטנה", fmt.format(it)) },
        zmanim.plagHamincha?.let { ZmanimItem("פלג המנחה", fmt.format(it)) }
    )
}

private fun buildEveningZmanim(zmanim: ZmanimModel): List<ZmanimItem> {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return listOfNotNull(
        zmanim.candleLighting?.let {
            ZmanimItem("הדלקת נרות", fmt.format(it), isHighlighted = true, highlightColor = Gold60)
        },
        zmanim.sunset?.let { ZmanimItem("שקיעת החמה", fmt.format(it), isHighlighted = true, highlightColor = HolidayRed.copy(0.8f)) },
        zmanim.tzaitHakochavim?.let { ZmanimItem("צאת הכוכבים", fmt.format(it)) },
        zmanim.tzaitHakochavimRT?.let { ZmanimItem("צאת הכוכבים (ר\"ת)", fmt.format(it)) }
    )
}
