package com.kim.minemind.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kim.minemind.state.UiCell


data class ProbabilityBucket(
    val glyph: String,
    val min: Float,   // inclusive
    val max: Float    // inclusive
)

private val PROBABILITY_BUCKETS = listOf(
    ProbabilityBucket("",   -1f,   -0.0001f),
    ProbabilityBucket("O",   0.0f,   0.05f),
    ProbabilityBucket(".",   0.05f,  0.15f),
    ProbabilityBucket(",",   0.15f,  0.25f),
    ProbabilityBucket(":",   0.25f,  0.35f),
    ProbabilityBucket("~",   0.35f,  0.45f),
    ProbabilityBucket("+",   0.45f,  0.55f),
    ProbabilityBucket("*",   0.55f,  0.65f),
    ProbabilityBucket("#",   0.65f,  0.75f),
    ProbabilityBucket("%",   0.75f,  0.85f),
    ProbabilityBucket("&",   0.85f,  0.95f),
    ProbabilityBucket("F",   0.95f,  1.0f),
)

fun probabilityToGlyph(p: Float?): String {
    val v = p ?: -1f
    return PROBABILITY_BUCKETS.firstOrNull { v >= it.min && v <= it.max }?.glyph ?: ""
}

fun probabilityBucketFor(p: Float?): ProbabilityBucket? {
    val v = p ?: return null
    return PROBABILITY_BUCKETS.firstOrNull { v >= it.min && v <= it.max }
}


fun componentColor(id: Int): Color {
    val palette = listOf(
        Color(0xFFE57373),
        Color(0xFF64B5F6),
        Color(0xFF81C784),
        Color(0xFFFFD54F),
        Color(0xFFBA68C8),
        Color(0xFF4DB6AC),
        Color(0xFFA1887F),
        Color(0xFF90A4AE)
    )
    return palette[id % palette.size]
}



@Composable
fun CellView(
    cell: UiCell,
    size: Dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {

    val overlay = cell.overlay

    // ---------- Background ----------
    val bgColor = when {
        cell.isFlagged -> Color.DarkGray

        cell.isExploded -> Color.Red

        overlay.conflict -> Color(0xFFFF6B6B)

        // ⭐ Component Debug Coloring
        overlay.componentId != null &&
                !cell.isRevealed &&
                !cell.isFlagged ->
            componentColor(overlay.componentId).copy(alpha = 0.35f)

        overlay.forcedFlag -> Color(0xFF7B1FA2)
        overlay.forcedOpen -> Color(0xFF1976D2)

        cell.isRevealed -> Color.LightGray
        else -> Color.DarkGray
    }


    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .background(bgColor, RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {

        // ---------- Foreground ----------
        when {

            // Priority visuals
            cell.isFlagged -> Text("🚩")
            cell.isExploded -> Text("💥")

            cell.isRevealed && cell.isMine ->
                Text("💣")

            cell.isRevealed && cell.adjacentMines > 0 ->
                Text(
                    text = cell.adjacentMines.toString(),
                    fontWeight = FontWeight.Bold
                )

            // ---------- Overlay Rendering ----------
            !cell.isRevealed && !cell.isFlagged -> {

                var glyph = probabilityToGlyph(cell.overlay.probability)

                if (overlay.forcedOpen) {
                    glyph = "O"
                }
                if (overlay.forcedFlag) {
                    glyph = "F"
                }

                if (glyph.isNotEmpty()) {
                    Text(
                        text = glyph,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

//                if (!cell.isRevealed && glyph.isNotEmpty()) {
//                    Text(
//                        text = glyph,
//                        fontSize = 10.sp,
//                        color = Color.White.copy(alpha = 0.8f)
//                    )
//                }

//
//                val glyph = probabilityToGlyph(overlay.probability)
//
//                if (glyph.isNotEmpty()) {
//
//                    val bucket = probabilityBucketFor(overlay.probability)
//
//                    val color = when {
//                        overlay.probability == null -> Color.White
//
//                        overlay.probability < 0.2f -> Color(0xFF4CAF50)
//                        overlay.probability < 0.4f -> Color(0xFF8BC34A)
//                        overlay.probability < 0.6f -> Color(0xFFFFEB3B)
//                        overlay.probability < 0.8f -> Color(0xFFFF9800)
//                        else -> Color(0xFFF44336)
//                    }
//
//                    Text(
//                        text = glyph,
//                        color = color,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
            }
        }
    }
}
