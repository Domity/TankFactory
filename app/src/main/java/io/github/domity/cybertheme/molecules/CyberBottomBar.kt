package io.github.domity.cybertheme.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import io.github.domity.cybertheme.atoms.CyberSurface
import io.github.domity.cybertheme.atoms.CyberText
import io.github.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        CyberSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = CutCornerShape(12.dp),
            color = CyberTheme.colors.surface.copy(alpha = 0.95f),
            borderWidth = 1.dp,
            borderColor = CyberTheme.colors.border
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

@Composable
fun RowScope.CyberBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primary = CyberTheme.colors.primary
    val textDim = CyberTheme.colors.textDim
    val textActive = CyberTheme.colors.text
    val unselectedBorder = remember(textDim) { textDim.copy(alpha = 0.1f) }

    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "item_progress"
    )

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(horizontal = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawWithCache {
                val cut = 8.dp.toPx()
                val path = Path().apply {
                    moveTo(cut, 0f)
                    lineTo(size.width - cut, 0f)
                    lineTo(size.width, cut)
                    lineTo(size.width, size.height - cut)
                    lineTo(size.width - cut, size.height)
                    lineTo(cut, size.height)
                    lineTo(0f, size.height - cut)
                    lineTo(0f, cut)
                    close()
                }

                val stroke1dp = Stroke(1.dp.toPx())

                onDrawBehind {
                    val p = progress

                    if (p > 0f) {
                        drawPath(path, color = primary, alpha = 0.15f * p)
                    }

                    val currentBorderColor = lerp(unselectedBorder, primary, p)
                    drawPath(path, color = currentBorderColor, style = stroke1dp)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CyberText(
            text = text,
            style = CyberTheme.typography.button,
            color = lerp(textDim, textActive, progress)
        )
    }
}