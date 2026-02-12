package com.kim.minemind.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kim.minemind.state.GameUiState
import kotlinx.coroutines.flow.StateFlow

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp

@Composable
fun BoardView(
    uiState: GameUiState,
    onCell: (Int) -> Unit,
    onCellLongPress: (Int) -> Unit,
//    visualState: StateFlow<VisualState>,
//    visualSettings: StateFlow<VisualSettings>
) {

    val rows = uiState.rows
    val cols = uiState.cols
    val cells = uiState.cells

//    val isVerify = uiState.isVerify
//    val isEnumerate = uiState.isEnumerate

    val shape = RoundedCornerShape(8.dp)
    val cellSize = 32.dp

    val boardW = cellSize * cols
    val boardH = cellSize * rows

    var scale by remember(rows, cols) { mutableStateOf(1f) }
    var offset by remember(rows, cols) { mutableStateOf(Offset.Zero) }



    val minScale = 0.6f
    val maxScale = 3.0f

    // how much background you want to be able to reveal on either side
    val slackDp: Dp = 120.dp

    val TAG = "BoardView"

//    val state by visualState.collectAsState()
//    val settings by visualSettings.collectAsState()

    BoxWithConstraints(
        modifier = Modifier //0xFF282A36
            //.border(4.dp, Color(0xFF0000FF), shape)
            .clip(shape)
            .background(Color(0xFF111318))
    ) {
        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()

        val density  = LocalDensity.current
        val boardWpx = with(density) { boardW.toPx() }
        val boardHpx = with(density) { boardH.toPx() }
        val slackPx  = with(density) { slackDp.toPx() }

        val centerOffset = remember(rows, cols, scale) {
            val scaledW = boardWpx * scale
            val scaledH = boardHpx * scale

            Offset(
                x = (viewportW - scaledW) / 2f,
                y = (viewportH - scaledH) / 2f
            )
        }


        fun clampOffset(rawOffset: Offset, newScale: Float): Offset {
            val scaledW = boardWpx * newScale
            val scaledH = boardHpx * newScale

            val centerX = (viewportW - scaledW) / 2f
            val centerY = (viewportH - scaledH) / 2f

            val minX: Float
            val maxX: Float
            val minY: Float
            val maxY: Float

//            if (scaledW <= viewportW) {
//                // float around center
//                minX = centerX - viewportW
//                maxX = centerX + viewportW
//            } else {
                // board larger → pan fully
                minX = viewportW - scaledW - slackPx
                maxX = viewportW + scaledW + slackPx
            //}

//            if (scaledH <= viewportH) {
//                minY = centerY - slackPx
//                maxY = centerY + slackPx
//            } else {
            maxY = viewportH + scaledH + slackPx
            minY = -maxY
            //}

            Log.d(TAG, "centerY="+centerY+", viewportH="+viewportH+", scaledH="+scaledH)
            Log.d(TAG, "minX="+minX+", maxX="+maxX)
            Log.d(TAG, "minY="+minY+", maxY="+maxY)

            return Offset(
                x = rawOffset.x.coerceIn(minX, maxX),
                y = rawOffset.y.coerceIn(minY, maxY)
            )
        }

        val centeredOffset = remember(rows, cols, viewportW, viewportH, boardWpx, boardHpx) {
            Offset(
                (viewportW - boardWpx) / 2f,
                (viewportH - boardHpx) / 2f
            )
        }

        if (offset == Offset.Zero) {
            offset = centeredOffset
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                        val newOffset = offset + pan
                        scale = newScale
                        offset = clampOffset(newOffset, newScale)
                    }

                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                userScrollEnabled = false,
                modifier = Modifier
                    .requiredSize(boardW, boardH)
                    .graphicsLayer {
                        // IMPORTANT: makes clamp math behave intuitively
                        transformOrigin = TransformOrigin(0f, 0f)

                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(
                    items = uiState.cells,
                    key = { it.id }
                ) { cell ->
                    CellView(
                        cell = cell,
                        size = cellSize,
                        onTap = { onCell(cell.id) },
                        onLongPress = { onCellLongPress(cell.id) }
                    )
                }
            }
        }
    }
}
