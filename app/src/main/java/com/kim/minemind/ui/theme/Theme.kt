package com.kim.minemind.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun MineMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
