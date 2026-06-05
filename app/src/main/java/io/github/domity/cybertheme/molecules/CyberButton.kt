package io.github.domity.cybertheme.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.domity.cybertheme.atoms.CyberText
import io.github.domity.cybertheme.foundation.CyberTheme

data class CutCorners(
    val topLeft: Dp = 8.dp,
    val topRight: Dp = 8.dp,
    val bottomRight: Dp = 8.dp,
    val bottomLeft: Dp = 8.dp
)

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    cutCorners: CutCorners = CutCorners(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isPressedState = interactionSource.collectIsPressedAsState()

    val themePrimary = CyberTheme.colors.primary
    val themeSecondary = CyberTheme.colors.secondary
    val baseColor = if (isPrimary) themePrimary else themeSecondary

    val contentAlpha = if (enabled) 1f else 0.4f
    val bgPressedColor = baseColor.copy(alpha = 0.2f * contentAlpha)

    val strokeColorNormal = baseColor.copy(alpha = 0.6f)
    val haloColorNormal = strokeColorNormal.copy(alpha = 0.3f * contentAlpha)
    val coreColorNormal = strokeColorNormal.copy(alpha = strokeColorNormal.alpha * contentAlpha)

    val haloColorPressed = baseColor.copy(alpha = 0.3f * contentAlpha)
    val coreColorPressed = baseColor.copy(alpha = baseColor.alpha * contentAlpha)
    val textColor = baseColor.copy(alpha = contentAlpha)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .drawWithCache {
                val tl = cutCorners.topLeft.toPx()
                val tr = cutCorners.topRight.toPx()
                val br = cutCorners.bottomRight.toPx()
                val bl = cutCorners.bottomLeft.toPx()

                val path = Path().apply {
                    moveTo(tl, 0f)
                    lineTo(size.width - tr, 0f)
                    lineTo(size.width, tr)
                    lineTo(size.width, size.height - br)
                    lineTo(size.width - br, size.height)
                    lineTo(bl, size.height)
                    lineTo(0f, size.height - bl)
                    lineTo(0f, tl)
                    close()
                }

                val stroke3Px = Stroke(width = 3.dp.toPx())
                val stroke1Px = Stroke(width = 1.dp.toPx())

                onDrawBehind {
                    val isPressed = isPressedState.value
                    val activeScale = if (isPressed && enabled) 0.95f else 1f

                    scale(activeScale) {
                        if (isPressed && enabled) {
                            drawPath(path, bgPressedColor)
                            drawPath(path, haloColorPressed, style = stroke3Px)
                            drawPath(path, coreColorPressed, style = stroke1Px)
                        } else {
                            if (enabled) {
                                drawPath(path, haloColorNormal, style = stroke3Px)
                            }
                            drawPath(path, coreColorNormal, style = stroke1Px)
                        }
                    }
                }
            }
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CyberText(
            text = text.uppercase(),
            style = CyberTheme.typography.button,
            color = textColor
        )
    }
}