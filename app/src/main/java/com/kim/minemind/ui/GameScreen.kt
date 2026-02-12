package com.kim.minemind.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kim.minemind.state.GameViewModel
import com.kim.minemind.ui.board.BoardView
import com.kim.minemind.ui.dialogs.NewGameDialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopMenu(
                onNewGame = vm::openNewGameDialog
            )
        }
    ) {
        BoardView(
            uiState = uiState,
            onCell = vm::onCellTap,
            onCellLongPress = vm::onToggleFlag
        )

        if (uiState.showNewGameDialog) {
            NewGameDialog(
                onStart = vm::startNewGame,
                onDismiss = vm::closeNewGameDialog
            )
        }
    }
}

