package com.kim.minemind.state

import com.kim.minemind.domain.Action
import com.kim.minemind.domain.Board
import com.kim.minemind.domain.CellState
import com.kim.minemind.domain.RNG
import com.kim.minemind.shared.GridMath


private const val CHECKPOINT_INTERVAL = 50

class GameSession(
    val rows: Int,
    val cols: Int,
    val mineCount: Int,
    val seed: Long
) {

    private var board: Board = Board.newGame(rows, cols, emptySet())


    val firstClickId: Int?
        get() = moves.firstOrNull { it.action == Action.OPEN }?.id

    private val moves = mutableListOf<MoveEvent>()
    private var cursor = 0

    private var phase: GamePhase = GamePhase.READY

    // checkpoint
    private var checkpoint: BoardSnapshot? = null
    private var checkpointCursor: Int = 0


    val currentBoard: Board
        get() = board

    fun applyMove(move: MoveEvent) {
        if (cursor < moves.size) {
            moves.subList(cursor, moves.size).clear()
        }

        moves.add(move)
        cursor++

        board = transform(board, move, cursor - 1)
        updatePhase()

        maybeCheckpoint()
    }
    private fun maybeCheckpoint() {
        if (cursor == 0) return

        if (cursor - checkpointCursor >= CHECKPOINT_INTERVAL) {
            checkpoint = board.toSnapshot()
            checkpointCursor = cursor
        }
    }

    fun updatePhase() {
        phase = when {
            firstClickId == null -> GamePhase.READY
            board.hasExploded() -> GamePhase.LOST
            board.isWin() -> GamePhase.WON
            else -> GamePhase.PLAYING
        }
    }

    fun undo(): Boolean {
        if (cursor == 0) return false
        cursor--
        rebuild()
        return true
    }

    fun redo(): Boolean {
        if (cursor >= moves.size) return false
        cursor++
        rebuild()
        return true
    }

    private fun rebuild() {

        val startIndex: Int

        if (checkpoint != null && checkpointCursor <= cursor) {
            board = Board.fromSnapshot(checkpoint!!)
            startIndex = checkpointCursor
        } else {
            board = Board.newGame(rows, cols, emptySet())
            startIndex = 0
        }
        for (i in startIndex until cursor) {
            board = transform(board, moves[i], i)
        }

        println(board.revealedCellIds())
        updatePhase()
    }

    private fun transform(board: Board, move: MoveEvent, index: Int): Board {
        var current = board

        if (index == 0 && move.action == Action.OPEN) {
            val mines = generateMines(move.id)
            current = Board.newGame(rows, cols, mines)
        }

        return when (move.action) {
            Action.OPEN -> current.reveal(move.id)
            Action.FLAG -> current.toggleFlag(move.id)
            Action.CHORD -> current.chord(move.id)
            else -> current
        }
    }

    fun snapshot(): PersistedGameState =
        PersistedGameState(rows,
            cols,
            mineCount,
            seed,
            firstClickId,
            moves,
            cursor,
            checkpoint,
            checkpointCursor)

    companion object {
        fun fromSnapshot(p: PersistedGameState): GameSession {
            val session = GameSession(p.rows, p.cols, p.mineCount, p.seed)
            session.moves.addAll(p.moves)
            session.cursor = p.cursor
            session.rebuild()
            return session
        }
    }

    private fun checkWin(board: Board): Boolean =
        board.allCells().all { it.isMine || it.state == CellState.REVEALED }

    private fun checkLose(board: Board): Boolean =
        board.allCells().any { it.state == CellState.EXPLODED ||
                (it.state == CellState.REVEALED && it.isMine)}

    private fun generateMines(firstClick: Int): Set<Int> {
        val derivedSeed =
            seed xor
                    (rows.toLong() shl 48) xor
                    (cols.toLong() shl 32) xor
                    (mineCount.toLong() shl 16) xor
                    firstClick.toLong()

        val rng = RNG(derivedSeed)

        val forbidden = buildSet {
            add(firstClick)
            GridMath.neighborsOf(firstClick, rows, cols).forEach { add(it) }
        }

        val candidates = (0 until rows * cols)
            .filterNot { it in forbidden }
            .toMutableList()

        rng.shuffle(candidates)
        return candidates.take(mineCount).toSet()
    }
}