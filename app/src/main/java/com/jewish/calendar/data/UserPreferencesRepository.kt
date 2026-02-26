package com.jewish.calendar.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jewish.calendar.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")

    val openAiApiKey: Flow<String> = context.userDataStore.data.map { prefs ->
        prefs[OPENAI_API_KEY]?.takeIf { it.isNotBlank() } ?: BuildConfig.OPENAI_API_KEY
    }

    suspend fun setOpenAiApiKey(key: String) {
        context.userDataStore.edit { prefs ->
            prefs[OPENAI_API_KEY] = key.trim()
        }
    }
}
