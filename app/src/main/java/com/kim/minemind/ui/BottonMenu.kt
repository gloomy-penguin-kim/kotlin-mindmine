package com.kim.minemind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kim.minemind.R
import com.kim.minemind.domain.Action
import com.kim.minemind.state.MenuItem
import com.kim.minemind.state.MenuState



@Composable
fun MineMindBottomMenu(
    state: MenuState,
    onAction: (MenuItem) -> Unit
) {

    Box(Modifier.fillMaxSize()) {

        // ===== FLOATING OVERLAY =====
        if (state.isExpanded) {
            Box(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onAction(MenuItem.EXPANDED)
                    }
            )

            FloatingSubMenu(state, onAction)
        }

        // ===== BOTTOM BAR =====
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.Transparent)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                FloatingIcon(
                    drawableId = R.drawable.plus,
                    selected = state.selected == Action.OPEN
                ) { onAction(MenuItem.OPEN) }

                FloatingIcon(
                    drawableId = R.drawable.flag,
                    selected = state.selected == Action.FLAG
                ) { onAction(MenuItem.FLAG) }

                FloatingIcon(
                    drawableId = R.drawable.chord,
                    selected = state.selected == Action.CHORD
                ) { onAction(MenuItem.CHORD) }

                FloatingIcon(
                    drawableId = R.drawable.question_mark,
                    selected = state.selected == Action.INFO
                ) { onAction(MenuItem.INFO) }

                FloatingIcon(
                    drawableId = R.drawable.undo,
                    selected = state.isUndo
                ) { onAction(MenuItem.UNDO) }

                FloatingIcon(
                    drawableId =
                        if (state.isExpanded)
                            R.drawable.expand_circle_up
                        else
                            R.drawable.expand_circle_down,
                    selected = state.isExpanded
                ) { onAction(MenuItem.EXPANDED) }
            }
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
                .padding(bottom = 90.dp, end = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FloatingIcon(
                drawableId = R.drawable.component,
                selected = state.isComponent
            ) { onAction(MenuItem.COMPONENT) }

            FloatingIcon(
                drawableId = R.drawable.analyze,
                selected = state.isAnalyze
            ) { onAction(MenuItem.ANALYZE) }

            FloatingIcon(
                drawableId = R.drawable.conflict,
                selected = state.isConflict
            ) { onAction(MenuItem.CONFLICT) }

            FloatingIcon(
                drawableId = R.drawable.verify,
                selected = state.isVerify
            ) { onAction(MenuItem.VERIFY) }

            FloatingIcon(
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
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(2.dp) ,
            tint = c
        )
    }
}

@Composable
fun FloatingIcon(
    drawableId: Int,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        shape = CircleShape,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(drawableId),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                tint = (if (selected) Color.Black else Color.Gray),
                contentDescription = null,
            )
        }
    }
}
