package uragent.app.Midi

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uragent.app.Midi.ui.FavoriteScreen
import uragent.app.Midi.ui.HomeScreen
import uragent.app.Midi.ui.PlayerScreen
import uragent.app.Midi.ui.PlaylistScreen
import uragent.app.Midi.ui.SearchScreen
import uragent.app.Midi.ui.SettingsScreen
import uragent.app.Midi.ui.theme.MidiPlayerTheme
import uragent.app.Midi.utils.BluetoothHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Bluetooth permissions based on Android version
        requestBluetoothPermissions()

        setContent {
            MidiPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        // Request the necessary Bluetooth permissions
        val requiredPermissions = BluetoothHelper.getRequiredBluetoothPermissions()
        ActivityCompat.requestPermissions(this, requiredPermissions, BLUETOOTH_PERMISSION_REQUEST_CODE)
    }

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 100
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: BluetoothViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPlayer = { navController.navigate("player") },
                onNavigateToPlaylist = { navController.navigate("playlist") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("player") {
            PlayerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable("playlist") {
            PlaylistScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable("favorites") {
            FavoriteScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}



