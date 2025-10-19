package com.rbtsoft.tankfactory

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun LSBTankViewerScreen(
    viewModel: LSBTankViewerViewModel = viewModel()
) {

    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val decodedBitmap by viewModel.decodedImage.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFA0A0A0))
                .clickable { imagePickerLauncher.launch("image/*") }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri == null) {
                Text(stringResource(id = R.string.lsb_tank_viewer_select_tank), color = Color.Black)
            } else {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.onPressDecoderButton()
                viewModel.decodeLSBTank()
            },
            enabled = selectedImageUri != null && !isDecoding,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isDecoding) stringResource(id = R.string.lsb_tank_viewer_decoding) else stringResource(id = R.string.lsb_tank_viewer_decode))
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFA0A0A0))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (decodedBitmap == null) {
                Text(stringResource(id = R.string.lsb_tank_viewer_decoded_result), color = Color.Black)
            } else {
                Image(
                    bitmap = decodedBitmap!!.asImageBitmap(),
                    contentDescription = "Decoded Hidden Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isDecoding) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.height(16.dp))

        //  保存按钮
        Button(
            onClick = {
                decodedBitmap?.let {
                    viewModel.saveImageToDownloads(it)
                }
            },
            enabled = decodedBitmap != null && !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) stringResource(id = R.string.lsb_tank_viewer_saving) else stringResource(id = R.string.lsb_tank_viewer_save))
        }
        Text(stringResource(id = R.string.lsb_tank_viewer_tips), color = MaterialTheme.colorScheme.onBackground)
    }
}