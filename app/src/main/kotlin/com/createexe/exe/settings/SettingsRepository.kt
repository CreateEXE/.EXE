package com.createexe.exe.settings
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "exe_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val KEY_LEFT_WEIGHT = floatPreferencesKey("left_weight")
    }

    val offlineMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_OFFLINE_MODE] ?: true }
    
    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_OFFLINE_MODE] = enabled }
    }
}
