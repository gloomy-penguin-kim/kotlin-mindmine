package com.kim.minemind.domain

import kotlinx.serialization.Serializable

@Serializable
data class Cell(
    val id: Int,
    val isMine: Boolean,
    val adjacentMines: Int,
    val state: CellState
)
