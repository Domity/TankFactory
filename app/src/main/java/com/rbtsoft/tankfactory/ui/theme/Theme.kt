package com.rbtsoft.tankfactory.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    background = PurpleGrey80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    background = PurpleGrey40,
)

@Composable
fun TankFactoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val bgcolorScheme = if (darkTheme) {
        colorScheme
    } else {
        colorScheme.copy(
            // 亮色模式的白色太亮了，调低一点
            background = Color(0xFFEEEEEE),
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