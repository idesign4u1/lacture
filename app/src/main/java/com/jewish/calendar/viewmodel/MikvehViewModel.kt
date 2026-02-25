package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.MikvehRepository
import com.jewish.calendar.model.CycleStatus
import com.jewish.calendar.model.MikvehLocation
import com.jewish.calendar.model.TevilahRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class MikvehUiState(
    val cycleStatus: CycleStatus? = null,
    val tevilahHistory: List<TevilahRecord> = emptyList(),
    val nearbyMikvahot: List<MikvehLocation> = emptyList(),
    val isLoading: Boolean = true,
    val showAddCycleDialog: Boolean = false,
    val showDailyCheckDialog: Boolean = false,
    val showDeleteCycleDialog: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class MikvehViewModel @Inject constructor(
    private val mikvehRepository: MikvehRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MikvehUiState())
    val uiState: StateFlow<MikvehUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val cycleStatus = mikvehRepository.getCycleStatus()
            val history = mikvehRepository.getTevilahHistory()
            _uiState.update {
                it.copy(
                    cycleStatus = cycleStatus,
                    tevilahHistory = history,
                    isLoading = false
                )
            }
        }
    }

    fun startNewCycle(date: Date = Date()) {
        viewModelScope.launch {
            mikvehRepository.startNewCycle(date)
            loadData()
            _uiState.update {
                it.copy(
                    showAddCycleDialog = false,
                    successMessage = "מחזור חדש נפתח"
                )
            }
        }
    }

    fun endCycle(date: Date = Date()) {
        viewModelScope.launch {
            mikvehRepository.endCurrentCycle(date)
            loadData()
            _uiState.update { it.copy(successMessage = "תאריך סיום המחזור נשמר") }
        }
    }

    fun deleteCycle() {
        viewModelScope.launch {
            val cycle = _uiState.value.cycleStatus?.currentCycle ?: return@launch
            mikvehRepository.deleteCycle(cycle)
            loadData()
            _uiState.update {
                it.copy(showDeleteCycleDialog = false, successMessage = "המחזור נמחק")
            }
        }
    }

    fun addDailyCheck(dayNumber: Int, isClean: Boolean, note: String = "") {
        viewModelScope.launch {
            val cycleId = _uiState.value.cycleStatus?.currentCycle?.id ?: return@launch
            mikvehRepository.addDailyCheck(cycleId, dayNumber, isClean, note)
            loadData()
            val msg = if (isClean) "בדיקה נקייה נרשמה ✓" else "בדיקה נרשמה"
            _uiState.update { it.copy(showDailyCheckDialog = false, successMessage = msg) }
        }
    }

    fun recordTevilah(mikvehName: String) {
        viewModelScope.launch {
            mikvehRepository.recordTevilah(Date(), mikvehName)
            loadData()
            _uiState.update { it.copy(successMessage = "הטבילה נרשמה בהצלחה") }
        }
    }

    fun deleteTevilah(record: TevilahRecord) {
        viewModelScope.launch {
            mikvehRepository.deleteTevilah(record)
            loadData()
            _uiState.update { it.copy(successMessage = "הרשומה נמחקה") }
        }
    }

    fun showAddCycleDialog() = _uiState.update { it.copy(showAddCycleDialog = true) }
    fun hideAddCycleDialog() = _uiState.update { it.copy(showAddCycleDialog = false) }
    fun showDailyCheckDialog() = _uiState.update { it.copy(showDailyCheckDialog = true) }
    fun hideDailyCheckDialog() = _uiState.update { it.copy(showDailyCheckDialog = false) }
    fun showDeleteCycleDialog() = _uiState.update { it.copy(showDeleteCycleDialog = true) }
    fun hideDeleteCycleDialog() = _uiState.update { it.copy(showDeleteCycleDialog = false) }
    fun clearMessage() = _uiState.update { it.copy(successMessage = null) }
}
