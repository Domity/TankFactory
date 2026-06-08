package io.github.domity.cybertheme.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.domity.cybertheme.atoms.CyberSurface
import io.github.domity.cybertheme.foundation.CyberTheme

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
                        horizontalArrangement = Arrangement.Center
                    ) {
                        buttons()
                    }
                }
            }
        }
    }
}
