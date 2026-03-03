package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.LocalSefariaRepository
import com.jewish.calendar.data.SefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Data models ────────────────────────────────────────────────────────

data class TorahMatch(
    val word: String,
    val gematria: Int,
    val sourceHe: String,    // e.g. "בראשית א:א"
    val context: String,     // full verse text
    val sefariaRef: String   // for Sefaria API, e.g. "Genesis 1:1"
)

data class GematriaUiState(
    val input: String = "",
    val gematriaValue: Int? = null,
    val letterBreakdown: String = "",
    val matches: List<TorahMatch> = emptyList(),
    val selectedMatch: TorahMatch? = null,
    val verseText: String = "",
    val isLoadingVerse: Boolean = false,
    val isIndexing: Boolean = true,
    val fontSize: Int = 22
)

// ── ViewModel ──────────────────────────────────────────────────────────

@HiltViewModel
class GematriaViewModel @Inject constructor(
    private val sefariaRepository: SefariaRepository,
    private val localRepo: LocalSefariaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GematriaUiState())
    val uiState: StateFlow<GematriaUiState> = _uiState.asStateFlow()

    // Standard gematria letter values
    private val letterValues = mapOf(
        'א' to 1,  'ב' to 2,  'ג' to 3,  'ד' to 4,  'ה' to 5,
        'ו' to 6,  'ז' to 7,  'ח' to 8,  'ט' to 9,  'י' to 10,
        'כ' to 20, 'ך' to 20, 'ל' to 30, 'מ' to 40, 'ם' to 40,
        'נ' to 50, 'ן' to 50, 'ס' to 60, 'ע' to 70, 'פ' to 80,
        'ף' to 80, 'צ' to 90, 'ץ' to 90, 'ק' to 100,'ר' to 200,
        'ש' to 300,'ת' to 400
    )

    // Full Torah index: gematria value → list of matches (capped at MAX_PER_VALUE)
    private val torahIndex = mutableMapOf<Int, MutableList<TorahMatch>>()
    private val MAX_PER_VALUE = 80

    // Torah books: (assetFile, hebrewName, englishApiName, chapterCount)
    private data class BookInfo(
        val assetFile: String,
        val hebrewName: String,
        val englishName: String,
        val chapterCount: Int
    )

    private val torahBooks = listOf(
        BookInfo("torah_genesis.json",     "בראשית",  "Genesis",     50),
        BookInfo("torah_exodus.json",      "שמות",    "Exodus",      40),
        BookInfo("torah_leviticus.json",   "ויקרא",   "Leviticus",   27),
        BookInfo("torah_numbers.json",     "במדבר",   "Numbers",     36),
        BookInfo("torah_deuteronomy.json", "דברים",   "Deuteronomy", 34)
    )

    init {
        buildTorahIndex()
    }

    private fun buildTorahIndex() {
        viewModelScope.launch(Dispatchers.IO) {
            for (book in torahBooks) {
                val allChapters = localRepo.getAllChapters(book.assetFile)
                allChapters.forEachIndexed { chapterIdx, verses ->
                    val chapterNum = chapterIdx + 1
                    verses.forEachIndexed { verseIdx, verse ->
                        val verseNum   = verseIdx + 1
                        val hebrewRef  = "${book.hebrewName} ${toHebrewNumeral(chapterNum)}:${toHebrewNumeral(verseNum)}"
                        val sefariaRef = "${book.englishName} $chapterNum:$verseNum"
                        val cleanVerse = stripNikud(verse)
                        val words      = cleanVerse.split(Regex("\\s+")).filter { it.isNotBlank() }

                        words.forEach { word ->
                            val letters = word.filter { it in letterValues }
                            if (letters.length >= 2) {
                                val value = letters.sumOf { letterValues[it]!! }
                                if (value > 0) {
                                    val bucket = torahIndex.getOrPut(value) { mutableListOf() }
                                    if (bucket.size < MAX_PER_VALUE) {
                                        bucket.add(
                                            TorahMatch(
                                                word      = word,
                                                gematria  = value,
                                                sourceHe  = hebrewRef,
                                                context   = verse.take(80).trimEnd(),
                                                sefariaRef = sefariaRef
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            _uiState.update { it.copy(isIndexing = false) }
        }
    }

    // ── Public actions ─────────────────────────────────────────────────

    fun onInputChange(text: String) {
        val hebrewOnly = text.filter { it in letterValues || it == ' ' }
        val value      = hebrewOnly.filter { it in letterValues }.sumOf { letterValues[it]!! }
        val breakdown  = buildBreakdown(hebrewOnly)
        val matches    = if (value > 0) (torahIndex[value] ?: emptyList()) else emptyList()
        _uiState.update {
            it.copy(
                input           = text,
                gematriaValue   = if (value > 0) value else null,
                letterBreakdown = breakdown,
                matches         = matches,
                selectedMatch   = null,
                verseText       = ""
            )
        }
    }

    fun selectMatch(match: TorahMatch) {
        _uiState.update { it.copy(selectedMatch = match, verseText = "", isLoadingVerse = true) }
        viewModelScope.launch {
            sefariaRepository.getTextForRef(match.sefariaRef)
                .onSuccess { text -> _uiState.update { it.copy(verseText = text, isLoadingVerse = false) } }
                .onFailure { _uiState.update { it.copy(verseText = match.context, isLoadingVerse = false) } }
        }
    }

    fun clearMatch()       = _uiState.update { it.copy(selectedMatch = null, verseText = "") }
    fun increaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize + 2).coerceAtMost(40)) }
    fun decreaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize - 2).coerceAtLeast(14)) }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun buildBreakdown(text: String): String {
        val letters = text.filter { it in letterValues }
        if (letters.isEmpty()) return ""
        val parts = letters.map { "$it(${letterValues[it]})" }
        val total = letters.sumOf { letterValues[it]!! }
        return parts.joinToString(" + ") + " = $total"
    }

    // Strip nikud/cantillation (U+0591–U+05C7) and keep Hebrew letters (U+05D0–U+05EA)
    private fun stripNikud(text: String): String =
        text.filter { c -> c.code < 0x0591 || c.code > 0x05C7 }

    // Hebrew numeral conversion (handles 1–999, respects 15=טו, 16=טז)
    private fun toHebrewNumeral(n: Int): String {
        if (n <= 0) return n.toString()
        val table = listOf(
            400 to "ת", 300 to "ש", 200 to "ר", 100 to "ק",
            90  to "צ", 80  to "פ", 70  to "ע", 60  to "ס",
            50  to "נ", 40  to "מ", 30  to "ל", 20  to "כ",
            19  to "יט", 18 to "יח", 17 to "יז", 16 to "טז", 15 to "טו",
            10  to "י",  9  to "ט",  8  to "ח",  7  to "ז",
            6   to "ו",  5  to "ה",  4  to "ד",  3  to "ג",
            2   to "ב",  1  to "א"
        )
        var rem = n
        val sb  = StringBuilder()
        for ((value, letter) in table) {
            while (rem >= value) { sb.append(letter); rem -= value }
        }
        return sb.toString()
    }
}
