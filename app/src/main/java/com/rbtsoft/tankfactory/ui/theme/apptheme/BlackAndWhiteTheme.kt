package com.rbtsoft.tankfactory.ui.theme.apptheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Grey = Color(0xFF808080)

internal val BlackAndWhiteLightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    background = Grey,
    onBackground = Black
)

internal val BlackAndWhiteDarkColorScheme = darkColorScheme(
    primary = Grey,
    onPrimary = Black,
    background = Black,
    onBackground = White
)
