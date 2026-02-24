package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.HebrewCalendarRepository
import com.jewish.calendar.model.HebrewDateModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class CalendarUiState(
    val today: HebrewDateModel? = null,
    val selectedDate: HebrewDateModel? = null,
    val currentMonthDays: List<HebrewDateModel> = emptyList(),
    val displayYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val displayMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: HebrewCalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadCurrentMonth()
    }

    private fun loadCurrentMonth() {
        viewModelScope.launch {
            val today = calendarRepository.getTodayHebrewDate()
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val days = calendarRepository.getMonthDays(year, month)

            _uiState.update {
                it.copy(
                    today = today,
                    selectedDate = today,
                    currentMonthDays = days,
                    displayYear = year,
                    displayMonth = month,
                    isLoading = false
                )
            }
        }
    }

    fun navigateMonth(forward: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, current.displayYear)
                set(Calendar.MONTH, current.displayMonth - 1)
                add(Calendar.MONTH, if (forward) 1 else -1)
            }
            val newYear = cal.get(Calendar.YEAR)
            val newMonth = cal.get(Calendar.MONTH) + 1
            val days = calendarRepository.getMonthDays(newYear, newMonth)

            _uiState.update {
                it.copy(
                    currentMonthDays = days,
                    displayYear = newYear,
                    displayMonth = newMonth
                )
            }
        }
    }

    fun selectDate(dateModel: HebrewDateModel) {
        _uiState.update { it.copy(selectedDate = dateModel) }
    }

    fun goToToday() {
        viewModelScope.launch {
            val today = calendarRepository.getTodayHebrewDate()
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val days = calendarRepository.getMonthDays(year, month)
            _uiState.update {
                it.copy(
                    today = today,
                    selectedDate = today,
                    currentMonthDays = days,
                    displayYear = year,
                    displayMonth = month
                )
            }
        }
    }

    fun getOmerText(count: Int): String = calendarRepository.getOmerText(count)
}
