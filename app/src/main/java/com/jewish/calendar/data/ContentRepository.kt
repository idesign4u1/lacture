package com.jewish.calendar.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

// ── Data models ────────────────────────────────────────────────────────

data class BlessingContent(
    val id: String = "",
    val emoji: String = "",
    val name: String = "",
    val trigger: String = "",
    val blessing: String = "",
    val note: String = ""
)

data class SpecialPrayerContent(
    val id: String = "",
    val emoji: String = "",
    val title: String = "",
    val subtitle: String = "",
    val text: String = ""
)

data class ShalomTipContent(
    val id: String = "",
    val emoji: String = "",
    val title: String = "",
    val content: String = ""
)

data class ChallaStepContent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val blessing: String? = null,
    val note: String? = null
)

data class ChallaRecipeContent(
    val id: String = "",
    val emoji: String = "",
    val name: String = "",
    val ingredients: String = ""
)

data class TrackingItemContent(
    val key: String = "",
    val emoji: String = "",
    val label: String = "",
    val goal: Int = 30
)

// ── Repository ─────────────────────────────────────────────────────────

private const val TAG = "ContentRepository"
private const val COL = "app_content"

@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val db: FirebaseFirestore get() = Firebase.firestore

    // ── Blessings ─────────────────────────────────────────────────────

    fun blessingsFlow(): Flow<List<BlessingContent>> = firestoreListFlow(
        collection = "blessings",
        localAsset = "content/blessings.json",
        mapper = { obj ->
            BlessingContent(
                id      = obj.optString("id"),
                emoji   = obj.optString("emoji"),
                name    = obj.optString("name"),
                trigger = obj.optString("trigger"),
                blessing = obj.optString("blessing"),
                note    = obj.optString("note")
            )
        },
        firestoreMapper = { map ->
            BlessingContent(
                id      = map["id"] as? String ?: "",
                emoji   = map["emoji"] as? String ?: "",
                name    = map["name"] as? String ?: "",
                trigger = map["trigger"] as? String ?: "",
                blessing = map["blessing"] as? String ?: "",
                note    = map["note"] as? String ?: ""
            )
        }
    )

    // ── Special prayers ───────────────────────────────────────────────

    fun specialPrayersFlow(): Flow<List<SpecialPrayerContent>> = firestoreListFlow(
        collection = "special_prayers",
        localAsset = "content/special_prayers.json",
        mapper = { obj ->
            SpecialPrayerContent(
                id       = obj.optString("id"),
                emoji    = obj.optString("emoji"),
                title    = obj.optString("title"),
                subtitle = obj.optString("subtitle"),
                text     = obj.optString("text")
            )
        },
        firestoreMapper = { map ->
            SpecialPrayerContent(
                id       = map["id"] as? String ?: "",
                emoji    = map["emoji"] as? String ?: "",
                title    = map["title"] as? String ?: "",
                subtitle = map["subtitle"] as? String ?: "",
                text     = map["text"] as? String ?: ""
            )
        }
    )

    // ── Shalom Bayit tips ─────────────────────────────────────────────

    fun shalomTipsFlow(): Flow<List<ShalomTipContent>> = firestoreListFlow(
        collection = "shalom_bayit_tips",
        localAsset = "content/shalom_bayit.json",
        arrayKey   = "tips",
        mapper = { obj ->
            ShalomTipContent(
                id      = obj.optString("id"),
                emoji   = obj.optString("emoji"),
                title   = obj.optString("title"),
                content = obj.optString("content")
            )
        },
        firestoreMapper = { map ->
            ShalomTipContent(
                id      = map["id"] as? String ?: "",
                emoji   = map["emoji"] as? String ?: "",
                title   = map["title"] as? String ?: "",
                content = map["content"] as? String ?: ""
            )
        }
    )

    fun shalomVersesFlow(): Flow<List<String>> = callbackFlow {
        // Try Firestore first
        val reg = db.collection(COL).document("shalom_bayit_verses")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) {
                    // Fallback to local
                    trySend(loadLocalShalomVerses())
                    return@addSnapshotListener
                }
                @Suppress("UNCHECKED_CAST")
                val list = snap.get("verses") as? List<String>
                trySend(list ?: loadLocalShalomVerses())
            }
        awaitClose { reg.remove() }
    }

    private fun loadLocalShalomVerses(): List<String> = try {
        val json = JSONObject(context.assets.open("content/shalom_bayit.json").bufferedReader().readText())
        val arr = json.getJSONArray("verses")
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) { emptyList() }

    // ── Challa steps ──────────────────────────────────────────────────

    fun challaStepsFlow(): Flow<List<ChallaStepContent>> = firestoreListFlow(
        collection = "challa_steps",
        localAsset = "content/challa.json",
        arrayKey   = "steps",
        mapper = { obj ->
            ChallaStepContent(
                id          = obj.optString("id"),
                title       = obj.optString("title"),
                description = obj.optString("description"),
                blessing    = obj.optString("blessing").takeIf { it.isNotBlank() },
                note        = obj.optString("note").takeIf { it.isNotBlank() }
            )
        },
        firestoreMapper = { map ->
            ChallaStepContent(
                id          = map["id"] as? String ?: "",
                title       = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                blessing    = map["blessing"] as? String,
                note        = map["note"] as? String
            )
        }
    )

    fun challaRecipesFlow(): Flow<List<ChallaRecipeContent>> = firestoreListFlow(
        collection = "challa_recipes",
        localAsset = "content/challa.json",
        arrayKey   = "recipes",
        mapper = { obj ->
            ChallaRecipeContent(
                id          = obj.optString("id"),
                emoji       = obj.optString("emoji"),
                name        = obj.optString("name"),
                ingredients = obj.optString("ingredients")
            )
        },
        firestoreMapper = { map ->
            ChallaRecipeContent(
                id          = map["id"] as? String ?: "",
                emoji       = map["emoji"] as? String ?: "",
                name        = map["name"] as? String ?: "",
                ingredients = map["ingredients"] as? String ?: ""
            )
        }
    )

    // ── Spiritual tracking ────────────────────────────────────────────

    fun trackingItemsFlow(): Flow<List<TrackingItemContent>> = firestoreListFlow(
        collection = "spiritual_tracking",
        localAsset = "content/spiritual_tracking.json",
        mapper = { obj ->
            TrackingItemContent(
                key   = obj.optString("key"),
                emoji = obj.optString("emoji"),
                label = obj.optString("label"),
                goal  = obj.optInt("goal", 30)
            )
        },
        firestoreMapper = { map ->
            TrackingItemContent(
                key   = map["key"] as? String ?: "",
                emoji = map["emoji"] as? String ?: "",
                label = map["label"] as? String ?: "",
                goal  = (map["goal"] as? Long)?.toInt() ?: 30
            )
        }
    )

    // ── Generic helpers ───────────────────────────────────────────────

    private fun <T> firestoreListFlow(
        collection: String,
        localAsset: String,
        arrayKey: String? = null,
        mapper: (JSONObject) -> T,
        firestoreMapper: (Map<String, Any>) -> T
    ): Flow<List<T>> = callbackFlow {
        var reg: ListenerRegistration? = null
        try {
            reg = db.collection(COL).document(collection)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.w(TAG, "Firestore error for $collection: ${err.message}")
                        trySend(loadLocalJson(localAsset, arrayKey, mapper))
                        return@addSnapshotListener
                    }
                    if (snap == null || !snap.exists()) {
                        trySend(loadLocalJson(localAsset, arrayKey, mapper))
                        return@addSnapshotListener
                    }
                    @Suppress("UNCHECKED_CAST")
                    val items = snap.get("items") as? List<Map<String, Any>>
                    if (items != null) {
                        trySend(items.mapNotNull {
                            try { firestoreMapper(it) } catch (e: Exception) { null }
                        })
                    } else {
                        trySend(loadLocalJson(localAsset, arrayKey, mapper))
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore unavailable for $collection: ${e.message}")
            trySend(loadLocalJson(localAsset, arrayKey, mapper))
        }
        awaitClose { reg?.remove() }
    }

    private fun <T> loadLocalJson(
        assetPath: String,
        arrayKey: String?,
        mapper: (JSONObject) -> T
    ): List<T> = try {
        val text = context.assets.open(assetPath).bufferedReader().readText()
        val arr: JSONArray = if (arrayKey != null) {
            JSONObject(text).getJSONArray(arrayKey)
        } else {
            JSONArray(text)
        }
        (0 until arr.length()).mapNotNull {
            try { mapper(arr.getJSONObject(it)) } catch (e: Exception) { null }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load local asset $assetPath: ${e.message}")
        emptyList()
    }
}
