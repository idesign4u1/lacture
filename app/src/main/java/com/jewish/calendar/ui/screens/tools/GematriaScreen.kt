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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.viewmodel.GematriaViewModel
import com.jewish.calendar.viewmodel.TorahMatch

// ── Palette ────────────────────────────────────────────────────────────

private val NavyDeep    = Color(0xFF001233)
private val NavyMid     = Color(0xFF001F5B)
private val NavyLight   = Color(0xFF002A7A)
private val GoldBright  = Color(0xFFD4AF37)
private val GoldSoft    = Color(0xFFF0D060)
private val WhiteAlpha  = Color(0xCCFFFFFF)
private val CardBg      = Color(0xFF0A2060)
private val CardBorder  = Color(0xFF1A3A8A)
private val MutedText   = Color(0xFF8AABDD)

// ── Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GematriaScreen(
    onBack: () -> Unit,
    viewModel: GematriaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NavyDeep, NavyMid, NavyLight))
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "מחשבון גימטרייה",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = GoldBright
                            )
                            Text(
                                "חיפוש בתורה לפי ערך",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = if (state.selectedMatch != null) viewModel::clearMatch else onBack
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "חזור",
                                tint = GoldBright
                            )
                        }
                    },
                    actions = {
                        if (state.selectedMatch != null) {
                            IconButton(onClick = { viewModel.decreaseFontSize() }) {
                                Icon(Icons.Default.Remove, "הקטן", tint = GoldBright)
                            }
                            Text(
                                "${state.fontSize}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldSoft,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                            IconButton(onClick = { viewModel.increaseFontSize() }) {
                                Icon(Icons.Default.Add, "הגדל", tint = GoldBright)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (state.selectedMatch != null) {
                VerseView(
                    match     = state.selectedMatch!!,
                    text      = state.verseText,
                    isLoading = state.isLoadingVerse,
                    fontSize  = state.fontSize,
                    modifier  = Modifier.padding(padding)
                )
            } else {
                LazyColumn(
                    state   = rememberLazyListState(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Input card
                    item {
                        Spacer(Modifier.height(4.dp))
                        InputCard(
                            input      = state.input,
                            value      = state.gematriaValue,
                            breakdown  = state.letterBreakdown,
                            onInput    = { viewModel.onInputChange(it) },
                            onClear    = { viewModel.onInputChange("") }
                        )
                    }

                    // Indexing banner
                    if (state.isIndexing) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldBright.copy(alpha = 0.08f))
                                    .border(1.dp, GoldBright.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    "סורק את התורה...",
                                    fontSize = 13.sp,
                                    color = GoldSoft
                                )
                                Spacer(Modifier.width(10.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = GoldBright,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    // Results header
                    if (state.gematriaValue != null) {
                        item {
                            val count = state.matches.size
                            Text(
                                text = when {
                                    state.isIndexing && count == 0 -> "ממתין לסיום סריקת התורה..."
                                    count > 0 -> "נמצאו $count התאמות בתורה"
                                    else -> "לא נמצאו התאמות בתורה"
                                },
                                fontSize = 14.sp,
                                color = if (count > 0) GoldSoft else MutedText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Match cards
                        items(state.matches) { match ->
                            MatchCard(match = match, onClick = { viewModel.selectMatch(match) })
                        }
                    } else if (state.input.isBlank()) {
                        item { HintCard() }
                    }
                }
            }
        }
    }
}

// ── Input card ─────────────────────────────────────────────────────────

@Composable
private fun InputCard(
    input: String,
    value: Int?,
    breakdown: String,
    onInput: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        // Hebrew input field
        OutlinedTextField(
            value         = input,
            onValueChange = onInput,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = {
                Text(
                    "הקלד מילה או משפט בעברית...",
                    color = MutedText,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            textStyle = TextStyle(
                fontSize  = 24.sp,
                color     = Color.White,
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Medium
            ),
            trailingIcon = {
                if (input.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, "נקה", tint = MutedText)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GoldBright,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Gematria value display
        if (value != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GoldBright.copy(alpha = 0.12f))
                    .border(1.dp, GoldBright.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "גִּימַטְרִיָּה",
                        fontSize = 13.sp,
                        color = GoldSoft,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "$value",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldBright
                    )
                }
            }

            // Letter breakdown
            if (breakdown.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = breakdown,
                    fontSize  = 13.sp,
                    color     = MutedText,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Hint card (empty state) ────────────────────────────────────────────

@Composable
private fun HintCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg.copy(alpha = 0.6f))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔢", fontSize = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "הקלד מילה או משפט בעברית",
            fontSize = 16.sp,
            color = WhiteAlpha,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "המחשבון יחשב את ערך הגימטרייה ויחפש\nמילים ומשפטים בעלי אותו ערך בתורה",
            fontSize = 13.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = CardBorder)
        Spacer(Modifier.height(14.dp))
        // Examples
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ExampleChip("אַהֲבָה")
            ExampleChip("יִשְׂרָאֵל")
            ExampleChip("תּוֹרָה")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "דוגמאות לחיפוש",
            fontSize = 11.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExampleChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GoldBright.copy(alpha = 0.12f))
            .border(1.dp, GoldBright.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 15.sp, color = GoldSoft)
    }
}

// ── Match card ─────────────────────────────────────────────────────────

@Composable
private fun MatchCard(match: TorahMatch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gold circle with gematria value
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(GoldBright.copy(alpha = 0.14f))
                .border(1.5.dp, GoldBright.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${match.gematria}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GoldBright
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Hebrew word
            Text(
                text       = match.word,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.height(2.dp))
            // Source
            Text(
                text     = match.sourceHe,
                fontSize = 12.sp,
                color    = GoldSoft
            )
            Spacer(Modifier.height(3.dp))
            // Context snippet
            Text(
                text     = match.context,
                fontSize = 12.sp,
                color    = MutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))
        Text("←", fontSize = 16.sp, color = GoldBright)
    }
}

// ── Verse view ─────────────────────────────────────────────────────────

@Composable
private fun VerseView(
    match: TorahMatch,
    text: String,
    isLoading: Boolean,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoldBright.copy(alpha = 0.10f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = match.word,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = GoldBright,
                    modifier   = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GoldBright.copy(alpha = 0.15f))
                        .border(1.dp, GoldBright.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${match.gematria}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldBright)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(match.sourceHe, fontSize = 13.sp, color = GoldSoft)
        }

        HorizontalDivider(color = GoldBright.copy(alpha = 0.2f))

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldBright)
                        Spacer(Modifier.height(12.dp))
                        Text("טוען פסוק...", color = MutedText, fontSize = 14.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    item {
                        Text(
                            text       = text,
                            fontSize   = fontSize.sp,
                            color      = Color.White,
                            lineHeight = (fontSize * 1.9f).sp,
                            textAlign  = TextAlign.Start
                        )
                        Spacer(Modifier.height(32.dp))
                        Text(
                            "⸻ ${match.sourceHe} ⸻",
                            fontSize  = 13.sp,
                            color     = GoldBright,
                            modifier  = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
