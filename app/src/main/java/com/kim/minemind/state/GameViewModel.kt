package com.kim.minemind.state

import androidx.lifecycle.ViewModel
import com.kim.minemind.analysis.Analyzer
import com.kim.minemind.analysis.AnalyzerOverlay
import com.kim.minemind.analysis.caching.BoardSignature
import com.kim.minemind.analysis.debug.buildComponentMap
import com.kim.minemind.analysis.frontier.Frontier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kim.minemind.domain.Board
import com.kim.minemind.domain.CellState
import com.kim.minemind.domain.findFlagConflicts
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private var board: Board? = null
    private var phase: GamePhase = GamePhase.READY
    private var moveCount: Int = 0

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val analyzer = Analyzer()
    private var lastSignature: BoardSignature? = null
    private var lastOverlay: AnalyzerOverlay? = null

    private var firstClickDone = false




    private val history = ArrayDeque<GameSnapshot>()

    private val redo = ArrayDeque<GameSnapshot>()   // optional but free


    init {
        startNewGame(rows = 25, cols = 25, mineCount = 90)
    }

    fun startNewGame(
        rows: Int,
        cols: Int,
        mineCount: Int,
        seed: Long = System.currentTimeMillis()
    ) {
        val total = rows * cols
        require(mineCount in 1 until total)

        val mineIds = generateMines(total, mineCount, seed)

        board = Board.newGame(rows, cols, mineIds)
        phase = GamePhase.PLAYING
        moveCount = 0
        history.clear()
        redo.clear()
        firstClickDone = false


        emitUiState(
            showNewGameDialog = false
        )
    }

    private fun regenerateSafeBoard(clickedId: Int): Board {

        val current = board ?: error("Board missing")

        // ⭐ Exclusion zone: clicked cell + neighbors
        val forbidden = buildSet {
            add(clickedId)
            current.neighborsOf(clickedId).forEach { add(it) }
        }

        val mineCount = current.allCells().count { it.isMine }

        val candidates =
            (0 until current.size)
                .filterNot { it in forbidden }
                .shuffled()

        require(candidates.size >= mineCount) {
            "Board too small for exclusion zone with this mine count"
        }

        val newMines = candidates.take(mineCount).toSet()

        return Board.newGame(
            current.rows,
            current.cols,
            newMines
        )
    }

    // TODO: first click safety
    private fun generateMines(
        total: Int,
        count: Int,
        seed: Long
    ): Set<Int> {
        val rnd = Random(seed)
        return generateSequence { rnd.nextInt(total) }
            .distinct()
            .take(count)
            .toSet()
    }

    fun onCellTap(id: Int) {
        val currentBoard = board ?: return
        if (phase != GamePhase.PLAYING) return

        val cell = currentBoard.cell(id)
        if (cell.state == CellState.FLAGGED) return

        pushHistory()

        // ⭐ First-click exclusion zone
        if (!firstClickDone) {

            board = regenerateSafeBoard(id)
            firstClickDone = true

            // Invalidate solver cache
            lastSignature = null
            lastOverlay = null
        }

        val newBoard = board!!.reveal(id)
        board = newBoard
        moveCount++

        if (cell.isMine) {
            phase = GamePhase.LOST
        } else if (checkWin(newBoard)) {
            phase = GamePhase.WON
        }

        emitUiState()
    }



    fun onToggleFlag(id: Int) {
        val currentBoard = board ?: return
        if (phase != GamePhase.PLAYING) return

        pushHistory()                     // ⭐ ADD THIS

        board = currentBoard.toggleFlag(id)
        emitUiState()
    }


    private fun checkWin(board: Board): Boolean {
        return board.allCells().all { cell ->
            cell.isMine || cell.state == CellState.REVEALED
        }
    }



    private fun pushHistory() {
        board?.let {
            history.addLast(GameSnapshot(it, phase, moveCount))
        }
    }

    fun undo() {

        if (history.isEmpty()) return

        board?.let {
            redo.addLast(
                GameSnapshot(
                    board = it,
                    phase = phase,
                    moveCount = moveCount
                )
            )
        }

        val snap = history.removeLast()
        board = snap.board
        phase = snap.phase
        moveCount = snap.moveCount

        emitUiState()
    }



    fun redo() {

        if (redo.isEmpty()) return

        // Save current state to undo stack
        board?.let {
            history.addLast(
                GameSnapshot(
                    board = it,
                    phase = phase,
                    moveCount = moveCount
                )
            )
        }

        // Restore snapshot
        val snap = redo.removeLast()
        board = snap.board
        phase = snap.phase
        moveCount = snap.moveCount

        emitUiState()
    }



    private fun buildOverlay(board: Board): AnalyzerOverlay {

        if (!_uiState.value.isEnumerating)
            return AnalyzerOverlay()

        val sig = board.signature()

        // Cache hit → skip expensive solver
        if (sig == lastSignature && lastOverlay != null)
            return lastOverlay!!

        val overlay = analyzer.analyze(board)

        lastSignature = sig
        lastOverlay = overlay

        return overlay
    }


    private fun emitUiState(
        showNewGameDialog: Boolean = _uiState.value.showNewGameDialog
    ) {

        val board = board ?: run {
            _uiState.value = GameUiState(showNewGameDialog = showNewGameDialog)
            return
        }

        val ui = _uiState.value

        val overlay =
            if (ui.shouldAnalyze())
                analyzer.analyze(board)
                    .withConflicts(
                        AnalyzerOverlay.detectFlagConflicts(board)
                    )
            else
                AnalyzerOverlay()

        val frontier = Frontier().build(board)
        val componentMap = buildComponentMap(frontier)


        val uiCells = board.allCells().map { cell ->

            val prob = overlay.probabilities[cell.id]
            val forcedOpen = cell.id in overlay.forcedOpens
            val forcedFlag = cell.id in overlay.forcedFlags
//            val conflict = overlay.conflicts.containsKey(cell.id)

            val conflict = overlay.conflicts[cell.id]

            UiCell(
                id = cell.id,
                isRevealed = cell.state == CellState.REVEALED,
                isFlagged = cell.state == CellState.FLAGGED,
                isMine = cell.isMine,
                adjacentMines = cell.adjacentMines,
                isExploded = phase == GamePhase.LOST && cell.isMine,

                conflict = conflict != null,

                overlay = CellOverlay(
                    probability = prob,
                    forcedOpen = forcedOpen,
                    forcedFlag = forcedFlag,
                    conflict = conflict != null,
                    componentId = componentMap[cell.id]
                )
            )
        }
        _uiState.value = _uiState.value.copy(
            rows = board.rows,
            cols = board.cols,
            cells = uiCells
        )

    }



    fun openNewGameDialog() {
        _uiState.value = _uiState.value.copy(showNewGameDialog = true)
    }

    fun closeNewGameDialog() {
        _uiState.value = _uiState.value.copy(showNewGameDialog = false)
    }

}


