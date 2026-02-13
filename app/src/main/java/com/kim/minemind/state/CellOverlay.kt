package com.kim.minemind.state

import androidx.compose.ui.graphics.Color
import com.kim.minemind.analysis.Conflict
import com.kim.minemind.analysis.rules.Rule
import kotlin.collections.mutableMapOf

data class CellOverlay(
    val probability: Float? = null,
    val forcedOpen: Boolean = false,
    val forcedFlag: Boolean = false,
    val conflict: Conflict? = null,
    val reasons: Rule? = null,
    val componentId: Int? = null,
    val componentColor: Color? = null
)