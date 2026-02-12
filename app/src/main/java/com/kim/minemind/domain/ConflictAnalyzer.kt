package com.kim.minemind.domain

import com.kim.minemind.analysis.Conflict
import com.kim.minemind.analysis.ConflictSource

fun findFlagConflicts(board: Board): MutableMap<Int, Conflict> {

    val conflicts: MutableMap<Int, Conflict> = LinkedHashMap()

    for (cell in board.allCells()) {

        if (cell.state != CellState.REVEALED) continue
        if (cell.adjacentMines == 0) continue

        val flags = board.neighborsOf(cell.id).count {
            board.cell(it).state == CellState.FLAGGED
        }

        val hidden = board.neighborsOf(cell.id).count {
            board.cell(it).state == CellState.HIDDEN
        }

        if (flags > cell.adjacentMines) {
            conflicts[cell.id] = Conflict(cell.id, ConflictSource.BOARD, mutableSetOf("Too many mines for known mine count"))
        }

        if (hidden < cell.adjacentMines - flags) {
            conflicts[cell.id] = Conflict(cell.id, ConflictSource.BOARD, mutableSetOf("Not enough hidden cells to reveal ro match mine count"))
        }
    }

    return conflicts
}
