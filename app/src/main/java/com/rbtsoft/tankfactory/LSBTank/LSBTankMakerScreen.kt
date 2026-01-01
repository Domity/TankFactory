package com.rbtsoft.tankfactory.lsbtank

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.rbtsoft.tankfactory.R
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.molecules.CyberButton
import com.domity.cybertheme.molecules.CyberLoading
import com.domity.cybertheme.molecules.CyberSlider
import com.domity.cybertheme.templates.CyberScaffold
import kotlin.math.roundToInt

@Composable
fun LSBTankMakerScreen(
    viewModel: LSBTankMakerViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onMakerScreenEntered()
    }

    val selectedImage1Uri by viewModel.selectedImage1Uri.collectAsState()
    val selectedImage2Uri by viewModel.selectedImage2Uri.collectAsState()
    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val isTooLarge by viewModel.isResultTooLarge.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var compress by remember { mutableIntStateOf(4) }

    val imagePickerLauncher1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) viewModel.setImage1Uri(uri) }
    )
    val imagePickerLauncher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) viewModel.setImage2Uri(uri) }
    )
    CyberScaffold(useSafeArea = true) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                ImageSelectionSlot(
                    modifier = Modifier.weight(1f),
                    uri = selectedImage1Uri,
                    placeholderText = stringResource(id = R.string.cover_image),
                    onClick = { imagePickerLauncher1.launch("image/*") }
                )

                ImageSelectionSlot(
                    modifier = Modifier.weight(1f),
                    uri = selectedImage2Uri,
                    placeholderText = stringResource(id = R.string.hidden_image),
                    onClick = { imagePickerLauncher2.launch("image/*") }
                )
            }

            Spacer(Modifier.height(24.dp))

            CyberSurface(
                modifier = Modifier.fillMaxWidth(),
                color = CyberTheme.colors.surface,
                borderWidth = 1.dp,
                borderColor = CyberTheme.colors.border
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CyberText(
                        text = stringResource(id = R.string.lsb_tank_maker_compress_level, compress),
                        color = CyberTheme.colors.text
                    )

                    Spacer(Modifier.height(12.dp))
                    CyberSlider(
                        value = compress.toFloat(),
                        onValueChange = { compress = it.roundToInt() },
                        range = 1f..7f,
                        steps = 6
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp),
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
                                    text = stringResource(id = R.string.image_too_large),
                                    color = CyberTheme.colors.secondary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            displayBitmap != null -> {
                                Image(
                                    bitmap = displayBitmap!!.asImageBitmap(),
                                    contentDescription = "Result",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                if (!isGenerating) {
                                    CyberText(
                                        text = stringResource(id = R.string.generated_image),
                                        color = CyberTheme.colors.textDim
                                    )
                                }
                            }
                        }
                        if (isGenerating) {
                            CyberSurface(
                                modifier = Modifier.fillMaxSize(),
                                color = CyberTheme.colors.background.copy(alpha = 0.7f)
                            ) {}

                            CyberLoading(
                                modifier = Modifier.align(Alignment.Center),
                                size = 48.dp,
                                color = CyberTheme.colors.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // 保存按钮
                Column {
                    CyberButton(
                        text = if (isSaving) stringResource(id = R.string.saving) else stringResource(id = R.string.save),
                        onClick = { viewModel.saveImageToDownload() },
                        enabled = (displayBitmap != null || isTooLarge) && !isSaving,
                        isPrimary = false,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            CyberButton(
                text = if (isGenerating) stringResource(id = R.string.making) else stringResource(id = R.string.make),
                onClick = {
                    viewModel.onPressMakerButton()
                    viewModel.generateLSBTank(compress)
                },
                enabled = selectedImage1Uri != null && selectedImage2Uri != null && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true
            )

            Spacer(Modifier.height(16.dp))

            // 提示文字
            CyberText(
                text = stringResource(id = R.string.lsb_tank_maker_tips),
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