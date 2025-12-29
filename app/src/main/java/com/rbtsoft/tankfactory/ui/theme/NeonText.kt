package com.rbtsoft.tankfactory.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import com.domity.cybertheme.atoms.CyberText
import kotlinx.coroutines.delay
import kotlin.random.Random


@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    neonColor: Color,
    style: TextStyle
) {
    val lightAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            val stableDuration = Random.nextLong(1000, 5000)
            delay(stableDuration)
            val flickerCount = Random.nextInt(3, 8)
            repeat(flickerCount) {
                val targetDimness = if (Random.nextBoolean()) 0f else 0.3f
                lightAlpha.snapTo(targetDimness)
                delay(Random.nextLong(20, 150))
                lightAlpha.snapTo(1f)
                delay(Random.nextLong(20, 100))
            }

            if (Random.nextInt(10) == 0) {
                lightAlpha.animateTo(0f, animationSpec = tween(50))
                delay(Random.nextLong(300, 800))
                lightAlpha.animateTo(1f, animationSpec = tween(20))
            }
        }
    }

    Box(modifier = modifier) {
        // 底座
        CyberText(
            text = text,
            style = style.copy(
                color = Color.Gray.copy(alpha = 0.3f),
                drawStyle = Stroke(width = 3f)
            )
        )
        // 辉光
        CyberText(
            text = text,
            style = style.copy(
                color = neonColor.copy(alpha = 0.6f * lightAlpha.value),
                drawStyle = Stroke(width = 5f),
                shadow = Shadow(
                    color = neonColor.copy(alpha = lightAlpha.value),
                    offset = Offset.Zero,
                    blurRadius = 30f * lightAlpha.value
                )
            )
        )
        // 核心
        CyberText(
            text = text,
            style = style.copy(
                color = Color.White.copy(alpha = 0.9f * lightAlpha.value),
                shadow = Shadow(
                    color = neonColor,
                    offset = Offset.Zero,
                    blurRadius = 8f * lightAlpha.value
                )
            )
        )
    }
}