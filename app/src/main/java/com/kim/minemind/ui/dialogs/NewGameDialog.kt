package com.kim.minemind.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NewGameDialog(
    onStart: (rows: Int, cols: Int, mines: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var rows by remember { mutableStateOf(9) }
    var cols by remember { mutableStateOf(9) }
    var mines by remember { mutableStateOf(10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Game") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Rows", rows) { rows = it }
                NumberField("Columns", cols) { cols = it }
                NumberField("Mines", mines) { mines = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onStart(rows, cols, mines) }
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true
    )
}

