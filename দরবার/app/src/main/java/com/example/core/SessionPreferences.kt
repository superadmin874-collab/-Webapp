package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dorbarDataStore: DataStore<Preferences> by preferencesDataStore(name = "dorbar_session_prefs")

data class LocalSessionPrefs(
    val themeMode: String = "SYSTEM",
    val reducedMotion: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val preferredWorkspaceMode: String = "STUDENT",
    val cachedStudentName: String = "",
    val cachedStudentCode: String = ""
)

class SessionPreferences(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val WORKSPACE_MODE = stringPreferencesKey("workspace_mode")
        val CACHED_STUDENT_NAME = stringPreferencesKey("cached_student_name")
        val CACHED_STUDENT_CODE = stringPreferencesKey("cached_student_code")
    }

    val prefsFlow: Flow<LocalSessionPrefs> = context.dorbarDataStore.data.map { prefs ->
        LocalSessionPrefs(
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            preferredWorkspaceMode = prefs[Keys.WORKSPACE_MODE] ?: "STUDENT",
            cachedStudentName = prefs[Keys.CACHED_STUDENT_NAME] ?: "",
            cachedStudentCode = prefs[Keys.CACHED_STUDENT_CODE] ?: ""
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dorbarDataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dorbarDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dorbarDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setWorkspaceMode(mode: String) {
        context.dorbarDataStore.edit { it[Keys.WORKSPACE_MODE] = mode }
    }

    suspend fun cacheStudentIdentity(fullName: String, studentCode: String) {
        context.dorbarDataStore.edit {
            it[Keys.CACHED_STUDENT_NAME] = fullName
            it[Keys.CACHED_STUDENT_CODE] = studentCode
        }
    }

    suspend fun clearStudentCache() {
        context.dorbarDataStore.edit {
            it.remove(Keys.CACHED_STUDENT_NAME)
            it.remove(Keys.CACHED_STUDENT_CODE)
            it[Keys.WORKSPACE_MODE] = "STUDENT"
        }
    }
}
