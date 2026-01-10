package com.domity.cybertheme.molecules

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberLoading(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = CyberTheme.colors.primary
) {
    // 合并动画状态。
    val transition = rememberInfiniteTransition(label = "l")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "p"
    )

    Spacer(
        modifier = modifier
            .size(size)
            .drawBehind {
                val w = this.size.width
                val strokeW = 3.dp.toPx()
                val thinStroke = Stroke(strokeW / 2, cap = StrokeCap.Butt)

                // 计算动画值
                val rotOuter = (p * 4 % 1f) * 360f
                val rotInner = 360f - (p * 8 % 1f) * 360f
                val alphaCycle = (p * 5 % 1f)
                val wave = if (alphaCycle < 0.5f) alphaCycle * 2 else (1 - alphaCycle) * 2
                val coreAlpha = 0.3f + wave * 0.7f

                // 绘制外圈
                rotate(rotOuter) {
                    val inset = strokeW
                    val dSize = Size(w - inset * 2, w - inset * 2)
                    val dTopLeft = Offset(inset, inset)
                    drawArc(color, 0f, 90f, false, dTopLeft, dSize, style = thinStroke)
                    drawArc(color, 120f, 90f, false, dTopLeft, dSize, style = thinStroke)
                    drawArc(color.copy(0.5f), 240f, 60f, false, dTopLeft, dSize, style = thinStroke)
                }

                // 绘制内圈
                rotate(rotInner) {
                    val innerD = w / 1.5f
                    val offset = (w - innerD) / 2

                    drawArc(
                        color = color,
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(offset, offset),
                        size = Size(innerD, innerD),
                        style = Stroke(strokeW, cap = StrokeCap.Square)
                    )
                }

                // 核心与十字准星
                val c = center
                drawCircle(color.copy(alpha = coreAlpha), radius = w / 10, center = c)

                // 准星线
                val lineLen = w / 4
                val lineAlpha = color.copy(alpha = 0.3f)
                drawLine(lineAlpha, Offset(c.x - lineLen, c.y), Offset(c.x + lineLen, c.y), strokeWidth = 2f)
                drawLine(lineAlpha, Offset(c.x, c.y - lineLen), Offset(c.x, c.y + lineLen), strokeWidth = 2f)
            }
    )
}