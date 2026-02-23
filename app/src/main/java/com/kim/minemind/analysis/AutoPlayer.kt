package com.kim.minemind.analysis


import com.kim.minemind.domain.Action
import com.kim.minemind.domain.Board
import com.kim.minemind.state.MoveEvent
import javax.inject.Inject

class AutoPlayer @Inject constructor(
    private val analyzer: Analyzer
) {

    fun nextMove(board: Board): MoveEvent? {

        val overlay = analyzer.analyze(board)

        // 1️⃣ Forced opens
        overlay.forcedOpens.firstOrNull()?.let {
            return MoveEvent(action=Action.OPEN, id=it)
        }

        // 2️⃣ Forced flags
        overlay.forcedFlags.firstOrNull()?.let {
            return MoveEvent(action=Action.FLAG, id=it)
        }

        // 3️⃣ High confidence probabilities
        val highConfidence = overlay.probabilities
            .filterValues { it != null && (it!! <= 0.05f || it >= 0.95f) }

        highConfidence.entries.firstOrNull()?.let { (id, p) ->
            return if (p!! <= 0.05f)
                MoveEvent(action=Action.OPEN, id=id)
            else
                MoveEvent(action=Action.FLAG, id=id)
        }

        // 4️⃣ Fallback: lowest entropy
        val best = overlay.probabilities
            .filterValues { it != null }
            .map { (id, p) -> Triple(id, p!!, minOf(p, 1f - p)) }
            .minByOrNull { it.third }

        return best?.let { (id, p, _) ->
            if (p <= 0.5f)
                MoveEvent(action=Action.OPEN, id=id)
            else
                MoveEvent(action=Action.FLAG, id=id)
        }
    }
}