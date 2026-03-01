package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.AuthRepository
import com.jewish.calendar.data.UserPreferencesRepository
import com.jewish.calendar.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val showOnboarding: Boolean = false,
    val step: Int = 1,               // 1 = gender, 2 = prayer style
    val gender: String = "",
    val prayerStyle: String = "ashkenaz",
    val isCompleting: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var existingUser: UserModel? = null

    init {
        viewModelScope.launch {
            if (prefs.isFirstLaunch()) {
                existingUser = authRepository.fetchUserProfile()
                val prayerStyle = prefs.getPrayerStyle()
                _uiState.update {
                    it.copy(
                        showOnboarding = true,
                        gender = existingUser?.gender ?: "",
                        prayerStyle = prayerStyle
                    )
                }
            }
        }
    }

    fun selectGender(gender: String) {
        _uiState.update { it.copy(gender = gender, step = 2) }
    }

    fun selectPrayerStyle(style: String) {
        _uiState.update { it.copy(prayerStyle = style) }
    }

    fun complete() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }
            val state = _uiState.value

            prefs.savePrayerStyle(state.prayerStyle)

            // Update profile — preserve existing fields, only change gender
            existingUser?.let { user ->
                authRepository.updateProfile(
                    displayName   = user.displayName,
                    gender        = state.gender,
                    birthDate     = user.birthDate,
                    maritalStatus = user.maritalStatus
                )
            }

            prefs.markFirstLaunchDone()
            _uiState.update { it.copy(showOnboarding = false, isCompleting = false) }
        }
    }

    fun skip() {
        viewModelScope.launch {
            prefs.markFirstLaunchDone()
            _uiState.update { it.copy(showOnboarding = false) }
        }
    }
}
