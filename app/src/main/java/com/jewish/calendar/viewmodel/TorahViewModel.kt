package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.LocalSefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Parasha (Torah portion) ────────────────────────────────────────────

data class Parasha(val name: String, val startChapter: Int)

// ── Torah book metadata ────────────────────────────────────────────────

data class TorahBook(
    val id: String,
    val hebrewName: String,
    val englishName: String,
    val assetFile: String,
    val chapterCount: Int,
    val accentHex: Long,
    val parashaList: List<Parasha>
)

val TORAH_BOOKS = listOf(
    TorahBook(
        id = "genesis", hebrewName = "בְּרֵאשִׁית", englishName = "Genesis",
        assetFile = "torah_genesis.json", chapterCount = 50, accentHex = 0xFF2E7D32,
        parashaList = listOf(
            Parasha("בְּרֵאשִׁית",  1),
            Parasha("נֹחַ",         6),
            Parasha("לֶךְ לְךָ",   12),
            Parasha("וַיֵּרָא",    18),
            Parasha("חַיֵּי שָׂרָה", 23),
            Parasha("תּוֹלְדוֹת",  25),
            Parasha("וַיֵּצֵא",    28),
            Parasha("וַיִּשְׁלַח", 32),
            Parasha("וַיֵּשֶׁב",   37),
            Parasha("מִקֵּץ",      41),
            Parasha("וַיִּגַּשׁ",  44),
            Parasha("וַיְחִי",     47)
        )
    ),
    TorahBook(
        id = "exodus", hebrewName = "שְׁמוֹת", englishName = "Exodus",
        assetFile = "torah_exodus.json", chapterCount = 40, accentHex = 0xFF1565C0,
        parashaList = listOf(
            Parasha("שְׁמוֹת",     1),
            Parasha("וָאֵרָא",     6),
            Parasha("בֹּא",       10),
            Parasha("בְּשַׁלַּח", 13),
            Parasha("יִתְרוֹ",    18),
            Parasha("מִשְׁפָּטִים", 21),
            Parasha("תְּרוּמָה",  25),
            Parasha("תְּצַוֶּה",  27),
            Parasha("כִּי תִשָּׂא", 30),
            Parasha("וַיַּקְהֵל", 35),
            Parasha("פְקוּדֵי",   38)
        )
    ),
    TorahBook(
        id = "leviticus", hebrewName = "וַיִּקְרָא", englishName = "Leviticus",
        assetFile = "torah_leviticus.json", chapterCount = 27, accentHex = 0xFF6A1B9A,
        parashaList = listOf(
            Parasha("וַיִּקְרָא",    1),
            Parasha("צַו",           6),
            Parasha("שְׁמִינִי",     9),
            Parasha("תַּזְרִיעַ",   12),
            Parasha("מְצֹרָע",      14),
            Parasha("אַחֲרֵי מוֹת", 16),
            Parasha("קְדֹשִׁים",    19),
            Parasha("אֱמֹר",        21),
            Parasha("בְּהַר",       25),
            Parasha("בְּחֻקֹּתַי",  26)
        )
    ),
    TorahBook(
        id = "numbers", hebrewName = "בְּמִדְבַּר", englishName = "Numbers",
        assetFile = "torah_numbers.json", chapterCount = 36, accentHex = 0xFF004D40,
        parashaList = listOf(
            Parasha("בְּמִדְבַּר",    1),
            Parasha("נָשׂוֹא",        4),
            Parasha("בְּהַעֲלֹתְךָ",  8),
            Parasha("שְׁלַח",        13),
            Parasha("קֹרַח",         16),
            Parasha("חֻקַּת",        19),
            Parasha("בָּלָק",        22),
            Parasha("פִּינְחָס",     25),
            Parasha("מַטּוֹת",       30),
            Parasha("מַסְעֵי",       33)
        )
    ),
    TorahBook(
        id = "deuteronomy", hebrewName = "דְּבָרִים", englishName = "Deuteronomy",
        assetFile = "torah_deuteronomy.json", chapterCount = 34, accentHex = 0xFF880E4F,
        parashaList = listOf(
            Parasha("דְּבָרִים",       1),
            Parasha("וָאֶתְחַנַּן",    3),
            Parasha("עֵקֶב",           7),
            Parasha("רְאֵה",          11),
            Parasha("שֹׁפְטִים",      16),
            Parasha("כִּי תֵצֵא",     21),
            Parasha("כִּי תָבוֹא",    26),
            Parasha("נִצָּבִים",      29),
            Parasha("וַיֵּלֶךְ",      31),
            Parasha("הַאֲזִינוּ",     32),
            Parasha("וְזֹאת הַבְּרָכָה", 33)
        )
    )
)

// ── Navigation state ───────────────────────────────────────────────────

sealed class TorahNavState {
    object BookList : TorahNavState()
    data class ChapterList(val book: TorahBook) : TorahNavState()
    data class Reading(
        val book: TorahBook,
        val chapter: Int,       // 1-based for display
        val verses: List<String>
    ) : TorahNavState()
}

// ── UI State ───────────────────────────────────────────────────────────

data class TorahUiState(
    val navState: TorahNavState = TorahNavState.BookList,
    val isLoading: Boolean = false,
    val fontSize: Int = 22
)

// ── ViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class TorahViewModel @Inject constructor(
    private val localRepo: LocalSefariaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TorahUiState())
    val uiState: StateFlow<TorahUiState> = _uiState.asStateFlow()

    // ── Navigation ─────────────────────────────────────────────────────

    fun selectBook(book: TorahBook) {
        _uiState.update { it.copy(navState = TorahNavState.ChapterList(book)) }
    }

    fun selectChapter(book: TorahBook, chapter: Int) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val verses = localRepo.getChapter(book.assetFile, chapter - 1)  // 0-based
            _uiState.update {
                it.copy(
                    navState  = TorahNavState.Reading(book, chapter, verses),
                    isLoading = false
                )
            }
        }
    }

    fun goBack() {
        val current = _uiState.value.navState
        _uiState.update {
            it.copy(
                navState = when (current) {
                    is TorahNavState.Reading     -> TorahNavState.ChapterList(current.book)
                    is TorahNavState.ChapterList -> TorahNavState.BookList
                    TorahNavState.BookList       -> TorahNavState.BookList
                }
            )
        }
    }

    fun canGoBack(): Boolean = _uiState.value.navState != TorahNavState.BookList

    // ── Font size ──────────────────────────────────────────────────────

    fun increaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize + 2).coerceAtMost(40)) }
    fun decreaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize - 2).coerceAtLeast(14)) }
}
