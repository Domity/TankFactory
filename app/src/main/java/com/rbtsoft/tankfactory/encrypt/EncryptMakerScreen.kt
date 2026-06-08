package com.rbtsoft.tankfactory.encrypt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domity.cybertheme.molecules.CyberLoading
import com.rbtsoft.tankfactory.R
import io.github.domity.cybertheme.atoms.CyberSurface
import io.github.domity.cybertheme.atoms.CyberText
import io.github.domity.cybertheme.foundation.CyberTheme
import io.github.domity.cybertheme.molecules.CyberButton
import io.github.domity.cybertheme.templates.CyberScaffold

@Composable
fun EncryptMakerScreen(
    viewModel: EncryptMakerViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }

    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val password by viewModel.password.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isDone by viewModel.isDone.collectAsState()

    val isFileSelected = selectedFileName != null
    val hasPassword = password.isNotEmpty()

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
                placeholderText = stringResource(id = R.string.select_image),
                onClick = { filePickerLauncher.launch("image/*") }
            )

            Spacer(Modifier.height(24.dp))

            if (isDone && hasPassword) {
                CyberSurface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberTheme.colors.surface,
                    borderWidth = 1.dp,
                    borderColor = CyberTheme.colors.primary,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CyberText(
                            text = stringResource(id = R.string.encrypt_maker_encrypt_done),
                            color = CyberTheme.colors.primary,
                            style = CyberTheme.typography.body
                        )
                        Spacer(Modifier.height(8.dp))
                        CyberText(
                            text = stringResource(id = R.string.encrypt_maker_password, password),
                            color = CyberTheme.colors.text,
                            style = CyberTheme.typography.button
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CyberButton(
                                text = stringResource(id = R.string.encrypt_maker_copy_password),
                                onClick = { viewModel.copyPasswordToClipboard() },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                            CyberButton(
                                text = if (isProcessing) stringResource(id = R.string.saving)
                                       else stringResource(id = R.string.save),
                                onClick = { viewModel.saveEncryptedFile() },
                                enabled = !isProcessing,
                                isPrimary = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                    text = stringResource(id = R.string.encrypt_maker_encrypting),
                    color = CyberTheme.colors.textDim,
                    style = CyberTheme.typography.body
                )
                Spacer(Modifier.height(24.dp))
            }

            CyberButton(
                text = if (isProcessing) stringResource(id = R.string.encrypt_maker_encrypting)
                       else stringResource(id = R.string.encrypt_maker_encrypt),
                onClick = { viewModel.encrypt() },
                enabled = isFileSelected && !isProcessing,
                modifier = Modifier.fillMaxWidth(0.65f),
                isPrimary = true
            )

            Spacer(Modifier.height(16.dp))

            CyberText(
                text = stringResource(id = R.string.encrypt_maker_tips),
                color = CyberTheme.colors.text
            )
        }
    }
}

@Composable
internal fun FileSelectionSlot(
    modifier: Modifier = Modifier,
    isFileSelected: Boolean,
    fileName: String?,
    placeholderText: String,
    onClick: () -> Unit
) {
    CyberSurface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        color = CyberTheme.colors.surface,
        borderWidth = 1.dp,
        borderColor = if (isFileSelected) CyberTheme.colors.primary else CyberTheme.colors.border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isFileSelected && fileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CyberText(
                        text = fileName,
                        color = CyberTheme.colors.text,
                        style = CyberTheme.typography.body
                    )
                }
            } else {
                CyberText(
                    text = placeholderText,
                    color = CyberTheme.colors.textDim,
                    style = CyberTheme.typography.body
                )
            }
        }
    }
}
