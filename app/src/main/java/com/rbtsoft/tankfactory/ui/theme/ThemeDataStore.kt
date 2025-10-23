package com.rbtsoft.tankfactory.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

enum class AppTheme{
    BLACK_AND_WHITE,
    ORANGE
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ThemeSettings(
    val useDynamicColor: Boolean,
    val selectedTheme: AppTheme
)

class ThemeDataStore(private val context: Context) {

    private object PreferenceKeys {
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
    }

    val themeSettingsFlow = context.dataStore.data.map {
        preferences ->
        val useDynamicColor = preferences[PreferenceKeys.USE_DYNAMIC_COLOR] ?: true
        val selectedTheme = AppTheme.valueOf(
            preferences[PreferenceKeys.SELECTED_THEME] ?: AppTheme.BLACK_AND_WHITE.name
        )
        ThemeSettings(useDynamicColor, selectedTheme)
    }

    suspend fun saveThemeSettings(useDynamicColor: Boolean, selectedTheme: AppTheme) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferenceKeys.USE_DYNAMIC_COLOR] = useDynamicColor
            preferences[PreferenceKeys.SELECTED_THEME] = selectedTheme.name
        }
    }
}
