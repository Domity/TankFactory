package io.github.domity.cybertheme.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.domity.cybertheme.atoms.CyberText
import io.github.domity.cybertheme.foundation.CyberTheme


@Composable
fun CyberInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = CyberTheme.typography.body,
    cutSize: Dp = 8.dp,
    borderWidth: Dp = 1.dp
) {
    val primary = CyberTheme.colors.primary
    val border = CyberTheme.colors.border
    val surface = CyberTheme.colors.surface
    val textColor = CyberTheme.colors.text
    val textDim = CyberTheme.colors.textDim

    val borderColor = if (value.isNotEmpty()) primary else border

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        textStyle = textStyle.copy(color = textColor),
        cursorBrush = SolidColor(primary),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val cut = cutSize.toPx()
                        val path = Path().apply {
                            moveTo(cut, 0f)
                            lineTo(w - cut, 0f)
                            lineTo(w, cut)
                            lineTo(w, h - cut)
                            lineTo(w - cut, h)
                            lineTo(cut, h)
                            lineTo(0f, h - cut)
                            lineTo(0f, cut)
                            close()
                        }
                        onDrawBehind {
                            drawPath(path, surface)
                            drawPath(path, borderColor, style = Stroke(borderWidth.toPx()))
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    CyberText(
                        text = placeholder,
                        color = textDim,
                        style = CyberTheme.typography.body
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier
    )
}
