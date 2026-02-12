package com.kim.minemind.domain

data class Cell(
    val id: Int,
    val isMine: Boolean,
    val adjacentMines: Int,
    val state: CellState
)
