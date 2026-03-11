package fr.caensup.gpsdemo

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
            GPSDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        LocationOnceScreen(
                            vm = LocationViewModel(
                                FusedLocationRepository(this@MainActivity)
                            )
                        )
                    }
                }
            }
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
        points = Polygon.pointsAsCircle(center, radiusMeters)
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
    snippet: String,
    isDrawingEnabled: Boolean,
    onDistanceChanged: (Double) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val controller = remember {
        // Le on distanceChanged crée dans la fonction LocationOnceScreen est passé à la fonction OsmMapBasic
        // est passé de nouveau en paramètre de la fonction TapToPolylineController
        TapToPolylineController(context, mapView, onDistanceChanged)
    }

    LaunchedEffect(isDrawingEnabled) {
        if (isDrawingEnabled) {
            controller.enable()
        } else {
            controller.disable()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                getController().setZoom(zoom)
                getController().setCenter(GeoPoint(centerLat, centerLng))
                addMarker(this, centerLat, centerLng, title, snippet)
                addCircle(this, centerLat, centerLng, 50.0)
            }
        },
        update = { map ->
            map.controller.setZoom(zoom)
            map.controller.setCenter(GeoPoint(centerLat, centerLng))
        }
    )
}


@Composable
fun LocationOnceScreen(vm: LocationViewModel) {
    val state by vm.state.collectAsState()

    // modeDessin indique si on est en mode dessin ou non pour dessiner sur la carte
    // modeDessin est un état mutable qui est initialisé à false
    // modeDessin est déclaré avec by remember qui permet de garder l'état même après la recomposition
    var modeDessin by remember { mutableStateOf(false) }

    // idem pour totalDistance
    var totalDistance by remember { mutableStateOf(0.0) }

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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = vm::refreshOnce) { Text("Rafraîchir") }
            Spacer(Modifier.padding(8.dp))
            Switch(
                checked = modeDessin,
                onCheckedChange = { modeDessin = it }
            )
            Spacer(Modifier.padding(4.dp))
            Text("Tracer")
        }
        
        Text("Distance : %.2f m".format(totalDistance))

        Spacer(Modifier.height(12.dp))

        if (state.latitude != null && state.longitude != null) {
            OsmMapBasic(
                centerLat = state.latitude!!,
                centerLng = state.longitude!!,
                zoom = 20.0,
                title = "CAENSUP!",
                snippet = "Fabrique à Techos",
                isDrawingEnabled = modeDessin,
                // TotalDistance qui est un reùmember mutableStateOf est passé en paramètre de la fonction OsmMapBasic
                onDistanceChanged = { totalDistance = it }
            )
        }
    }
}
