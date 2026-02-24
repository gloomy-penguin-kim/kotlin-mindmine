package com.kim.minemind.analysis


import com.kim.minemind.domain.Action
import com.kim.minemind.domain.Board
import com.kim.minemind.domain.CellState
import com.kim.minemind.state.MoveEvent
import javax.inject.Inject

class AutoPlayer @Inject constructor(
    private val analyzer: Analyzer
) {

    fun nextMove(board: Board): MoveEvent? {

        val overlay = analyzer.analyze(board)


        overlay.forcedOpens
            .firstOrNull { isLegalMove(board, Action.OPEN, it) }
            ?.let {
                return MoveEvent(action = Action.OPEN, id = it)
            }


        overlay.forcedFlags
            .firstOrNull { isLegalMove(board, Action.FLAG, it) }
            ?.let {
                return MoveEvent(action = Action.FLAG, id = it)
            }


        val highConfidence = overlay.probabilities
            .filterValues { it != null && (it <= 0.05f || it >= 0.95f) }

        highConfidence.entries
            .firstOrNull { (id, p) ->
                val action = if (p!! <= 0.05f) Action.OPEN else Action.FLAG
                isLegalMove(board, action, id)
            }
            ?.let { (id, p) ->
                return if (p!! <= 0.05f)
                    MoveEvent(action = Action.OPEN, id = id)
                else
                    MoveEvent(action = Action.FLAG, id = id)
            }

        overlay.probabilities
            .filterValues { it != null}
            .map { (id, p) -> Triple(id, p!!, minOf(p, 1f - p)) }
            .minByOrNull { it.third }

             ?.let { (id, p, _) ->
                val action = if (board.cell(id).isMine) Action.FLAG else Action.OPEN
                if (p <= 0.5f)
                    return MoveEvent(action=action, id=id)
                else
                    return MoveEvent(action=action, id=id)
            }
        board.allCells()
            .firstOrNull { it.state == CellState.HIDDEN }
            .let { cell ->
                val action = if (cell!!.isMine) Action.FLAG else Action.OPEN
                return MoveEvent(action = action, id = cell.id)
            }
    }

    private fun isLegalMove(board: Board, action: Action, id: Int): Boolean {
        val cell = board.cell(id)

        return when (action) {
            Action.OPEN -> cell.state != CellState.REVEALED && cell.state != CellState.FLAGGED && !cell.isMine
            Action.FLAG -> cell.state != CellState.REVEALED && cell.state != CellState.FLAGGED && cell.isMine
            else -> false
        }
    }
}