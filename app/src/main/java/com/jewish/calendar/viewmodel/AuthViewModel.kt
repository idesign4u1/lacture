package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.AuthRepository
import com.jewish.calendar.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: UserModel? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isLoggedIn) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val user = authRepository.fetchUserProfile()
                _uiState.update {
                    it.copy(isLoggedIn = user != null, currentUser = user, isLoading = false)
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoggedIn = true, currentUser = user, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun register(email: String, password: String, displayName: String, gender: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.register(email, password, displayName, gender)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoggedIn = true, currentUser = user, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update { AuthUiState() }
    }

    fun refreshUser() {
        viewModelScope.launch {
            val user = authRepository.fetchUserProfile()
            if (user != null) _uiState.update { it.copy(currentUser = user) }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
