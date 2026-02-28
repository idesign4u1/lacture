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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import kotlin.math.*

// ── Palette ───────────────────────────────────────────────────────────

private val IsraelBlue    = Color(0xFF003E7E)
private val TempleGold    = Color(0xFFD4AF37)
private val Parchment     = Color(0xFFF5E6C8)
private val ParchmentDark = Color(0xFFD9C4A0)
private val AlignGreen    = Color(0xFF2E7D32)
private val NorthRed      = Color(0xFFCC0000)

// ── Jerusalem bearing ─────────────────────────────────────────────────

private const val JERUSALEM_LAT = 31.7683
private const val JERUSALEM_LON = 35.2137
private const val THRESHOLD_DEG = 5f

private fun calcBearing(fromLat: Double, fromLon: Double): Float {
    val lat1 = Math.toRadians(fromLat)
    val lat2 = Math.toRadians(JERUSALEM_LAT)
    val dLon = Math.toRadians(JERUSALEM_LON - fromLon)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
}

// ── Star of David drawing helper ──────────────────────────────────────

private fun DrawScope.drawStarOfDavid(center: Offset, r: Float, color: Color, sw: Float = 4f) {
    val h = r * (sqrt(3.0) / 2.0).toFloat()
    val style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Triangle pointing up
    drawPath(Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + h, center.y + r / 2f)
        lineTo(center.x - h, center.y + r / 2f)
        close()
    }, color, style = style)

    // Triangle pointing down
    drawPath(Path().apply {
        moveTo(center.x, center.y + r)
        lineTo(center.x + h, center.y - r / 2f)
        lineTo(center.x - h, center.y - r / 2f)
        close()
    }, color, style = style)
}

