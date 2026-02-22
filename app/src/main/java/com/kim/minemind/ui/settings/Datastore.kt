package com.kim.minemind.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.visualSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "visual_settings"
)

val Context.gameStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "game_state"
)
