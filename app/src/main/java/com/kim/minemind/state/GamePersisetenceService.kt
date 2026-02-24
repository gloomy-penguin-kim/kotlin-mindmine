package com.kim.minemind.state

import com.kim.minemind.domain.Board
import com.kim.minemind.shared.SerializationModule
import com.kim.minemind.ui.settings.GameStateRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GamePersistenceService @Inject constructor(
    private val repo: GameStateRepository,
    private val json: Json
) {

    suspend fun save(session: GameSession, menuState: MenuState) {
        val persistence = PersistedAppState(session.snapshot(), menuState.toSnapshot())
        val jsonString = json.encodeToString<PersistedAppState>(persistence)
        repo.save(jsonString)
    }

    suspend fun load(): Pair<GameSession, MenuState>? {
        val jsonString = repo.load() ?: return null

        return try {
            val snap = json.decodeFromString<PersistedAppState>(jsonString)

            val session = snap.gameSession?.let {
                GameSession.fromSnapshot(it)
            } ?: return null

            val menu = snap.menuState?.let {
                MenuState.fromSnapshot(it)
            } ?: MenuState()

            session to menu

        } catch (e: Exception) {
            null
        }
    }
}