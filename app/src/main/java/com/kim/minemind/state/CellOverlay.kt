package com.kim.minemind.state

data class CellOverlay(
    val probability: Float? = null,
    val forcedOpen: Boolean = false,
    val forcedFlag: Boolean = false,
    val conflict: Boolean = false,


    val componentId: Int? = null
)