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

@Composable
//  默认动态取色
fun TankFactoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) BlackAndWhiteDarkColorScheme else BlackAndWhiteLightColorScheme
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
