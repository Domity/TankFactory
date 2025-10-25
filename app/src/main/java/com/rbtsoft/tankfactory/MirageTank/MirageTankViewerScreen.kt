package com.rbtsoft.tankfactory.miragetank

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rbtsoft.tankfactory.R
import com.rbtsoft.tankfactory.ui.theme.MirageTankImageTheme
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme

@Composable
fun MirageTankViewerScreen() {
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        MirageTankImageTheme(isDarkMode = isDarkMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri == null) {
                    Text(
                        text = stringResource(id = R.string.mirage_tank_viewer_select_image_prompt),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                } else {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Switch(
            checked = isDarkMode,
            onCheckedChange = { isDarkMode = it },
        )

        Text(
            text = stringResource(id = R.string.mirage_tank_viewer_tip),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TankFactoryTheme {
        MirageTankViewerScreen()
    }
}
