package com.rbtsoft.tankfactory.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rbtsoft.tankfactory.ui.theme.AppTheme
import com.rbtsoft.tankfactory.ui.theme.ThemeDataStore
import kotlinx.coroutines.launch

@Composable
fun Settings() {
    val context = LocalContext.current
    val themeDataStore = remember { ThemeDataStore(context) }
    val themeSettings by themeDataStore.themeSettingsFlow.collectAsState(
        initial = null
    )
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold {
        innerPadding ->
        themeSettings?.let {
            settings ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "动态主题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = settings.useDynamicColor,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                themeDataStore.saveThemeSettings(
                                    useDynamicColor = isChecked,
                                    selectedTheme = settings.selectedTheme
                                )
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "选择主题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (settings.selectedTheme) {
                            AppTheme.BLACK_AND_WHITE -> "黑白"
                            AppTheme.ORANGE -> "户橙风"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeChooserDialog(
            onDismiss = { showThemeDialog = false },
            onThemeSelected = {
                theme ->
                scope.launch {
                    themeDataStore.saveThemeSettings(
                        useDynamicColor = false,
                        selectedTheme = theme
                    )
                }
            }
        )
    }
}

@Composable
private fun ThemeChooserDialog(onDismiss: () -> Unit, onThemeSelected: (AppTheme) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                Text(
                    text = "黑白",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onThemeSelected(AppTheme.BLACK_AND_WHITE)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp)
                )
                Text(
                    text = "户橙风",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onThemeSelected(AppTheme.ORANGE)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
