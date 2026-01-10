package com.rbtsoft.tankfactory.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
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

    var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    BasicText(
        text = text,
        style = style.copy(color = Color.Transparent),
        onTextLayout = { layoutResult = it },
        modifier = modifier.drawBehind {
            val layout = layoutResult ?: return@drawBehind
            val mp = layout.multiParagraph

            drawIntoCanvas { canvas ->
                // 底座
                mp.paint(
                    canvas = canvas,
                    color = Color.Gray.copy(alpha = 0.3f),
                    shadow = null,
                    decoration = null,
                    drawStyle = Stroke(width = 3f)
                )

                // 辉光
                if (alpha > 0f) {
                    mp.paint(
                        canvas = canvas,
                        color = neonColor.copy(alpha = 0.6f * alpha),
                        shadow = Shadow(
                            color = neonColor.copy(alpha = alpha),
                            offset = Offset.Zero,
                            blurRadius = 30f * alpha
                        ),
                        decoration = null,
                        drawStyle = Stroke(width = 5f)
                    )

                    // 核心
                    mp.paint(
                        canvas = canvas,
                        color = Color.White.copy(alpha = 0.9f * alpha),
                        shadow = Shadow(
                            color = neonColor,
                            offset = Offset.Zero,
                            blurRadius = 8f * alpha
                        ),
                        decoration = null,
                        drawStyle = null // 默认为 Fill
                    )
                }
            }
        }
    )
}