package com.kim.minemind.state

import kotlinx.serialization.Serializable

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
) {
    companion object {
        fun toSnapshot(menuState: MenuState): MenuStateSnapshot {
            return MenuStateSnapshot(
                menuState.selected,
                menuState.isVerify,
                menuState.isAnalyze,
                menuState.isConflict,
                menuState.isComponent
            )
        }

        fun fromSnapshot(snap: MenuStateSnapshot): MenuState {
            return MenuState(
                selected = snap.selected,
                isVerify = snap.isVerify,
                isAnalyze = snap.isAnalyze,
                isConflict = snap.isConflict,
                isComponent = snap.isComponent
            )
        }
    }
}

@Serializable
data class MenuStateSnapshot(
    val selected: MenuItem? = MenuItem.OPEN,
    val isVerify: Boolean = false,
    val isAnalyze: Boolean = true,
    val isConflict: Boolean = true,
    val isComponent: Boolean = true
)

