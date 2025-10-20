package com.rbtsoft.tankfactory.ui.theme.apptheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Renamed from Purple40/PurpleGrey80 etc. for clarity
private val Black = Color(0xFF000000)
private val Grey = Color(0xFF808080)

// Renamed from DarkColorScheme
internal val BlackAndWhiteDarkColorScheme = darkColorScheme(
    primary = Grey,
    background = Black,
)

// Renamed from LightColorScheme
internal val BlackAndWhiteLightColorScheme = lightColorScheme(
    primary = Black,
    background = Grey,
)
