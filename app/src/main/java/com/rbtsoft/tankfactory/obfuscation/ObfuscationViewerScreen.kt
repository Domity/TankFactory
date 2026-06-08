package com.rbtsoft.tankfactory.obfuscation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domity.cybertheme.molecules.CyberLoading
import com.rbtsoft.tankfactory.R
import io.github.domity.cybertheme.atoms.CyberSurface
import io.github.domity.cybertheme.atoms.CyberText
import io.github.domity.cybertheme.foundation.CyberTheme
import io.github.domity.cybertheme.molecules.CyberButton
import io.github.domity.cybertheme.molecules.CyberInput
import io.github.domity.cybertheme.templates.CyberScaffold

@Composable
fun ObfuscationViewerScreen(
    viewModel: ObfuscationViewerViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }

    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDone by viewModel.isDone.collectAsState()
    val decryptedFileName by viewModel.decryptedFileName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val isFileSelected = selectedFileName != null
    val password = remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) viewModel.setFileUri(uri) }
    )

    CyberScaffold(useSafeArea = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FileSelectionSlot(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                isFileSelected = isFileSelected,
                fileName = selectedFileName,
                placeholderText = stringResource(id = R.string.obfuscation_viewer_select_file),
                onClick = { filePickerLauncher.launch("*/*") }
            )

            Spacer(Modifier.height(24.dp))

            CyberInput(
                value = password.value,
                onValueChange = {
                    password.value = it
                    viewModel.setPassword(it)
                },
                placeholder = stringResource(id = R.string.obfuscation_viewer_enter_password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                CyberText(
                    text = errorMessage!!,
                    color = CyberTheme.colors.secondary,
                    style = CyberTheme.typography.body
                )
            }

            Spacer(Modifier.height(24.dp))

            if (isDone) {
                CyberSurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberTheme.colors.surface,
                    borderWidth = 1.dp,
                    borderColor = CyberTheme.colors.primary
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CyberText(
                            text = stringResource(
                                id = R.string.obfuscation_viewer_decrypt_done,
                                decryptedFileName ?: ""
                            ),
                            color = CyberTheme.colors.primary,
                            style = CyberTheme.typography.body
                        )
                        Spacer(Modifier.height(12.dp))
                        CyberButton(
                            text = if (isProcessing) stringResource(id = R.string.saving)
                                   else stringResource(id = R.string.save),
                            onClick = { viewModel.saveDecryptedFile() },
                            enabled = !isProcessing,
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            if (isProcessing && !isDone) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CyberLoading(size = 48.dp, color = CyberTheme.colors.primary)
                }
                CyberText(
                    text = stringResource(id = R.string.obfuscation_viewer_decrypting),
                    color = CyberTheme.colors.textDim,
                    style = CyberTheme.typography.body
                )
                Spacer(Modifier.height(24.dp))
            }

            CyberButton(
                text = if (isProcessing) stringResource(id = R.string.obfuscation_viewer_decrypting)
                       else stringResource(id = R.string.obfuscation_viewer_decrypt),
                onClick = { viewModel.decrypt() },
                enabled = isFileSelected && password.value.isNotEmpty() && !isProcessing,
                modifier = Modifier.fillMaxWidth(0.65f),
                isPrimary = true
            )

            Spacer(Modifier.height(16.dp))

            CyberText(
                text = stringResource(id = R.string.obfuscation_viewer_tips),
                color = CyberTheme.colors.text,
                style = CyberTheme.typography.body.copy(fontSize = 12.sp)
            )
        }
    }
}
