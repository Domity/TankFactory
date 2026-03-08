package com.rbtsoft.tankfactory.lsbtank

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.molecules.CyberButton
import com.domity.cybertheme.molecules.CyberLoading
import com.domity.cybertheme.templates.CyberScaffold
import com.rbtsoft.tankfactory.R
import com.rbtsoft.tankfactory.ui.components.FastUriImage

@Composable
fun LSBTankViewerScreen(
    viewModel: LSBTankViewerViewModel = viewModel()
) {
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val isTooLarge by viewModel.isResultTooLarge.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDecoding by viewModel.isDecoding.collectAsState()
    val cachedDisplayBitmap = remember(displayBitmap) {
        displayBitmap?.asImageBitmap()
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                ) {
                    ImageSelectionSlot(
                        modifier = Modifier.fillMaxSize(),
                        uri = selectedImageUri,
                        placeholderText = stringResource(id = R.string.select_image).uppercase(),
                        onClick = { imagePickerLauncher.launch("image/*") }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
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
                    CyberButton(
                        text = if (isSaving) stringResource(id = R.string.saving) else stringResource(id = R.string.save),
                        onClick = { viewModel.saveImageToDownload() },
                        enabled = (cachedDisplayBitmap != null || isTooLarge) && !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = false
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            CyberSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = CyberTheme.colors.surface,
                borderWidth = 1.dp,
                borderColor = if (cachedDisplayBitmap != null) CyberTheme.colors.primary else CyberTheme.colors.border
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isTooLarge -> {
                            CyberText(
                                text = stringResource(id = R.string.image_too_large),
                                color = CyberTheme.colors.secondary,
                                modifier = Modifier.padding(8.dp),
                                style = CyberTheme.typography.body
                            )
                        }
                        cachedDisplayBitmap != null -> {
                            Image(
                                bitmap = cachedDisplayBitmap,
                                contentDescription = "Decoded Hidden Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            if (!isDecoding) {
                                CyberText(
                                    text = stringResource(id = R.string.generated_image),
                                    color = CyberTheme.colors.textDim,
                                    style = CyberTheme.typography.body
                                )
                            }
                        }
                    }

                    if (isDecoding) {
                        CyberSurface(
                            modifier = Modifier.fillMaxSize(),
                            color = CyberTheme.colors.background.copy(alpha = 0.7f),
                            borderWidth = 0.dp,
                            borderColor = Color.Transparent
                        ) {}

                        CyberLoading(
                            modifier = Modifier.align(Alignment.Center),
                            size = 64.dp,
                            color = CyberTheme.colors.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            CyberText(
                stringResource(id = R.string.lsb_tank_viewer_tips),
                color = CyberTheme.colors.text,
                style = CyberTheme.typography.body.copy(fontSize = 12.sp),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ImageSelectionSlot(
    modifier: Modifier = Modifier,
    uri: Uri?,
    placeholderText: String,
    onClick: () -> Unit
) {
    CyberSurface(
        modifier = modifier
            .clickable(onClick = onClick),
        color = CyberTheme.colors.surface,
        borderWidth = 1.dp,
        borderColor = if (uri != null) CyberTheme.colors.primary else CyberTheme.colors.border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uri == null) {
                CyberText(
                    text = placeholderText,
                    color = CyberTheme.colors.textDim,
                    style = CyberTheme.typography.body
                )
            } else {
                FastUriImage(
                    uri = uri,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
