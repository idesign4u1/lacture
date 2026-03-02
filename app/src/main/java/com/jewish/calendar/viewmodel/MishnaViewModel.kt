package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.LocalSefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Mishna tractate metadata ───────────────────────────────────────────

data class MishnaTractate(
    val id: String,
    val hebrewName: String,
    val seder: String,
    val assetFile: String,
    val chapterCount: Int,
    val description: String,
    val accentHex: Long
)

val MISHNA_TRACTATES = listOf(
    MishnaTractate(
        id          = "berakhot",
        hebrewName  = "בְּרָכוֹת",
        seder       = "סדר זרעים",
        assetFile   = "mishna_berakhot.json",
        chapterCount = 9,
        description = "ברכות, קריאת שמע ותפילה",
        accentHex   = 0xFF1565C0
    ),
    MishnaTractate(
        id          = "avot",
        hebrewName  = "פִּרְקֵי אָבוֹת",
        seder       = "סדר נזיקין",
        assetFile   = "mishna_pirkei_avot.json",
        chapterCount = 6,
        description = "מוסר ומסורת — נאמר בשבתות",
        accentHex   = 0xFF6A1B9A
    )
)

// ── Nav state ──────────────────────────────────────────────────────────

sealed class MishnaNavState {
    object TractateList : MishnaNavState()
    data class ChapterList(val tractate: MishnaTractate) : MishnaNavState()
    data class Reading(
        val tractate: MishnaTractate,
        val chapter: Int,
        val mishnas: List<String>
    ) : MishnaNavState()
}

// ── UI State ───────────────────────────────────────────────────────────

data class MishnaUiState(
    val navState: MishnaNavState = MishnaNavState.TractateList,
    val isLoading: Boolean = false,
    val fontSize: Int = 22
)

// ── ViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class MishnaViewModel @Inject constructor(
    private val localRepo: LocalSefariaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MishnaUiState())
    val uiState: StateFlow<MishnaUiState> = _uiState.asStateFlow()

    fun selectTractate(tractate: MishnaTractate) {
        _uiState.update { it.copy(navState = MishnaNavState.ChapterList(tractate)) }
    }

    fun selectChapter(tractate: MishnaTractate, chapter: Int) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val mishnas = localRepo.getChapter(tractate.assetFile, chapter - 1)
            _uiState.update {
                it.copy(
                    navState  = MishnaNavState.Reading(tractate, chapter, mishnas),
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
                    is MishnaNavState.Reading     -> MishnaNavState.ChapterList(current.tractate)
                    is MishnaNavState.ChapterList -> MishnaNavState.TractateList
                    MishnaNavState.TractateList   -> MishnaNavState.TractateList
                }
            )
        }
    }

    fun canGoBack(): Boolean = _uiState.value.navState != MishnaNavState.TractateList

    fun increaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize + 2).coerceAtMost(40)) }
    fun decreaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize - 2).coerceAtLeast(14)) }
}
