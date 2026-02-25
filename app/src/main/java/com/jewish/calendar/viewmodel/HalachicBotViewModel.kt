package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.BuildConfig
import com.jewish.calendar.data.ClaudeMessage
import com.jewish.calendar.data.ClaudeRepository
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
    val apiKeyMissing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HalachicBotViewModel @Inject constructor(
    private val claudeRepository: ClaudeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<ClaudeMessage>()

    // Use the API key embedded at build time from local.properties
    private var apiKey: String = BuildConfig.CLAUDE_API_KEY

    init {
        _uiState.update { it.copy(apiKeyMissing = apiKey.isBlank()) }
    }

    fun setApiKey(key: String) {
        apiKey = key.trim()
        _uiState.update { it.copy(apiKeyMissing = apiKey.isBlank()) }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(question: String = _uiState.value.inputText) {
        if (question.isBlank()) return
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(apiKeyMissing = true) }
            return
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = question,
            isUser = true
        )
        val loadingMessage = ChatMessage(
            id = "loading",
            content = "",
            isUser = false,
            isLoading = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + loadingMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val result = claudeRepository.askHalachicQuestion(
                question = question,
                conversationHistory = conversationHistory,
                apiKey = apiKey
            )

            result.onSuccess { answer ->
                conversationHistory.add(ClaudeMessage("user", question))
                conversationHistory.add(ClaudeMessage("assistant", answer))

                if (conversationHistory.size > 20) {
                    repeat(2) { conversationHistory.removeAt(0) }
                }

                val botMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = answer,
                    isUser = false
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages.filter { m -> !m.isLoading } + botMessage,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                val errorMessage = when {
                    error.message?.contains("401") == true -> "מפתח API לא תקין"
                    error.message?.contains("network") == true -> "שגיאת רשת - בדוק חיבור"
                    else -> "שגיאה: ${error.message}"
                }
                _uiState.update {
                    it.copy(
                        messages = it.messages.filter { m -> !m.isLoading },
                        isLoading = false,
                        error = errorMessage
                    )
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
