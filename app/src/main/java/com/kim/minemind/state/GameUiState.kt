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

    // UI chrome
    val showNewGameDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showAboutDialog: Boolean = false,

    val isEnumerating: Boolean = true,
    val isVerify: Boolean = false,
    val isConflict: Boolean = true,
    val isAutoBot: Boolean = false,

    val infoCell: UiCell? = null,
    val focusCellId: Int? = null,
) {
    fun shouldAnalyze() =
        (isEnumerating) && (phase == GamePhase.PLAYING ||  phase == GamePhase.READY)
}


@Serializable
data class PersistedGameState(
    val rows: Int,
    val cols: Int,
    val mineCount: Int,
    val seed: Long,

    val firstClickId: Int?,

    val moves: List<MoveEvent>,
    val cursor: Int,

    val checkpoint: BoardSnapshot?,  // 👈 optional
    val checkpointCursor: Int        // move index snapshot represents
)

@Serializable
data class MoveEvent(
    val id: Int,
    val action: Action // OPEN, FLAG, CHORD
)
