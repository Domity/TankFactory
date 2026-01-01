package com.rbtsoft.tankfactory.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.domity.cybertheme.foundation.CyberColors
import com.domity.cybertheme.foundation.CyberTypography
import com.domity.cybertheme.foundation.LocalCyberColors
import com.domity.cybertheme.foundation.LocalCyberTypography

@Composable
fun TankFactoryTheme(
    content: @Composable () -> Unit
) {
    val fixedCyberColors = CyberColors(
        // 这才是改了有用的地方
        primary = Color(0xFFA5A6FD),
        secondary = Color(0xFFFF0000),
        background = Color(0xFF050505),
        surface = Color(0xFF121212),
        border = Color(0xFF333333),
        text = Color(0xFFE0E0E0),
        textDim = Color(0xFF606060)
    )

    CompositionLocalProvider(
        LocalCyberColors provides fixedCyberColors,
        LocalCyberTypography provides CyberTypography(),
        content = content
    )
}

@Composable
fun MirageTankImageTheme(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val forcedColors = if (!isDarkMode) {
        CyberColors(
            background = Color.Black,
            surface = Color.Black,
            text = Color.White,
            primary = Color.White,
            secondary = Color.Gray,
            border = Color.DarkGray
        )
    } else {
        CyberColors(
            background = Color.White,
            surface = Color.White,
            text = Color.Black,
            primary = Color.Black,
            secondary = Color.Gray,
            textDim = Color.DarkGray,
            border = Color.LightGray
        )
    }

    CompositionLocalProvider(
        LocalCyberColors provides forcedColors,
        LocalCyberTypography provides CyberTypography(),
        content = content
    )
}