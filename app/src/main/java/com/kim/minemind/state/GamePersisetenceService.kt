package com.kim.minemind.state

import com.kim.minemind.shared.SerializationModule
import com.kim.minemind.ui.settings.GameStateRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GamePersistenceService @Inject constructor(
    private val repo: GameStateRepository,
    private val json: Json
) {

    suspend fun save(session: GameSession) {

    }

    suspend fun load(): GameSession? {
        return null
    }
}