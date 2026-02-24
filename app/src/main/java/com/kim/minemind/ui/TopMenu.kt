package com.kim.minemind.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kim.minemind.state.GameUiState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopMenu(
    onNewGame: () -> Unit,
    uiState: GameUiState
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(

        title = {
            Row {
                Text("Moves: ${uiState.moveCount}")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Time: ${uiState.elapsedSeconds}")
            }
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("New Game") },
                    onClick = {
                        expanded = false
                        onNewGame()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = { expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = { expanded = false }
                )
            }
        }
    )
}

