package com.rbtsoft.tankfactory
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun LSBTankMakerScreen(
    viewModel: LSBTankMainViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onMakerScreenEntered()
    }

    val selectedImage1Uri by viewModel.selectedImage1Uri.collectAsState()
    val selectedImage2Uri by viewModel.selectedImage2Uri.collectAsState()
    val encodedBitmap by viewModel.encodedImage.collectAsState()
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
                        Text("表图", color = Color.Black)
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
                        Text("里图", color = Color.Black)
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
            Slider(
                value = compress.toFloat(),
                onValueChange = { compress = it.roundToInt().coerceIn(1,7) },
                valueRange = 1f..7f,

            )
            Text("压缩度: $compress", color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .background(Color(0xFFA0A0A0))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (encodedBitmap == null) {
                        Text("生成图", color = Color.Black)
                    } else {
                        Image(
                            bitmap = encodedBitmap!!.asImageBitmap(),
                            contentDescription = "Encoded LSB Tank",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        encodedBitmap?.let {
                            viewModel.saveImageToDownloads(it)
                        }
                    },
                    enabled = encodedBitmap != null && !isSaving
                ) {
                    Text("保存")
                }
            }
        }

        Spacer(Modifier.height(16.dp))


        Button(
            onClick = {
                viewModel.onPressMakerButton()
                viewModel.generateLSBTank(compress)
            },
            enabled = selectedImage1Uri != null && selectedImage2Uri != null && !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("制作无影坦克")
        }
        Text("提示\n1.更低的压缩度意味着更好的表图质量和更长的制作时间\n" +
                "2.图片默认保存到download目录", color = MaterialTheme.colorScheme.onBackground)
    }
}