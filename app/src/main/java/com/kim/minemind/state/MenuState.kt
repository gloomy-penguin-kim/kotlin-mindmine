package com.kim.minemind.state

import com.kim.minemind.domain.Action
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
    EXPANDED;

    companion object
}




data class MenuState(
    val selected: Action = Action.OPEN,

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
        fun toAction(menuItem: MenuItem): Action {
            return when {
                menuItem == MenuItem.OPEN -> Action.OPEN
                menuItem == MenuItem.FLAG -> Action.FLAG
                menuItem == MenuItem.CHORD -> Action.CHORD
                menuItem == MenuItem.INFO -> Action.INFO
                else -> Action.OPEN
            }
        }
    }
}


fun MenuItem.Companion.toAction(item: MenuItem): Action {
    return when (item) {
        MenuItem.OPEN -> Action.OPEN
        MenuItem.FLAG -> Action.FLAG
        MenuItem.CHORD -> Action.CHORD
        MenuItem.INFO -> Action.INFO
        else -> Action.OPEN
    }
}


@Serializable
data class MenuStateSnapshot(
    val selected: Action = Action.OPEN,
    val isVerify: Boolean = false,
    val isAnalyze: Boolean = true,
    val isConflict: Boolean = true,
    val isComponent: Boolean = true
)

