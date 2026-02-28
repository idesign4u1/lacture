package com.jewish.calendar.ui.screens.tools

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import kotlin.math.*

private const val JERUSALEM_LAT = 31.7683
private const val JERUSALEM_LON = 35.2137
private const val ALIGNMENT_THRESHOLD = 5f // degrees

/** Calculates the bearing (degrees, 0=North clockwise) from [fromLat]/[fromLon] to Jerusalem. */
private fun calcBearingToJerusalem(fromLat: Double, fromLon: Double): Float {
    val lat1 = Math.toRadians(fromLat)
    val lat2 = Math.toRadians(JERUSALEM_LAT)
    val dLon = Math.toRadians(JERUSALEM_LON - fromLon)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "כלים נוספים",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
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
            JerusalemCompassCard()
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun JerusalemCompassCard() {
    val context = LocalContext.current

    // Compass state
    var azimuth by remember { mutableFloatStateOf(0f) }
    var jerusBearing by remember { mutableFloatStateOf(0f) }
    var isAligned by remember { mutableStateOf(false) }

    // Fetch last known location to compute bearing to Jerusalem
    LaunchedEffect(Unit) {
        try {
            val loc = LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
            if (loc != null) {
                jerusBearing = calcBearingToJerusalem(loc.latitude, loc.longitude)
            }
        } catch (_: Exception) { /* keep default Jerusalem bearing */ }
    }

    // Register accelerometer + magnetometer for compass heading
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelValues = remember { FloatArray(3) }
    val magnetValues = remember { FloatArray(3) }
    val rotationMatrix = remember { FloatArray(9) }
    val orientationAngles = remember { FloatArray(3) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> event.values.copyInto(accelValues)
                    Sensor.TYPE_MAGNETIC_FIELD -> event.values.copyInto(magnetValues)
                }
                if (SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magnetValues)) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    azimuth = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360) % 360)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            ?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Vibrate once when the phone aligns with Jerusalem
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    LaunchedEffect(azimuth) {
        val diff = abs(azimuth - jerusBearing).let { if (it > 180f) 360f - it else it }
        val nowAligned = diff < ALIGNMENT_THRESHOLD
        if (nowAligned && !isAligned) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(400)
            }
        }
        isAligned = nowAligned
    }

    // Diff angle for display (0–180)
    val diffAngle = abs(azimuth - jerusBearing).let { if (it > 180f) 360f - it else it }

    val alignedGreen = Color(0xFF2E7D32)
    val goldColor = Color(0xFFD4AF37)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAligned) alignedGreen.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "✡  מצפן לכיוון ירושלים  ✡",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Status
            if (isAligned) {
                Text(
                    text = "🙏  פונה לכיוון ירושלים!",
                    color = alignedGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            } else {
                Text(
                    text = "סובב את הטלפון לכיוון ירושלים",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(28.dp))

            // Compass dial + Jerusalem arrow
            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                // Compass rose: rotates so that geographic North stays at the top
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(-azimuth)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = min(size.width, size.height) / 2f * 0.92f

                    // Background circle
                    drawCircle(color = surfaceVariant.copy(alpha = 0.5f), radius = r, center = Offset(cx, cy))

                    // Outer ring
                    drawCircle(
                        color = if (isAligned) alignedGreen else Color(0xFFB0A090),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 5f)
                    )

                    // Degree tick marks (0=North at top, canvas 0=right → offset by -90°)
                    for (deg in 0..350 step 5) {
                        val rad = Math.toRadians((deg - 90).toDouble())
                        val isCardinal = deg % 90 == 0
                        val isMajor = deg % 30 == 0
                        val tickLen = when {
                            isCardinal -> r * 0.20f
                            isMajor -> r * 0.11f
                            else -> r * 0.05f
                        }
                        val ox = cx + cos(rad).toFloat() * r
                        val oy = cy + sin(rad).toFloat() * r
                        val ix = cx + cos(rad).toFloat() * (r - tickLen)
                        val iy = cy + sin(rad).toFloat() * (r - tickLen)
                        drawLine(
                            color = if (isCardinal) Color(0xFF6D4C41) else Color(0xFFBCAAA4),
                            start = Offset(ix, iy),
                            end = Offset(ox, oy),
                            strokeWidth = if (isCardinal) 4f else 1.5f
                        )
                    }

                    // North needle — red, pointing up inside the rotated canvas
                    val northRad = Math.toRadians(-90.0)
                    drawLine(
                        color = Color.Red,
                        start = Offset(cx, cy),
                        end = Offset(
                            cx + cos(northRad).toFloat() * r * 0.58f,
                            cy + sin(northRad).toFloat() * r * 0.58f
                        ),
                        strokeWidth = 7f
                    )
                    // South needle — gray
                    val southRad = Math.toRadians(90.0)
                    drawLine(
                        color = Color.Gray,
                        start = Offset(cx, cy),
                        end = Offset(
                            cx + cos(southRad).toFloat() * r * 0.42f,
                            cy + sin(southRad).toFloat() * r * 0.42f
                        ),
                        strokeWidth = 5f
                    )

                    // Center dot
                    drawCircle(color = Color(0xFF5D4037), radius = 10f, center = Offset(cx, cy))
                }

                // Jerusalem arrow: angle = bearing - azimuth (screen-relative direction to Jerusalem)
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "כיוון ירושלים",
                    tint = if (isAligned) alignedGreen else goldColor,
                    modifier = Modifier
                        .size(60.dp)
                        .rotate(jerusBearing - azimuth)
                )
            }

            Spacer(Modifier.height(22.dp))

            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompassInfoItem(label = "מצפן", value = "${azimuth.toInt()}°")
                CompassInfoItem(label = "לירושלים", value = "${jerusBearing.toInt()}°")
                CompassInfoItem(
                    label = "הפרש",
                    value = "${diffAngle.toInt()}°",
                    highlight = isAligned
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "החץ הזהב מצביע לכיוון ירושלים · N = צפון (אדום)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompassInfoItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = if (highlight) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
