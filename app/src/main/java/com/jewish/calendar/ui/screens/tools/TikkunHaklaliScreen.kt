package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.viewmodel.TikkunHaklaliViewModel
import com.jewish.calendar.viewmodel.TikkunPsalm

// ── Palette ────────────────────────────────────────────────────────────

private val TikkunBg       = Color(0xFFF5F0FF)   // light lavender
private val TikkunBgDark   = Color(0xFFE8DFFF)
private val TikkunPurple   = Color(0xFF6A1B9A)
private val TikkunGold     = Color(0xFFD4AF37)
private val TikkunInk      = Color(0xFF2C1810)
private val TikkunMuted    = Color(0xFF7B6B8A)
private val TikkunGreen    = Color(0xFF2E7D32)
private val TikkunCardDone = Color(0xFFEDF7ED)
private val TikkunCard     = Color(0xFFFAF7FF)

// ── Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikkunHaklaliScreen(
    onBack: () -> Unit,
    viewModel: TikkunHaklaliViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "תיקון הכללי",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = TikkunPurple
                        )
                        Text(
                            "רבי נחמן מברסלב",
                            fontSize = 12.sp,
                            color = TikkunMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (state.selected != null) viewModel::clearSelection else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = TikkunPurple)
                    }
                },
                actions = {
                    if (state.selected != null) {
                        IconButton(onClick = { viewModel.decreaseFontSize() }) {
                            Icon(Icons.Default.Remove, "הקטן", tint = TikkunPurple)
                        }
                        Text(
                            "${state.fontSize}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TikkunInk,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        IconButton(onClick = { viewModel.increaseFontSize() }) {
                            Icon(Icons.Default.Add, "הגדל", tint = TikkunPurple)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TikkunBgDark)
            )
        },
        containerColor = TikkunBg
    ) { padding ->
        if (state.selected == null) {
            PsalmList(
                psalms      = viewModel.psalms,
                completedIds = state.completedIds,
                onSelect    = { viewModel.selectPsalm(it) },
                modifier    = Modifier.padding(padding)
            )
        } else {
            PsalmTextView(
                psalm     = state.selected!!,
                text      = state.text,
                isLoading = state.isLoading,
                fontSize  = state.fontSize,
                isDone    = state.selected!!.number in state.completedIds,
                onMarkDone = { viewModel.markCompleted(state.selected!!.number) },
                modifier  = Modifier.padding(padding)
            )
        }
    }
}

// ── Psalm list ─────────────────────────────────────────────────────────

@Composable
private fun PsalmList(
    psalms: List<TikkunPsalm>,
    completedIds: Set<Int>,
    onSelect: (TikkunPsalm) -> Unit,
    modifier: Modifier = Modifier
) {
    val doneCount = completedIds.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Progress header
        item {
            ProgressHeader(done = doneCount, total = psalms.size)
            Spacer(Modifier.height(4.dp))
        }

        items(psalms) { psalm ->
            PsalmCard(
                psalm    = psalm,
                isDone   = psalm.number in completedIds,
                onClick  = { onSelect(psalm) }
            )
        }
    }
}

@Composable
private fun ProgressHeader(done: Int, total: Int) {
    val fraction = if (total > 0) done.toFloat() / total else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(TikkunPurple.copy(alpha = 0.12f), TikkunGold.copy(alpha = 0.1f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "עשרה מזמורים",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TikkunPurple
            )
            Text(
                "$done / $total",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (done == total) TikkunGreen else TikkunPurple
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color    = if (done == total) TikkunGreen else TikkunPurple,
            trackColor = TikkunPurple.copy(alpha = 0.15f)
        )
        if (done == total) {
            Spacer(Modifier.height(6.dp))
            Text(
                "תיקון הכללי הושלם! ✓",
                fontSize = 13.sp,
                color = TikkunGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PsalmCard(psalm: TikkunPsalm, isDone: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isDone) 0.dp else 3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDone) TikkunCardDone else TikkunCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Psalm number circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDone) TikkunGreen.copy(alpha = 0.15f)
                        else TikkunPurple.copy(alpha = 0.12f)
                    )
                    .border(
                        1.5.dp,
                        if (isDone) TikkunGreen.copy(alpha = 0.5f) else TikkunGold.copy(alpha = 0.6f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, null, tint = TikkunGreen, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        psalm.hebrewNumber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TikkunPurple
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "תהילים ${psalm.hebrewNumber} (${psalm.number})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) TikkunGreen else TikkunInk
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    psalm.openingLine,
                    fontSize = 13.sp,
                    color = TikkunMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Psalm text view ────────────────────────────────────────────────────

@Composable
private fun PsalmTextView(
    psalm: TikkunPsalm,
    text: String,
    isLoading: Boolean,
    fontSize: Int,
    isDone: Boolean,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Psalm header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(TikkunPurple.copy(alpha = 0.1f), TikkunGold.copy(alpha = 0.06f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    "תהילים ${psalm.hebrewNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TikkunPurple
                )
                Text(psalm.openingLine, fontSize = 13.sp, color = TikkunMuted)
            }
        }

        HorizontalDivider(color = TikkunGold.copy(alpha = 0.3f))

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TikkunPurple)
                        Spacer(Modifier.height(12.dp))
                        Text("טוען מזמור...", color = TikkunMuted, fontSize = 14.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 18.dp)
                ) {
                    item {
                        Text(
                            text = text,
                            fontSize = fontSize.sp,
                            color = TikkunInk,
                            lineHeight = (fontSize * 1.9f).sp,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(32.dp))
                        Text(
                            "⸻ סוף ⸻",
                            fontSize = 14.sp,
                            color = TikkunGold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Mark done button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TikkunBgDark)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = TikkunGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("המזמור הושלם", color = TikkunGreen, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = onMarkDone,
                            colors = ButtonDefaults.buttonColors(containerColor = TikkunPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("סיימתי לקרוא ✓", fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
