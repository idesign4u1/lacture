package com.jewish.calendar.data

import com.jewish.calendar.model.HALACHIC_SYSTEM_PROMPT
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName
import javax.inject.Inject
import javax.inject.Singleton

// OpenAI API data classes
data class OpenAiRequest(
    val model: String = "gpt-4o",
    val messages: List<OpenAiMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class OpenAiMessage(
    val role: String,   // "system", "user", or "assistant"
    val content: String
)

data class OpenAiResponse(
    val id: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage
)

data class OpenAiChoice(
    val message: OpenAiMessage,
    @SerializedName("finish_reason") val finishReason: String
)

data class OpenAiUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// Keep legacy type alias so HalachicBotViewModel doesn't need updating
typealias ClaudeMessage = OpenAiMessage

interface OpenAiApiService {
    @POST("chat/completions")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}

@Singleton
class ClaudeRepository @Inject constructor() {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenAiApiService::class.java)

    suspend fun askHalachicQuestion(
        question: String,
        conversationHistory: List<OpenAiMessage>,
        apiKey: String,
        systemPrompt: String = HALACHIC_SYSTEM_PROMPT
    ): Result<String> {
        return try {
            val messages = mutableListOf<OpenAiMessage>()
            messages.add(OpenAiMessage("system", systemPrompt))
            messages.addAll(conversationHistory)
            messages.add(OpenAiMessage("user", question))

            val request = OpenAiRequest(messages = messages)
            val response = api.sendMessage("Bearer $apiKey", request)
            val answer = response.choices.firstOrNull()?.message?.content ?: "לא התקבלה תשובה"
            Result.success(answer)
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val body = e.response()?.errorBody()?.string() ?: e.message()
            Result.failure(Exception("HTTP$code: $body"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
