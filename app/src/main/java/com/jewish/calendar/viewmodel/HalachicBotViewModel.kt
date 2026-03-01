package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.BuildConfig
import com.jewish.calendar.data.AuthRepository
import com.jewish.calendar.data.ClaudeMessage
import com.jewish.calendar.data.ClaudeRepository
import com.jewish.calendar.data.UserPreferencesRepository
import com.jewish.calendar.model.ChatMessage
import com.jewish.calendar.model.buildHalachicSystemPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
    val prayerStyle: String = "ashkenaz"
)

@HiltViewModel
class HalachicBotViewModel @Inject constructor(
    private val claudeRepository: ClaudeRepository,
    private val prefs: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<ClaudeMessage>()
    private val apiKey: String = BuildConfig.OPENAI_API_KEY

    init { loadUserInfo() }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val style = prefs.getPrayerStyle()
            val user  = authRepository.fetchUserProfile()
            val name  = user?.displayName?.trim() ?: ""
            _uiState.update { it.copy(prayerStyle = style, userName = name) }
            val greeting = buildGreeting(name)
            _uiState.update {
                it.copy(messages = listOf(
                    ChatMessage(id = "greeting", content = greeting, isUser = false)
                ))
            }
        }
    }

    private fun buildGreeting(name: String): String {
        val nameStr = if (name.isNotBlank()) name else "חבר/ה יקר/ה"
        return "מה שלומך $nameStr 😊\n\nשמי הרב שמואל כהן.\nחשוב לציין: התשובות הן לעיון בלבד ואינן מחליפות שאלת רב.\n\nבמה אוכל לעזור?"
    }

    fun onInputChanged(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage(question: String = _uiState.value.inputText) {
        if (question.isBlank()) return

        val userMsg    = ChatMessage(id = UUID.randomUUID().toString(), content = question, isUser = true)
        val loadingMsg = ChatMessage(id = "loading", content = "", isUser = false, isLoading = true)

        _uiState.update {
            it.copy(messages = it.messages + userMsg + loadingMsg, inputText = "", isLoading = true, error = null)
        }

        viewModelScope.launch {
            val systemPrompt = buildHalachicSystemPrompt(
                prayerStyle = _uiState.value.prayerStyle,
                userName    = _uiState.value.userName
            )
            val result = claudeRepository.askHalachicQuestion(
                question = question,
                conversationHistory = conversationHistory,
                apiKey = apiKey,
                systemPrompt = systemPrompt
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
                    error.message?.contains("401") == true  -> "שגיאה: מפתח API לא תקין"
                    error.message?.contains("400") == true  -> "שגיאה בבקשה לשרת (400)"
                    error.message?.contains("429") == true  -> "חריגה ממגבלת בקשות — נסה שוב"
                    error.message?.contains("network") == true ||
                    error.message?.contains("timeout") == true -> "שגיאת רשת — בדוק חיבור"
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
        val greeting = buildGreeting(_uiState.value.userName)
        _uiState.update { it.copy(
            messages = listOf(ChatMessage(id = "greeting", content = greeting, isUser = false)),
            error = null
        )}
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
