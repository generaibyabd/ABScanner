package com.abdeveloper.abscanner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "scanner_settings")

data class UserSettings(
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val languageCode: String = "en",
    val autoOpenLinks: Boolean = false,
    val wifiBehavior: String = "ASK", // OFF, ASK, OFFER
    val beep: Boolean = true,
    val vibrate: Boolean = true,
    val keepScreenOn: Boolean = true,
    val scanSpeed: String = "BALANCED", // FAST, BALANCED, BATTERY
    val defaultSearchEngine: String = "GOOGLE", // GOOGLE, DUCKDUCKGO, BING, BRAVE
    val startInBatchMode: Boolean = false,
    val flagSecure: Boolean = false
)

class ScannerPreferences(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_LANG = stringPreferencesKey("lang_code")
        val KEY_AUTO_OPEN_LINKS = booleanPreferencesKey("auto_open_links")
        val KEY_WIFI_BEHAVIOR = stringPreferencesKey("wifi_behavior")
        val KEY_BEEP = booleanPreferencesKey("beep")
        val KEY_VIBRATE = booleanPreferencesKey("vibrate")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_SCAN_SPEED = stringPreferencesKey("scan_speed")
        val KEY_SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val KEY_BATCH_MODE = booleanPreferencesKey("batch_mode_default")
        val KEY_FLAG_SECURE = booleanPreferencesKey("flag_secure")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[KEY_THEME] ?: "SYSTEM",
            languageCode = prefs[KEY_LANG] ?: "en",
            autoOpenLinks = prefs[KEY_AUTO_OPEN_LINKS] ?: false,
            wifiBehavior = prefs[KEY_WIFI_BEHAVIOR] ?: "ASK",
            beep = prefs[KEY_BEEP] ?: true,
            vibrate = prefs[KEY_VIBRATE] ?: true,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: true,
            scanSpeed = prefs[KEY_SCAN_SPEED] ?: "BALANCED",
            defaultSearchEngine = prefs[KEY_SEARCH_ENGINE] ?: "GOOGLE",
            startInBatchMode = prefs[KEY_BATCH_MODE] ?: false,
            flagSecure = prefs[KEY_FLAG_SECURE] ?: false
        )
    }

    suspend fun updateThemeMode(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANG] = lang }
    }

    suspend fun updateAutoOpenLinks(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_OPEN_LINKS] = enabled }
    }

    suspend fun updateWifiBehavior(behavior: String) {
        context.dataStore.edit { it[KEY_WIFI_BEHAVIOR] = behavior }
    }

    suspend fun updateBeep(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BEEP] = enabled }
    }

    suspend fun updateVibrate(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATE] = enabled }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateScanSpeed(speed: String) {
        context.dataStore.edit { it[KEY_SCAN_SPEED] = speed }
    }

    suspend fun updateSearchEngine(engine: String) {
        context.dataStore.edit { it[KEY_SEARCH_ENGINE] = engine }
    }

    suspend fun updateStartInBatchMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BATCH_MODE] = enabled }
    }

    suspend fun updateFlagSecure(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLAG_SECURE] = enabled }
    }
}
