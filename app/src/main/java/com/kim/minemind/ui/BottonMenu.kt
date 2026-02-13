package com.kim.minemind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kim.minemind.R
import com.kim.minemind.state.MenuItem
import com.kim.minemind.state.MenuState


@Composable
fun CircleMenuButton(
    drawableId: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
//    val alpha = if (selected) 1f else 0.3f

    var c = if (selected) Color.Black else Color.Gray

    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(drawableId),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White),
            tint = c
        )
    }
}

@Composable
fun MineMindBottomMenu(
    state: MenuState,
    onAction: (MenuItem) -> Unit
) {
    Box(
        modifier = Modifier
            .height(120.dp)
            .background(Color.Black)

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircleMenuButton(
                drawableId = R.drawable.plus,
                selected = state.selected == MenuItem.OPEN
            ) { onAction(MenuItem.OPEN) }

            CircleMenuButton(
                drawableId = R.drawable.flag,
                selected = state.selected == MenuItem.FLAG
            ) { onAction(MenuItem.FLAG) }

            CircleMenuButton(
                drawableId = R.drawable.chord,
                selected = state.selected == MenuItem.CHORD
            ) { onAction(MenuItem.CHORD) }

            CircleMenuButton(
                drawableId = R.drawable.question_mark,
                selected = state.selected == MenuItem.INFO
            ) { onAction(MenuItem.INFO) }

            CircleMenuButton(
                drawableId = R.drawable.undo,
                selected = state.selected == MenuItem.UNDO
            ) { onAction(MenuItem.UNDO) }

            CircleMenuButton(
                drawableId = R.drawable.expand_circle_down,
                selected = state.selected == MenuItem.EXPANDED
            ) { onAction(MenuItem.EXPANDED) }
        }


        // FLOATING SUBMENU
        if (state.expanded) {
            FloatingSubMenu(state, onAction)
        }
   }
}

@Composable
fun FloatingSubMenu(
    state: MenuState,
    onAction: (MenuItem) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 70.dp, end = 12.dp)
        ) {

            CircleMenuButton(
                drawableId = R.drawable.analyze,
                selected = state.isAnalyze
            ) { onAction(MenuItem.ANALYZE) }

            CircleMenuButton(
                drawableId = R.drawable.verify,
                selected = state.isVerify
            ) { onAction(MenuItem.VERIFY) }

            CircleMenuButton(
                drawableId = R.drawable.conflict,
                selected = state.isConflict
            ) { onAction(MenuItem.CONFLICT) }

            CircleMenuButton(
                drawableId = R.drawable.autobot,
                selected = state.isAutoBot
            ) { onAction(MenuItem.AUTO) }

//            FloatingIcon(Icons.Settings) {
//                // future
//            }
        }
    }
}


@Composable
fun FloatingIcon(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, null)
        }
    }
}
