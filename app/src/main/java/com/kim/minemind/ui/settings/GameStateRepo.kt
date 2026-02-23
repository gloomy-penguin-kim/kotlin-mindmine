package com.kim.minemind.ui.settings


import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// GameStateRepository.kt
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GameStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val SNAPSHOT = stringPreferencesKey("game_snapshot")
    }

    val snapshotFlow: Flow<String?> =
        dataStore.data.map { it[SNAPSHOT] }

    suspend fun save(json: String) {
        dataStore.edit { prefs ->
            prefs[SNAPSHOT] = json
        }
    }

    //    Even better: be defensive with IO errors
//    DataStore can throw IOException (corruption, disk issues).
//    The recommended pattern is:
    suspend fun load(): String? {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[SNAPSHOT]
    }

//    suspend fun remove() {
//        dataStore.edit { prefs ->
//            prefs.remove(SNAPSHOT)
//        }
//    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(SNAPSHOT)
        }
    }

}
