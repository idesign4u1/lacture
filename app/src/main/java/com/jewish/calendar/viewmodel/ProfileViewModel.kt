package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.AuthRepository
import com.jewish.calendar.data.CalendarEvent
import com.jewish.calendar.data.CalendarEventDao
import com.jewish.calendar.data.HebrewCalendarRepository
import com.jewish.calendar.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ProfileUiState(
    val user: UserModel? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val calendarRepository: HebrewCalendarRepository,
    private val eventDao: CalendarEventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.fetchUserProfile()
            _uiState.update { it.copy(user = user, isLoading = false) }
        }
    }

    fun saveProfile(
        displayName: String,
        gender: String,
        birthDate: String,
        maritalStatus: String
    ) {
        _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
        viewModelScope.launch {
            authRepository.updateProfile(displayName, gender, birthDate, maritalStatus)
                .onSuccess { updatedUser ->
                    if (birthDate.isNotBlank()) addBirthdayToCalendar(updatedUser, birthDate)
                    _uiState.update { it.copy(user = updatedUser, isSaving = false, saveSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            authRepository.changePassword(currentPassword, newPassword)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saveSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null, saveSuccess = false) }

    private suspend fun addBirthdayToCalendar(user: UserModel, birthDateStr: String) {
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = formatter.parse(birthDateStr) ?: return
            val birthCal = Calendar.getInstance().apply { time = birthDate }

            val todayCal = Calendar.getInstance()
            val thisYearBirthday = Calendar.getInstance().apply {
                set(Calendar.YEAR, todayCal.get(Calendar.YEAR))
                set(Calendar.MONTH, birthCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, birthCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (thisYearBirthday.before(todayCal)) thisYearBirthday.add(Calendar.YEAR, 1)

            val hebrewDate = calendarRepository.getHebrewDateForDay(thisYearBirthday.time)
            val eventTitle = "🎂 יום הולדת — ${user.displayName.ifBlank { "שלי" }}"

            eventDao.insertEvent(CalendarEvent(
                date = thisYearBirthday.timeInMillis,
                title = eventTitle,
                description = "תאריך עברי: ${hebrewDate.hebrewDateString}"
            ))

            val nextYearBirthday = (thisYearBirthday.clone() as Calendar).also { it.add(Calendar.YEAR, 1) }
            val nextHebrewDate = calendarRepository.getHebrewDateForDay(nextYearBirthday.time)
            eventDao.insertEvent(CalendarEvent(
                date = nextYearBirthday.timeInMillis,
                title = eventTitle,
                description = "תאריך עברי: ${nextHebrewDate.hebrewDateString}"
            ))
        } catch (_: Exception) { }
    }
}
