package com.kim.minemind.solver

import com.kim.minemind.analysis.frontier.Frontier
import com.kim.minemind.domain.Board
import com.kim.minemind.domain.Cell
import org.junit.Assert.*
import org.junit.Test
class FrontierTest {

    private fun baseBoard(
        rows: Int,
        cols: Int,
        mines: Set<Int> = emptySet()
    ): Board =
        Board.newGame(rows, cols, mines)

    private fun Board.withRevealed(id: Int): Board =
        reveal(id)

    private fun Board.withFlag(id: Int): Board =
        toggleFlag(id)

    // ---------- TESTS ----------

    @Test
    fun `one revealed number builds single component`() {

        // center cell sees 2 mines
        val mines = setOf(0, 1)

        val board =
            baseBoard(3,3, mines)
                .withRevealed(4)

        val components = Frontier().build(board)

        assertEquals(1, components.size)

        val comp = components[0]

        assertEquals(8, comp.k)
        assertEquals(1, comp.constraints.size)

        val c = comp.constraints[0]
        assertEquals(2, c.remaining)
        assertEquals(8, c.mask.cardinality())
    }

    @Test
    fun `flag reduces unknown count`() {

        val mines = setOf(0, 1)

        val board =
            baseBoard(3,3, mines)
                .withRevealed(4)
                .withFlag(1) // flag one of the actual mines

        val components = Frontier().build(board)

        assertEquals(1, components.size)

        val comp = components[0]
        assertEquals(7, comp.k)

        val c = comp.constraints[0]
        assertEquals(1, c.remaining)
        assertEquals(7, c.mask.cardinality())
    }

    @Test
    fun `independent regions split components`() {

        val mines = setOf(
            6,   // near top-left
            18   // near bottom-right
        )

        val board =
            baseBoard(5,5, mines)
                .withRevealed(0)
                .withRevealed(24)

        val comps = Frontier().build(board)

        assertEquals(2, comps.size)
    }


    @Test
    fun `duplicate scopes deduplicated`() {

        val mines = setOf(4)

        val board =
            baseBoard(3,3, mines)
                .withRevealed(1)
                .withRevealed(7)

        val comps = Frontier().build(board)

        val comp = comps.single()

        assertEquals(2, comp.constraints.size)
    }
}