// ── Screen ────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var azimuth by remember { mutableFloatStateOf(0f) }
    var jerusBearing by remember { mutableFloatStateOf(0f) }
    var isAligned by remember { mutableStateOf(false) }

    // Fetch location for accurate bearing
    LaunchedEffect(Unit) {
        try {
            val loc = LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
            if (loc != null) jerusBearing = calcBearing(loc.latitude, loc.longitude)
        } catch (_: Exception) {}
    }

    // Sensor registration
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accel = remember { FloatArray(3) }
    val magnet = remember { FloatArray(3) }
    val rotMat = remember { FloatArray(9) }
    val orient = remember { FloatArray(3) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER  -> e.values.copyInto(accel)
                    Sensor.TYPE_MAGNETIC_FIELD -> e.values.copyInto(magnet)
                }
                if (SensorManager.getRotationMatrix(rotMat, null, accel, magnet)) {
                    SensorManager.getOrientation(rotMat, orient)
                    azimuth = ((Math.toDegrees(orient[0].toDouble()).toFloat() + 360) % 360)
                }
            }
            override fun onAccuracyChanged(s: Sensor, a: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            ?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Vibrate on alignment
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    LaunchedEffect(azimuth) {
        val diff = abs(azimuth - jerusBearing).let { if (it > 180f) 360f - it else it }
        val aligned = diff < THRESHOLD_DEG
        if (aligned && !isAligned) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION")
                vibrator.vibrate(500)
        }
        isAligned = aligned
    }

    val diff = abs(azimuth - jerusBearing).let { if (it > 180f) 360f - it else it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "✡ מצפן ירושלים ✡",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IsraelBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "חזור",
                            tint = IsraelBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status banner
            StatusBanner(isAligned = isAligned, diff = diff)

            Spacer(Modifier.height(24.dp))

            // Compass
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Layer 1: Compass rose — rotates so North stays at top of screen
                val nativeBlue = IsraelBlue.toArgb()
                Canvas(modifier = Modifier.fillMaxSize().rotate(-azimuth)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = min(size.width, size.height) / 2f * 0.92f

                    // Parchment background
                    drawCircle(color = Parchment, radius = r, center = Offset(cx, cy))

                    // Outer decorative ring (Israeli blue)
                    drawCircle(
                        color = IsraelBlue, radius = r, center = Offset(cx, cy),
                        style = Stroke(width = 10f)
                    )
                    // Inner gold ring
                    drawCircle(
                        color = TempleGold, radius = r * 0.87f, center = Offset(cx, cy),
                        style = Stroke(width = 4f)
                    )
                    // Second inner ring (thinner)
                    drawCircle(
                        color = ParchmentDark, radius = r * 0.82f, center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    // Tick marks (degree 0 = North → canvas -90°)
                    for (deg in 0..350 step 5) {
                        val rad = Math.toRadians((deg - 90).toDouble())
                        val isCard = deg % 90 == 0
                        val isMajor = deg % 45 == 0
                        val tickLen = when {
                            isCard  -> r * 0.22f
                            isMajor -> r * 0.13f
                            else    -> r * 0.06f
                        }
                        val rOuter = r
                        val rInner = r - tickLen
                        val ox = cx + cos(rad).toFloat() * rOuter
                        val oy = cy + sin(rad).toFloat() * rOuter
                        val ix = cx + cos(rad).toFloat() * rInner
                        val iy = cy + sin(rad).toFloat() * rInner
                        drawLine(
                            color = if (isCard) IsraelBlue else Color(0xFFBCAAA4),
                            start = Offset(ix, iy),
                            end = Offset(ox, oy),
                            strokeWidth = if (isCard) 5f else if (isMajor) 2.5f else 1.5f
                        )
                    }

                    // Small Stars of David at the 4 cardinal directions
                    val starRadius = r * 0.68f
                    listOf(0f, 90f, 180f, 270f).forEach { deg ->
                        val rad = Math.toRadians((deg - 90).toDouble())
                        val starCenter = Offset(
                            cx + cos(rad).toFloat() * starRadius,
                            cy + sin(rad).toFloat() * starRadius
                        )
                        drawStarOfDavid(starCenter, r = r * 0.07f, color = TempleGold, sw = 3f)
                    }

                    // Hebrew direction labels
                    drawIntoCanvas { canvas ->
                        val boldPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            textSize = r * 0.16f
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val lblR = r * 0.52f
                        val textBaseOffset = boldPaint.textSize * 0.36f

                        // North (צ) — red
                        boldPaint.color = NorthRed.toArgb()
                        canvas.nativeCanvas.drawText("צ", cx, cy - lblR + textBaseOffset, boldPaint)

                        // East (מז), South (ד), West (מע) — Israeli blue
                        boldPaint.color = nativeBlue
                        canvas.nativeCanvas.drawText("מז", cx + lblR, cy + textBaseOffset, boldPaint)
                        canvas.nativeCanvas.drawText("ד",  cx, cy + lblR + textBaseOffset, boldPaint)
                        canvas.nativeCanvas.drawText("מע", cx - lblR, cy + textBaseOffset, boldPaint)
                    }

                    // North needle (red, pointing up in canvas = toward degree 0)
                    val northRad = Math.toRadians(-90.0)
                    drawLine(
                        color = NorthRed,
                        start = Offset(cx, cy),
                        end = Offset(
                            cx + cos(northRad).toFloat() * r * 0.44f,
                            cy + sin(northRad).toFloat() * r * 0.44f
                        ),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    // South needle (dark gray)
                    val southRad = Math.toRadians(90.0)
                    drawLine(
                        color = Color(0xFF616161),
                        start = Offset(cx, cy),
                        end = Offset(
                            cx + cos(southRad).toFloat() * r * 0.30f,
                            cy + sin(southRad).toFloat() * r * 0.30f
                        ),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    // Center jewel (Star of David)
                    drawStarOfDavid(Offset(cx, cy), r = r * 0.08f, color = TempleGold, sw = 3.5f)
                }

                // Layer 2: Jerusalem direction indicator — does NOT rotate with the compass rose
                val jerusColor = if (isAligned) AlignGreen else TempleGold
                Canvas(modifier = Modifier.fillMaxSize().rotate(jerusBearing - azimuth)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = min(size.width, size.height) / 2f * 0.92f

                    // Dotted line toward Jerusalem
                    val tipY = cy - r * 0.70f
                    drawLine(
                        color = jerusColor,
                        start = Offset(cx, cy - r * 0.12f),
                        end = Offset(cx, tipY + r * 0.13f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(18f, 10f)
                        )
                    )
                    // Star of David marking Jerusalem
                    drawStarOfDavid(Offset(cx, tipY), r = r * 0.13f, color = jerusColor, sw = 5f)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Info chips
            Card(
                colors = CardDefaults.cardColors(containerColor = Parchment),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoChip("מצפן", "${azimuth.toInt()}°", IsraelBlue)
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    InfoChip("ירושלים", "${jerusBearing.toInt()}°", TempleGold)
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    InfoChip(
                        label = "הפרש",
                        value = "${diff.toInt()}°",
                        color = if (isAligned) AlignGreen else Color(0xFF795548)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "✡ הסמל הזהב מצביע לכיוון ירושלים · N = צ = צפון (אדום) ✡",
                style = MaterialTheme.typography.bodySmall,
                color = IsraelBlue.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(isAligned: Boolean, diff: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isAligned) AlignGreen else IsraelBlue,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAligned) "🙏  פונה לכיוון ירושלים!" else "סובב את הטלפון לכיוון ירושלים  ✡",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF795548))
    }
}
