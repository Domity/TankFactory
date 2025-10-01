package com.rbtsoft.tankfactory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rbtsoft.tankfactory.ui.about.AboutDialog
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TankFactoryTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") { MainMenuScreen(navController) }
        composable("view_Mtank") { MirageTankViewerScreen() }
        composable("make_Mtank") { MirageTankMakerScreen() }
        composable("make_LSBtank") { LSBTankMakerScreen() }
        composable("view_LSBTank") {LSBTankViewerScreen()}
    }
}

@Composable
fun MainMenuScreen(navController: NavController) {
    var showAboutDialog by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Tank Factory",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = { navController.navigate("view_Mtank") }) {
                Text("查看幻影坦克")
            }
            Button(onClick = { navController.navigate("make_Mtank") }) {
                Text("制作幻影坦克")
            }
            Button(onClick = { navController.navigate("view_LSBTank")} ) {
                Text("查看无影坦克")
            }
            Button(onClick = { navController.navigate("make_LSBtank") }) {
                Text("制作无影坦克")
            }

        }
        Button(onClick = { showAboutDialog=true },
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Text("关于")
        }
    }
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

