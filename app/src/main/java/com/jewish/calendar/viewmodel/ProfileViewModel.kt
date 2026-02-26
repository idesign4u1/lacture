package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.AuthRepository
import com.jewish.calendar.data.CalendarEvent
import com.jewish.calendar.data.CalendarEventDao
import com.jewish.calendar.data.HebrewCalendarRepository
import com.jewish.calendar.data.UserPreferencesRepository
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
    val error: String? = null,
    val openAiApiKey: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPrefs: UserPreferencesRepository,
    private val calendarRepository: HebrewCalendarRepository,
    private val eventDao: CalendarEventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.fetchUserProfile()
            val key = userPrefs.openAiApiKey.first()
            _uiState.update { it.copy(user = user, isLoading = false, openAiApiKey = key) }
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
                    // Add birthday to calendar if date was set
                    if (birthDate.isNotBlank()) {
                        addBirthdayToCalendar(updatedUser, birthDate)
                    }
                    _uiState.update {
                        it.copy(user = updatedUser, isSaving = false, saveSuccess = true)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            userPrefs.setOpenAiApiKey(key)
            _uiState.update { it.copy(openAiApiKey = key, saveSuccess = true) }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            authRepository.changePassword(currentPassword, newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null, saveSuccess = false) }

    private suspend fun addBirthdayToCalendar(user: UserModel, birthDateStr: String) {
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = formatter.parse(birthDateStr) ?: return
            val birthCal = Calendar.getInstance().apply { time = birthDate }
            val birthMonth = birthCal.get(Calendar.MONTH)
            val birthDay = birthCal.get(Calendar.DAY_OF_MONTH)

            // Compute this year's birthday
            val todayCal = Calendar.getInstance()
            val thisYearBirthday = Calendar.getInstance().apply {
                set(Calendar.YEAR, todayCal.get(Calendar.YEAR))
                set(Calendar.MONTH, birthMonth)
                set(Calendar.DAY_OF_MONTH, birthDay)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }

            // If this year's birthday has already passed, use next year
            if (thisYearBirthday.before(todayCal)) {
                thisYearBirthday.add(Calendar.YEAR, 1)
            }

            // Compute Hebrew birthday
            val hebrewDate = calendarRepository.getHebrewDateForDay(thisYearBirthday.time)
            val hebrewDateStr = hebrewDate.hebrewDateString

            val name = user.displayName.ifBlank { "שלי" }

            // Delete any existing birthday events for this user
            // (re-insert fresh so birthdate changes are reflected)
            val eventTitle = "🎂 יום הולדת — $name"

            // Add Gregorian birthday event
            eventDao.insertEvent(
                CalendarEvent(
                    date = thisYearBirthday.timeInMillis,
                    title = eventTitle,
                    description = "תאריך עברי: $hebrewDateStr"
                )
            )

            // Also add next-year so it appears on the upcoming Hebrew date
            val nextYearBirthday = thisYearBirthday.clone() as Calendar
            nextYearBirthday.add(Calendar.YEAR, 1)
            val nextHebrewDate = calendarRepository.getHebrewDateForDay(nextYearBirthday.time)
            eventDao.insertEvent(
                CalendarEvent(
                    date = nextYearBirthday.timeInMillis,
                    title = eventTitle,
                    description = "תאריך עברי: ${nextHebrewDate.hebrewDateString}"
                )
            )
        } catch (_: Exception) { /* silently ignore calendar errors */ }
    }
}
