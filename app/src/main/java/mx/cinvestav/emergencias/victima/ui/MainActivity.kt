package mx.cinvestav.emergencias.victima.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cinvestav.emergencias.victima.App
import mx.cinvestav.emergencias.victima.ui.alerta.AlertaScreen
import mx.cinvestav.emergencias.victima.ui.alerta.AlertaViewModel
import mx.cinvestav.emergencias.victima.ui.salud.SaludScreen
import mx.cinvestav.emergencias.victima.ui.salud.SaludViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container

        setContent {
            MaterialTheme {
                val permisoLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permisoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val alertaVm: AlertaViewModel =
                    viewModel(factory = AlertaViewModel.Factory(container))
                val saludVm: SaludViewModel =
                    viewModel(factory = SaludViewModel.Factory(container))

                var pestanaSeleccionada by remember { mutableIntStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = pestanaSeleccionada == 0,
                                onClick = { pestanaSeleccionada = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                label = { Text("Inicio") }
                            )
                            NavigationBarItem(
                                selected = pestanaSeleccionada == 1,
                                onClick = { pestanaSeleccionada = 1 },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = "Salud") },
                                label = { Text("Mi Salud") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (pestanaSeleccionada) {
                        0 -> AlertaScreen(
                            viewModel = alertaVm,
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> SaludScreen(
                            viewModel = saludVm,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
