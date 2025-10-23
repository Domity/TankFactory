package com.rbtsoft.tankfactory.ui.theme.apptheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val orange_light_primary = Color(0xFF8F4C00)
private val orange_light_onPrimary = Color(0xFFFFFFFF)
private val orange_light_background = Color(0xFFFFF8F5)
private val orange_light_onBackground = Color(0xFF201B16)

private val orange_dark_primary = Color(0xFFFF9300)
private val orange_dark_onPrimary = Color(0xFFFFFFFF)
private val orange_dark_background = Color(0xFFFFB868)
private val orange_dark_onBackground = Color(0xFF000000)

internal val OrangeLightColorScheme = lightColorScheme(
    primary = orange_light_primary,
    onPrimary = orange_light_onPrimary,
    background = orange_light_background,
    onBackground = orange_light_onBackground
)

internal val OrangeDarkColorScheme = darkColorScheme(
    primary = orange_dark_primary,
    onPrimary = orange_dark_onPrimary,
    background = orange_dark_background,
    onBackground = orange_dark_onBackground
)
