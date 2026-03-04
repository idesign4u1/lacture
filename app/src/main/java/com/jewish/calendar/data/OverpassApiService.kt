package com.jewish.calendar.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Overpass API models ────────────────────────────────────────────────

data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

data class OverpassElement(
    val type: String = "",
    val id: Long = 0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tags: Map<String, String> = emptyMap()
) {
    val name: String get() = tags["name:he"] ?: tags["name"] ?: "בית כנסת"
    val address: String get() = buildString {
        tags["addr:street"]?.let { append(it) }
        tags["addr:housenumber"]?.let { append(" $it") }
        tags["addr:city"]?.let { if (isNotEmpty()) append(", ") ; append(it) }
    }
}

// ── Retrofit interface ─────────────────────────────────────────────────

interface OverpassApiInterface {
    @GET("api/interpreter")
    suspend fun query(@Query("data") query: String): OverpassResponse
}

// ── Repository ─────────────────────────────────────────────────────────

@Singleton
class SynagogueRepository @Inject constructor() {

    private val api: OverpassApiInterface by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(OverpassApiInterface::class.java)
    }

    /**
     * Finds synagogues within [radiusMeters] of the given coordinates.
     * Uses Overpass QL to query OpenStreetMap for Jewish places of worship.
     */
    suspend fun findSynagoguesNearby(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 3000
    ): List<OverpassElement> {
        val query = """
            [out:json][timeout:25];
            (
              node["amenity"="place_of_worship"]["religion"="jewish"](around:$radiusMeters,$lat,$lon);
              way["amenity"="place_of_worship"]["religion"="jewish"](around:$radiusMeters,$lat,$lon);
            );
            out center;
        """.trimIndent()

        return try {
            val response = api.query(query)
            response.elements.map { element ->
                // For ways, use center coordinates if available
                if (element.type == "way" && element.lat == 0.0) {
                    element // center coordinates already in lat/lon for out center;
                } else {
                    element
                }
            }.filter { it.lat != 0.0 && it.lon != 0.0 }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
