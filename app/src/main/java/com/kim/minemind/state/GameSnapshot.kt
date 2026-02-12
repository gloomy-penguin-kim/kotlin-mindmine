package com.kim.minemind.state

import com.kim.minemind.domain.Board

data class GameSnapshot(
    val board: Board,
    val phase: GamePhase,
    val moveCount: Int
)
