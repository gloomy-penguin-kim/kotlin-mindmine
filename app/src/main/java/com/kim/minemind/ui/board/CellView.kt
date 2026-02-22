package com.kim.minemind.ui.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kim.minemind.state.MenuState
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
    ProbabilityBucket("#",   0.65f,   0.75f),
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
        Color(0xFF64B5F6),
        Color(0xFF81C784),
        Color(0xFFE57373),
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
    menuState: MenuState,
    size: Dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {

    val overlay = cell.overlay

    val cellProbability = cell.overlay.probability ?: -1f

    val hasConflict = (menuState.isConflict && overlay.conflict != null)

    // ---------- Background ----------
    val bgColor = when {

        cell.isExploded && !cell.isFlagged -> Color.Red

        cell.isFlagged -> when {
            menuState.isVerify && !cell.isMine -> Color(0xFFFF6B6B)
            else -> Color.Gray
        }

        hasConflict -> Color(0xFFFF6B6B)

        menuState.isComponent &&
                overlay.componentId != null &&
                !cell.isRevealed ->
            overlay.componentColor!!.copy(alpha = 0.25f)

        !hasConflict && menuState.isAnalyze && cellProbability >= 0 && overlay.forcedFlag ->
            Color(0xFF7B1FA2)

        !hasConflict && menuState.isAnalyze && cellProbability >= 0 && overlay.forcedOpen ->
            Color(0xFF1976D2)

        cell.isRevealed -> Color.LightGray

        else -> Color.DarkGray
    }

    var glyph = ""
    var color = Color.DarkGray
    var fWeight = FontWeight.Normal
    var fSize = 12.sp

    if (cell.isFlagged) {
        glyph = "F"
        if (!hasConflict) {
            if (menuState.isVerify && !cell.isMine)
                color = Color.DarkGray
            else Color.White.copy(alpha = 0.8f)
        }
        else Color.DarkGray
        fWeight = FontWeight.Bold
    }
    else if (cell.isExploded) {
        glyph = "@"
        fWeight = FontWeight.Bold
    }
    else if (cell.isRevealed) {
        if (cell.isMine) {
            glyph = "*"
            fWeight = FontWeight.Bold
        }
        else if (cell.adjacentMines > 0) {
            glyph = cell.adjacentMines.toString()
            fWeight = FontWeight.Bold
        }
    }
    else {
        glyph = probabilityToGlyph(cell.overlay.probability)

        if (overlay.forcedOpen) {
            glyph = "O"
            fWeight = FontWeight.Normal
        }
        if (overlay.forcedFlag) {
            glyph = "F"
            fWeight = FontWeight.Normal
        }
        if (hasConflict) {
            glyph = "C"
            fWeight = FontWeight.Normal
        }
        else if (!menuState.isAnalyze) {
            glyph = ""
        }

        if (glyph.isNotEmpty()) {
            color = Color.White.copy(alpha = 0.8f)
            fSize = 12.sp
            fWeight = FontWeight.Normal
        }
    }


    val animatedColor by animateColorAsState(
        targetValue = bgColor ?: Color.Transparent,
        animationSpec = tween(durationMillis = 350),
        label = "componentColor"
    )


    val animatedTextColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(350),
        label = "cellTextColor"
    )

    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .background(animatedColor, RoundedCornerShape(1.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = glyph,
            color = animatedTextColor,
            fontSize = fSize,
            fontWeight = fWeight,
        )
        // ---------- Foreground ----------
//        when {
//
//            // Priority visuals
//            cell.isFlagged ->
//                Text(
//                    text = "F",
//                    color = Color.White.copy(alpha = 0.8f),
//                    fontWeight = FontWeight.Bold
//                )
//            cell.isExploded ->
//                Text(
//                    text = "@",
//                    fontWeight = FontWeight.Bold
//                )
//
//            cell.isRevealed && cell.isMine ->
//                Text(
//                    text = "*",
//                    fontWeight = FontWeight.Bold
//                )
//
//            cell.isRevealed && cell.adjacentMines > 0 ->
//                Text(
//                    text = cell.adjacentMines.toString(),
//                    fontWeight = FontWeight.Bold
//                )
//
//            // ---------- Overlay Rendering ----------
//            !cell.isRevealed && !cell.isFlagged -> {
//
//                var glyph = probabilityToGlyph(cell.overlay.probability)
//
//                if (overlay.forcedOpen) {
//                    glyph = "O"
//                }
//                if (overlay.forcedFlag) {
//                    glyph = "F"
//                }
//                if (hasConflict) {
//                    glyph = "C"
//                }
//                else if (!menuState.isAnalyze) {
//                    glyph = ""
//                }
//
//                if (glyph.isNotEmpty()) {
//                    Text(
//                        text = glyph,
//                        fontSize = 10.sp,
//                        color = Color.White.copy(alpha = 0.8f)
//                    )
//                }

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
//        }
//    }
}
