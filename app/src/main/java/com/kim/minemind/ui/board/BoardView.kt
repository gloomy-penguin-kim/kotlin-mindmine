package com.kim.minemind.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kim.minemind.state.GameUiState

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import com.kim.minemind.state.MenuState
import kotlin.collections.get
import kotlin.div
import kotlin.math.max
import kotlin.math.min
import kotlin.times


@Composable
fun BoardView(
    uiState: GameUiState,
    menuState: MenuState,
    onCell: (Int) -> Unit,
    onCellLongPress: (Int) -> Unit,
//    visualState: StateFlow<VisualState>,
//    visualSettings: StateFlow<VisualSettings>
) {

    val rows = uiState.rows
    val cols = uiState.cols
    val cells = uiState.cells

    val focusCellId = uiState.focusCellId

//    val isVerify = uiState.isVerify
//    val isEnumerate = uiState.isEnumerate

    val shape = RoundedCornerShape(8.dp)
    val cellSize = 32.dp

    val boardW = cellSize * cols
    val boardH = cellSize * rows

    var scale by remember(rows, cols) { mutableStateOf(1f) }
    var offset by remember(rows, cols) { mutableStateOf(Offset.Zero) }

    Log.d("BoardView", "rows=$rows cols=$cols cells=${cells.size}")

    if (rows == 0 || cols == 0 || cells.isEmpty()) {
        return
    }

    val minScale = 0.6f
    val maxScale = 3.0f

    // how much background you want to be able to reveal on either side
    val slackDp: Dp = 120.dp

    val TAG = "BoardView"

    val boardKey = rows to cols


//    val state by visualState.collectAsState()
//    val settings by visualSettings.collectAsState()

    BoxWithConstraints(
        modifier = Modifier //0xFF282A36
//            .border(4.dp, Color(0xFF0000FF), shape)
            .background(Color(0xFF111318))
    ) {


        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()

        val density = LocalDensity.current
        val boardWpx = with(density) { boardW.toPx() }
        val boardHpx = with(density) { boardH.toPx() }
        val slackPx = with(density) { slackDp.toPx() }



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


        fun centerOnCell(cellId: Int): Offset {
            val row = cellId / cols
            val col = cellId % cols

            val cellPx = with(density) { cellSize.toPx() }

            val cellCenterX = (col * cellPx) + (cellPx / 2f)
            val cellCenterY = (row * cellPx) + (cellPx / 2f)

            val scaledCellX = cellCenterX * scale
            val scaledCellY = cellCenterY * scale

            val targetOffset = Offset(
                x = (viewportW / 2f - scaledCellX) + slackPx,
                y = (viewportH / 2f - scaledCellY) + slackPx
            )

            offset = clampOffset(targetOffset, scale)
            return offset
        }

        LaunchedEffect(boardKey, viewportW, viewportH) {
            if (viewportW <= 0f || viewportH <= 0f) return@LaunchedEffect
            if (scale != 1f || offset != Offset.Zero) return@LaunchedEffect
            val fitScale = 1.5f
            scale = fitScale
            val centered = Offset(
                (viewportW - boardWpx * fitScale) /2f,
                (viewportH - boardHpx * fitScale) /2f
            )
            offset = clampOffset(centered, fitScale)
        }
        val animOffset = remember { Animatable(offset, Offset.VectorConverter) }

        LaunchedEffect(focusCellId) {
            if (focusCellId != null) {
                val newOffset = centerOnCell(focusCellId)
                animOffset.animateTo(newOffset)
                offset = animOffset.value
            }
        }
        val cellSizePx = with(density) { cellSize.toPx() }

        LaunchedEffect(focusCellId) {

            val id = focusCellId ?: return@LaunchedEffect

            if (viewportW <= 0f || viewportH <= 0f) return@LaunchedEffect

            val row = id / cols
            val col = id % cols

            val cellLeft = col * cellSizePx
            val cellTop = row * cellSizePx
            val cellSizeScaled = cellSizePx * scale

            val screenLeft = cellLeft * scale + offset.x
            val screenTop = cellTop * scale + offset.y
            val screenRight = screenLeft + cellSizeScaled
            val screenBottom = screenTop + cellSizeScaled

            val isVisible =
                screenLeft >= 0f &&
                        screenTop >= 0f &&
                        screenRight <= viewportW &&
                        screenBottom <= viewportH

            if (!isVisible) {
                // center it
                val targetX = viewportW / 2f - (cellLeft + cellSizePx / 2f) * scale
                val targetY = viewportH / 2f - (cellTop + cellSizePx / 2f) * scale

                offset = clampOffset(Offset(targetX, targetY), scale)
            }
        }





        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(boardW, boardH)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom)
                                .coerceIn(minScale, maxScale)

                            val newOffset = clampOffset(offset + pan, newScale)

                            scale = newScale
                            offset = newOffset
                        }
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.cells, key = { it.id }) { cell ->
                        CellView(
                            cell = cell,
                            menuState = menuState,
                            size = cellSize,
                            onTap = { onCell(cell.id) },
                            onLongPress = { onCellLongPress(cell.id) }
                        )
                    }
                }
            }
        }

        // ================= DEBUG OVERLAY =================

        val scaledW = boardWpx * scale
        val scaledH = boardHpx * scale

        val idealCenter = Offset(
            (viewportW - scaledW) / 2f,
            (viewportH - scaledH) / 2f
        )

        val error = Offset(
            offset.x - idealCenter.x,
            offset.y - idealCenter.y
        )

//        Box(
//            Modifier
//                .background(Color(0xAA000000))
//                .padding(8.dp)
//        ) {
//            Text(
//                """
//Scale: ${"%.3f".format(scale)}
//
//Offset:
//x=${"%.1f".format(offset.x)}
//y=${"%.1f".format(offset.y)}
//
//Ideal Center:
//x=${"%.1f".format(idealCenter.x)}
//y=${"%.1f".format(idealCenter.y)}
//
//Center Error:
//x=${"%.1f".format(error.x)}
//y=${"%.1f".format(error.y)}
//
//Viewport:
//${viewportW.toInt()} x ${viewportH.toInt()}
//
//Scaled Board:
//${scaledW.toInt()} x ${scaledH.toInt()}
//        """.trimIndent(),
//                color = Color.White
//            )
//        }

    }


}
