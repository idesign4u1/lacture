package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.CalendarEvent
import com.jewish.calendar.data.CalendarEventDao
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
    val isLoading: Boolean = false,
    val selectedDayEvents: List<CalendarEvent> = emptyList(),
    val showAddEventDialog: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: HebrewCalendarRepository,
    private val eventDao: CalendarEventDao
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
            loadEventsForDate(today.gregorianDate)
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
        loadEventsForDate(dateModel.gregorianDate)
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
            loadEventsForDate(today.gregorianDate)
        }
    }

    fun getOmerText(count: Int): String = calendarRepository.getOmerText(count)

    // --- Event management ---

    private fun loadEventsForDate(date: Date) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = cal.timeInMillis
            val events = eventDao.getEventsForDate(startOfDay, endOfDay)
            _uiState.update { it.copy(selectedDayEvents = events) }
        }
    }

    fun showAddEventDialog() = _uiState.update { it.copy(showAddEventDialog = true) }
    fun hideAddEventDialog() = _uiState.update { it.copy(showAddEventDialog = false) }

    fun addEvent(title: String, description: String) {
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate?.gregorianDate ?: return@launch
            val cal = Calendar.getInstance().apply {
                time = selectedDate
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            eventDao.insertEvent(
                CalendarEvent(date = cal.timeInMillis, title = title, description = description)
            )
            loadEventsForDate(selectedDate)
            _uiState.update { it.copy(showAddEventDialog = false) }
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            eventDao.deleteEvent(event)
            _uiState.value.selectedDate?.gregorianDate?.let { loadEventsForDate(it) }
        }
    }
}
