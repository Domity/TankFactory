package com.rbtsoft.tankfactory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.domity.cybertheme.foundation.CyberTheme
import com.domity.cybertheme.molecules.CyberButton
import com.domity.cybertheme.molecules.NeonText
import com.domity.cybertheme.templates.CyberScaffold
import com.rbtsoft.tankfactory.lsbtank.LSBTankMakerScreen
import com.rbtsoft.tankfactory.lsbtank.LSBTankViewerScreen
import com.rbtsoft.tankfactory.miragetank.MirageTankMakerScreen
import com.rbtsoft.tankfactory.miragetank.MirageTankViewerScreen
import com.rbtsoft.tankfactory.ui.about.AboutDialog
import com.rbtsoft.tankfactory.ui.theme.TankFactoryTheme

object ScreenRoutes {
    const val MAIN_MENU = "main_menu"
    const val VIEW_MIRAGE = "view_Mtank"
    const val MAKE_MIRAGE = "make_Mtank"
    const val VIEW_LSB = "view_LSBTank"
    const val MAKE_LSB = "make_LSBtank"
}

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

    NavHost(navController = navController, startDestination = ScreenRoutes.MAIN_MENU) {
        composable(ScreenRoutes.MAIN_MENU) {
            MainMenuScreen(
                onNavigate = { route ->
                    navController.navigateSingleTopTo(route)
                }
            )
        }
        composable(ScreenRoutes.VIEW_MIRAGE) { MirageTankViewerScreen() }
        composable(ScreenRoutes.MAKE_MIRAGE) { MirageTankMakerScreen() }
        composable(ScreenRoutes.MAKE_LSB) { LSBTankMakerScreen() }
        composable(ScreenRoutes.VIEW_LSB) { LSBTankViewerScreen() }
    }
}

fun NavController.navigateSingleTopTo(route: String) {
    this.navigate(route) {
        launchSingleTop = true
    }
}

@Composable
fun MainMenuScreen(
    onNavigate: (String) -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    CyberScaffold(useSafeArea = true) {
        Box(modifier = Modifier.fillMaxSize()) {

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
                    style = CyberTheme.typography.headline.copy(fontSize = 48.sp),
                )

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberButton(
                        text = stringResource(id = R.string.view_mirage_tank),
                        onClick = { onNavigate(ScreenRoutes.VIEW_MIRAGE) },
                        modifier = Modifier.weight(1f)
                    )
                    CyberButton(
                        text = stringResource(id = R.string.make_mirage_tank),
                        onClick = { onNavigate(ScreenRoutes.MAKE_MIRAGE) },
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
                        onClick = { onNavigate(ScreenRoutes.VIEW_LSB) },
                        modifier = Modifier.weight(1f)
                    )
                    CyberButton(
                        text = stringResource(id = R.string.make_lsb_tank),
                        onClick = { onNavigate(ScreenRoutes.MAKE_LSB) },
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
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
