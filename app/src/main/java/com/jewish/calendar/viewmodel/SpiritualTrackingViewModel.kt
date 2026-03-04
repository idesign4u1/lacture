package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.ContentRepository
import com.jewish.calendar.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

private val Context.spiritualDataStore by preferencesDataStore(name = "spiritual_tracking")

@Singleton
class SpiritualTrackingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val PRAYER_KEY  = intPreferencesKey("prayer_days")
    private val TORAH_KEY   = intPreferencesKey("torah_days")
    private val CHESED_KEY  = intPreferencesKey("chesed_days")
    private val TEHILIM_KEY = intPreferencesKey("tehilim_days")

    suspend fun getPrayerDays(): Int  = context.spiritualDataStore.data.map { it[PRAYER_KEY]  ?: 0 }.first()
    suspend fun getTorahDays(): Int   = context.spiritualDataStore.data.map { it[TORAH_KEY]   ?: 0 }.first()
    suspend fun getChesedDays(): Int  = context.spiritualDataStore.data.map { it[CHESED_KEY]  ?: 0 }.first()
    suspend fun getTehilimDays(): Int = context.spiritualDataStore.data.map { it[TEHILIM_KEY] ?: 0 }.first()

    suspend fun increment(key: String) {
        val prefKey = when (key) {
            "prayer"  -> PRAYER_KEY
            "torah"   -> TORAH_KEY
            "chesed"  -> CHESED_KEY
            "tehilim" -> TEHILIM_KEY
            else -> return
        }
        context.spiritualDataStore.edit { prefs ->
            prefs[prefKey] = (prefs[prefKey] ?: 0) + 1
        }
    }

    suspend fun decrement(key: String) {
        val prefKey = when (key) {
            "prayer"  -> PRAYER_KEY
            "torah"   -> TORAH_KEY
            "chesed"  -> CHESED_KEY
            "tehilim" -> TEHILIM_KEY
            else -> return
        }
        context.spiritualDataStore.edit { prefs ->
            prefs[prefKey] = maxOf(0, (prefs[prefKey] ?: 0) - 1)
        }
    }

    suspend fun reset(key: String) {
        val prefKey = when (key) {
            "prayer"  -> PRAYER_KEY
            "torah"   -> TORAH_KEY
            "chesed"  -> CHESED_KEY
            "tehilim" -> TEHILIM_KEY
            else -> return
        }
        context.spiritualDataStore.edit { prefs -> prefs[prefKey] = 0 }
    }
}

data class TrackingItem(
    val key: String,
    val label: String,
    val emoji: String,
    val count: Int,
    val goal: Int
)

data class SpiritualTrackingUiState(
    val items: List<TrackingItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SpiritualTrackingViewModel @Inject constructor(
    private val repo: SpiritualTrackingRepository,
    private val contentRepo: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpiritualTrackingUiState())
    val uiState: StateFlow<SpiritualTrackingUiState> = _uiState.asStateFlow()

    init {
        // Re-build items whenever content or counts change
        viewModelScope.launch {
            contentRepo.trackingItemsFlow().collect { definitions ->
                load(definitions)
            }
        }
    }

    private suspend fun load(
        definitions: List<com.jewish.calendar.data.TrackingItemContent> =
            listOf(
                com.jewish.calendar.data.TrackingItemContent("prayer",  "🙏", "תפילה",           30),
                com.jewish.calendar.data.TrackingItemContent("torah",   "📖", "לימוד תורה",      20),
                com.jewish.calendar.data.TrackingItemContent("chesed",  "❤️", "גמילות חסדים",   10),
                com.jewish.calendar.data.TrackingItemContent("tehilim", "📜", "תהילים",          15)
            )
    ) {
        val prayer  = repo.getPrayerDays()
        val torah   = repo.getTorahDays()
        val chesed  = repo.getChesedDays()
        val tehilim = repo.getTehilimDays()
        val counts  = mapOf("prayer" to prayer, "torah" to torah, "chesed" to chesed, "tehilim" to tehilim)

        val items = definitions.map { def ->
            TrackingItem(
                key   = def.key,
                label = def.label,
                emoji = def.emoji,
                count = counts[def.key] ?: 0,
                goal  = def.goal
            )
        }
        _uiState.update { it.copy(items = items, isLoading = false) }
    }

    fun increment(key: String) {
        viewModelScope.launch { repo.increment(key); load() }
    }

    fun decrement(key: String) {
        viewModelScope.launch { repo.decrement(key); load() }
    }

    fun reset(key: String) {
        viewModelScope.launch { repo.reset(key); load() }
    }
}
