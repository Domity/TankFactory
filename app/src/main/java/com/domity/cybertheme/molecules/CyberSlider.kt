package com.domity.cybertheme.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
    // 颜色与尺寸
    val primaryColor = CyberTheme.colors.primary
    val trackColor = CyberTheme.colors.surface
    val borderColor = CyberTheme.colors.border

    val thumbWidth = 12.dp
    val thumbHeight = 24.dp
    val trackHeight = 8.dp

    // 状态计算
    val fraction = remember(value, range) {
        ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    }

    // 布局容器
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp), // 控件高度
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val thumbWidthPx = with(LocalDensity.current) { thumbWidth.toPx() }
        val draggableWidth = widthPx - thumbWidthPx

        // 处理触摸输入
        fun updateValue(pxOffset: Float) {
            val rawFraction = (pxOffset / draggableWidth).coerceIn(0f, 1f)
            var newValue = range.start + rawFraction * (range.endInclusive - range.start)

            if (steps > 0) {
                val stepSize = (range.endInclusive - range.start) / (steps + 1)
                val stepIndex = ((newValue - range.start) / stepSize).roundToInt()
                newValue = range.start + stepIndex * stepSize
            }

            if (newValue != value) {
                onValueChange(newValue)
            }
        }

        // 绘制轨道
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(trackColor, CutCornerShape(2.dp))
                .border(1.dp, borderColor, CutCornerShape(2.dp))
                // 触摸监听
                .pointerInput(range, steps, widthPx) {
                    detectTapGestures { offset ->
                        // 计算中心点相对位置
                        val adjustedX = offset.x - (thumbWidthPx / 2)
                        updateValue(adjustedX)
                    }
                }
                .pointerInput(range, steps, widthPx) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val adjustedX = change.position.x - (thumbWidthPx / 2)
                        updateValue(adjustedX)
                    }
                }
        )

        // 绘制已激活部分
        Box(
            modifier = Modifier
                .width(maxWidth * fraction)
                .height(trackHeight)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.2f),
                            primaryColor
                        )
                    ),
                    CutCornerShape(topStart = 2.dp, bottomStart = 2.dp)
                )
        )

        // 绘制滑块
        val thumbOffsetX = (draggableWidth * fraction).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetX, 0) }
                .size(width = thumbWidth, height = thumbHeight)
                .shadow(8.dp, spotColor = primaryColor) // 辉光
                .background(Color.Black) // 黑色芯
                .border(1.dp, primaryColor, CutCornerShape(2.dp)) // 边框
        ) {
            // 滑块中间加一条亮线 漂亮
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(Color.White)
                    .align(Alignment.Center)
            )
        }
    }
}