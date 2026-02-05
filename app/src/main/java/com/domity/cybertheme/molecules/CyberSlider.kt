package com.domity.cybertheme.molecules

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.foundation.CyberTheme
import kotlin.math.roundToInt

@Composable
fun CyberSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val primaryColor = CyberTheme.colors.primary
    val trackColor = CyberTheme.colors.surface
    val borderColor = CyberTheme.colors.border
    val path = remember { Path() }

    val shadowPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            style = android.graphics.Paint.Style.FILL
        }
    }

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(range, steps) {
                val widthPx = size.width.toFloat()
                val thumbWidthPx = 12.dp.toPx()

                fun update(xPos: Float) {
                    val draggableWidth = widthPx - thumbWidthPx
                    val rawFraction = ((xPos - thumbWidthPx / 2) / draggableWidth).coerceIn(0f, 1f)
                    var newValue = range.start + rawFraction * (range.endInclusive - range.start)

                    if (steps > 0) {
                        val stepSize = (range.endInclusive - range.start) / (steps + 1)
                        val stepIndex = ((newValue - range.start) / stepSize).roundToInt()
                        newValue = range.start + stepIndex * stepSize
                    }
                    if (newValue != value) onValueChange(newValue)
                }

                awaitEachGesture {
                    val down = awaitFirstDown()
                    update(down.position.x)

                    drag(down.id) { change ->
                        change.consume()
                        update(change.position.x)
                    }
                }
            }
            .drawBehind {
                val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                val thumbW = 12.dp.toPx()
                val thumbH = 24.dp.toPx()
                val trackH = 8.dp.toPx()
                val trackTop = center.y - trackH / 2
                fun buildCutPath(rectSize: Size, offset: Offset, cut: Float, onlyLeft: Boolean = false) {
                    path.rewind()
                    val w = rectSize.width
                    val h = rectSize.height
                    val x = offset.x
                    val y = offset.y
                    path.moveTo(x + cut, y)
                    if (onlyLeft) {
                        path.lineTo(x + w, y)
                        path.lineTo(x + w, y + h)
                    } else {
                        path.lineTo(x + w - cut, y)
                        path.lineTo(x + w, y + cut)
                        path.lineTo(x + w, y + h - cut)
                        path.lineTo(x + w - cut, y + h)
                    }
                    path.lineTo(x + cut, y + h)
                    path.lineTo(x, y + h - cut)
                    path.lineTo(x, y + cut)
                    path.close()
                }

                // 轨道背景
                buildCutPath(Size(size.width, trackH), Offset(0f, trackTop), 2.dp.toPx())
                drawPath(path, trackColor)
                drawPath(path, borderColor, style = Stroke(1.dp.toPx()))

                // 激活部分
                val activeWidth = size.width * fraction
                if (activeWidth > 0) {
                    buildCutPath(Size(activeWidth, trackH), Offset(0f, trackTop), 2.dp.toPx(), onlyLeft = true)
                    drawPath(
                        path,
                        brush = Brush.horizontalGradient(
                            listOf(primaryColor.copy(alpha = 0.2f), primaryColor),
                            startX = 0f,
                            endX = activeWidth
                        )
                    )
                }

                // 滑块
                val draggableWidth = size.width - thumbW
                val thumbX = draggableWidth * fraction
                val thumbTop = center.y - thumbH / 2
                val thumbOffset = Offset(thumbX, thumbTop)

                // 滑块路径
                buildCutPath(Size(thumbW, thumbH), thumbOffset, 2.dp.toPx())

                // 辉光
                drawIntoCanvas { canvas ->
                    shadowPaint.setShadowLayer(
                        8.dp.toPx(),
                        0f, 0f,
                        primaryColor.toArgb()
                    )
                    // 阴影
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), shadowPaint)
                }

                // 滑块主体
                drawPath(path, Color.Black)

                // 滑块边框
                drawPath(path, primaryColor, style = Stroke(1.dp.toPx()))

                // 亮线
                drawLine(
                    Color.White,
                    start = Offset(thumbX + thumbW / 2, center.y - 6.dp.toPx()),
                    end = Offset(thumbX + thumbW / 2, center.y + 6.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
    )
}