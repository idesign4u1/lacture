package com.jewish.calendar.ui.screens.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.viewmodel.*

// ── Hebrew numerals ────────────────────────────────────────────────────

private val mishnaHebrewNumerals = listOf(
    "א","ב","ג","ד","ה","ו","ז","ח","ט","י",
    "יא","יב","יג","יד","טו","טז","יז","יח","יט","כ",
    "כא","כב","כג","כד","כה","כו","כז","כח","כט","ל"
)

private fun hebrewChapter(n: Int): String = mishnaHebrewNumerals.getOrElse(n - 1) { n.toString() }

// ── Screen entry ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MishnaScreen(
    onBack: () -> Unit,
    viewModel: MishnaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    BackHandler {
        if (viewModel.canGoBack()) viewModel.goBack() else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val nav = state.navState) {
                            is MishnaNavState.TractateList -> "מִשְׁנָה"
                            is MishnaNavState.ChapterList  -> nav.tractate.hebrewName
                            is MishnaNavState.Reading      ->
                                "${nav.tractate.hebrewName}  ·  פרק ${hebrewChapter(nav.chapter)}"
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.End,
                        modifier   = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.canGoBack()) viewModel.goBack() else onBack()
                    }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "חזרה")
                    }
                },
                actions = {
                    if (state.navState is MishnaNavState.Reading) {
                        IconButton(onClick = { viewModel.decreaseFontSize() }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "הקטן")
                        }
                        IconButton(onClick = { viewModel.increaseFontSize() }) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "הגדל")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (val nav = state.navState) {
                    is MishnaNavState.TractateList -> TractateListView(
                        onSelect = { viewModel.selectTractate(it) }
                    )
                    is MishnaNavState.ChapterList  -> MishnaChapterListView(
                        tractate = nav.tractate,
                        onSelect = { viewModel.selectChapter(nav.tractate, it) }
                    )
                    is MishnaNavState.Reading      -> MishnaReadingView(
                        tractate = nav.tractate,
                        chapter  = nav.chapter,
                        mishnas  = nav.mishnas,
                        fontSize = state.fontSize
                    )
                }
            }
        }
    }
}

// ── Tractate list ──────────────────────────────────────────────────────

@Composable
private fun TractateListView(onSelect: (MishnaTractate) -> Unit) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "בחר מסכת",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.End,
                modifier   = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }
        items(MISHNA_TRACTATES) { tractate ->
            TractateCard(tractate = tractate, onClick = { onSelect(tractate) })
        }
    }
}

@Composable
private fun TractateCard(tractate: MishnaTractate, onClick: () -> Unit) {
    val accent = Color(tractate.accentHex)
    ElevatedCard(
        onClick    = onClick,
        modifier   = Modifier.fillMaxWidth(),
        shape      = RoundedCornerShape(18.dp),
        elevation  = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.06f))
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    tractate.hebrewName,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accent
                )
                Text(
                    tractate.seder,
                    fontSize = 12.sp,
                    color    = accent.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tractate.description,
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${tractate.chapterCount} פרקים",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier         = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📚", fontSize = 26.sp)
            }
        }
    }
}

// ── Chapter grid ───────────────────────────────────────────────────────

@Composable
private fun MishnaChapterListView(tractate: MishnaTractate, onSelect: (Int) -> Unit) {
    val accent = Color(tractate.accentHex)

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            "בחר פרק",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.End,
            color      = accent,
            modifier   = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns               = GridCells.Adaptive(minSize = 88.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tractate.chapterCount) { idx ->
                val ch = idx + 1
                Box(
                    modifier         = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.10f))
                        .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                        .clickable { onSelect(ch) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("פרק", fontSize = 11.sp, color = accent.copy(alpha = 0.7f))
                        Text(
                            hebrewChapter(ch),
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = accent
                        )
                    }
                }
            }
        }
    }
}

// ── Mishna reading view ────────────────────────────────────────────────

@Composable
private fun MishnaReadingView(
    tractate: MishnaTractate,
    chapter: Int,
    mishnas: List<String>,
    fontSize: Int
) {
    val accent = Color(tractate.accentHex)

    if (mishnas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("לא ניתן לטעון את הפרק", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(mishnas) { idx, mishna ->
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accent.copy(alpha = 0.06f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Mishna number header
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier         = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "משנה ${hebrewChapter(idx + 1)}",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = accent
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text      = mishna,
                        fontSize  = fontSize.sp,
                        lineHeight = (fontSize * 1.8f).sp,
                        textAlign  = TextAlign.End,
                        style      = LocalTextStyle.current.copy(
                            textDirection = TextDirection.Rtl
                        )
                    )
                }
            }
        }
    }
}
