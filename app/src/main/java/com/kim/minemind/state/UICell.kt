package com.kim.minemind.state

data class UiCell(
    val id: Int,
    val isRevealed: Boolean,
    val isFlagged: Boolean,
    val isMine: Boolean,
    val adjacentMines: Int,
    val isExploded: Boolean,

    val overlay: CellOverlay = CellOverlay(),
    val conflict: Boolean = false
)