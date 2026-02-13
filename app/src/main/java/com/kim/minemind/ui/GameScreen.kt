package com.kim.minemind.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kim.minemind.state.GameViewModel
import com.kim.minemind.ui.board.BoardView
import com.kim.minemind.ui.dialogs.CellInfoDialog
import com.kim.minemind.ui.dialogs.NewGameDialog


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val menuState by vm.menuState.collectAsState()

    Scaffold(
        topBar = {
            TopMenu(
                onNewGame = vm::openNewGameDialog
            )
        },
        bottomBar = {
            MineMindBottomMenu(
                state = menuState,
                onAction = vm::onMenuAction
            )
        }
    ) {
        padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
            uiState.infoCell?.let {
                CellInfoDialog(
                    cell = it,
                    onDismiss = { vm.hideInfo() }
                )
            }
        }
    }

}