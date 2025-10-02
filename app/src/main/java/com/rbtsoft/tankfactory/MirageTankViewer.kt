package com.rbtsoft.tankfactory

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.compose.AsyncImage
import com.rbtsoft.tankfactory.ui.theme.MirageTankImageTheme
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme

class MirageTankViewMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TankFactoryTheme{
                MirageTankViewerScreen()
            }
        }
    }

}

@Composable
fun MirageTankViewerScreen() {
    // --- 状态管理 ---
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        MirageTankImageTheme(isDarkMode = isDarkMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri == null) {
                    Text(
                        text = "请点击左下角按钮选择图片",
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

        //Text("提示\n没有任何效果？幻影坦克可能已被破坏", color = MaterialTheme.colorScheme.onBackground)

//        FloatingActionButton(
//            onClick = {
//                photoPickerLauncher.launch("image/*")
//            },
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .padding(16.dp),
//            shape = CircleShape,
//            containerColor = MaterialTheme.colorScheme.primaryContainer
//        ) {
//            Icon(
//                imageVector = Icons.Filled.AddPhotoAlternate,
//                contentDescription = "Select Image"
//            )
//        }
//
//        FloatingActionButton(
//            onClick = {
//                isDarkMode = !isDarkMode
//            },
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp),
//            shape = CircleShape,
//            containerColor = MaterialTheme.colorScheme.secondaryContainer
//        ) {
//            Icon(
//                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
//                contentDescription = "Toggle Theme"
//            )
//        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // 整个 Row 居中对齐到底部
                .padding(bottom = 32.dp, start = 64.dp, end = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // 按钮之间留出空间
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 选择图片按钮 (FloatingActionButton)
            FloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch("image/*")
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Select Image"
                )
            }

            // 2. 切换主题按钮 (FloatingActionButton)
            FloatingActionButton(
                onClick = {
                    isDarkMode = !isDarkMode
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle Theme"
                )
            }
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