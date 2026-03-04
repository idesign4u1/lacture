package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.jewish.calendar.data.BlessingContent
import com.jewish.calendar.viewmodel.BlessingsViewModel
import java.util.Calendar

private val BlBg    = Color(0xFFFFFBF0)
private val BlGold  = Color(0xFFD4AF37)
private val BlBlue  = Color(0xFF1565C0)
private val BlDeep  = Color(0xFF1A2340)
private val BlMuted = Color(0xFF6B7A9E)

// Legacy alias kept so the rest of the screen code compiles unchanged
private typealias BlessingItem = BlessingContent

private fun getDailyBlessings(all: List<BlessingContent>): List<BlessingContent> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 10 -> all.take(6)
        hour < 14 -> all.filter { it.name in listOf("המוציא לחם", "בורא פרי העץ", "בורא פרי האדמה", "שהכל נהיה בדברו", "מזונות") }
        else -> all.drop(6)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlessingsScreen(
    onBack: () -> Unit,
    viewModel: BlessingsViewModel = hiltViewModel()
) {
    val allBlessings by viewModel.blessings.collectAsState()
    var selected by remember { mutableStateOf<BlessingContent?>(null) }
    var showAll by remember { mutableStateOf(false) }

    val displayList = if (showAll) allBlessings else getDailyBlessings(allBlessings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ברכות יומיות", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = BlBlue)
                        Text(if (showAll) "כל הברכות" else "מומלץ לשעה זו", fontSize = 12.sp, color = BlMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (selected != null) ({ selected = null }) else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = BlBlue)
                    }
                },
                actions = {
                    if (selected == null) {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(if (showAll) "מומלצות" else "הכל", color = BlGold, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlBg)
            )
        },
        containerColor = BlBg
    ) { padding ->
        if (selected == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(BlGold.copy(0.2f), BlBlue.copy(0.08f))))
                            .padding(16.dp)
                    ) {
                        Text(
                            "\"בָּרְכוּ אֶת ה׳ הַמְבֹרָךְ\"\nמאה ברכות ביום — מצווה חשובה",
                            fontSize = 14.sp,
                            color = BlDeep,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                items(displayList) { blessing ->
                    ElevatedCard(
                        onClick = { selected = blessing },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.elevatedCardElevation(2.dp),
                        colors = CardDefaults.elevatedCardColors(Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(blessing.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(blessing.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlBlue)
                                Text(blessing.trigger, fontSize = 13.sp, color = BlMuted)
                                if (blessing.note.isNotBlank()) {
                                    Text(blessing.note, fontSize = 11.sp, color = BlGold, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(BlGold.copy(0.2f), BlBlue.copy(0.08f))))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(selected!!.emoji, fontSize = 44.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(selected!!.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlBlue, textAlign = TextAlign.Center)
                        Text(selected!!.trigger, fontSize = 14.sp, color = BlMuted, textAlign = TextAlign.Center)
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(3.dp),
                    colors = CardDefaults.elevatedCardColors(Color.White)
                ) {
                    Text(
                        selected!!.blessing,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlDeep,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp).fillMaxWidth()
                    )
                }

                if (selected!!.note.isNotBlank()) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.elevatedCardElevation(1.dp),
                        colors = CardDefaults.elevatedCardColors(Color(0xFFFFF8E1))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(selected!!.note, fontSize = 14.sp, color = BlDeep)
                        }
                    }
                }
            }
        }
    }
}
