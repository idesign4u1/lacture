package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.AdminRepository
import com.jewish.calendar.model.AdminConfig
import com.jewish.calendar.model.DEFAULT_TOOLS
import com.jewish.calendar.model.ToolConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val config: AdminConfig = AdminConfig(),
    /** All tools merged with current visibility flag */
    val tools: List<ToolConfig> = DEFAULT_TOOLS,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        repo.configFlow
            .onEach { config ->
                _uiState.value = _uiState.value.copy(
                    config = config,
                    tools = DEFAULT_TOOLS.map { tool ->
                        tool.copy(isVisible = tool.id !in config.hiddenToolIds)
                    }
                )
            }
            .launchIn(viewModelScope)
    }

    // ── PIN ───────────────────────────────────────────────────────────

    fun changePin(newPin: String) {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(errorMessage = "הPIN חייב להיות 4 ספרות")
            return
        }
        viewModelScope.launch {
            repo.setPin(newPin)
            _uiState.value = _uiState.value.copy(saveSuccess = true, errorMessage = null)
        }
    }

    // ── Tool visibility ───────────────────────────────────────────────

    fun setToolVisible(toolId: String, visible: Boolean) {
        viewModelScope.launch {
            repo.toggleToolVisibility(toolId, hide = !visible)
        }
    }

    fun showAllTools() {
        viewModelScope.launch {
            repo.setHiddenTools(emptySet())
        }
    }

    // ── Primary color ─────────────────────────────────────────────────

    fun setPrimaryColor(colorArgb: Long?) {
        viewModelScope.launch {
            repo.setPrimaryColor(colorArgb)
        }
    }

    // ── Daily messages ────────────────────────────────────────────────

    fun setDailyMessages(messages: List<String>) {
        viewModelScope.launch {
            repo.setDailyMessages(messages)
            _uiState.value = _uiState.value.copy(saveSuccess = true)
        }
    }

    // ── Bot welcome text ──────────────────────────────────────────────

    fun setBotWelcomeText(text: String) {
        viewModelScope.launch {
            repo.setBotWelcomeText(text)
            _uiState.value = _uiState.value.copy(saveSuccess = true)
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────

    fun resetToDefaults() {
        viewModelScope.launch {
            repo.resetToDefaults()
            _uiState.value = _uiState.value.copy(saveSuccess = true)
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, errorMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
