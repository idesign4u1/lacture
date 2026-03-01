package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.jewish.calendar.viewmodel.PsalmEntry
import com.jewish.calendar.viewmodel.PsalmsTab
import com.jewish.calendar.viewmodel.PsalmsViewModel

private val PsBg      = Color(0xFFF8F5FF)
private val PsBgDark  = Color(0xFFEDE7FF)
private val PsPurple  = Color(0xFF4527A0)
private val PsGold    = Color(0xFFD4AF37)
private val PsInk     = Color(0xFF1A1040)
private val PsMuted   = Color(0xFF7C6B9E)
private val PsGreen   = Color(0xFF2E7D32)
private val PsCard    = Color(0xFFFAF8FF)
private val PsDone    = Color(0xFFEDF7ED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalmsScreen(
    onBack: () -> Unit,
    viewModel: PsalmsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "תהילים",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = PsPurple
                        )
                        Text(
                            "${state.readSet.size} / 150 נקראו",
                            fontSize = 12.sp,
                            color = PsMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (state.selectedPsalm != null) viewModel::clearPsalm else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = PsPurple)
                    }
                },
                actions = {
                    if (state.selectedPsalm != null) {
                        IconButton(onClick = { viewModel.decreaseFontSize() }) {
                            Icon(Icons.Default.Remove, "הקטן", tint = PsPurple)
                        }
                        Text(
                            "${state.fontSize}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PsInk,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        IconButton(onClick = { viewModel.increaseFontSize() }) {
                            Icon(Icons.Default.Add, "הגדל", tint = PsPurple)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PsBgDark)
            )
        },
        containerColor = PsBg
    ) { padding ->
        if (state.selectedPsalm == null) {
            PsalmListView(
                psalms       = viewModel.psalmsByTab(state.tab),
                readSet      = state.readSet,
                selectedTab  = state.tab,
                onTabSelect  = { viewModel.selectTab(it) },
                onSelect     = { viewModel.selectPsalm(it) },
                modifier     = Modifier.padding(padding)
            )
        } else {
            PsalmReadView(
                psalm    = state.selectedPsalm!!,
                text     = state.text,
                isLoading = state.isLoading,
                fontSize  = state.fontSize,
                isRead    = state.selectedPsalm!!.number in state.readSet,
                onMarkRead = { viewModel.markRead(state.selectedPsalm!!.number) },
                modifier   = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun PsalmListView(
    psalms: List<PsalmEntry>,
    readSet: Set<Int>,
    selectedTab: PsalmsTab,
    onTabSelect: (PsalmsTab) -> Unit,
    onSelect: (PsalmEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Progress
        item {
            LinearProgressIndicator(
                progress = { if (150 > 0) readSet.size.toFloat() / 150f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = PsPurple,
                trackColor = PsPurple.copy(0.15f)
            )
            Spacer(Modifier.height(12.dp))
        }

        // Tabs
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PsalmsTab.entries) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick  = { onTabSelect(tab) },
                        label    = { Text(tab.label, fontSize = 13.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PsPurple,
                            selectedLabelColor     = Color.White
                        ),
                        shape = CircleShape
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        items(psalms) { psalm ->
            val isDone = psalm.number in readSet
            ElevatedCard(
                onClick  = { onSelect(psalm) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                elevation = CardDefaults.elevatedCardElevation(if (isDone) 0.dp else 2.dp),
                colors   = CardDefaults.elevatedCardColors(if (isDone) PsDone else PsCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Number circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDone) PsGreen.copy(0.15f)
                                else PsPurple.copy(0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.Check, null, tint = PsGreen, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                "${psalm.number}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PsPurple
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            psalm.heTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) PsGreen else PsInk
                        )
                        Text(
                            psalm.opening,
                            fontSize = 12.sp,
                            color = PsMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PsalmReadView(
    psalm: PsalmEntry,
    text: String,
    isLoading: Boolean,
    fontSize: Int,
    isRead: Boolean,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(PsPurple.copy(0.1f), PsGold.copy(0.06f)))
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    psalm.heTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PsPurple
                )
                Text(psalm.opening, fontSize = 13.sp, color = PsMuted)
            }
        }
        HorizontalDivider(color = PsGold.copy(0.3f))

        when {
            isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PsPurple)
                    Spacer(Modifier.height(12.dp))
                    Text("טוען מזמור...", color = PsMuted, fontSize = 14.sp)
                }
            }
            else -> LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 18.dp)
            ) {
                item {
                    Text(
                        text = text,
                        fontSize = fontSize.sp,
                        color = PsInk,
                        lineHeight = (fontSize * 1.9f).sp,
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "⸻ סוף המזמור ⸻",
                        fontSize = 14.sp,
                        color = PsGold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PsBgDark)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRead) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = PsGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("המזמור נקרא", color = PsGreen, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onMarkRead,
                    colors = ButtonDefaults.buttonColors(containerColor = PsPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("סיימתי לקרוא ✓", fontSize = 15.sp)
                }
            }
        }
    }
}
