package com.kim.minemind.state

enum class MenuItem {
    OPEN,
    FLAG,
    CHORD,
    INFO,
    VERIFY,
    ANALYZE,
    UNDO,
    CONFLICT,

    COMPONENT,
    AUTO,
    EXPANDED
}

data class MenuState(
    val selected: MenuItem? = MenuItem.OPEN,

    val isExpanded: Boolean = false,

    val isVerify: Boolean = false,
    val isAnalyze: Boolean = true,
    val isConflict: Boolean = true,
    val isAutoBot: Boolean = false,
    val isUndo: Boolean = false,
    val isComponent: Boolean = true,

    val cellFocusId: Int? = null,
    val cellInfo: UiCell? = null,

    val showNewGameDialog: Boolean = false,
    val showCellInfoDialog: Boolean = false,
)
