package com.rbtsoft.tankfactory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.rbtsoft.tankfactory.lsbtank.LSBTankMakerScreen
import com.rbtsoft.tankfactory.lsbtank.LSBTankViewerScreen
import com.rbtsoft.tankfactory.miragetank.MirageTankMakerScreen
import com.rbtsoft.tankfactory.miragetank.MirageTankViewerScreen
import com.rbtsoft.tankfactory.ui.about.AboutDialog
import com.rbtsoft.tankfactory.ui.theme.NeonText
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
        composable("view_LSBTank") { LSBTankViewerScreen() }
    }
}

@Composable
fun MainMenuScreen(navController: NavController) {
    var showAboutDialog by remember { mutableStateOf(false) }
    val systemNeonColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            NeonText(
                text = "Tank Factory",
                neonColor = systemNeonColor,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { navController.navigate("view_Mtank") }) {
                    Text(stringResource(id = R.string.view_mirage_tank))
                }
                Button(onClick = { navController.navigate("make_Mtank") }) {
                    Text(stringResource(id = R.string.make_mirage_tank))
                }
            }
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { navController.navigate("view_LSBTank") }) {
                    Text(stringResource(id = R.string.view_lsb_tank))
                }
                Button(onClick = { navController.navigate("make_LSBtank") }) {
                    Text(stringResource(id = R.string.make_lsb_tank))
                }
            }

        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { showAboutDialog = true }) {
                Text(stringResource(id = R.string.about))
            }
        }
    }
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
