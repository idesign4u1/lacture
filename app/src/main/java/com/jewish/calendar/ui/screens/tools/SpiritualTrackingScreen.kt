package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.viewmodel.SpiritualTrackingViewModel
import com.jewish.calendar.viewmodel.TrackingItem

private val StBg    = Color(0xFFF0F7FF)
private val StBlue  = Color(0xFF1A237E)
private val StGold  = Color(0xFFD4AF37)
private val StGreen = Color(0xFF2E7D32)
private val StMuted = Color(0xFF5C6BC0)
private val StInk   = Color(0xFF0D1B3E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiritualTrackingScreen(
    onBack: () -> Unit,
    viewModel: SpiritualTrackingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var resetKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("מעקב רוחני", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = StBlue)
                        Text("החודש הנוכחי", fontSize = 12.sp, color = StMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = StBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StBg)
            )
        },
        containerColor = StBg
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Motivational banner
                val totalDone = state.items.sumOf { it.count }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(StBlue.copy(0.12f), StGold.copy(0.1f)))
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✡", fontSize = 32.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "סה\"כ: $totalDone רגעים של קשר עם הקב\"ה",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StBlue,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "\"עֹבֶד אֱלֹהִים לֹא יַעֲבֹד מִפַּחַד אֶלָּא מֵאַהֲבָה\"",
                            fontSize = 12.sp,
                            color = StMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                state.items.forEach { item ->
                    TrackingCard(
                        item = item,
                        onIncrement = { viewModel.increment(item.key) },
                        onDecrement = { viewModel.decrement(item.key) },
                        onResetRequest = { resetKey = item.key }
                    )
                }
            }
        }
    }

    if (resetKey != null) {
        val item = state.items.find { it.key == resetKey }
        AlertDialog(
            onDismissRequest = { resetKey = null },
            title = { Text("איפוס ${item?.label ?: ""}", fontWeight = FontWeight.Bold) },
            text = { Text("האם לאפס את המונה ל-0?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset(resetKey!!)
                    resetKey = null
                }) {
                    Text("אפס", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetKey = null }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
private fun TrackingCard(
    item: TrackingItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onResetRequest: () -> Unit
) {
    val fraction = if (item.goal > 0) (item.count.toFloat() / item.goal).coerceIn(0f, 1f) else 0f
    val isComplete = item.count >= item.goal
    val cardColor = if (isComplete) Color(0xFFEDF7ED) else Color.White

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(if (isComplete) 0.dp else 3.dp),
        colors = CardDefaults.elevatedCardColors(cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            item.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) StGreen else StInk
                        )
                        Text(
                            "יעד: ${item.goal} פעמים",
                            fontSize = 12.sp,
                            color = StMuted
                        )
                    }
                }
                // Count display
                Text(
                    "${item.count}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isComplete) StGreen else StBlue
                )
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isComplete) StGreen else StBlue,
                trackColor = StBlue.copy(0.12f)
            )

            Text(
                "${item.count} / ${item.goal}  (${(fraction * 100).toInt()}%)",
                fontSize = 12.sp,
                color = StMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.End
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onResetRequest, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("אפס", fontSize = 12.sp, color = StMuted)
                }
                Row {
                    OutlinedIconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Remove, "הפחת", modifier = Modifier.size(16.dp), tint = StBlue)
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = StBlue)
                    ) {
                        Icon(Icons.Default.Add, "הוסף", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
