package com.kim.minemind.state

import com.kim.minemind.domain.Board
import kotlinx.serialization.Serializable

@Serializable
data class GameSnapshot(
    val board: Board,
    val phase: GamePhase,
    val moveCount: Int,
    val firstClickDone: Boolean,
    val menuState: MenuState
)
