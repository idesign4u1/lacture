package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.jewish.calendar.viewmodel.NusachType
import com.jewish.calendar.viewmodel.PrayerSectionData
import com.jewish.calendar.viewmodel.SiddurPrayerTime
import com.jewish.calendar.viewmodel.SiddurViewModel

// ── Color palette ──────────────────────────────────────────────────────

private val Parchment      = Color(0xFFFDF6E3)
private val ParchmentDark  = Color(0xFFEDE0C8)
private val InkBrown       = Color(0xFF2C1810)
private val InkLight       = Color(0xFF5C4033)
private val SiddurGold     = Color(0xFFD4AF37)
private val SiddurBlue     = Color(0xFF003E7E)
private val SectionBg      = Color(0xFFF5ECD7)
private val NoteColor      = Color(0xFF8B6914)

// ── Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiddurScreen(
    onBack: () -> Unit,
    viewModel: SiddurViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sections = remember(state.selectedCategory, state.selectedNusach) {
        viewModel.getSectionsFor(state.selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "סידור תפילה",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = InkBrown
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "חזור",
                            tint = SiddurBlue
                        )
                    }
                },
                actions = {
                    // Font size controls
                    IconButton(onClick = { viewModel.decreaseFontSize() }) {
                        Icon(Icons.Default.Remove, contentDescription = "הקטן גופן", tint = SiddurBlue)
                    }
                    Text(
                        "${state.fontSize}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBrown,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(onClick = { viewModel.increaseFontSize() }) {
                        Icon(Icons.Default.Add, contentDescription = "הגדל גופן", tint = SiddurBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ParchmentDark)
            )
        },
        containerColor = Parchment
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Smart suggestion banner
            SmartSuggestionBanner(
                suggested = state.currentTime,
                selected  = state.selectedCategory
            )

            // Nusach (prayer style) selector
            NusachSelector(
                selected = state.selectedNusach,
                onSelect = { viewModel.selectNusach(it) }
            )

            // Prayer category tabs
            CategoryTabs(
                selected   = state.selectedCategory,
                onSelect   = { viewModel.selectCategory(it) }
            )

            HorizontalDivider(color = SiddurGold.copy(alpha = 0.4f), thickness = 1.dp)

            if (state.selectedSection == null) {
                // Section list
                SectionList(
                    sections  = sections,
                    onSelect  = { viewModel.selectSection(it) }
                )
            } else {
                // Prayer text view
                PrayerTextView(
                    section   = state.selectedSection!!,
                    text      = state.displayedText,
                    isLoading = state.isLoadingText,
                    fontSize  = state.fontSize,
                    onBack    = { viewModel.clearSection() }
                )
            }
        }
    }
}

// ── Smart suggestion banner ────────────────────────────────────────────

@Composable
private fun SmartSuggestionBanner(suggested: SiddurPrayerTime, selected: SiddurPrayerTime) {
    val isMatch = suggested == selected
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isMatch) SiddurBlue.copy(alpha = 0.08f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (isMatch) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(suggested.icon, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "עכשיו זמן ${suggested.hebrewName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SiddurBlue
                )
            }
        }
    }
}

// ── Nusach selector ────────────────────────────────────────────────────

@Composable
private fun NusachSelector(
    selected: NusachType,
    onSelect: (NusachType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ParchmentDark.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "נוסח תפילה:",
            fontSize = 11.sp,
            color = InkLight,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.height(4.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            reverseLayout = true
        ) {
            items(NusachType.entries) { nusach ->
                val isSelected = nusach == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) SiddurBlue else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) SiddurBlue else NoteColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(nusach) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${nusach.icon} ${nusach.shortName}",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else InkBrown
                    )
                }
            }
        }
    }
    HorizontalDivider(color = SiddurGold.copy(alpha = 0.25f), thickness = 1.dp)
}

// ── Category tabs ──────────────────────────────────────────────────────

@Composable
private fun CategoryTabs(
    selected: SiddurPrayerTime,
    onSelect: (SiddurPrayerTime) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(ParchmentDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SiddurPrayerTime.entries) { time ->
            val isSelected = time == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) SiddurBlue else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) SiddurBlue else SiddurGold.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(time) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${time.icon} ${time.hebrewName}",
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else InkBrown
                )
            }
        }
    }
}

// ── Section list ───────────────────────────────────────────────────────

@Composable
private fun SectionList(
    sections: List<PrayerSectionData>,
    onSelect: (PrayerSectionData) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sections) { section ->
            SectionCard(section = section, onClick = { onSelect(section) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionCard(section: PrayerSectionData, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SectionBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gold left indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SiddurGold)
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBrown
                )
                if (section.openingLine.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = section.openingLine,
                        fontSize = 13.sp,
                        color = InkLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (section.halachicNote.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "• ${section.halachicNote}",
                        fontSize = 11.sp,
                        color = NoteColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (section.nusachNote.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SiddurGold.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = section.nusachNote,
                            fontSize = 10.sp,
                            color = NoteColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Source badge
            if (section.sefariaRef != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SiddurGold.copy(alpha = 0.15f))
                        .border(1.dp, SiddurGold.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("ספריא", fontSize = 10.sp, color = NoteColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Prayer text view ───────────────────────────────────────────────────

@Composable
private fun PrayerTextView(
    section: PrayerSectionData,
    text: String,
    isLoading: Boolean,
    fontSize: Int,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(SiddurBlue.copy(alpha = 0.08f), SiddurGold.copy(alpha = 0.06f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור", tint = SiddurBlue)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBrown
                )
                if (section.halachicNote.isNotBlank()) {
                    Text(
                        text = section.halachicNote,
                        fontSize = 12.sp,
                        color = NoteColor
                    )
                }
            }
        }

        HorizontalDivider(color = SiddurGold.copy(alpha = 0.3f))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SiddurBlue)
                        Spacer(Modifier.height(12.dp))
                        Text("טוען טקסט...", color = InkLight, fontSize = 14.sp)
                    }
                }
            }
            text.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("לא ניתן לטעון את הטקסט", color = InkLight)
                }
            }
            else -> {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    item {
                        Text(
                            text = text,
                            fontSize = fontSize.sp,
                            color = InkBrown,
                            lineHeight = (fontSize * 1.9f).sp,
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(Modifier.height(40.dp))
                        // Ornamental divider at bottom
                        Text(
                            text = "⸻ סוף ⸻",
                            fontSize = 14.sp,
                            color = SiddurGold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
