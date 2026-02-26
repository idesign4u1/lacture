package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.SefariaRepository
import com.jewish.calendar.data.StudyItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class DailyStudyUiState(
    val items: List<StudyItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedItem: StudyItem? = null,
    val selectedText: String? = null,
    val isLoadingText: Boolean = false
)

@HiltViewModel
class DailyStudyViewModel @Inject constructor(
    private val sefariaRepository: SefariaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyStudyUiState())
    val uiState: StateFlow<DailyStudyUiState> = _uiState.asStateFlow()

    init {
        loadStudy()
    }

    private fun loadStudy() {
        viewModelScope.launch {
            sefariaRepository.getDailyStudy()
                .onSuccess { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, error = "לא ניתן לטעון לימוד יומי") }
                }
        }
    }

    fun loadStudyForDate(date: Date) {
        _uiState.update { it.copy(isLoading = true, items = emptyList(), error = null) }
        viewModelScope.launch {
            sefariaRepository.getDailyStudy(date)
                .onSuccess { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, error = "לא ניתן לטעון לימוד יומי") }
                }
        }
    }

    fun openItem(item: StudyItem) {
        _uiState.update { it.copy(selectedItem = item, selectedText = null, isLoadingText = true) }
        viewModelScope.launch {
            sefariaRepository.getTextForRef(item.ref)
                .onSuccess { text -> _uiState.update { it.copy(selectedText = text, isLoadingText = false) } }
                .onFailure { _uiState.update { it.copy(selectedText = "שגיאה בטעינת הטקסט", isLoadingText = false) } }
        }
    }

    fun closeDialog() = _uiState.update { it.copy(selectedItem = null, selectedText = null) }
}
