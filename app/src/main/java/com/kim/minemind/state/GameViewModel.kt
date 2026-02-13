package com.kim.minemind.state

import androidx.compose.ui.graphics.Color
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
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private var board: Board? = null
    private var phase: GamePhase = GamePhase.READY
    private var moveCount: Int = 0

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _menuState = MutableStateFlow(MenuState())
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    private val analyzer = Analyzer()
    private var lastSignature: BoardSignature? = null
    private var lastOverlay: AnalyzerOverlay? = null

    private var firstClickDone = false

    private val history = ArrayDeque<GameSnapshot>()

    private val redo = ArrayDeque<GameSnapshot>()   // optional but free

    private val componentColors = mutableMapOf<Int, Color>()

    private var prevGroups: Map<Int, Set<Int>> = emptyMap()
    private var prevColors: Map<Int, Color> = emptyMap()

    private var nextColorIndex = 0

    private val palette = listOf(
        Color(0xFFEF5350),
        Color(0xFF42A5F5),
        Color(0xFF66BB6A),
        Color(0xFFFFCA28),
        Color(0xFFAB47BC),
        Color(0xFF26C6DA),
        Color(0xFFFF7043),
        Color(0xFF8D6E63)
    )




    fun onMenuAction(menuState: MenuItem) {

        println(menuState)
        if (menuState in setOf(MenuItem.OPEN, MenuItem.FLAG, MenuItem.CHORD, MenuItem.INFO)) {
            _menuState.value = _menuState.value.copy(
                selected = menuState
            )
        }
        else if (menuState == MenuItem.ANALYZE) {
            _menuState.value = _menuState.value.copy(
                isAnalyze = !_menuState.value.isAnalyze
            )
            _uiState.value = _uiState.value.copy(
                isEnumerating = false
            )
        }
        else if (menuState == MenuItem.VERIFY) {
            _menuState.value = _menuState.value.copy(
                isVerify = !_menuState.value.isVerify
            )
        }
        else if (menuState == MenuItem.CONFLICT) {
            _menuState.value = _menuState.value.copy(
                isConflict = !_menuState.value.isConflict
            )
        }
        else if (menuState == MenuItem.UNDO && !_menuState.value.isUndo) {
            _menuState.value = _menuState.value.copy(
                isUndo = true
            )
            undo()
            _menuState.value = _menuState.value.copy(
                isUndo = false
            )
        }
    }

    init {
        startNewGame(rows = 25, cols = 25, mineCount = 120)
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

        println(menuState)
        if (menuState.value.selected == MenuItem.INFO) {
            showInfo(id)
            return
        }

        if (phase != GamePhase.PLAYING) return

        // First-click safe regen
        if (!firstClickDone && menuState.value.selected == MenuItem.OPEN) {
            board = regenerateSafeBoard(id)
            firstClickDone = true
            invalidateCaches()
        }

        val before = board!!

        val after = when(menuState.value.selected) {
            MenuItem.OPEN -> before.reveal(id)
            MenuItem.FLAG -> before.toggleFlag(id)
            MenuItem.CHORD -> before.chord(id)
            else -> before
        }

        if (after !== before) {
            pushHistory()
            board = after
            moveCount++
        }

         if (board!!.allCells().any { it.state == CellState.EXPLODED })
            phase = GamePhase.LOST
        else if (checkWin(board!!))
            phase = GamePhase.WON


        emitUiState()
    }


    fun onToggleFlag(id: Int) {
        val currentBoard = board ?: return

        println("TOGGLE state before = ${currentBoard.cell(id).state}")

        if (phase == GamePhase.LOST || phase == GamePhase.WON) return

        pushHistory()

        board = currentBoard.toggleFlag(id)

        println("TOGGLE state after = ${currentBoard.cell(id).state}")
        emitUiState()
    }


    private fun checkWin(board: Board): Boolean {
        return board.allCells().all { cell ->
            cell.isMine || cell.state == CellState.REVEALED
        }
    }

    private fun pushHistory() {
        board?.let {
            history.addLast(
                GameSnapshot(
                    it.copy(),          // ⭐ CRITICAL CHANGE
                    phase,
                    moveCount,
                    firstClickDone
                )
            )
        }
    }

    fun undo() {

        if (history.isEmpty()) return

        board?.let {
            redo.addLast(
                GameSnapshot(
                    board = it.copy(),
                    phase = phase,
                    moveCount = moveCount,
                    firstClickDone = firstClickDone
                )
            )
        }

        invalidateCaches()

        val snap = history.removeLast()
        board = snap.board.copy()
        phase = snap.phase
        moveCount = snap.moveCount

        emitUiState()
    }


    private fun invalidateCaches() {
        lastSignature = null
        lastOverlay = null
    }

    fun redo() {

        if (redo.isEmpty()) return

        // Save current state to undo stack
        board?.let {
            history.addLast(
                GameSnapshot(
                    board = it,
                    phase = phase,
                    moveCount = moveCount,
                    firstClickDone = firstClickDone
                )
            )
        }

        // Restore snapshot
        val snap = redo.removeLast()
        board = snap.board.copy()
        phase = snap.phase
        moveCount = snap.moveCount
        firstClickDone = snap.firstClickDone

        emitUiState()
    }



    fun figureOutColors(groups: Map<Int, Set<Int>>): Map<Int, Color> {

        val idToColor = mutableMapOf<Int, Color>()
        val newPrev = mutableMapOf<Int, Set<Int>>()

        for ((cid, cells) in groups) {

            val match = prevGroups
                .maxByOrNull { (_, oldCells) ->
                    cells.intersect(oldCells).size
                }

            val color =
                if (match != null &&
                    cells.intersect(match.value).isNotEmpty()
                ) {
                    // reuse previous color
                    prevColors[match.key]!!
                } else {
                    // new component
                    palette[nextColorIndex++ % palette.size]
                }

            idToColor[cid] = color
            newPrev[cid] = cells
        }

        prevGroups = newPrev
        prevColors = idToColor

        return idToColor
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

        val frontier = Frontier().build(board!!)
        val componentMap = buildComponentMap(frontier)

        // Reverse grouping
        val groups = componentMap.entries.groupBy(
            { it.value },
            { it.key }
        ).mapValues { it.value.toSet() }

        val componentIdToColor = figureOutColors(groups)

        println(overlay.reasons)

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
                    conflict = conflict,
                    componentId = componentMap[cell.id],
                    componentColor = componentMap[cell.id]?.let { componentIdToColor[it] },
                    reasons = overlay.reasons[cell.id]
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

    fun showInfo(cellId: Int) {
        val cell = _uiState.value.cells.firstOrNull { it.id == cellId }
        _uiState.value = _uiState.value.copy(infoCell = cell)
    }

    fun hideInfo() {
        _uiState.value = _uiState.value.copy(infoCell = null)
    }


}


