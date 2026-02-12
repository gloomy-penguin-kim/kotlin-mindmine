package com.kim.minemind.state

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

    val isEnumerating: Boolean = true
) {
    fun shouldAnalyze() =
        isEnumerating && (phase == GamePhase.PLAYING ||  phase == GamePhase.READY)
}
