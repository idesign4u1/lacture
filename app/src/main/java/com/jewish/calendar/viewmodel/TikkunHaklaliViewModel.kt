package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.SefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TikkunPsalm(
    val number: Int,
    val hebrewNumber: String,
    val openingLine: String,
    val sefariaRef: String
)

data class TikkunUiState(
    val selected: TikkunPsalm? = null,
    val text: String = "",
    val isLoading: Boolean = false,
    val completedIds: Set<Int> = emptySet(),
    val fontSize: Int = 22
)

@HiltViewModel
class TikkunHaklaliViewModel @Inject constructor(
    private val sefariaRepository: SefariaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TikkunUiState())
    val uiState: StateFlow<TikkunUiState> = _uiState.asStateFlow()

    val psalms: List<TikkunPsalm> = listOf(
        TikkunPsalm(16,  "טז",  "מִכְתָּם לְדָוִד שָׁמְרֵנִי אֵל",           "Psalms 16"),
        TikkunPsalm(32,  "לב",  "לְדָוִד מַשְׂכִּיל אַשְׁרֵי נְשׂוּי פֶּשַׁע", "Psalms 32"),
        TikkunPsalm(41,  "מא",  "לַמְנַצֵּחַ מִזְמוֹר לְדָוִד",               "Psalms 41"),
        TikkunPsalm(42,  "מב",  "כְּאַיָּל תַּעֲרֹג עַל אֲפִיקֵי מָיִם",      "Psalms 42"),
        TikkunPsalm(59,  "נט",  "לַמְנַצֵּחַ אַל תַּשְׁחֵת לְדָוִד מִכְתָּם", "Psalms 59"),
        TikkunPsalm(77,  "עז",  "לַמְנַצֵּחַ עַל יְדוּתוּן לְאָסָף",          "Psalms 77"),
        TikkunPsalm(90,  "צ",   "תְּפִלָּה לְמֹשֶׁה אִישׁ הָאֱלֹהִים",        "Psalms 90"),
        TikkunPsalm(105, "קה",  "הוֹדוּ לַה׳ קִרְאוּ בִשְׁמוֹ",              "Psalms 105"),
        TikkunPsalm(137, "קלז", "עַל נַהֲרוֹת בָּבֶל שָׁם יָשַׁבְנוּ",       "Psalms 137"),
        TikkunPsalm(150, "קנ",  "הַלְלוּיָהּ הַלְלוּ אֵל בְּקָדְשׁוֹ",       "Psalms 150")
    )

    fun selectPsalm(psalm: TikkunPsalm) {
        _uiState.update { it.copy(selected = psalm, text = "", isLoading = true) }
        viewModelScope.launch {
            sefariaRepository.getTextForRef(psalm.sefariaRef)
                .onSuccess { text -> _uiState.update { it.copy(text = text, isLoading = false) } }
                .onFailure  { _uiState.update { it.copy(text = "לא ניתן לטעון את הטקסט", isLoading = false) } }
        }
    }

    fun markCompleted(psalmNumber: Int) =
        _uiState.update { it.copy(completedIds = it.completedIds + psalmNumber) }

    fun clearSelection() = _uiState.update { it.copy(selected = null, text = "") }
    fun increaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize + 2).coerceAtMost(40)) }
    fun decreaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize - 2).coerceAtLeast(14)) }
}
