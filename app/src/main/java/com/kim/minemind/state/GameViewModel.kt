package com.kim.minemind.state

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.kim.minemind.domain.RNG
import com.kim.minemind.shared.GridMath
import com.kim.minemind.ui.settings.GameStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield


@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameStateRepo: GameStateRepository
) : ViewModel() {

    // ------------------------------------------------------------
    // Core game state
    // ------------------------------------------------------------
    private var board: Board? = null
    private var phase: GamePhase = GamePhase.READY
    private var moveCount = 0
    private var firstClickDone = false

    private var pendingSeed: Long = 0L
    private var pendingMineCount: Int = 0

    // ------------------------------------------------------------
    // UI state
    // ------------------------------------------------------------
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _menuState = MutableStateFlow(MenuState())
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    // ------------------------------------------------------------
    // Analysis + caching
    // ------------------------------------------------------------
    private val analyzer = Analyzer()
    private var lastSignature: BoardSignature? = null
    private var lastOverlay: AnalyzerOverlay? = null

    // ------------------------------------------------------------
    // Undo / redo
    // ------------------------------------------------------------
    private val history = ArrayDeque<GameSnapshot>()
    private val redo = ArrayDeque<GameSnapshot>()

    // ------------------------------------------------------------
    // Autobot
    // ------------------------------------------------------------
    private var autobotJob: Job? = null

    // ------------------------------------------------------------
    // Component coloring
    // ------------------------------------------------------------
    private var prevGroups: Map<Int, Set<Int>> = emptyMap()
    private var prevColors: Map<Int, Color> = emptyMap()
    private var nextColorIndex = 0

    private val palette = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A),
        Color(0xFFFFCA28), Color(0xFFAB47BC), Color(0xFF26C6DA),
        Color(0xFFFF7043), Color(0xFF8D6E63)
    )

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // ------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------
    init {
        viewModelScope.launch {
            val encoded = gameStateRepo.snapshotFlow.first()
            if (encoded.isNullOrBlank()) {
                startNewGame(25, 25, 140)
            } else {
                runCatching {
                    val snap = json.decodeFromString<GameSnapshot>(encoded)
                    restoreFromSnapshot(snap)
                }.onFailure {
                    startNewGame(25, 25, 140)
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Menu actions
    // ------------------------------------------------------------
    fun onMenuAction(item: MenuItem) {
        when (item) {
            MenuItem.OPEN, MenuItem.FLAG, MenuItem.CHORD, MenuItem.INFO ->
                selectTool(item)

            MenuItem.ANALYZE -> toggleAnalyze()
            MenuItem.COMPONENT -> toggleMenuFlag { it.copy(isComponent = !it.isComponent) }
            MenuItem.VERIFY -> toggleMenuFlag { it.copy(isVerify = !it.isVerify) }
            MenuItem.CONFLICT -> toggleMenuFlag { it.copy(isConflict = !it.isConflict) }

            MenuItem.AUTO -> toggleAutobot()
            MenuItem.UNDO -> performUndo()
            MenuItem.EXPANDED -> toggleMenuFlag { it.copy(isExpanded = !it.isExpanded) }

            else -> {}
        }
    }

    private fun selectTool(item: MenuItem) {
        closeExpandedMenu()
        _menuState.value = _menuState.value.copy(
            selected = item,
            isExpanded = false,
            isAutoBot = false
        )
    }

    private fun toggleAnalyze() {
        val newAnalyze = !_menuState.value.isAnalyze
        _menuState.value = _menuState.value.copy(
            isAnalyze = newAnalyze,
            isAutoBot = false
        )
        autobotJob?.cancel()
        _uiState.value = _uiState.value.copy(isEnumerating = newAnalyze)
        emitUiState()
    }

    private fun toggleMenuFlag(block: (MenuState) -> MenuState) {
        _menuState.value = block(_menuState.value).copy(isAutoBot = false)
    }

    private fun toggleAutobot() {
        val newValue = !_menuState.value.isAutoBot
        _menuState.value = _menuState.value.copy(isAutoBot = newValue)
        if (newValue) autobot()
        else {
            autobotJob?.cancel()
        }
    }

    private fun performUndo() {
        if (_menuState.value.isUndo) return
        closeExpandedMenu()
        _menuState.value = _menuState.value.copy(isUndo = true, isExpanded = false)
        undo()
        _menuState.value = _menuState.value.copy(isUndo = false)
    }

    fun closeExpandedMenu() {
        autobotJob?.cancel()
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = null,
        )
//        _uiState.value = _uiState.value.copy(
//            isAutoBot = false,
//            infoCell = null,
//        )
    }

    // ------------------------------------------------------------
    // New game
    // ------------------------------------------------------------
    fun startNewGame(rows: Int, cols: Int, mineCount: Int, seed: Long = 0L) {
        require(mineCount in 1 until rows * cols)

        board = Board.newGame(rows, cols, emptySet())
        phase = GamePhase.READY
        moveCount = 0
        firstClickDone = false
        history.clear()
        redo.clear()

        pendingSeed = seed
        pendingMineCount = mineCount

        emitUiState(showNewGameDialog = false)
    }

    // ------------------------------------------------------------
    // First click mine generation
    // ------------------------------------------------------------
    private fun generateMines(firstClick: Int): Set<Int> {
        val b = board ?: error("Board missing")

        val derivedSeed = pendingSeed xor (b.rows.toLong() shl 32) xor b.cols.toLong()
        val rng = RNG(derivedSeed)

        val forbidden = buildSet {
            add(firstClick)
            GridMath.neighborsOf(firstClick, b.rows, b.cols).forEach { add(it) }
        }

        val candidates = (0 until b.size).filterNot { it in forbidden }.toMutableList()
        require(candidates.size >= pendingMineCount)

        rng.shuffle(candidates)
        return candidates.take(pendingMineCount).toSet()
    }

    // ------------------------------------------------------------
    // Cell interactions
    // ------------------------------------------------------------
    fun onCellTap(id: Int) {
        closeExpandedMenu()
        val b = board ?: return

        if (menuState.value.selected == MenuItem.INFO) {
            showInfo(id)
            return
        }

        if (!firstClickDone && menuState.value.selected == MenuItem.OPEN) {
            val mines = generateMines(id)
            board = Board.newGame(b.rows, b.cols, mines)
            firstClickDone = true
            phase = GamePhase.PLAYING
            invalidateCaches()
        }

        if (phase != GamePhase.PLAYING) return

        val before = board!!
        val after = when (menuState.value.selected) {
            MenuItem.OPEN -> before.reveal(id)
            MenuItem.FLAG -> before.toggleFlag(id)
            MenuItem.CHORD -> before.chord(id)
            else -> before
        }

        if (after !== before) {
            pushHistory()
            board = after
            moveCount++
            persistSnapshot()
        }

        updatePhase()
        emitUiState()
    }

    private fun updatePhase() {
        val b = board ?: return
        phase = when {
            b.allCells().any { it.state == CellState.EXPLODED } -> GamePhase.LOST
            checkWin(b) -> GamePhase.WON
            else -> GamePhase.PLAYING
        }
    }

    private fun checkWin(board: Board): Boolean =
        board.allCells().all { it.isMine || it.state == CellState.REVEALED }

    // ------------------------------------------------------------
    // Flag toggle
    // ------------------------------------------------------------
    fun onToggleFlag(id: Int) {
        val b = board ?: return
        if (phase == GamePhase.LOST || phase == GamePhase.WON) return

        pushHistory()
        board = b.toggleFlag(id)
        emitUiState()
        persistSnapshot()
    }

    // ------------------------------------------------------------
    // Undo / redo
    // ------------------------------------------------------------

    private val MAX_HISTORY = 300 // cap so datastore doesn't explode

    private fun currentSnapshot(): GameSnapshot {
        val b = board ?: error("Board missing")
        return GameSnapshot(
            board = b.toSnapshot(),
            phase = phase,
            moveCount = moveCount,
            firstClickDone = firstClickDone,
            menuState = MenuState.toSnapshot(_menuState.value)
        )
    }

//    private fun persistAll() {
//        val state = PersistedGameState(
//            current = currentSnapshot(),
//            history = history.takeLast(MAX_HISTORY).toList(),
//            redo = redo.takeLast(MAX_HISTORY).toList()
//        )
//        viewModelScope.launch {
//            gameStateRepo.save(json.encodeToString(state))
//        }
//    }

    private fun pushHistory() {
        board?.let {
            history.addLast(
                GameSnapshot(
                    board!!.toSnapshot(), // it.snapshot()
                    phase,
                    moveCount,
                    firstClickDone,
                    MenuState.toSnapshot(_menuState.value)
                )
            )
        }
    }

    fun undo() {
        if (history.isEmpty()) return

        board?.let {
            redo.addLast(
                GameSnapshot(
                    board!!.toSnapshot(),
                    phase,
                    moveCount,
                    firstClickDone,
                    MenuState.toSnapshot(_menuState.value)
                )
            )
        }

        val snap = history.removeLast()
        restoreFromSnapshot(snap)
        emitUiState()
    }

    fun redo() {
        if (redo.isEmpty()) return

        board?.let {
            history.addLast(
                GameSnapshot(
                    board!!.toSnapshot(),
                    phase,
                    moveCount,
                    firstClickDone,
                    MenuState.toSnapshot(_menuState.value)
                )
            )
        }

        val snap = redo.removeLast()
        restoreFromSnapshot(snap)
        emitUiState()
    }

    private fun restoreFromSnapshot(snap: GameSnapshot) {
        board = Board.fromSnapshot(snap.board)
        phase = snap.phase
        moveCount = snap.moveCount
        firstClickDone = snap.firstClickDone
        _menuState.value = MenuState.fromSnapshot(snap.menuState)
        invalidateCaches()
        emitUiState()
    }

    private fun persistSnapshot() {
        val b = board ?: return
        val snap = GameSnapshot(
            board = b.toSnapshot(),
            phase = phase,
            moveCount = moveCount,
            firstClickDone = firstClickDone,
            menuState = MenuState.toSnapshot(_menuState.value)
        )
        viewModelScope.launch {
            gameStateRepo.save(json.encodeToString<GameSnapshot>( snap))
        }
    }

    // ------------------------------------------------------------
    // Autobot
    // ------------------------------------------------------------
    fun autobot() {
        if (!firstClickDone) return

        autobotJob?.cancel()
        autobotJob = viewModelScope.launch(Dispatchers.Default) {
            runAutobotLoop()
            _menuState.value = _menuState.value.copy(isAutoBot = false, isExpanded = false)
        }
    }

    private suspend fun runAutobotLoop() {
        while (menuState.value.isAutoBot && phase == GamePhase.PLAYING) {
            val moved =
                runForcedOpens() ||
                        runForcedFlags() ||
                        runHighConfidenceMoves() ||
                        runFallbackMove()

            if (!moved) break
        }
    }

    private suspend fun runForcedOpens(): Boolean =
        runAutobotSet(
            { it.forcedOpens },
            { board, id -> board.reveal(id) }
        )

    private suspend fun runForcedFlags(): Boolean =
        runAutobotSet(
            { it.forcedFlags },
            { board, id ->
                if (board.cell(id).state != CellState.FLAGGED) board.toggleFlag(id) else board
            }
        )

    private suspend fun runHighConfidenceMoves(): Boolean =
        runAutobotProbabilities { p -> p <= 0.05 || p >= 0.95 }

    private suspend fun runFallbackMove(): Boolean {
        val b = board ?: return false
        val overlay = analyzer.analyze(b)

        val best = overlay.probabilities
            .filterValues { it != null }
            .map { (id, p) -> Triple(id, p!!, minOf(p, 1f - p)) }
            .minByOrNull { it.third } ?: return false

        val (id, p) = best
        return applyAutobotMove(id) { board ->
            if (!board.cell(id).isMine) board.reveal(id) else board.toggleFlag(id)
        }
    }

    private suspend fun runAutobotSet(
        selector: (AnalyzerOverlay) -> Set<Int>,
        action: (Board, Int) -> Board
    ): Boolean {
        var moved = false
        while (true) {
            val b = board ?: break
            val overlay = analyzer.analyze(b)
            val ids = selector(overlay)
            if (ids.isEmpty()) break

            var localMoved = false
            for (id in ids) {
                if (!menuState.value.isAutoBot || phase != GamePhase.PLAYING) break
                localMoved = applyAutobotMove(id) { action(it, id) } || localMoved
            }
            moved = moved || localMoved
            if (!localMoved) break
        }
        return moved
    }

    private suspend fun runAutobotProbabilities(
        predicate: (Float) -> Boolean
    ): Boolean {
        val b = board ?: return false
        val overlay = analyzer.analyze(b)

        val moves = overlay.probabilities.filterValues { it != null && predicate(it!!) }
        var moved = false

        for ((id, p) in moves) {
            moved = applyAutobotMove(id) { board ->
                if (p!! <= 0.05) board.reveal(id) else board.toggleFlag(id)
            } || moved
        }
        return moved
    }

    private suspend fun applyAutobotMove(
        id: Int,
        transform: (Board) -> Board
    ): Boolean {
        val before = board ?: return false

        if (!menuState.value.isAutoBot || phase != GamePhase.PLAYING) return false

        val after = transform(before)
        if (after === before) return false

        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(focusCellId = id)
            pushHistory()
            board = after
            moveCount++
            emitUiState()
        }

        yield()
        delay(600L)

        persistSnapshot()
        return true
    }

    // ------------------------------------------------------------
    // Component coloring
    // ------------------------------------------------------------
    fun figureOutColors(groups: Map<Int, Set<Int>>): Map<Int, Color> {
        val idToColor = mutableMapOf<Int, Color>()
        val newPrev = mutableMapOf<Int, Set<Int>>()

        for ((cid, cells) in groups) {
            val match = prevGroups.maxByOrNull { (_, oldCells) ->
                cells.intersect(oldCells).size
            }

            val color =
                if (match != null && cells.intersect(match.value).isNotEmpty()) {
                    prevColors[match.key]!!
                } else {
                    palette[nextColorIndex++ % palette.size]
                }

            idToColor[cid] = color
            newPrev[cid] = cells
        }

        prevGroups = newPrev
        prevColors = idToColor
        return idToColor
    }

    // ------------------------------------------------------------
    // UI state emission
    // ------------------------------------------------------------
    private fun emitUiState(showNewGameDialog: Boolean = _uiState.value.showNewGameDialog) {
        val b = board ?: run {
            _uiState.value = GameUiState(showNewGameDialog = showNewGameDialog)
            return
        }

        val ui = _uiState.value
        val overlay =
            if (ui.shouldAnalyze()) {
                println("ui should analyze")
                analyzer.analyze(b).withConflicts(AnalyzerOverlay.detectFlagConflicts(b))
            }
            else {
                println("ui should NOT analyze")
                AnalyzerOverlay()
            }

        val frontier = Frontier().build(b)
        val componentMap = buildComponentMap(frontier)

        val groups = componentMap.entries
            .groupBy({ it.value }, { it.key })
            .mapValues { it.value.toSet() }

        val colors = figureOutColors(groups)

        val uiCells = b.allCells().map { cell ->
            val prob = overlay.probabilities[cell.id]
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
                    forcedOpen = cell.id in overlay.forcedOpens,
                    forcedFlag = cell.id in overlay.forcedFlags,
                    conflict = conflict,
                    componentId = componentMap[cell.id],
                    componentColor = componentMap[cell.id]?.let { colors[it] },
                    reasons = overlay.reasons[cell.id]
                )
            )
        }

        _uiState.value = _uiState.value.copy(
            rows = b.rows,
            cols = b.cols,
            cells = uiCells,
            showNewGameDialog = showNewGameDialog
        )
    }

    fun showNewGameDialog() {
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = null,
            showNewGameDialog = true,
            showCellInfoDialog = false,
        )
        closeExpandedMenu()
    }

    // ------------------------------------------------------------
    // Info dialog
    // ------------------------------------------------------------
    fun showInfo(id: Int) {
        val cell = _uiState.value.cells.firstOrNull { it.id == id }
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = cell,
            showNewGameDialog = false,
            showCellInfoDialog = true,
        )
        closeExpandedMenu()
    }

    fun hideInfo() {
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = null,
            showNewGameDialog = false,
            showCellInfoDialog = false,
        )
        closeExpandedMenu()
    }

    private fun invalidateCaches() {
        lastSignature = null
        lastOverlay = null
    }
}
