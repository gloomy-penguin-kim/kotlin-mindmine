package com.kim.minemind.ui.board

import androidx.compose.ui.graphics.Color
import com.kim.minemind.analysis.AutoPlayer
import com.kim.minemind.state.GameSession

class ComponentColorAssigner(
    private val palette: List<Color> = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A),
        Color(0xFFFFCA28), Color(0xFFAB47BC), Color(0xFF26C6DA),
        Color(0xFFFF7043), Color(0xFF8D6E63)
    )
) {

    // ------------------------------------------------------------
    // Component coloring
    // ------------------------------------------------------------
    private var prevGroups: Map<Int, Set<Int>> = emptyMap()
    private var prevColors: Map<Int, Color> = emptyMap()
    private var nextColorIndex = 0

    fun assign(groups: Map<Int, Set<Int>>): Map<Int, Color> {

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
}