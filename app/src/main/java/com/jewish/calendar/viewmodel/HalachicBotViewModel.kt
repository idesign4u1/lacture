package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.ClaudeMessage
import com.jewish.calendar.data.ClaudeRepository
import com.jewish.calendar.data.UserPreferencesRepository
import com.jewish.calendar.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HalachicBotViewModel @Inject constructor(
    private val claudeRepository: ClaudeRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<ClaudeMessage>()

    // Live API key from DataStore (falls back to BuildConfig if not set by user)
    private val apiKey: StateFlow<String> = userPrefs.openAiApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(question: String = _uiState.value.inputText) {
        if (question.isBlank()) return

        val userMessage = ChatMessage(id = UUID.randomUUID().toString(), content = question, isUser = true)
        val loadingMessage = ChatMessage(id = "loading", content = "", isUser = false, isLoading = true)

        _uiState.update {
            it.copy(messages = it.messages + userMessage + loadingMessage, inputText = "", isLoading = true, error = null)
        }

        viewModelScope.launch {
            val currentKey = apiKey.value
            if (currentKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        messages = it.messages.filter { m -> !m.isLoading },
                        isLoading = false,
                        error = "מפתח OpenAI API חסר — הגדר אותו בפרופיל שלך"
                    )
                }
                return@launch
            }

            val result = claudeRepository.askHalachicQuestion(
                question = question,
                conversationHistory = conversationHistory,
                apiKey = currentKey
            )

            result.onSuccess { answer ->
                conversationHistory.add(ClaudeMessage("user", question))
                conversationHistory.add(ClaudeMessage("assistant", answer))
                if (conversationHistory.size > 20) repeat(2) { conversationHistory.removeAt(0) }

                _uiState.update {
                    it.copy(
                        messages = it.messages.filter { m -> !m.isLoading } +
                            ChatMessage(id = UUID.randomUUID().toString(), content = answer, isUser = false),
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                val msg = when {
                    error.message?.contains("401") == true ->
                        "מפתח API לא תקין (401) — עדכן אותו בפרופיל שלך"
                    error.message?.contains("400") == true -> "שגיאה בבקשה לשרת (400)"
                    error.message?.contains("429") == true -> "חריגה ממגבלת בקשות — נסה שוב בעוד רגע"
                    error.message?.contains("network") == true ||
                    error.message?.contains("timeout") == true -> "שגיאת רשת — בדוק חיבור לאינטרנט"
                    else -> "שגיאה: ${error.message}"
                }
                _uiState.update {
                    it.copy(messages = it.messages.filter { m -> !m.isLoading }, isLoading = false, error = msg)
                }
            }
        }
    }

    fun clearConversation() {
        conversationHistory.clear()
        _uiState.update { it.copy(messages = emptyList(), error = null) }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
