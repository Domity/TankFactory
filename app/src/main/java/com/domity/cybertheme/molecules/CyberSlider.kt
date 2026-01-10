package com.domity.cybertheme.molecules

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.foundation.CyberTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

    // 用于绘制辉光
    val shadowPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK // 阴影主体颜色
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
                coroutineScope {
                    launch { detectTapGestures { offset -> update(offset.x) } }
                    launch { detectHorizontalDragGestures { change, _ -> change.consume(); update(change.position.x) } }
                }
            }
            .drawBehind {
                val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                val thumbW = 12.dp.toPx()
                val thumbH = 24.dp.toPx()
                val trackH = 8.dp.toPx()
                val trackTop = center.y - trackH / 2
                // 定义切角
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

                // 绘制轨道背景
                buildCutPath(Size(size.width, trackH), Offset(0f, trackTop), 2.dp.toPx())
                drawPath(path, trackColor)
                drawPath(path, borderColor, style = Stroke(1.dp.toPx()))

                // 绘制激活部分
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

                // 绘制滑块
                val draggableWidth = size.width - thumbW
                val thumbX = draggableWidth * fraction
                val thumbTop = center.y - thumbH / 2
                val thumbOffset = Offset(thumbX, thumbTop)

                // 构建滑块路径
                buildCutPath(Size(thumbW, thumbH), thumbOffset, 2.dp.toPx())

                // 绘制辉光
                drawIntoCanvas { canvas ->
                    shadowPaint.setShadowLayer(
                        8.dp.toPx(), // 半径
                        0f, 0f,      // 偏移
                        primaryColor.toArgb()
                    )
                    // 绘制阴影
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), shadowPaint)
                }

                // 绘制滑块主体
                drawPath(path, Color.Black)

                // 绘制滑块边框
                drawPath(path, primaryColor, style = Stroke(1.dp.toPx()))

                // 绘制中间亮线
                drawLine(
                    Color.White,
                    start = Offset(thumbX + thumbW / 2, center.y - 6.dp.toPx()),
                    end = Offset(thumbX + thumbW / 2, center.y + 6.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
    )
}