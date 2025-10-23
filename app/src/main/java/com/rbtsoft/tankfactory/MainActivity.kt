package com.rbtsoft.tankfactory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rbtsoft.tankfactory.ui.about.AboutDialog
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rbtsoft.tankfactory.LSBTank.LSBTankMakerScreen
import com.rbtsoft.tankfactory.LSBTank.LSBTankViewerScreen
import com.rbtsoft.tankfactory.MirageTank.MirageTankMakerScreen
import com.rbtsoft.tankfactory.MirageTank.MirageTankViewerScreen
import com.rbtsoft.tankfactory.settings.Settings
import com.rbtsoft.tankfactory.ui.theme.ThemeDataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themeDataStore = ThemeDataStore(this)

        setContent {
            val themeSettings by themeDataStore.themeSettingsFlow.collectAsState(
                initial = null
            )
            if (themeSettings != null) {
                TankFactoryTheme(
                    useDynamicColor = themeSettings!!.useDynamicColor,
                    selectedTheme = themeSettings!!.selectedTheme
                ) {
                    AppNavigator()
                }
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
        composable("settings") { Settings() }
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
                Text(stringResource(id = R.string.main_menu_view_mirage_tank))
            }
            Button(onClick = { navController.navigate("make_Mtank") }) {
                Text(stringResource(id = R.string.main_menu_make_mirage_tank))
            }
            Button(onClick = { navController.navigate("view_LSBTank")} ) {
                Text(stringResource(id = R.string.main_menu_view_lsb_tank))
            }
            Button(onClick = { navController.navigate("make_LSBtank") }) {
                Text(stringResource(id = R.string.main_menu_make_lsb_tank))
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
            Button(onClick = { navController.navigate("settings") }) {
                Text(stringResource(id = R.string.main_menu_settings))
            }
            Button(onClick = { showAboutDialog = true }) {
                Text(stringResource(id = R.string.main_menu_about))
            }
        }
    }
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
