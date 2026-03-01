package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.viewmodel.DailyInspirationViewModel

private val InspBg      = Color(0xFFFFFBF0)
private val InspGold    = Color(0xFFD4AF37)
private val InspDeep    = Color(0xFF5D4037)
private val InspPurple  = Color(0xFF4A148C)
private val InspMuted   = Color(0xFF8D6E63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyInspirationScreen(
    onBack: () -> Unit,
    viewModel: DailyInspirationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "חיזוק יומי",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = InspDeep
                        )
                        if (state.hebrewDate.isNotBlank()) {
                            Text(state.hebrewDate, fontSize = 12.sp, color = InspMuted)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = InspDeep)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "רענן", tint = InspGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InspBg)
            )
        },
        containerColor = InspBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Parasha banner
            if (state.parasha.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(InspGold.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    Text(
                        "פרשת השבוע: ${state.parasha}",
                        fontSize = 14.sp,
                        color = InspDeep,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            state.inspiration?.let { insp ->
                // Verse card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(InspGold.copy(alpha = 0.18f), InspPurple.copy(alpha = 0.08f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "✦",
                            fontSize = 24.sp,
                            color = InspGold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = insp.verse,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = InspDeep,
                            textAlign = TextAlign.Center,
                            lineHeight = 38.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = insp.source,
                            fontSize = 14.sp,
                            color = InspGold,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "✦",
                            fontSize = 24.sp,
                            color = InspGold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Teaching card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "מחשבה להעמקה",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = InspPurple,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = insp.teaching,
                            fontSize = 17.sp,
                            color = InspDeep,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "— ${insp.teacher}",
                            fontSize = 13.sp,
                            color = InspMuted,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Practice box
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "לעיון ותרגול",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = InspPurple,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "• קרא את הפסוק פעמיים בקול\n• חשוב: איך אוכל ליישם זאת היום?\n• שתף מישהו שאהוב עליך במחשבה הזו",
                            fontSize = 15.sp,
                            color = InspDeep,
                            lineHeight = 26.sp
                        )
                    }
                }
            }
        }
    }
}
