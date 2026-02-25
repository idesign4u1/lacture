package com.jewish.calendar.data

import com.jewish.calendar.model.HALACHIC_SYSTEM_PROMPT
import com.jewish.calendar.model.HalachicSource
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName
import javax.inject.Inject
import javax.inject.Singleton

// Claude API data classes
data class ClaudeRequest(
    val model: String = "claude-sonnet-4-6",
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val system: String = HALACHIC_SYSTEM_PROMPT,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,   // "user" or "assistant"
    val content: String
)

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    val usage: ClaudeUsage
)

data class ClaudeContent(
    val type: String,
    val text: String
)

data class ClaudeUsage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)

interface ClaudeApiService {
    @Headers("anthropic-version: 2023-06-01", "content-type: application/json")
    @POST("messages")
    suspend fun sendMessage(
        @retrofit2.http.Header("x-api-key") apiKey: String,
        @Body request: ClaudeRequest
    ): ClaudeResponse
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
        .baseUrl("https://api.anthropic.com/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ClaudeApiService::class.java)

    suspend fun askHalachicQuestion(
        question: String,
        conversationHistory: List<ClaudeMessage>,
        apiKey: String
    ): Result<String> {
        return try {
            val messages = conversationHistory + ClaudeMessage("user", question)
            val request = ClaudeRequest(messages = messages)
            val response = api.sendMessage(apiKey, request)
            val answer = response.content.firstOrNull()?.text ?: "לא התקבלה תשובה"
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
