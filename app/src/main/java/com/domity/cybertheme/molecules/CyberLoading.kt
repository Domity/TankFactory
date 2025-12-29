package com.domity.cybertheme.molecules

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberLoading(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = CyberTheme.colors.primary
) {
    // 定义动画控制器
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_loading")

    // 外圈动画
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "outer"
    )

    // 内圈动画
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "inner"
    )

    // 呼吸动画
    val coreAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2, h / 2)
        val strokeWidth = 3.dp.toPx()

        // 绘制外圈
        withTransform({
            rotate(outerRotation, center)
        }) {
            val outerRadius = w / 2 - strokeWidth
            val arcSize = Size(outerRadius * 2, outerRadius * 2)
            val arcTopLeft = Offset(strokeWidth, strokeWidth)

            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth / 2, cap = StrokeCap.Butt)
            )
            drawArc(
                color = color,
                startAngle = 120f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth / 2, cap = StrokeCap.Butt)
            )
            drawArc(
                color = color.copy(alpha = 0.5f),
                startAngle = 240f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth / 2, cap = StrokeCap.Butt)
            )
        }

        // 绘制内圈
        withTransform({
            rotate(innerRotation, center)
        }) {
            val innerRadius = w / 3
            val arcSize = Size(innerRadius * 2, innerRadius * 2)
            val arcTopLeft = Offset(center.x - innerRadius, center.y - innerRadius)

            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
            )
        }

        drawCircle(
            color = color.copy(alpha = coreAlpha),
            radius = w / 10,
            center = center
        )

        drawLine(
            color = color.copy(alpha = 0.3f),
            start = Offset(center.x - w/4, center.y),
            end = Offset(center.x + w/4, center.y),
            strokeWidth = 2f
        )
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = Offset(center.x, center.y - h/4),
            end = Offset(center.x, center.y + h/4),
            strokeWidth = 2f
        )
    }
}