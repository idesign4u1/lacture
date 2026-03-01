package com.jewish.calendar.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jewish.calendar.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val OPENAI_API_KEY   = stringPreferencesKey("openai_api_key")
    private val OMER_LAST_DATE   = stringPreferencesKey("omer_last_date")
    private val OMER_STREAK      = intPreferencesKey("omer_streak")

    val openAiApiKey: Flow<String> = context.userDataStore.data.map { prefs ->
        prefs[OPENAI_API_KEY]?.takeIf { it.isNotBlank() } ?: BuildConfig.OPENAI_API_KEY
    }

    suspend fun setOpenAiApiKey(key: String) {
        context.userDataStore.edit { prefs -> prefs[OPENAI_API_KEY] = key.trim() }
    }

    suspend fun getOmerLastDate(): String =
        context.userDataStore.data.map { it[OMER_LAST_DATE] ?: "" }.first()

    suspend fun getOmerStreak(): Int =
        context.userDataStore.data.map { it[OMER_STREAK] ?: 0 }.first()

    suspend fun saveOmerConfirm(date: String, streak: Int) {
        context.userDataStore.edit { prefs ->
            prefs[OMER_LAST_DATE] = date
            prefs[OMER_STREAK]    = streak
        }
    }
}
