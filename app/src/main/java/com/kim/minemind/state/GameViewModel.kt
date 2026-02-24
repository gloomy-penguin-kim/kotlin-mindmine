package com.kim.minemind.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kim.minemind.analysis.Analyzer
import com.kim.minemind.analysis.AutoPlayer
import com.kim.minemind.domain.Action
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kim.minemind.ui.board.BoardUiMapper
import com.kim.minemind.ui.board.ComponentColorAssigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class GameViewModel @Inject constructor(
    private val persistence: GamePersistenceService,
    private val autoPlayer: AutoPlayer
) : ViewModel() {

    private lateinit var session: GameSession

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _menuState = MutableStateFlow(MenuState())
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    // ------------------------------------------------------------
    // Autobot
    // ------------------------------------------------------------
    private var autobotJob: Job? = null

    private val analyzer = Analyzer()
    private val colorAssigner = ComponentColorAssigner()
    private val uiMapper = BoardUiMapper(analyzer, colorAssigner)



    init {
        viewModelScope.launch {
            val p = persistence.load()
            if (p != null) {
                session = p.first
                _menuState.value = p.second
                _menuState.value = _menuState.value.copy(
                    cellFocusId = session.getLastMoveId()
                )
            }
            else {
                session = GameSession(
                    rows = 25,
                    cols = 25,
                    mineCount = 140,
                    seed = 42L
                )
            }
            emit()
        }
    }

    fun startNewGame(rows: Int, cols: Int, mines: Int) {
        session = GameSession(rows, cols, mines, seed = 42L)
        _menuState.value = _menuState.value.copy(
            showNewGameDialog = false
        )
        emit()
    }

    fun onCellLongPress(id: Int) {
        val move = MoveEvent(action=Action.FLAG, id=id)
        session.applyMove(move)
        if (session.moveCount == 1) {
            startTimer()
        }

        if (session.phase == GamePhase.WON || session.phase == GamePhase.LOST) {
            stopTimer()
        }

        emit()
    }

    fun onCellTap(id: Int) {
        closeExpandedMenu()
        if (menuState.value.selected == Action.INFO) {
            showInfo(id)
            return
        }
        val moveEvent = MoveEvent(action=menuState.value.selected, id=id)
        session.applyMove(moveEvent)
        if (session.moveCount == 1) {
            startTimer()
        }

        if (session.phase == GamePhase.WON || session.phase == GamePhase.LOST) {
            stopTimer()
        }

        emit()
    }

    fun undo() {
        val id = session.undo()
        _menuState.value = _menuState.value.copy(
            cellFocusId = id
        )
        emit()
    }

    fun redo() {
        session.redo()
        emit()
    }

    private fun emit() {
        _uiState.value = uiMapper.map(
            board = session.currentBoard,
            phase = session.phase,
            menuState = _menuState.value
        )
        viewModelScope.launch {
            persistence.save(session, menuState.value)
        }
    }

    fun autobot() {
        autobotJob?.cancel()
        autobotJob = viewModelScope.launch {
            while (true) {
                val move = autoPlayer.nextMove(session.currentBoard) ?: break
                _menuState.value = _menuState.value.copy(
                    cellFocusId = move.id
                )
                session.applyMove(move)
                if (session.moveCount == 1) {
                    startTimer()
                }

                if (session.phase == GamePhase.WON || session.phase == GamePhase.LOST) {
                    stopTimer()
                }

                emit()
                delay(400)
            }
        }
        closeExpandedMenu()
    }

    // ------------------------------------------------------------
    // Timer times
    // ------------------------------------------------------------
    private var timerJob: Job? = null
    private var startTime: Long? = null

    private fun startTimer() {
        if (timerJob != null) return

        startTime = System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            while (isActive) {
                updateUiState()
                delay(1000)
            }
        }
    }

    private fun updateUiState() {
        _uiState.value = GameUiState(
//            board = session.currentBoard,
            phase = session.phase,
            moveCount = session.moveCount,
            elapsedSeconds = computeElapsed()
        )
    }

    private fun computeElapsed(): Int {
        val start = startTime ?: return 0
        return ((System.currentTimeMillis() - start) / 1000).toInt()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ------------------------------------------------------------
    // Menu actions
    // ------------------------------------------------------------
    fun onMenuAction(item: MenuItem) {
        autobotJob?.cancel()
        if (item != MenuItem.AUTO)
            _menuState.value = _menuState.value.copy(isAutoBot = false)

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
            selected = MenuItem.toAction(item),
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
        emit()
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

    // ------------------------------------------------------------
    // Info dialog
    // ------------------------------------------------------------

    fun closeExpandedMenu() {
        autobotJob?.cancel()
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = null,
        )
    }


    fun showNewGameDialog() {
        closeExpandedMenu()
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = null,
            showNewGameDialog = true,
            showCellInfoDialog = false,
        )
    }

    fun showInfo(id: Int) {
        closeExpandedMenu()
        val cell = _uiState.value.cells.firstOrNull { it.id == id }
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = cell,
            showNewGameDialog = false,
            showCellInfoDialog = true,
        )
    }

    fun hideInfo() {
        closeExpandedMenu()
        _menuState.value = _menuState.value.copy(
            isExpanded = false,
            isAutoBot = false,
            cellInfo = null,
            showNewGameDialog = false,
            showCellInfoDialog = false,
        )
    }

}
