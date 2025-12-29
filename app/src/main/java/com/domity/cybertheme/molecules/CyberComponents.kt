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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.foundation.CyberTheme

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
    val scale = if (isPressed && enabled) 0.95f else 1f
    val shape = CutCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 12.dp)

    CyberSurface(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f } // 禁用时降低透明度
            .clickable(interactionSource, null, enabled = enabled, onClick = onClick),
        shape = shape,
        color = if(isPressed && enabled) mainColor.copy(alpha = 0.2f) else Color.Transparent,
        borderWidth = 1.dp,
        borderColor = if(isPressed && enabled) mainColor else mainColor.copy(alpha = 0.6f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CyberText(
                text = text.uppercase(),
                style = CyberTheme.typography.button,
                color = mainColor
            )
        }
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
