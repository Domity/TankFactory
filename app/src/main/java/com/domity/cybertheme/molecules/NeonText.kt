package com.domity.cybertheme.molecules

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    neonColor: Color,
    style: TextStyle
) {
    var alpha by remember { mutableFloatStateOf(1f) }

    // 故障闪烁逻辑
    LaunchedEffect(Unit) {
        while (true) {
            // 稳定亮起
            delay(Random.nextLong(1000, 5000))

            // 快速闪烁
            repeat(Random.nextInt(3, 8)) {
                // 随机变暗或全黑
                alpha = if (Random.nextBoolean()) 0f else 0.3f
                delay(Random.nextLong(20, 150))
                alpha = 1f
                delay(Random.nextLong(20, 100))
            }

            // 偶尔的长熄灭
            if (Random.nextFloat() < 0.1f) {
                alpha = 0f
                delay(Random.nextLong(300, 800))
                alpha = 1f
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val baseStroke = remember(density) { Stroke(width = with(density) { 1.dp.toPx() }) }
    val glowStroke = remember(density) { Stroke(width = with(density) { 2.dp.toPx() }) }
    val largeBlur = remember(density) { with(density) { 10.dp.toPx() } }
    val smallBlur = remember(density) { with(density) { 3.dp.toPx() } }

    BasicText(
        text = text,
        style = style.copy(color = Color.Transparent),
        onTextLayout = { layoutResult = it },
        modifier = modifier.drawBehind {
            val layout = layoutResult ?: return@drawBehind
            val mp = layout.multiParagraph
            val currentAlpha = alpha

            drawIntoCanvas { canvas ->
                // 底座
                mp.paint(
                    canvas = canvas,
                    color = Color.Gray.copy(alpha = 0.3f),
                    shadow = null,
                    decoration = null,
                    drawStyle = baseStroke
                )

                // 辉光
                if (currentAlpha > 0f) {
                    mp.paint(
                        canvas = canvas,
                        color = neonColor.copy(alpha = 0.6f * currentAlpha),
                        shadow = Shadow(
                            color = neonColor.copy(alpha = currentAlpha),
                            offset = Offset.Zero,
                            blurRadius = largeBlur * currentAlpha
                        ),
                        decoration = null,
                        drawStyle = glowStroke
                    )

                    // 核心
                    mp.paint(
                        canvas = canvas,
                        color = Color.White.copy(alpha = 0.9f * currentAlpha),
                        shadow = Shadow(
                            color = neonColor,
                            offset = Offset.Zero,
                            blurRadius = smallBlur * currentAlpha
                        ),
                        decoration = null,
                        drawStyle = null
                    )
                }
            }
        }
    )
}