package com.rbtsoft.tankfactory.miragetank

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbtsoft.tankfactory.ui.theme.MirageTankImageTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.rbtsoft.tankfactory.R

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


    val imagePickerLauncher1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) viewModel.setImage1Uri(uri) }
    )
    val imagePickerLauncher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) viewModel.setImage2Uri(uri) }
    )
    var generatedImageDarkBackground by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFA0A0A0))
                        .clickable { imagePickerLauncher1.launch("image/*") }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImage1Uri == null) {
                        Text(stringResource(id = R.string.cover_image), color = Color.Black)
                    } else {
                        AsyncImage(
                            model = selectedImage1Uri,
                            contentDescription = "Cover Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }


            Spacer(Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFA0A0A0))
                        .clickable { imagePickerLauncher2.launch("image/*") }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImage2Uri == null) {
                        Text(stringResource(id = R.string.hidden_image), color = Color.Black)
                    } else {
                        AsyncImage(
                            model = selectedImage2Uri,
                            contentDescription = "Hidden Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

        }

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Slider(value = photo1K, onValueChange = { photo1K = it }, valueRange = 0.1f..2.0f)
            Text(stringResource(id = R.string.mirage_tank_maker_cover_image_brightness, photo1K), color = MaterialTheme.colorScheme.onBackground)

            Slider(value = photo2K, onValueChange = { photo2K = it }, valueRange = 0.1f..2.0f)
            Text(stringResource(id = R.string.mirage_tank_maker_hidden_image_brightness, photo2K), color = MaterialTheme.colorScheme.onBackground)

            Slider(value = threshold, onValueChange = { threshold = it }, valueRange = 1f..250f)
            Text(stringResource(id = R.string.mirage_tank_maker_threshold, threshold.toInt()), color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            MirageTankImageTheme(isDarkMode = generatedImageDarkBackground) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isTooLarge -> {
                            Text(
                                stringResource(id = R.string.image_too_large),
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                        displayBitmap != null -> {
                            Image(
                                bitmap = displayBitmap!!.asImageBitmap(),
                                contentDescription = "Encoded Mirage Tank",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Text(stringResource(id = R.string.generated_image), color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("", color = MaterialTheme.colorScheme.onBackground)
                    Switch(
                        checked = generatedImageDarkBackground,
                        onCheckedChange = { generatedImageDarkBackground = it }
                    )
                }
                Button(
                    onClick = {
                        viewModel.saveImageToDownload()
                    },
                    enabled = !isGenerating && !isSaving && (isTooLarge || displayBitmap != null)
                ) {
                    Text(if (isSaving) stringResource(id = R.string.saving) else stringResource(id = R.string.save))
                }

            }
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.onPressMakerButton()
                viewModel.generateMirageTank(photo1K, photo2K, threshold.toInt())
            },
            enabled = selectedImage1Uri != null && selectedImage2Uri != null && !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isGenerating) stringResource(id = R.string.making)else stringResource(id = R.string.make))
        }
        Text(stringResource(id = R.string.mirage_tank_maker_tips), color = MaterialTheme.colorScheme.onBackground)
    }
}