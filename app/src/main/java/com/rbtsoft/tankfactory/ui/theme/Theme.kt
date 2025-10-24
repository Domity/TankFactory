package com.rbtsoft.tankfactory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun TankFactoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MirageTankImageTheme(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (isDarkMode) {
        darkColorScheme(
            background = Color.Black,
            onBackground = Color.White
        )
    } else {
        lightColorScheme(
            background = Color.White,
            onBackground = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
