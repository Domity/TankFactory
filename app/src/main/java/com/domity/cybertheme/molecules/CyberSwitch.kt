package com.domity.cybertheme.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val p by animateFloatAsState(if (checked) 1f else 0f, label = "p")
    val primary = CyberTheme.colors.primary
    val surface = CyberTheme.colors.surface
    val offColor = CyberTheme.colors.textDim
    val path = remember { Path() }

    Spacer(
        modifier = modifier
            .size(48.dp, 24.dp)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = androidx.compose.ui.semantics.Role.Switch,
                indication = null,
                interactionSource = null
            )
            .drawBehind {
                // 颜色计算
                fun c(start: Color, end: Color) = Color(
                    start.red + (end.red - start.red) * p,
                    start.green + (end.green - start.green) * p,
                    start.blue + (end.blue - start.blue) * p,
                    start.alpha + (end.alpha - start.alpha) * p
                )
                val trackColor = c(surface, primary.copy(alpha = 0.2f))
                val activeColor = c(offColor, primary)

                // 绘制切角路径
                fun buildCutPath(rectSize: Size, offset: Offset, cut: Float) {
                    path.rewind() // 重置路径
                    val w = rectSize.width
                    val h = rectSize.height
                    val x = offset.x
                    val y = offset.y
                    path.moveTo(x + cut, y)
                    path.lineTo(x + w - cut, y)
                    path.lineTo(x + w, y + cut)
                    path.lineTo(x + w, y + h - cut)
                    path.lineTo(x + w - cut, y + h)
                    path.lineTo(x + cut, y + h)
                    path.lineTo(x, y + h - cut)
                    path.lineTo(x, y + cut)
                    path.close()
                }

                // 绘制轨道
                buildCutPath(size, Offset.Zero, 4.dp.toPx())
                drawPath(path, trackColor, style = Fill)
                drawPath(path, activeColor, style = Stroke(1.dp.toPx()))

                // 绘制滑块
                val thumbSize = 16.dp.toPx()
                val padding = 4.dp.toPx()
                val thumbX = padding + (size.width - thumbSize - 2 * padding) * p
                val thumbOffset = Offset(thumbX, (size.height - thumbSize) / 2)
                buildCutPath(Size(thumbSize, thumbSize), thumbOffset, 2.dp.toPx())
                // 滑块填充
                drawPath(path, activeColor, style = Fill)

                // 装饰线
                val centerX = thumbOffset.x + thumbSize / 2
                val centerY = thumbOffset.y + thumbSize / 2
                drawLine(
                    Color.Black.copy(0.5f),
                    start = Offset(centerX, centerY - 4.dp.toPx()),
                    end = Offset(centerX, centerY + 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
    )
}