package com.kim.minemind.state

import com.kim.minemind.domain.Action
import kotlinx.serialization.Serializable

data class GameUiState(
    // board geometry
    val rows: Int = 0,
    val cols: Int = 0,

    // flattened grid for Compose performance
    val cells: List<UiCell> = emptyList(),

    // game status
    val phase: GamePhase = GamePhase.READY,
    val moveCount: Int = 0,

    val infoCell: UiCell? = null,
    val focusCellId: Int? = null,

    val elapsedSeconds: Int = 0
)

@Serializable
data class PersistedAppState(
    val gameSession: GameSessionSnapshot? = null,
    val menuState: MenuStateSnapshot? = null
)

@Serializable
data class BoardSnapshot(
    val rows: Int,
    val cols: Int,
    val mines: Set<Int>,
    val revealed: Set<Int>,
    val flagged: Set<Int>
)

@Serializable
data class GameSessionSnapshot(
    val rows: Int,
    val cols: Int,
    val mineCount: Int,
    val seed: Long,
    val firstClickId: Int? = null,
    val moves: MutableList<MoveEvent> = mutableListOf(),
    val cursor: Int? = moves.size
)

@Serializable
data class MoveEvent(
    val id: Int,
    val action: Action // OPEN, FLAG, CHORD
)
