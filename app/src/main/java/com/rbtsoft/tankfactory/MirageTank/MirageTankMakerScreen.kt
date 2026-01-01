package com.rbtsoft.tankfactory.miragetank

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rbtsoft.tankfactory.R
import com.rbtsoft.tankfactory.ui.theme.MirageTankImageTheme
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.molecules.CyberButton
import com.domity.cybertheme.molecules.CyberLoading
import com.domity.cybertheme.molecules.CyberSlider
import com.domity.cybertheme.molecules.CyberSwitch
import com.domity.cybertheme.templates.CyberScaffold

@Composable
fun MirageTankMakerScreen(
    viewModel: MirageTankMakerViewModel = viewModel()
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
    var photo1K by remember { mutableFloatStateOf(1.0f) }
    var photo2K by remember { mutableFloatStateOf(1.0f) }
    var threshold by remember { mutableFloatStateOf(127f) }
    var generatedImageDarkBackground by remember { mutableStateOf(false) }

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
                .padding(16.dp),
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
                    ControlRow(
                        label = stringResource(id = R.string.mirage_tank_maker_cover_image_brightness, photo1K),
                        value = photo1K,
                        range = 0.1f..2.0f,
                        onValueChange = { photo1K = it }
                    )
                    Spacer(Modifier.height(16.dp))

                    ControlRow(
                        label = stringResource(id = R.string.mirage_tank_maker_hidden_image_brightness, photo2K),
                        value = photo2K,
                        range = 0.1f..2.0f,
                        onValueChange = { photo2K = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    ControlRow(
                        label = stringResource(id = R.string.mirage_tank_maker_threshold, threshold.toInt()),
                        value = threshold,
                        range = 1f..250f,
                        onValueChange = { threshold = it }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                MirageTankImageTheme(isDarkMode = generatedImageDarkBackground) {
                    CyberSurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp),
                        color = CyberTheme.colors.background,
                        borderWidth = 1.dp,
                        borderColor = CyberTheme.colors.border
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isTooLarge -> {
                                    CyberText(
                                        text = stringResource(id = R.string.image_too_large),
                                        color = Color.Red
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
                                            color = CyberTheme.colors.text
                                        )
                                    }
                                }
                            }
                            if (isGenerating) {
                                CyberSurface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = CyberTheme.colors.background.copy(alpha = 0.7f)
                                ) {}
                                CyberTheme{
                                    CyberLoading(
                                        modifier = Modifier.align(Alignment.Center),
                                        size = 48.dp,
                                        color = CyberTheme.colors.primary
                                    )
                                }

                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CyberSwitch(
                            checked = generatedImageDarkBackground,
                            onCheckedChange = { generatedImageDarkBackground = it }
                        )
                    }

                    CyberButton(
                        text = if (isSaving) stringResource(id = R.string.saving) else stringResource(id = R.string.save),
                        onClick = { viewModel.saveImageToDownload() },
                        enabled = !isGenerating && !isSaving && (isTooLarge || displayBitmap != null),
                        modifier = Modifier.width(80.dp),
                        isPrimary = false
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            CyberButton(
                text = if (isGenerating) stringResource(id = R.string.making) else stringResource(id = R.string.make),
                onClick = {
                    viewModel.onPressMakerButton()
                    viewModel.generateMirageTank(photo1K, photo2K, threshold.toInt())
                },
                enabled = selectedImage1Uri != null && selectedImage2Uri != null && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true
            )

            Spacer(Modifier.height(16.dp))

            CyberText(
                text = stringResource(id = R.string.mirage_tank_maker_tips),
                color = CyberTheme.colors.text,
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

@Composable
private fun ControlRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        CyberText(
            text = label,
            color = CyberTheme.colors.text,
            style = CyberTheme.typography.body
        )
        Spacer(Modifier.height(8.dp))
        CyberSlider(
            value = value,
            onValueChange = onValueChange,
            range = range
        )
    }
}