package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.LocalSefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Torah book metadata ────────────────────────────────────────────────

data class TorahBook(
    val id: String,
    val hebrewName: String,
    val englishName: String,
    val assetFile: String,
    val chapterCount: Int,
    val accentHex: Long
)

val TORAH_BOOKS = listOf(
    TorahBook("genesis",     "בְּרֵאשִׁית", "Genesis",     "torah_genesis.json",     50, 0xFF2E7D32),
    TorahBook("exodus",      "שְׁמוֹת",    "Exodus",      "torah_exodus.json",      40, 0xFF1565C0),
    TorahBook("leviticus",   "וַיִּקְרָא", "Leviticus",   "torah_leviticus.json",   27, 0xFF6A1B9A),
    TorahBook("numbers",     "בְּמִדְבַּר", "Numbers",     "torah_numbers.json",     36, 0xFF004D40),
    TorahBook("deuteronomy", "דְּבָרִים",  "Deuteronomy", "torah_deuteronomy.json", 34, 0xFF880E4F)
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
