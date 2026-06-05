package io.github.domity.cybertheme.molecules

import android.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.domity.cybertheme.foundation.CyberTheme
import kotlin.math.ceil
import kotlin.math.roundToInt

data class CyberChartData(
    val xLabel: String,
    val value: Float // 单位: 分钟
)

@Composable
fun CyberLineChart(
    data: List<CyberChartData>,
    modifier: Modifier = Modifier,
    yAxisLabelCount: Int = 5,
    formatYLabel: (Float) -> String = { ChartFormatter.formatYAxis(it) },
    formatTooltipLabel: (Float) -> String = { ChartFormatter.formatTooltip(it) }
) {
    val primaryColor = CyberTheme.colors.primary
    val secondaryColor = CyberTheme.colors.secondary
    val gridColor = CyberTheme.colors.border
    val textColor = CyberTheme.colors.textDim
    val surfaceColor = CyberTheme.colors.surface
    val backgroundColor = CyberTheme.colors.background

    val baseTextStyle = CyberTheme.typography.button
    val textStyle = remember(baseTextStyle, textColor) { baseTextStyle.copy(color = textColor) }
    val tooltipTextStyle = remember(baseTextStyle, backgroundColor) { baseTextStyle.copy(color = backgroundColor) }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    val topPaddingPx = with(density) { 28.dp.toPx() }
    val bottomPaddingPx = with(density) { 32.dp.toPx() }
    val pointSpacingPx = with(density) { 72.dp.toPx() }
    val startPaddingPx = with(density) { 32.dp.toPx() }
    val endPaddingPx = with(density) { 32.dp.toPx() }

    val strokeWidth2Px = with(density) { 2.dp.toPx() }
    val strokeWidth1Px = with(density) { 1.dp.toPx() }
    val strokeWidth3Px = with(density) { 3.dp.toPx() }

    val circleRadius6Px = with(density) { 6.dp.toPx() }
    val circleRadius2_5Px = with(density) { 2.5.dp.toPx() }

    val margin8Px = with(density) { 8.dp.toPx() }
    val tooltipPadXPx = with(density) { 12.dp.toPx() }
    val tooltipPadYPx = with(density) { 6.dp.toPx() }
    val tooltipCutPx = with(density) { 6.dp.toPx() }
    val tooltipOffsetPx = with(density) { 24.dp.toPx() }

    val targetMaxY = remember(data) {
        val maxMinutes = data.maxOfOrNull { it.value } ?: 0f
        calculateNiceMaxY(maxMinutes, yAxisLabelCount)
    }

    val animatedMaxY by animateFloatAsState(
        targetValue = targetMaxY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "YAxisScaleAnim"
    )

    val xLabelLayouts = remember(data, textStyle) {
        data.map { textMeasurer.measure(it.xLabel, textStyle) }
    }
    val tooltipLayouts = remember(data, tooltipTextStyle) {
        data.map { textMeasurer.measure(formatTooltipLabel(it.value), tooltipTextStyle) }
    }

    val gridLineColor = remember(gridColor) { gridColor.copy(alpha = 0.3f) }
    val gridAuxLineColor = remember(gridColor) { gridColor.copy(alpha = 0.15f) }
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    val linePath = remember { Path() }
    val tooltipPath = remember { Path() }

    val primaryArgb = remember(primaryColor) { primaryColor.toArgb() }
    val secondaryArgb = remember(secondaryColor) { secondaryColor.toArgb() }

    val lineShadowPaint = remember(density, primaryArgb) {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = strokeWidth3Px
            setShadowLayer(with(density) { 16.dp.toPx() }, 0f, 0f, primaryArgb)
        }
    }

    remember(density, primaryArgb) {
        Paint().apply {
            style = Paint.Style.FILL
            setShadowLayer(with(density) { 12.dp.toPx() }, 0f, 0f, primaryArgb)
        }
    }

    val tooltipShadowPaintSecondary = remember(density, secondaryArgb) {
        Paint().apply {
            style = Paint.Style.FILL
            setShadowLayer(with(density) { 12.dp.toPx() }, 0f, 0f, secondaryArgb)
        }
    }

    val pointShadowPaintPrimary = remember(density, primaryArgb) {
        Paint().apply { setShadowLayer(with(density) { 10.dp.toPx() }, 0f, 0f, primaryArgb) }
    }

    val pointShadowPaintSecondary = remember(density, secondaryArgb) {
        Paint().apply { setShadowLayer(with(density) { 10.dp.toPx() }, 0f, 0f, secondaryArgb) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = 8.dp)
    ) {
        // Y 轴 Canvas
        Canvas(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
        ) {
            val drawHeight = size.height - topPaddingPx - bottomPaddingPx
            val step = animatedMaxY / (yAxisLabelCount - 1)

            for (i in 0 until yAxisLabelCount) {
                val yVal = i * step
                val yPx = size.height - bottomPaddingPx - (yVal / animatedMaxY) * drawHeight
                val textLayout = textMeasurer.measure(formatYLabel(yVal), textStyle)

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(size.width - textLayout.size.width - margin8Px, yPx - textLayout.size.height / 2f)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onGloballyPositioned {
                    if (viewportWidthPx != it.size.width.toFloat()) viewportWidthPx = it.size.width.toFloat()
                }
                .horizontalScroll(scrollState)
        ) {
            val chartWidthDp = with(density) {
                (startPaddingPx + endPaddingPx + pointSpacingPx * (data.size.coerceAtLeast(1) - 1)).toDp()
            }

            // 主图表 Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(chartWidthDp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val rawIndex = ((offset.x - startPaddingPx) / pointSpacingPx).roundToInt()
                            highlightedIndex = rawIndex.takeIf { it in data.indices }
                        }
                    }
            ) {
                val drawHeight = size.height - topPaddingPx - bottomPaddingPx
                val bottomYPx = size.height - bottomPaddingPx

                // 画横向网格线
                val step = animatedMaxY / (yAxisLabelCount - 1)
                for (i in 0 until yAxisLabelCount) {
                    val yVal = i * step
                    val yPx = bottomYPx - (yVal / animatedMaxY) * drawHeight
                    drawLine(gridLineColor, Offset(0f, yPx), Offset(size.width, yPx), strokeWidth = strokeWidth1Px)
                }

                // 画纵向辅助线
                for (index in data.indices) {
                    val xPx = startPaddingPx + index * pointSpacingPx
                    drawLine(gridAuxLineColor, Offset(xPx, topPaddingPx), Offset(xPx, bottomYPx), strokeWidth = strokeWidth1Px)
                }

                // 画折线及阴影
                if (data.size > 1) {
                    linePath.rewind()
                    for (index in data.indices) {
                        val xPx = startPaddingPx + index * pointSpacingPx
                        val yPx = bottomYPx - (data[index].value / animatedMaxY) * drawHeight
                        if (index == 0) linePath.moveTo(xPx, yPx) else linePath.lineTo(xPx, yPx)
                    }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawPath(linePath.asAndroidPath(), lineShadowPaint)
                    }
                    drawPath(linePath, primaryColor, style = Stroke(width = strokeWidth2Px))
                }

                val scrollX = scrollState.value.toFloat()
                val safeViewportWidth = if (viewportWidthPx > 0f) viewportWidthPx else size.width
                val visibleLeft = scrollX
                val visibleRight = scrollX + safeViewportWidth

                // 画点与交互浮窗
                for (index in data.indices) {
                    val xPx = startPaddingPx + index * pointSpacingPx
                    val yPx = bottomYPx - (data[index].value / animatedMaxY) * drawHeight

                    val isHighlighted = index == highlightedIndex
                    val color = if (isHighlighted) secondaryColor else primaryColor

                    val labelLayout = xLabelLayouts[index]
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(xPx - labelLayout.size.width / 2f, bottomYPx + margin8Px)
                    )

                    if (isHighlighted) {
                        drawLine(
                            color.copy(alpha = 0.5f),
                            Offset(xPx, topPaddingPx / 2),
                            Offset(xPx, bottomYPx),
                            strokeWidth = strokeWidth2Px,
                            pathEffect = dashEffect
                        )

                        val tooltipLayout = tooltipLayouts[index]
                        val tWidth = tooltipLayout.size.width + tooltipPadXPx * 2
                        val tHeight = tooltipLayout.size.height + tooltipPadYPx * 2

                        // 保证弹窗永远在可视区域内部
                        var boxLeft = xPx - tWidth / 2
                        if (boxLeft < visibleLeft + margin8Px) boxLeft = visibleLeft + margin8Px
                        if (boxLeft + tWidth > visibleRight - margin8Px) boxLeft = visibleRight - tWidth - margin8Px

                        val isAbove = yPx > topPaddingPx
                        val boxTop = if (isAbove) yPx - tooltipOffsetPx - tHeight else yPx + tooltipOffsetPx
                        val connectorY = if (isAbove) boxTop + tHeight else boxTop

                        // 画引线
                        val boxAnchorX = xPx.coerceIn(boxLeft + tooltipCutPx, boxLeft + tWidth - tooltipCutPx)
                        drawLine(
                            color = color,
                            start = Offset(xPx, yPx),
                            end = Offset(boxAnchorX, connectorY),
                            strokeWidth = strokeWidth1Px
                        )

                        // 画弹窗外壳
                        tooltipPath.rewind()
                        tooltipPath.moveTo(boxLeft + tooltipCutPx, boxTop)
                        tooltipPath.lineTo(boxLeft + tWidth - tooltipCutPx, boxTop)
                        tooltipPath.lineTo(boxLeft + tWidth, boxTop + tooltipCutPx)
                        tooltipPath.lineTo(boxLeft + tWidth, boxTop + tHeight - tooltipCutPx)
                        tooltipPath.lineTo(boxLeft + tWidth - tooltipCutPx, boxTop + tHeight)
                        tooltipPath.lineTo(boxLeft + tooltipCutPx, boxTop + tHeight)
                        tooltipPath.lineTo(boxLeft, boxTop + tHeight - tooltipCutPx)
                        tooltipPath.lineTo(boxLeft, boxTop + tooltipCutPx)
                        tooltipPath.close()

                        drawIntoCanvas { canvas ->
                            val shadowPaint = tooltipShadowPaintSecondary
                            canvas.nativeCanvas.drawPath(tooltipPath.asAndroidPath(), shadowPaint)
                        }
                        drawPath(tooltipPath, color, style = Fill)
                        drawText(
                            textLayoutResult = tooltipLayout,
                            topLeft = Offset(boxLeft + tooltipPadXPx, boxTop + tooltipPadYPx)
                        )
                    }

                    // 绘制节点
                    drawIntoCanvas { canvas ->
                        val pointShadowPaint = if (isHighlighted) pointShadowPaintSecondary else pointShadowPaintPrimary
                        canvas.nativeCanvas.drawCircle(xPx, yPx, circleRadius6Px, pointShadowPaint)
                    }
                    drawCircle(surfaceColor, radius = circleRadius6Px, center = Offset(xPx, yPx), style = Fill)
                    drawCircle(color, radius = circleRadius6Px, center = Offset(xPx, yPx), style = Stroke(width = strokeWidth2Px))
                    drawCircle(color, radius = circleRadius2_5Px, center = Offset(xPx, yPx), style = Fill)
                }
            }
        }
    }
}

private fun calculateNiceMaxY(maxMinutes: Float, labelCount: Int): Float {
    if (maxMinutes <= 0f) return 1f
    val stepCount = labelCount - 1

    val stepMinutes = when {
        maxMinutes <= 5 -> 1f
        maxMinutes <= 20 -> 5f
        maxMinutes <= 60 -> 15f
        maxMinutes <= 120 -> 30f
        maxMinutes <= 300 -> 60f
        else -> ceil((maxMinutes / stepCount) / 60.0).toFloat() * 60f
    }
    return stepMinutes * stepCount
}

object ChartFormatter {
    fun formatYAxis(minutes: Float): String {
        if (minutes == 0f) return "0"
        val totalSecs = (minutes * 60f).roundToInt()
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60

        return when {
            h > 0 && m == 0 -> "${h}h"
            h > 0 -> "${h}h${m}m"
            else -> "${m}m"
        }
    }

    fun formatTooltip(minutes: Float): String {
        if (minutes == 0f) return "0s"
        val totalSecs = (minutes * 60f).roundToInt()
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60

        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            if (s > 0 || (h == 0 && m == 0)) append("${s}s")
        }.trim()
    }
}