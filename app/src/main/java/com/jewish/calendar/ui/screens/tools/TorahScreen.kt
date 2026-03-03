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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

// ── Hebrew verse numerals (up to 176) ─────────────────────────────────

private val hebrewNumerals = listOf(
    "א","ב","ג","ד","ה","ו","ז","ח","ט","י",
    "יא","יב","יג","יד","טו","טז","יז","יח","יט","כ",
    "כא","כב","כג","כד","כה","כו","כז","כח","כט","ל",
    "לא","לב","לג","לד","לה","לו","לז","לח","לט","מ",
    "מא","מב","מג","מד","מה","מו","מז","מח","מט","נ",
    "נא","נב","נג","נד","נה","נו","נז","נח","נט","ס",
    "סא","סב","סג","סד","סה","סו","סז","סח","סט","ע",
    "עא","עב","עג","עד","עה","עו","עז","עח","עט","פ",
    "פא","פב","פג","פד","פה","פו","פז","פח","פט","צ",
    "צא","צב","צג","צד","צה","צו","צז","צח","צט","ק",
    "קא","קב","קג","קד","קה","קו","קז","קח","קט","קי",
    "קיא","קיב","קיג","קיד","קטו","קטז","קיז","קיח","קיט","קכ",
    "קכא","קכב","קכג","קכד","קכה","קכו","קכז","קכח","קכט","קל",
    "קלא","קלב","קלג","קלד","קלה","קלו","קלז","קלח","קלט","קמ",
    "קמא","קמב","קמג","קמד","קמה","קמו","קמז","קמח","קמט","קנ",
    "קנא","קנב","קנג","קנד","קנה","קנו","קנז","קנח","קנט","קס",
    "קסא","קסב","קסג","קסד","קסה","קסו","קסז","קסח","קסט","קע",
    "קעא","קעב","קעג","קעד","קעה","קעו"
)

private fun hebrewChapter(n: Int): String = hebrewNumerals.getOrElse(n - 1) { n.toString() }

// ── Main screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorahScreen(
    onBack: () -> Unit,
    viewModel: TorahViewModel = hiltViewModel()
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
                            is TorahNavState.BookList       -> "תּוֹרָה"
                            is TorahNavState.ChapterList    -> nav.book.hebrewName
                            is TorahNavState.Reading        ->
                                "${nav.book.hebrewName}  ·  פרק ${hebrewChapter(nav.chapter)}"
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.End,
                        modifier   = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    if (viewModel.canGoBack()) {
                        IconButton(onClick = { viewModel.goBack() }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "חזרה")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "חזרה")
                        }
                    }
                },
                actions = {
                    if (state.navState is TorahNavState.Reading) {
                        IconButton(onClick = { viewModel.decreaseFontSize() }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "הקטן גופן")
                        }
                        IconButton(onClick = { viewModel.increaseFontSize() }) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "הגדל גופן")
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
                    is TorahNavState.BookList    -> BookListView(
                        onSelectBook = { viewModel.selectBook(it) }
                    )
                    is TorahNavState.ChapterList -> ParashaListView(
                        book            = nav.book,
                        onSelectChapter = { viewModel.selectChapter(nav.book, it) }
                    )
                    is TorahNavState.Reading     -> ReadingView(
                        book    = nav.book,
                        chapter = nav.chapter,
                        verses  = nav.verses,
                        fontSize = state.fontSize
                    )
                }
            }
        }
    }
}

// ── Level 1: Book selection ────────────────────────────────────────────

@Composable
private fun BookListView(onSelectBook: (TorahBook) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "חמישה חומשי תורה",
            style     = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.End,
            modifier  = Modifier.fillMaxWidth()
        )

        TORAH_BOOKS.forEach { book ->
            BookCard(book = book, onClick = { onSelectBook(book) })
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BookCard(book: TorahBook, onClick: () -> Unit) {
    val accent = Color(book.accentHex)
    ElevatedCard(
        onClick    = onClick,
        modifier   = Modifier.fillMaxWidth(),
        shape      = RoundedCornerShape(20.dp),
        elevation  = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.06f))
                .padding(20.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    book.hebrewName,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accent
                )
                Text(
                    "${book.parashaList.size} פרשיות · ${book.chapterCount} פרקים",
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier           = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment   = Alignment.Center
            ) {
                Text("📜", fontSize = 30.sp)
            }
        }
    }
}

// ── Level 2: Parasha list ──────────────────────────────────────────────

@Composable
private fun ParashaListView(book: TorahBook, onSelectChapter: (Int) -> Unit) {
    val accent = Color(book.accentHex)
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "בחר פרשה",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.End,
                color      = accent,
                modifier   = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
        }
        items(book.parashaList) { parasha ->
            ParashaCard(
                parasha = parasha,
                accent  = accent,
                onClick = { onSelectChapter(parasha.startChapter) }
            )
        }
    }
}

@Composable
private fun ParashaCard(parasha: Parasha, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "פרק ${hebrewChapter(parasha.startChapter)}",
            fontSize = 13.sp,
            color    = accent.copy(alpha = 0.7f)
        )
        Text(
            "פרשת ${parasha.name}",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = accent
        )
    }
}

// ── Level 3: Reading view ──────────────────────────────────────────────

@Composable
private fun ReadingView(book: TorahBook, chapter: Int, verses: List<String>, fontSize: Int) {
    val accent = Color(book.accentHex)

    if (verses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("לא ניתן לטעון את הפרק", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(verses) { idx, verse ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.Top
            ) {
                // Verse text
                Text(
                    text      = verse,
                    fontSize  = fontSize.sp,
                    lineHeight = (fontSize * 1.7f).sp,
                    textAlign  = TextAlign.End,
                    style      = LocalTextStyle.current.copy(
                        textDirection = TextDirection.Rtl
                    ),
                    modifier  = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                // Verse number badge
                Box(
                    modifier           = Modifier
                        .padding(top = 4.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment   = Alignment.Center
                ) {
                    Text(
                        hebrewChapter(idx + 1),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = accent
                    )
                }
            }

            if (idx < verses.size - 1) {
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 34.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
