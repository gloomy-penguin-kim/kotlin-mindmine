package com.kim.minemind.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kim.minemind.state.UiCell

@Composable
fun CellInfoDialog(
    cell: UiCell,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Cell ${cell.id}")
        },

        text = {
            Column {

                Text("Revealed: ${cell.isRevealed}")
                Text("Flagged: ${cell.isFlagged}")
                Text("Mine: ${cell.isMine}")
                Text("Adj Mines: ${cell.adjacentMines}")

                Spacer(Modifier.height(8.dp))

                val ov = cell.overlay

                Text("Probability: ${ov.probability ?: "—"}")
                Text("Forced Open: ${ov.forcedOpen}")
                Text("Forced Flag: ${ov.forcedFlag}")
                Text("Conflict: ${ov.conflict}")
                Text("Rules: ${ov.reasons}")
                Text("Component: ${ov.componentId ?: "—"}")
            }
        },

        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


