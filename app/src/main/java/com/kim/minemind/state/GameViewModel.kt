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
            session = persistence.load() ?: GameSession(
                rows = 25,
                cols = 25,
                mineCount = 140,
                seed = 24L
            )
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
        emit()
    }

    fun undo() {
        session.undo()
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
    }

    fun autobot() {
        autobotJob?.cancel()
        autobotJob = viewModelScope.launch {
            while (true) {
                val move = autoPlayer.nextMove(session.currentBoard) ?: break
                session.applyMove(move)
                emit()
                delay(400)
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
        _uiState.value = _uiState.value.copy(isEnumerating = newAnalyze)
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
