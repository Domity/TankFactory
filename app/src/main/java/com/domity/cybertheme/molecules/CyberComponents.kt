package com.domity.cybertheme.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.foundation.CyberTheme
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

// 按钮
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val mainColor = if (isPrimary) CyberTheme.colors.primary else CyberTheme.colors.secondary

    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(interactionSource, null, enabled = enabled, onClick = onClick)
            .drawBehind {
                val alpha = if (enabled) 1f else 0.4f
                val pressScale = if (isPressed && enabled) 0.95f else 1f

                scale(pressScale) {
                    val cutSize = 12.dp.toPx()
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width - cutSize, 0f)
                        lineTo(size.width, cutSize) //右上切角
                        lineTo(size.width, size.height)
                        lineTo(cutSize, size.height)
                        lineTo(0f, size.height - cutSize) //左下切角
                        close()
                    }
                    // 绘制背景
                    if (isPressed && enabled) {
                        drawPath(path, mainColor.copy(alpha = 0.2f * alpha))
                    }
                    // 绘制边框
                    val borderC =
                        if (isPressed && enabled) mainColor else mainColor.copy(alpha = 0.6f)
                    drawPath(
                        path,
                        borderC.copy(alpha = borderC.alpha * alpha),
                        style = Stroke(1.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CyberText(
            text = text.uppercase(),
            style = CyberTheme.typography.button,
            color = mainColor.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

// 弹窗
@Composable
fun CyberDialog(
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        CyberSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = CutCornerShape(16.dp),
            color = CyberTheme.colors.surface,
            borderWidth = 1.dp,
            borderColor = CyberTheme.colors.primary
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (title != null) {
                    title()
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (content != null) {
                    content()
                }
                Spacer(modifier = Modifier.height(32.dp))
                if (buttons != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        buttons()
                    }
                }
            }
        }
    }
}
