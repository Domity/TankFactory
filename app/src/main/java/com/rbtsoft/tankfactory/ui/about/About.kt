package com.rbtsoft.tankfactory.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.molecules.CyberButton
import com.domity.cybertheme.molecules.CyberDialog
import com.rbtsoft.tankfactory.BuildConfig
import com.rbtsoft.tankfactory.R

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    CyberDialog(
        onDismissRequest = onDismiss,
        title = {
            CyberText(
                text = "Tank Factory",
                style = CyberTheme.typography.headline,
                color = CyberTheme.colors.primary
            )
        },
        content = {
            Column(horizontalAlignment = Alignment.Start) {
                CyberText(
                    text = stringResource(id = R.string.about_dialog_description),
                    style = CyberTheme.typography.body,
                    color = CyberTheme.colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                CyberText(
                    text = stringResource(id = R.string.about_dialog_developer),
                    style = CyberTheme.typography.body,
                    color = CyberTheme.colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                CyberText(
                    text = stringResource(id = R.string.about_dialog_version, BuildConfig.VERSION_NAME),
                    style = CyberTheme.typography.body,
                    color = CyberTheme.colors.text
                )
            }
        },
        buttons = {
            CyberButton(
                text = stringResource(id = R.string.about_dialog_project_home),
                onClick = { uriHandler.openUri("https://github.com/Domity/TankFactory") },
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            CyberButton(
                text = stringResource(id = R.string.about_dialog_confirm),
                onClick = onDismiss,
                isPrimary = true,
                modifier = Modifier.weight(1f)
            )
        }
    )
}
