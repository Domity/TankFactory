package io.github.domity.cybertheme.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.domity.cybertheme.atoms.CyberSurface
import io.github.domity.cybertheme.atoms.CyberText
import io.github.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberListItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: androidx.compose.ui.graphics.Color = CyberTheme.colors.text,
    subtitleColor: androidx.compose.ui.graphics.Color = CyberTheme.colors.textDim,
    onClick: @Composable (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val containerModifier = modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
        .then(if (onClick != null) Modifier.clickable { } else Modifier)

    CyberSurface(
        modifier = containerModifier,
        color = CyberTheme.colors.surface.copy(alpha = 0.5f),
        borderWidth = 1.dp,
        borderColor = CyberTheme.colors.border,
        shape = CutCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                CyberText(
                    text = title,
                    style = CyberTheme.typography.body,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                CyberText(
                    text = subtitle,
                    style = CyberTheme.typography.button,
                    color = subtitleColor
                )
            }

            if (trailingContent != null) {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    trailingContent()
                }
            }
        }
    }
}