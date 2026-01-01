package com.example.otakumaster.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun lightAndDarkColor (
    lightColor: Color,
    darkColor: Color
): Color {
    return if (isSystemInDarkTheme()) darkColor else lightColor
}