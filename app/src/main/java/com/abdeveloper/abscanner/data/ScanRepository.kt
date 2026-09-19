package com.abdeveloper.abscanner.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScanRepository(
    private val scanDao: ScanDao,
    private val preferences: ScannerPreferences
) {
    val historyFlow: Flow<List<ScanItem>> = scanDao.getAllHistory()
    val savedFlow: Flow<List<ScanItem>> = scanDao.getSavedItems()
    val settingsFlow: Flow<UserSettings> = preferences.settingsFlow

    fun search(query: String): Flow<List<ScanItem>> = scanDao.searchHistory(query)

    suspend fun insertScan(item: ScanItem): Long = scanDao.insert(item)

    suspend fun toggleSaved(id: Long, currentStatus: Boolean) {
        scanDao.updateSaved(id, !currentStatus)
    }

    suspend fun deleteScan(id: Long) = scanDao.delete(id)

    suspend fun clearHistoryOnly() = scanDao.clearHistoryOnly()

    suspend fun clearAll() = scanDao.clearAll()

    suspend fun exportJson(items: List<ScanItem>): String {
        val json = Json { prettyPrint = true }
        return json.encodeToString(items)
    }

    suspend fun importJson(jsonString: String): Int {
        val json = Json { ignoreUnknownKeys = true }
        val list = json.decodeFromString<List<ScanItem>>(jsonString)
        var count = 0
        for (item in list) {
            scanDao.insert(item.copy(id = 0))
            count++
        }
        return count
    }

    suspend fun updateTheme(theme: String) = preferences.updateThemeMode(theme)
    suspend fun updateLanguage(lang: String) = preferences.updateLanguage(lang)
    suspend fun updateAutoOpenLinks(enabled: Boolean) = preferences.updateAutoOpenLinks(enabled)
    suspend fun updateWifiBehavior(behavior: String) = preferences.updateWifiBehavior(behavior)
    suspend fun updateBeep(enabled: Boolean) = preferences.updateBeep(enabled)
    suspend fun updateVibrate(enabled: Boolean) = preferences.updateVibrate(enabled)
    suspend fun updateKeepScreenOn(enabled: Boolean) = preferences.updateKeepScreenOn(enabled)
    suspend fun updateScanSpeed(speed: String) = preferences.updateScanSpeed(speed)
    suspend fun updateSearchEngine(engine: String) = preferences.updateSearchEngine(engine)
    suspend fun updateStartInBatchMode(enabled: Boolean) = preferences.updateStartInBatchMode(enabled)
    suspend fun updateFlagSecure(enabled: Boolean) = preferences.updateFlagSecure(enabled)
}
