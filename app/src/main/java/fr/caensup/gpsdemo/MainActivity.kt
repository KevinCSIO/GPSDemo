package fr.caensup.gpsdemo

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.caensup.gpsdemo.ui.theme.GPSDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
             LocationOnceScreen(
                 vm = LocationViewModel(
                     FusedLocationRepository( this)) )
        }
    }
}


@Composable
fun LocationPermissionGate(
    onPermissionResult: (granted: Boolean) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> onPermissionResult(granted) }

    Column(Modifier.padding(16.dp)) {
        Text("Localisation : permission requise")
        Spacer(Modifier.height(8.dp))
        Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
            Text("Autoriser la localisation")
        }
        Text(
            "Astuce : vous pouvez aussi demander COARSE si la précision fine n'est pas indispensable.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}


@Composable
fun LocationOnceScreen(vm: LocationViewModel) {
    val state by vm.state.collectAsState()

    if (!state.permissionGranted) {
        LocationPermissionGate(onPermissionResult = vm::onPermission)
        return
    }

    Column(Modifier.padding(16.dp)) {
        Text("Dernière position connue")
        Spacer(Modifier.height(8.dp))

        Text("Lat: ${state.latitude ?: "-"}")
        Text("Lng: ${state.longitude ?: "-"}")
        Text("Accuracy: ${state.accuracyMeters?.let { "$it m" } ?: "-"}")
        Text("Provider: ${state.provider ?: "-"}")

        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFFFCC66))
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = vm::refreshOnce) { Text("Rafraîchir") }
    }
}