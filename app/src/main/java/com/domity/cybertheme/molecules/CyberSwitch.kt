package com.domity.cybertheme.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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
            .drawWithCache {
                val path = Path()
                val cut = 4.dp.toPx()
                val thumbCut = 2.dp.toPx()
                fun setPath(w: Float, h: Float, ox: Float, oy: Float, c: Float) {
                    path.rewind()
                    path.moveTo(ox + c, oy)
                    path.lineTo(ox + w - c, oy)
                    path.lineTo(ox + w, oy + c)
                    path.lineTo(ox + w, oy + h - c)
                    path.lineTo(ox + w - c, oy + h)
                    path.lineTo(ox + c, oy + h)
                    path.lineTo(ox, oy + h - c)
                    path.lineTo(ox, oy + c)
                    path.close()
                }

                onDrawBehind {
                    fun lerpColor(start: Color, end: Color, t: Float): Color {
                        return Color(
                            red = start.red + (end.red - start.red) * t,
                            green = start.green + (end.green - start.green) * t,
                            blue = start.blue + (end.blue - start.blue) * t,
                            alpha = start.alpha + (end.alpha - start.alpha) * t
                        )
                    }

                    val trackColor = lerpColor(surface, primary.copy(alpha = 0.2f), p)
                    val activeColor = lerpColor(offColor, primary, p)
                    setPath(size.width, size.height, 0f, 0f, cut)
                    drawPath(path, trackColor, style = Fill)
                    drawPath(path, activeColor, style = Stroke(1.dp.toPx()))
                    val thumbSize = 16.dp.toPx()
                    val padding = 4.dp.toPx()
                    val thumbX = padding + (size.width - thumbSize - 2 * padding) * p
                    val thumbOffset = (size.height - thumbSize) / 2
                    setPath(thumbSize, thumbSize, thumbX, thumbOffset, thumbCut)
                    if (checked) {
                        drawPath(path, primary.copy(alpha = 0.4f), style = Stroke(3.dp.toPx()))
                    }

                    drawPath(path, activeColor, style = Fill)
                    val centerX = thumbX + thumbSize / 2
                    val centerY = thumbOffset + thumbSize / 2
                    drawLine(
                        Color.Black.copy(0.5f),
                        start = Offset(centerX, centerY - 4.dp.toPx()),
                        end = Offset(centerX, centerY + 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
    )
}