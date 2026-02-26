package com.jewish.calendar.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Sefaria Calendar API data classes ─────────────────────────────────

data class SefariaCalendarsResponse(
    @SerializedName("calendar_items") val calendarItems: List<SefariaCalendarItem> = emptyList()
)

data class SefariaCalendarItem(
    val title: SefariaLocalizedText = SefariaLocalizedText(),
    @SerializedName("displayValue") val displayValue: SefariaLocalizedText? = null,
    val ref: String = "",
    val url: String? = null,
    val order: Int = 0
)

data class SefariaLocalizedText(
    val he: String = "",
    val en: String = ""
)

data class SefariaTextResponse(
    val he: JsonElement? = null,
    @SerializedName("heRef") val heRef: String = "",
    val book: String = ""
)

// ── Study item model ──────────────────────────────────────────────────

data class StudyItem(
    val titleHe: String,
    val subtitleHe: String,
    val ref: String
)

// ── Retrofit interface ────────────────────────────────────────────────

interface SefariaApiService {
    @GET("api/calendars")
    suspend fun getCalendars(): SefariaCalendarsResponse

    @GET("api/texts/{ref}")
    suspend fun getText(
        @Path("ref", encoded = false) ref: String
    ): SefariaTextResponse
}

// ── Repository ────────────────────────────────────────────────────────

@Singleton
class SefariaRepository @Inject constructor() {

    private val api: SefariaApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.sefaria.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SefariaApiService::class.java)
    }

    // Mapping: English title keyword → Hebrew display label
    private val studyKeywords = listOf(
        "Rambam"          to "רמב\"ם יומי",
        "Daf Yomi"        to "דף יומי",
        "Mishna"          to "משנה יומית",
        "Sefer HaMitzvot" to "ספר המצוות",
        "Tanya"           to "תניא יומי",
        "Kitzur"          to "הלכה יומית",
        "Halacha"         to "הלכה יומית",
        "Shemirat"        to "שמירת הלשון"
    )

    suspend fun getDailyStudy(): Result<List<StudyItem>> = try {
        val items = api.getCalendars().calendarItems.mapNotNull { item ->
            val match = studyKeywords.firstOrNull { (key, _) ->
                item.title.en.contains(key, ignoreCase = true)
            }
            if (match != null && item.ref.isNotBlank()) {
                StudyItem(
                    titleHe = match.second,
                    subtitleHe = item.displayValue?.he?.ifBlank { item.title.he } ?: item.title.he,
                    ref = item.ref
                )
            } else null
        }
        Result.success(items)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTextForRef(ref: String): Result<String> = try {
        val response = api.getText(ref)
        val text = extractHebrewText(response.he)
        if (text.isNotBlank()) Result.success(text)
        else Result.failure(Exception("לא נמצא תוכן"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun extractHebrewText(je: JsonElement?): String {
        if (je == null || je.isJsonNull) return ""
        return when {
            je.isJsonPrimitive -> stripHtml(je.asString)
            je.isJsonArray -> je.asJsonArray.joinToString("\n\n") { el ->
                when {
                    el.isJsonPrimitive -> stripHtml(el.asString)
                    el.isJsonArray -> el.asJsonArray
                        .filter { it.isJsonPrimitive }
                        .joinToString(" ") { stripHtml(it.asString) }
                    else -> ""
                }
            }.trim()
            else -> ""
        }
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]+>"), "").trim()
}
