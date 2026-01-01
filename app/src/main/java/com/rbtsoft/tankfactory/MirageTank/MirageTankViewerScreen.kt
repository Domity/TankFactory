package com.rbtsoft.tankfactory.miragetank

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.atoms.CyberText
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.molecules.CyberSwitch
import com.domity.cybertheme.templates.CyberScaffold
import com.rbtsoft.tankfactory.R
import com.rbtsoft.tankfactory.ui.theme.MirageTankImageTheme
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme

@Composable
fun MirageTankViewerScreen() {
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri }
    )

    CyberScaffold(useSafeArea = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            MirageTankImageTheme(isDarkMode = isDarkMode) {
                CyberSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .padding(24.dp)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    color = CyberTheme.colors.background,
                    borderWidth = 1.dp,
                    borderColor = CyberTheme.colors.border
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri == null) {
                            CyberText(
                                text = stringResource(id = R.string.select_image).uppercase(),
                                color = CyberTheme.colors.text
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
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                CyberSwitch(
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CyberText(
                text = stringResource(id = R.string.mirage_tank_viewer_tips),
                modifier = Modifier.padding(bottom = 16.dp),
                color = CyberTheme.colors.text,
                style = CyberTheme.typography.body
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TankFactoryTheme {
        MirageTankViewerScreen()
    }
}