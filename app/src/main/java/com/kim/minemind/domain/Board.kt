package com.kim.minemind.domain

import com.kim.minemind.analysis.caching.BoardSignature
import kotlinx.serialization.Serializable

@Serializable
class Board private constructor(
    val rows: Int,
    val cols: Int,
    private val cells: List<Cell>,
    private val neighborsCache: Array<IntArray>
) {
    val size: Int
        get() = rows * cols

    fun cell(id: Int): Cell =
        cells[id]

    fun allCells(): List<Cell> =
        cells

    companion object {
        fun newGame(rows: Int, cols: Int, mineIds: Set<Int>): Board {
            require(rows > 0 && cols > 0)
            require(mineIds.all { it in 0 until rows * cols })

            val adjCounts = computeAdjacency(rows, cols, mineIds)

            val cells = List(rows * cols) { id ->
                Cell(
                    id = id,
                    isMine = id in mineIds,
                    adjacentMines = adjCounts[id],
                    state = CellState.HIDDEN
                )
            }

            val neighborsCache = Array(rows * cols) { id ->
                computeNeighbors(id, rows, cols)
            }

            return Board(rows, cols, cells, neighborsCache)
        }

        private fun computeNeighbors(id: Int, rows: Int, cols: Int): IntArray {
            val r = id / cols
            val c = id % cols

            val tmp = IntArray(8)
            var n = 0

            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols) {
                    tmp[n++] = nr * cols + nc
                }
            }
            return tmp.copyOf(n)
        }


        fun fromSnapshot(snap: Board): Board {
            val rows = snap.rows
            val cols = snap.cols

            // 1. Extract mine IDs
            val mineIds = snap.cells
                .filter { it.isMine }
                .map { it.id }
                .toSet()

            // 2. Recompute adjacency counts
            val adjCounts = computeAdjacency(rows, cols, mineIds)

            // 3. Rebuild cells with correct state
            val cells = List(rows * cols) { id ->
                val snapCell = snap.cells[id]
                Cell(
                    id = id,
                    isMine = snapCell.isMine,
                    adjacentMines = adjCounts[id],
                    state = snapCell.state
                )
            }

            // 4. Rebuild neighbors cache
            val neighborsCache = Array(rows * cols) { id ->
                computeNeighbors(id, rows, cols)
            }

            return Board(
                rows = rows,
                cols = cols,
                cells = cells,
                neighborsCache = neighborsCache
            )
        }
    }


    fun isCellVisible(id: Int): Boolean {
        return cells[id].state in listOf(CellState.REVEALED, CellState.FLAGGED)
    }

    fun chord(id: Int): Board {
        val cell = cells[id]
        if (cell.state != CellState.REVEALED) return this
        if (cell.adjacentMines == 0) return this

        val neighbors = neighborsOf(id)

        val flagged = neighbors.count {
            cells[it].state == CellState.FLAGGED
        }

        if (flagged == cell.adjacentMines)
            return this

        var board = this

        for (n in neighbors) {
            val nc = board.cells[n]
            // Skip flags
            if (nc.state == CellState.FLAGGED)
                continue
            // Reveal like normal click
            board = board.reveal(n)
        }

        return board
    }


    fun reveal(id: Int): Board {
        val cell = cells[id]
        if (cell.state != CellState.HIDDEN) return this

        // Mine hit: reveal only this cell
        if (cell.isMine) {
            return copyCell(id) {
                it.copy(state = CellState.EXPLODED)
            }
        }

        // Safe reveal: flood fill if needed
        return floodReveal(id)
    }

    fun toggleFlag(id: Int): Board {
        val cell = cells[id]
        if (cell.state == CellState.REVEALED ||
            cell.state == CellState.EXPLODED) return this

        // TODO: this thing right here
        val newState = when (cell.state) {
            CellState.HIDDEN -> CellState.FLAGGED
            CellState.FLAGGED -> CellState.HIDDEN
            CellState.REVEALED -> CellState.REVEALED
            CellState.EXPLODED -> CellState.EXPLODED
        }

        return copyCell(id) {
            it.copy(state = newState)
        }
    }

    private fun copyCell(id: Int, transform: (Cell) -> Cell): Board {
        val newCells = cells.toMutableList()
        newCells[id] = transform(cells[id])
        return Board(rows, cols, newCells, neighborsCache)
    }

    private fun floodReveal(startId: Int): Board {
        val newCells = cells.toMutableList()
        val stack = ArrayDeque<Int>()
        stack.add(startId)

        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            val cell = newCells[id]

            if (cell.state == CellState.REVEALED) continue
            if (cell.state == CellState.FLAGGED) continue

            newCells[id] = cell.copy(state = CellState.REVEALED)

            if (cell.adjacentMines == 0) {
                neighborsOf(id).forEach { stack.add(it) }
            }
        }
        return Board(rows, cols, newCells, neighborsCache)
    }

    fun neighborsOf(id: Int): IntArray =
        neighborsCache[id]


    fun signature(): BoardSignature {
        var h = 1

        for (c in cells) {
            // encode visible state only
            val v = when (c.state) {
                CellState.HIDDEN -> 0
                CellState.FLAGGED -> 1
                CellState.REVEALED -> 2
                CellState.EXPLODED -> 3
            }

            // include number ONLY if revealed
            val n = if (c.state == CellState.REVEALED) c.adjacentMines else 0

            h = 31 * h + (v shl 4) + n
        }

        return BoardSignature(rows, cols, h)
    }

    fun copy(): Board {
        return Board(
            rows,
            cols,
            cells.map { it.copy() },
            Array(neighborsCache.size) { i ->
                neighborsCache[i].copyOf()
            }
        )
    }


}

private fun computeAdjacency(
    rows: Int,
    cols: Int,
    mines: Set<Int>
): IntArray {
    val counts = IntArray(rows * cols)

    for (mine in mines) {
        val r = mine / cols
        val c = mine % cols
        for (dr in -1..1)
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols) {
                    counts[nr * cols + nc]++
                }
            }
    }
    return counts
}