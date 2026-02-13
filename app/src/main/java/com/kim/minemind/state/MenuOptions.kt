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
    AUTO,
    EXPANDED
}

data class MenuState(
    val selected: MenuItem? = MenuItem.OPEN,
    val expanded: Boolean = false,

    val isVerify: Boolean = false,
    val isAnalyze: Boolean = true,
    val isConflict: Boolean = true,
    val isAutoBot: Boolean = false,
    val isUndo: Boolean = false
)
