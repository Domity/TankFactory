package io.github.domity.tankfactory.encrypt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domity.cybertheme.molecules.CyberLoading
import io.github.domity.cybertheme.atoms.CyberSurface
import io.github.domity.cybertheme.atoms.CyberText
import io.github.domity.cybertheme.foundation.CyberTheme
import io.github.domity.cybertheme.molecules.CyberButton
import io.github.domity.cybertheme.molecules.CyberInput
import io.github.domity.cybertheme.molecules.CyberSwitch
import io.github.domity.cybertheme.templates.CyberScaffold
import io.github.domity.tankfactory.R

@Composable
fun EncryptViewerScreen(
    viewModel: EncryptViewerViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }

    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDone by viewModel.isDone.collectAsState()
    val decryptedFileName by viewModel.decryptedFileName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) viewModel.setFileUri(uri) }
    )
    val onSelectFileClick = remember { { filePickerLauncher.launch("*/*") } }

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
                fileName = selectedFileName,
                placeholderText = stringResource(id = R.string.encrypt_viewer_select_file),
                onClick = onSelectFileClick
            )

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CyberInput(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.setPassword(it)
                    },
                    placeholder = stringResource(id = R.string.encrypt_viewer_enter_password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
                Spacer(Modifier.width(12.dp))
                CyberSwitch(
                    checked = passwordVisible,
                    onCheckedChange = { passwordVisible = it }
                )
            }
            errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                CyberText(
                    text = message,
                    color = CyberTheme.colors.secondary,
                    style = CyberTheme.typography.body
                )
            }

            Spacer(Modifier.height(24.dp))
            if (isDone) {
                DecryptedSuccessPanel(
                    decryptedFileName = decryptedFileName,
                    isProcessing = isProcessing,
                    onSave = viewModel::saveDecryptedFile
                )
                Spacer(Modifier.height(24.dp))
            }
            if (isProcessing && !isDone) {
                DecryptProcessingIndicator()
                Spacer(Modifier.height(24.dp))
            }

            CyberButton(
                text = stringResource(
                    id = if (isProcessing) R.string.encrypt_viewer_decrypting else R.string.encrypt_viewer_decrypt
                ),
                onClick = viewModel::decrypt,
                enabled = selectedFileName != null && password.isNotEmpty() && !isProcessing,
                modifier = Modifier.fillMaxWidth(0.5f),
                isPrimary = true
            )

            Spacer(Modifier.height(16.dp))

            CyberText(
                text = stringResource(id = R.string.encrypt_viewer_tips),
                color = CyberTheme.colors.text,
            )
        }
    }
}

@Composable
private fun DecryptedSuccessPanel(
    decryptedFileName: String?,
    isProcessing: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberSurface(
        modifier = modifier.fillMaxWidth(),
        color = CyberTheme.colors.surface,
        borderWidth = 1.dp,
        borderColor = CyberTheme.colors.primary
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CyberText(
                text = stringResource(
                    id = R.string.encrypt_viewer_decrypt_done,
                    decryptedFileName ?: ""
                ),
                color = CyberTheme.colors.primary,
                style = CyberTheme.typography.body
            )
            Spacer(Modifier.height(12.dp))
            CyberButton(
                text = stringResource(id = if (isProcessing) R.string.saving else R.string.save),
                onClick = onSave,
                enabled = !isProcessing,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DecryptProcessingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        CyberLoading(size = 48.dp, color = CyberTheme.colors.primary)
    }
    CyberText(
        text = stringResource(id = R.string.encrypt_viewer_decrypting),
        color = CyberTheme.colors.textDim,
        style = CyberTheme.typography.body
    )
}