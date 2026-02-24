package com.kim.minemind.ui.board

import com.kim.minemind.analysis.Analyzer
import com.kim.minemind.analysis.AnalyzerOverlay
import com.kim.minemind.analysis.debug.buildComponentMap
import com.kim.minemind.analysis.frontier.Frontier
import com.kim.minemind.domain.Board
import com.kim.minemind.domain.CellState
import com.kim.minemind.state.CellOverlay
import com.kim.minemind.state.GamePhase
import com.kim.minemind.state.GameUiState
import com.kim.minemind.state.MenuState
import com.kim.minemind.state.UiCell

class BoardUiMapper(
    private val analyzer: Analyzer,
    private val colorAssigner: ComponentColorAssigner
) {

    fun map(
        board: Board,
        phase: GamePhase,
        menuState: MenuState,
        showNewGameDialog: Boolean = false
    ): GameUiState {

        val overlay =
            if (menuState.isAnalyze && phase == GamePhase.PLAYING)
                analyzer.analyze(board)
                    .withConflicts(AnalyzerOverlay.detectFlagConflicts(board))
            else
                AnalyzerOverlay()

        val frontier = Frontier().build(board)
        val componentMap = buildComponentMap(frontier)

        val groups = componentMap.entries
            .groupBy({ it.value }, { it.key })
            .mapValues { it.value.toSet() }

        val colors = colorAssigner.assign(groups)

        val uiCells = board.allCells().map { cell ->

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

        return GameUiState(
            rows = board.rows,
            cols = board.cols,
            cells = uiCells
        )
    }
}