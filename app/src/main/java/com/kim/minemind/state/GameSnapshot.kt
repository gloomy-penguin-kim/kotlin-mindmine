package com.kim.minemind.state

import kotlinx.serialization.Serializable

@Serializable
data class GameSnapshot(
    val board: BoardSnapshot,
    val phase: GamePhase,
    val moveCount: Int,
    val firstClickDone: Boolean,
    val menuState: MenuStateSnapshot
)

@Serializable
data class BoardSnapshot(
    val rows: Int,
    val cols: Int,
    val mines: Set<Int>,
    val revealed: Set<Int>,
    val flagged: Set<Int>
)
