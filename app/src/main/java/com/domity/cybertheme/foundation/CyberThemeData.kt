package com.domity.cybertheme.foundation

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
class CyberColors(
    // 这只是定义，改了没用
    val background: Color = Color(0xFF050505),    // 极黑背景
    val surface: Color = Color(0xFF121212),       // 表面颜色
    val primary: Color = Color(0xFFFFD700),       // 主霓虹色
    val secondary: Color = Color(0xFFFF0000),     // 副霓虹色
    val text: Color = Color(0xFFE0E0E0),          // 主文本
    val textDim: Color = Color(0xFF666666),       // 暗文本
    val border: Color = Color(0xFF333333)         // 装饰性边框
)

@Immutable
class CyberTypography(
    val headline: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = 2.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    val button: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp
    )
)

val LocalCyberColors = staticCompositionLocalOf { CyberColors() }
val LocalCyberTypography = staticCompositionLocalOf { CyberTypography() }

@Composable
fun CyberTheme(
    content: @Composable () -> Unit
) {
    val colors = remember { CyberColors() }
    val typography = remember { CyberTypography() }

    CompositionLocalProvider(
        LocalCyberColors provides colors,
        LocalCyberTypography provides typography,
        content = content
    )
}

object CyberTheme {
    val colors: CyberColors
        @Composable get() = LocalCyberColors.current
    val typography: CyberTypography
        @Composable get() = LocalCyberTypography.current
}