package com.rbtsoft.tankfactory.lsbtank

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.molecules.CyberButton
import com.domity.cybertheme.molecules.CyberLoading
import com.domity.cybertheme.templates.CyberScaffold
import com.rbtsoft.tankfactory.R

@Composable
fun LSBTankViewerScreen(
    viewModel: LSBTankViewerViewModel = viewModel()
) {

    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val isTooLarge by viewModel.isResultTooLarge.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDecoding by viewModel.isDecoding.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setImageUri(uri)
            }
        }
    )

    CyberScaffold(useSafeArea = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                ImageSelectionSlot(
                    modifier = Modifier.fillMaxSize(),
                    uri = selectedImageUri,
                    placeholderText = stringResource(id = R.string.select_image).uppercase(),
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            Spacer(Modifier.height(16.dp))
            CyberButton(
                text = if (isDecoding) stringResource(id = R.string.decoding) else stringResource(id = R.string.decode),
                onClick = {
                    viewModel.onPressDecoderButton()
                    viewModel.decodeLSBTank()
                },
                enabled = selectedImageUri != null && !isDecoding,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true
            )

            Spacer(Modifier.height(24.dp))

            CyberSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = CyberTheme.colors.surface,
                borderWidth = 1.dp,
                borderColor = if (displayBitmap != null) CyberTheme.colors.primary else CyberTheme.colors.border
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isTooLarge -> {
                            CyberText(
                                text=stringResource(id = R.string.image_too_large),
                                color = CyberTheme.colors.secondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        displayBitmap != null -> {
                            Image(
                                bitmap = displayBitmap!!.asImageBitmap(),
                                contentDescription = "Decoded Hidden Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            if (!isDecoding) {
                                CyberText(
                                    text = stringResource(id = R.string.generated_image),
                                    color = CyberTheme.colors.textDim
                                )
                            }
                        }
                    }

                    if (isDecoding) {
                        CyberSurface(
                            modifier = Modifier.fillMaxSize(),
                            color = CyberTheme.colors.background.copy(alpha = 0.7f)
                        ) {}

                        CyberLoading(
                            modifier = Modifier.align(Alignment.Center),
                            size = 64.dp,
                            color = CyberTheme.colors.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            CyberButton(
                text = if (isSaving) stringResource(id = R.string.saving) else stringResource(id = R.string.save),
                onClick = { viewModel.saveImageToDownload() },
                enabled = (displayBitmap != null || isTooLarge) && !isSaving,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            CyberText(
                stringResource(id = R.string.lsb_tank_viewer_tips),
                color = CyberTheme.colors.textDim,
                style = CyberTheme.typography.body.copy(fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun ImageSelectionSlot(
    modifier: Modifier = Modifier,
    uri: Any?,
    placeholderText: String,
    onClick: () -> Unit
) {
    CyberSurface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        color = CyberTheme.colors.surface,
        borderWidth = 1.dp,
        borderColor = if (uri != null) CyberTheme.colors.primary else CyberTheme.colors.border
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uri == null) {
                CyberText(
                    text = placeholderText,
                    color = CyberTheme.colors.textDim
                )
            } else {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}