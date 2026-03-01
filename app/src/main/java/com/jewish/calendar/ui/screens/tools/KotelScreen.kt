package com.jewish.calendar.ui.screens.tools

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

// ── Palette ───────────────────────────────────────────────────────────

private val IsraelBlue  = Color(0xFF003E7E)
private val TempleGold  = Color(0xFFD4AF37)
private val Parchment   = Color(0xFFF5E6C8)
private val LiveRed     = Color(0xFFE53935)
private val DarkBg      = Color(0xFF080F1C)
private val DarkSurface = Color(0xFF0F1E35)

// ── YouTube stream definitions ────────────────────────────────────────

private data class KotelStream(
    val nameHe: String,
    val descHe: String,
    val videoId: String,
    val emoji: String,
    val gradStart: Color,
    val gradEnd: Color
) {
    val embedUrl: String get() =
        "https://www.youtube.com/embed/$videoId" +
        "?autoplay=1&controls=1&rel=0&modestbranding=1&playsinline=1"
}

private val STREAMS = listOf(
    KotelStream(
        nameHe    = "כיכר הכותל",
        descHe    = "שידור חי – מבט כללי על כיכר הכותל",
        videoId   = "yAl9L9mvDzY",
        emoji     = "🕍",
        gradStart = Color(0xFF0D47A1),
        gradEnd   = Color(0xFF1976D2)
    ),
    KotelStream(
        nameHe    = "אזור התפילה",
        descHe    = "מבט ישיר על הכותל המערבי",
        videoId   = "QFK1-XaBnlg",
        emoji     = "🙏",
        gradStart = Color(0xFF4A0080),
        gradEnd   = Color(0xFF7B1FA2)
    ),
    KotelStream(
        nameHe    = "זווית שלישית",
        descHe    = "שידור נוסף – פנורמה מהכותל",
        videoId   = "77akujLn4k8",
        emoji     = "✡️",
        gradStart = Color(0xFF1B5E20),
        gradEnd   = Color(0xFF388E3C)
    ),
)

// ── Screen ────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotelScreen(onBack: () -> Unit) {

    var selectedIdx by remember { mutableIntStateOf(0) }
    var isLoading   by remember { mutableStateOf(true) }
    var hasError    by remember { mutableStateOf(false) }
    var webViewRef  by remember { mutableStateOf<WebView?>(null) }
    val listState   = rememberLazyListState()

    // ── Jerusalem live clock ──────────────────────────────────────────
    var clockTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"))
            val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            val m = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
            val s = cal.get(Calendar.SECOND).toString().padStart(2, '0')
            clockTime = "$h:$m:$s"
            delay(1000)
        }
    }

    // ── Animations ────────────────────────────────────────────────────
    val liveTransition = rememberInfiniteTransition(label = "live")
    val liveAlpha by liveTransition.animateFloat(
        initialValue = 1f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "alpha"
    )
    val liveScale by liveTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "scale"
    )
    val shimmerOffset by liveTransition.animateFloat(
        initialValue = -300f, targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "shimmer"
    )

    // ── Stream switch ─────────────────────────────────────────────────
    LaunchedEffect(selectedIdx) {
        isLoading = true
        hasError  = false
        webViewRef?.loadUrl(STREAMS[selectedIdx].embedUrl)
        listState.animateScrollToItem(selectedIdx)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .scale(liveScale)
                                    .background(LiveRed.copy(alpha = liveAlpha), CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "● LIVE",
                                color = LiveRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 3.sp
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "הכותל המערבי",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = IsraelBlue
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕑 ירושלים  ", color = TempleGold, fontSize = 11.sp)
                            Text(
                                clockTime,
                                color = TempleGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        hasError  = false
                        webViewRef?.loadUrl(STREAMS[selectedIdx].embedUrl)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "רענן", tint = TempleGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Now watching bar ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(STREAMS[selectedIdx].emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            STREAMS[selectedIdx].nameHe,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            STREAMS[selectedIdx].descHe,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
                // Shimmer gold badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(TempleGold, Color(0xFFFFF176), TempleGold),
                                start = androidx.compose.ui.geometry.Offset(shimmerOffset - 100f, 0f),
                                end   = androidx.compose.ui.geometry.Offset(shimmerOffset + 100f, 0f)
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "▶ יוטיוב חי",
                        color = Color(0xFF3E2000),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }

            // ── YouTube WebView ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkBg)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled                = true
                                domStorageEnabled                = true
                                mediaPlaybackRequiresUserGesture = false
                                loadWithOverviewMode             = true
                                useWideViewPort                  = true
                                setSupportZoom(false)
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasError  = true
                                    }
                                }
                            }
                            webChromeClient = WebChromeClient()
                            loadUrl(STREAMS[0].embedUrl)
                        }.also { webViewRef = it }
                    },
                    update = { webViewRef = it },
                    modifier = Modifier.fillMaxSize()
                )

                // Loading overlay
                if (isLoading && !hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(STREAMS[selectedIdx].emoji, fontSize = 52.sp)
                            Spacer(Modifier.height(20.dp))
                            CircularProgressIndicator(
                                color = TempleGold,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "מתחבר לשידור יוטיוב...",
                                color = TempleGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                STREAMS[selectedIdx].nameHe,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Error overlay
                if (hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("⚠️", fontSize = 44.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "השידור אינו זמין כעת",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "נסה שידור אחר מהרשימה למטה",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = {
                                    isLoading = true
                                    hasError  = false
                                    webViewRef?.loadUrl(STREAMS[selectedIdx].embedUrl)
                                },
                                border = BorderStroke(1.5.dp, TempleGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TempleGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("נסה שוב", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Stream selector ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(top = 10.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "▼  בחר שידור",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "${selectedIdx + 1} / ${STREAMS.size}",
                        color = TempleGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(STREAMS) { idx, stream ->
                        StreamCard(
                            stream     = stream,
                            isSelected = idx == selectedIdx,
                            onClick    = { selectedIdx = idx }
                        )
                    }
                }
            }

            // ── Gold divider footer ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                TempleGold,
                                Color(0xFFFFF176),
                                TempleGold,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ── Stream card ───────────────────────────────────────────────────────

@Composable
private fun StreamCard(
    stream: KotelStream,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    val shadowElevation by animateFloatAsState(
        targetValue = if (isSelected) 16f else 2f,
        label = "elevation"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .width(150.dp)
            .height(100.dp)
            .shadow(shadowElevation.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(listOf(stream.gradStart, stream.gradEnd))
            )
            .then(
                if (isSelected)
                    Modifier.border(2.dp, TempleGold, RoundedCornerShape(18.dp))
                else
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color.White.copy(0.10f), Color.Transparent))
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(stream.emoji, fontSize = 22.sp)
                if (isSelected) {
                    // Active dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF69F0AE), CircleShape)
                    )
                }
            }

            Column {
                Text(
                    stream.nameHe,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stream.descHe,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
