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
import com.rbtsoft.tankfactory.ui.theme.apptheme.BlackAndWhiteDarkColorScheme
import com.rbtsoft.tankfactory.ui.theme.apptheme.BlackAndWhiteLightColorScheme
import com.rbtsoft.tankfactory.ui.theme.apptheme.OrangeDarkColorScheme
import com.rbtsoft.tankfactory.ui.theme.apptheme.OrangeLightColorScheme

@Composable
fun TankFactoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean,
    selectedTheme: AppTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            when (selectedTheme) {
                AppTheme.BLACK_AND_WHITE -> {
                    if (darkTheme) BlackAndWhiteDarkColorScheme else BlackAndWhiteLightColorScheme
                }
                AppTheme.ORANGE -> {
                    if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
                }
            }
        }
    }

    val bgcolorScheme = if (darkTheme) {
        colorScheme
    } else {
        colorScheme.copy(
            background = if (useDynamicColor) colorScheme.background else Color(0xFFEEEEEE),
        )
    }
    MaterialTheme(
        colorScheme = bgcolorScheme,
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
