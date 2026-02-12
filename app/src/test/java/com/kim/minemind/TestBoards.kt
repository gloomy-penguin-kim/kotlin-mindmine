package com.kim.minemind

import com.kim.minemind.domain.Board

object TestBoards {

    fun empty(rows: Int, cols: Int): Board =
        Board.newGame(rows, cols, emptySet())

    fun revealed(
        rows: Int,
        cols: Int,
        reveal: List<Int>
    ): Board {
        var b = empty(rows, cols)
        for (id in reveal)
            b = b.reveal(id)
        return b
    }

    fun flagged(
        rows: Int,
        cols: Int,
        flags: List<Int>
    ): Board {
        var b = empty(rows, cols)
        for (id in flags)
            b = b.toggleFlag(id)
        return b
    }
}
