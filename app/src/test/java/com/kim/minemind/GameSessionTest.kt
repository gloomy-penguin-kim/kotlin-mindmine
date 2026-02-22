package com.kim.minemind

import com.kim.minemind.domain.Action
import com.kim.minemind.state.GameSession
import com.kim.minemind.state.MoveEvent

import org.junit.Assert.*
import org.junit.Test

class GameSessionTest {

    @Test
    fun first_click_generates_same_mines() {

        val s1 = GameSession(10, 10, 10, seed = 42L)
        val s2 = GameSession(10, 10, 10, seed = 42L)

        s1.applyMove(MoveEvent(action = Action.OPEN, id = 15))
        s2.applyMove(MoveEvent(action = Action.OPEN, id = 15))

        assertEquals(
            s1.currentBoard.allCells(),
            s2.currentBoard.allCells()
        )
    }

    @Test
    fun undo_restores_previous_board() {

        val session = GameSession(25, 25, 140, 42L)

        session.applyMove(MoveEvent(action = Action.OPEN, id = 15))
        val boardAfterFirst = session.currentBoard

        session.applyMove(MoveEvent(action = Action.OPEN, id = 20))
        session.undo()

        val x = boardAfterFirst.revealedCellIds()
        val y = session.currentBoard.revealedCellIds()

        assertEquals(x, y)

        assertEquals(boardAfterFirst.toSnapshot(), session.currentBoard.toSnapshot())

        assertEquals(boardAfterFirst.allCells(), session.currentBoard.allCells())
    }

    @Test
    fun redo_restores_forward_state() {

        val session = GameSession(10, 10, 10, 42L)

        session.applyMove(MoveEvent(action = Action.OPEN, id = 15))
        session.applyMove(MoveEvent(action = Action.OPEN, id = 20))

        val finalBoard = session.currentBoard

        session.undo()
        session.redo()

        val x = finalBoard.revealedCellIds()
        val y = session.currentBoard.revealedCellIds()

        assertEquals(x, y)

        assertEquals(finalBoard.toSnapshot(), session.currentBoard.toSnapshot())

        assertEquals(finalBoard.allCells(), session.currentBoard.allCells())
    }

    @Test
    fun replay_matches_original() {

        val moves = listOf(
            MoveEvent(action = Action.OPEN, id = 15),
            MoveEvent(action = Action.OPEN, id = 22),
            MoveEvent(action = Action.OPEN, id = 16)
        )

        val s1 = GameSession(10,10,10,42L)
        val s2 = GameSession(10,10,10,42L)

        moves.forEach { s1.applyMove(it) }

        moves.forEach { s2.applyMove(it) }

        assertEquals(s1.currentBoard.toSnapshot(), s2.currentBoard.toSnapshot())
    }


}