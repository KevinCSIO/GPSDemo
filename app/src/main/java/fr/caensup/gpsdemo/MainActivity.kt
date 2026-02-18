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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import fr.caensup.gpsdemo.ui.theme.GPSDemoTheme
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

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



fun addCircle(
    mapView: MapView,
    latitude: Double,
    longitude: Double,
    radiusMeters: Double
): Polygon {
    val center = GeoPoint(latitude, longitude)

    val circle = Polygon().apply {
        // Génère une liste de points qui approximent un cercle (géodésique simple)
        points = Polygon.pointsAsCircle(center, radiusMeters)

        // Style
        outlinePaint.color = android.graphics.Color.RED
        outlinePaint.strokeWidth = 2f

        title = "Zone d'intérêt"
    }

    mapView.overlays.add(circle)
    mapView.invalidate()
    return circle
}



fun addMarker(map: MapView, lat: Double, lng: Double, title: String, snippet: String) {
    val marker = Marker(map).apply {
        position = GeoPoint(lat, lng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title = title
        this.snippet = snippet
    }
    map.overlays.add(marker)
    map.invalidate()
}


@Composable
fun OsmMapBasic(
    modifier: Modifier = Modifier.fillMaxSize(),
    centerLat: Double,
    centerLng: Double,
    zoom: Double = 15.0,
    title: String,
    snippet: String
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK) // tuiles OSM "classiques"
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(centerLat, centerLng))
                addMarker( this, centerLat, centerLng, title, snippet)
                addCircle(  this, centerLat, centerLng, 50.0)
                TapToPolylineController(context, this).enable()
            }
        },
        update = { map ->
            // Recentrage si paramètres changent
            map.controller.setZoom(zoom)
            map.controller.setCenter(GeoPoint(centerLat, centerLng))
            addMarker( map, centerLat, centerLng, title, snippet)
            addCircle(  map, centerLat, centerLng, 50.0)

        }
    )
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
        Spacer(Modifier.height(12.dp))
        // Visualiser une carte OSM avec la position actuelle
        if (state.latitude != null && state.longitude != null) {
            OsmMapBasic( centerLat = state.latitude!!, centerLng = state.longitude!!, zoom = 20.0, title = "CAENSUP!", snippet = "Fabrique à Techos")
        }


    }
}