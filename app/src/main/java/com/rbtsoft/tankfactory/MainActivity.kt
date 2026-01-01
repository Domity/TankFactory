package com.rbtsoft.tankfactory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.domity.cybertheme.molecules.CyberButton
import com.rbtsoft.tankfactory.lsbtank.LSBTankMakerScreen
import com.rbtsoft.tankfactory.lsbtank.LSBTankViewerScreen
import com.rbtsoft.tankfactory.miragetank.MirageTankMakerScreen
import com.rbtsoft.tankfactory.miragetank.MirageTankViewerScreen
import com.rbtsoft.tankfactory.ui.about.AboutDialog
import com.rbtsoft.tankfactory.ui.theme.NeonText
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme
import androidx.compose.ui.unit.sp
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.templates.CyberScaffold

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
        composable("view_LSBTank") { LSBTankViewerScreen() }
    }
}

@Composable
fun MainMenuScreen(navController: NavController) {
    var showAboutDialog by remember { mutableStateOf(false) }
    CyberScaffold(useSafeArea = true) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NeonText(
                text = "TANK FACTORY",
                neonColor = CyberTheme.colors.primary,
                style = CyberTheme.typography.h1.copy(fontSize = 48.sp),

            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CyberButton(
                    text = stringResource(id = R.string.view_mirage_tank),
                    onClick = { navController.navigate("view_Mtank") },
                    modifier = Modifier.weight(1f)
                )
                CyberButton(
                    text = stringResource(id = R.string.make_mirage_tank),
                    onClick = { navController.navigate("make_Mtank") },
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CyberButton(
                    text = stringResource(id = R.string.view_lsb_tank),
                    onClick = { navController.navigate("view_LSBTank") },
                    modifier = Modifier.weight(1f)
                )
                CyberButton(
                    text = stringResource(id = R.string.make_lsb_tank),
                    onClick = { navController.navigate("make_LSBtank") },
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            CyberButton(
                text = stringResource(id = R.string.about),
                onClick = { showAboutDialog = true },
                isPrimary = false,
                modifier = Modifier.width(120.dp)
            )
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}