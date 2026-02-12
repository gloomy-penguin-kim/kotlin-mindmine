package com.kim.minemind.analysis

import com.kim.minemind.analysis.rules.Rule
import com.kim.minemind.domain.Board
import com.kim.minemind.domain.CellState
import com.kim.minemind.analysis.Conflict

/**
 * Snapshot of solver/analyzer output consumed by UI.
 *
 * Immutable by design — build new copies when updating.
 */
data class AnalyzerOverlay(

    // gid -> probability of mine
    val probabilities: Map<Int, Float?> = emptyMap(),

    // solver deductions
    val forcedFlags: Set<Int> = emptySet(),
    val forcedOpens: Set<Int> = emptySet(),

    // constraint conflicts (gid -> conflict)
    val conflicts: Map<Int, Conflict> = emptyMap(),

    // explanation metadata
    val reasons: Map<Int, Rule> = emptyMap(),

    // ⭐ DEBUG ONLY
    val componentIds: Map<Int, Int> = emptyMap()
) {

    /**
     * Replace conflicts snapshot
     */
    fun withConflicts(newConflicts: Map<Int, Conflict>) =
        copy(conflicts = newConflicts)

    /**
     * Merge conflicts snapshot
     */
    fun addConflicts(newConflicts: Map<Int, Conflict>) =
        copy(
            conflicts = LinkedHashMap<Int, Conflict>().apply {
                putAll(conflicts)
                putAll(newConflicts)
            }
        )

    fun withComponentMap(map: Map<Int, Int>) =
        copy(componentIds = map)


    companion object {

        /**
         * Detects "too many flags" constraint violations.
         *
         * This belongs in analysis layer — NOT UI.
         */
        fun detectFlagConflicts(board: Board): Map<Int, Conflict> {

            val out = LinkedHashMap<Int, Conflict>()

            for (cell in board.allCells()) {

                if (cell.state != CellState.REVEALED) continue
                if (cell.adjacentMines == 0) continue

                val flags = board.neighborsOf(cell.id).count {
                    board.cell(it).state == CellState.FLAGGED
                }

                val hidden = board.neighborsOf(cell.id).count {
                    board.cell(it).state == CellState.HIDDEN
                }

                if (flags > cell.adjacentMines) {
                    out[cell.id] = Conflict(
                        cell.id,
                        ConflictSource.BOARD,
                        mutableSetOf("Too many flags near this adjacent mine count")
                    )
                }
//
//                if (hidden < cell.adjacentMines - flags) {
//                    out[cell.id] = Conflict(
//                        cell.id,
//                        ConflictSource.BOARD,
//                        mutableSetOf("Not enough flags to be revealed near this mine count")
//                    )
//                }
            }
            return out
        }
    }
}

///////////////////////////////////////////////////////////////
// Conflict Model
///////////////////////////////////////////////////////////////
//
//sealed class Conflict {
//
//    /**
//     * Example:
//     * Cell shows "1" but has 2 flags nearby
//     */
//    data class TooManyFlags(
//        val expected: Int,
//        val actual: Int
//    ) : Conflict()
//
//    /**
//     * Future extension examples:
//     */
//
//    data class ImpossibleConstraint(
//        val remaining: Int,
//        val unknown: Int
//    ) : Conflict()
//
//    object SolverContradiction : Conflict()
//}
