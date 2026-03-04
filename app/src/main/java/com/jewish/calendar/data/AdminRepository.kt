package com.jewish.calendar.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jewish.calendar.model.AdminConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.adminDataStore by preferencesDataStore(name = "admin_prefs")

@Singleton
class AdminRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val CONFIG_KEY           = stringPreferencesKey("admin_config")
    private val PIN_KEY              = stringPreferencesKey("admin_pin")
    private val HIDDEN_TOOLS_KEY     = stringPreferencesKey("hidden_tools")
    private val PRIMARY_COLOR_KEY    = stringPreferencesKey("primary_color")
    private val DAILY_MESSAGES_KEY   = stringPreferencesKey("daily_messages")
    private val BOT_WELCOME_KEY      = stringPreferencesKey("bot_welcome")

    // ── Full config as Flow ───────────────────────────────────────────

    val configFlow: Flow<AdminConfig> = context.adminDataStore.data.map { prefs ->
        AdminConfig(
            pin                 = prefs[PIN_KEY] ?: "1234",
            hiddenToolIds       = parseStringSet(prefs[HIDDEN_TOOLS_KEY]),
            customPrimaryColor  = prefs[PRIMARY_COLOR_KEY]?.toLongOrNull(),
            customDailyMessages = parseStringList(prefs[DAILY_MESSAGES_KEY]),
            botWelcomeText      = prefs[BOT_WELCOME_KEY] ?: ""
        )
    }

    suspend fun getConfig(): AdminConfig = configFlow.first()

    // ── PIN ───────────────────────────────────────────────────────────

    suspend fun getPin(): String =
        context.adminDataStore.data.map { it[PIN_KEY] ?: "1234" }.first()

    suspend fun setPin(newPin: String) {
        context.adminDataStore.edit { it[PIN_KEY] = newPin }
    }

    suspend fun checkPin(input: String): Boolean = input == getPin()

    // ── Hidden tools ──────────────────────────────────────────────────

    suspend fun setHiddenTools(ids: Set<String>) {
        context.adminDataStore.edit { prefs ->
            prefs[HIDDEN_TOOLS_KEY] = gson.toJson(ids.toList())
        }
    }

    suspend fun toggleToolVisibility(id: String, hide: Boolean) {
        val current = getConfig().hiddenToolIds.toMutableSet()
        if (hide) current.add(id) else current.remove(id)
        setHiddenTools(current)
    }

    val hiddenToolsFlow: Flow<Set<String>> = context.adminDataStore.data.map { prefs ->
        parseStringSet(prefs[HIDDEN_TOOLS_KEY])
    }

    // ── Primary color ─────────────────────────────────────────────────

    suspend fun setPrimaryColor(colorArgb: Long?) {
        context.adminDataStore.edit { prefs ->
            if (colorArgb != null) prefs[PRIMARY_COLOR_KEY] = colorArgb.toString()
            else prefs.remove(PRIMARY_COLOR_KEY)
        }
    }

    val primaryColorFlow: Flow<Long?> = context.adminDataStore.data.map { prefs ->
        prefs[PRIMARY_COLOR_KEY]?.toLongOrNull()
    }

    // ── Daily messages ────────────────────────────────────────────────

    suspend fun setDailyMessages(messages: List<String>) {
        context.adminDataStore.edit { prefs ->
            prefs[DAILY_MESSAGES_KEY] = gson.toJson(messages)
        }
    }

    val dailyMessagesFlow: Flow<List<String>> = context.adminDataStore.data.map { prefs ->
        parseStringList(prefs[DAILY_MESSAGES_KEY])
    }

    // ── Bot welcome text ──────────────────────────────────────────────

    suspend fun setBotWelcomeText(text: String) {
        context.adminDataStore.edit { prefs ->
            prefs[BOT_WELCOME_KEY] = text
        }
    }

    val botWelcomeFlow: Flow<String> = context.adminDataStore.data.map { prefs ->
        prefs[BOT_WELCOME_KEY] ?: ""
    }

    // ── Reset ─────────────────────────────────────────────────────────

    suspend fun resetToDefaults() {
        context.adminDataStore.edit { prefs ->
            prefs.remove(HIDDEN_TOOLS_KEY)
            prefs.remove(PRIMARY_COLOR_KEY)
            prefs.remove(DAILY_MESSAGES_KEY)
            prefs.remove(BOT_WELCOME_KEY)
            // Intentionally keep PIN – admin doesn't lose their PIN on reset
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun parseStringSet(json: String?): Set<String> {
        if (json.isNullOrBlank()) return emptySet()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            (gson.fromJson<List<String>>(json, type) ?: emptyList()).toSet()
        } catch (_: Exception) { emptySet() }
    }

    private fun parseStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
}
