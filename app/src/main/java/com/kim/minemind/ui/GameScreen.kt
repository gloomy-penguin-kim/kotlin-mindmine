package com.kim.minemind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kim.minemind.state.GameViewModel
import com.kim.minemind.ui.board.BoardView
import com.kim.minemind.ui.dialogs.CellInfoDialog
import com.kim.minemind.ui.dialogs.NewGameDialog

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    vm: GameViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val menuState by vm.menuState.collectAsState()


    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopMenu(
                onNewGame = vm::showNewGameDialog,
                uiState = uiState,
            )
        },
//        bottomBar = {
//            MineMindBottomMenu(
//                state = menuState,
//                onAction = vm::onMenuAction
//            )
//        }
    ) {
            padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {

            // ===== Board =====
            BoardView(
                uiState = uiState,
                onCell = vm::onCellTap,
                onCellLongPress = vm::onCellLongPress,
                menuState = menuState
            )


            // 👇 Floating controls go here
            MineMindBottomMenu(
                state = menuState,
                onAction = vm::onMenuAction
            )

            // ===== Floating submenu overlay =====
            if (menuState.isExpanded) {
                FloatingSubMenu(
                    state = menuState,
                    onAction = vm::onMenuAction
                )
            }

            // ===== Dialogs =====
            if (menuState.showNewGameDialog) {
                NewGameDialog(
                    onStart = vm::startNewGame,
                    onDismiss = vm::hideInfo
                )
            }

            menuState.cellInfo?.let {
                CellInfoDialog(
                    cell = it,
                    onDismiss = { vm.hideInfo() }
                )
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.closeExpandedMenu()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

}