package com.domity.cybertheme.molecules

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 尺寸定义
    val switchWidth = 48.dp
    val switchHeight = 24.dp
    val thumbSize = 16.dp
    val padding = 4.dp

    // 滑块位置动画
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) switchWidth - thumbSize - padding else padding,
        animationSpec = tween(durationMillis = 200),
        label = "thumb_offset"
    )

    // 颜色动画
    val trackColor by animateColorAsState(
        targetValue = if (checked) CyberTheme.colors.primary.copy(alpha = 0.2f) else CyberTheme.colors.surface,
        label = "track_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) CyberTheme.colors.primary else CyberTheme.colors.textDim,
        label = "border_color"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) CyberTheme.colors.primary else CyberTheme.colors.textDim,
        label = "thumb_color"
    )

    // 交互源
    val interactionSource = remember { MutableInteractionSource() }

    // UI
    Box(
        modifier = modifier
            .width(switchWidth)
            .size(height = switchHeight, width = switchWidth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .background(trackColor, CutCornerShape(4.dp)) // 轨道底色
            .border(1.dp, borderColor, CutCornerShape(4.dp)), // 轨道边框
        contentAlignment = Alignment.CenterStart
    ) {
        // 滑块
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .background(thumbColor, CutCornerShape(2.dp))
                .border(1.dp, Color.Black.copy(alpha = 0.5f), CutCornerShape(2.dp))
        ) {
            // 装饰
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 8.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .align(Alignment.Center)
            )
        }
    }
}