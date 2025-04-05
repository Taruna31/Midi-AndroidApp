package uragent.app.midiplayer

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uragent.app.midiplayer.ui.*
import uragent.app.midiplayer.ui.theme.MidiPlayerTheme
import uragent.app.midiplayer.utils.BluetoothHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.app.Activity
import androidx.activity.compose.LocalActivity
import uragent.app.midiplayer.ui.CollectionScreen
import uragent.app.midiplayer.ui.PianoScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions()

        setContent {
            MidiPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val requiredPermissions = BluetoothHelper.getRequiredBluetoothPermissions()
        ActivityCompat.requestPermissions(this, requiredPermissions, BLUETOOTH_PERMISSION_REQUEST_CODE)
    }

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 100
    }
}

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object Home : Screen(
        route = "home",
        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
        label = "Home"
    )
    object Search : Screen(
        route = "search",
        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        label = "Search"
    )
    object Library : Screen(
        route = "library",
        icon = { Icon(Icons.Filled.ArrowBack, contentDescription = "Your Library") },
        label = "Koleksi"
    )
    object Piano : Screen(
        route = "piano",
        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Piano") },
        label = "Pianika"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Search, Screen.Library, Screen.Piano)
    val activity = LocalActivity.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            // Lock orientation to landscape for Piano screen
                            if (screen == Screen.Piano) {
                                if (activity != null) {
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            } else {
                                if (activity != null) {
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                            
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                PlayerScreen(
                    viewModel = viewModel(),
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable(Screen.Search.route) { 
                SearchScreen(viewModel = viewModel())
            }
            composable(Screen.Library.route) { 
                CollectionScreen(viewModel = viewModel())
            }
            composable(Screen.Piano.route) { 
                PianoScreen(viewModel = viewModel())
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel(),
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}



