package com.domity.cybertheme.atoms


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = CyberTheme.typography.body,
    color: Color = CyberTheme.colors.text
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color)
    )
}

@Composable
fun CyberSurface(
    modifier: Modifier = Modifier,
    color: Color = CyberTheme.colors.surface,
    shape: Shape = RectangleShape,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.drawBehind {
            // 绘制背景
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = color
            )
            // 绘制边框
            if (borderWidth > 0.dp) {
                drawOutline(
                    outline = outline,
                    color = borderColor,
                    style = Stroke(width = borderWidth.toPx())
                )
            }
        }
    ) {
        content()
    }
}