package fr.caensup.gpsdemo

import android.content.Context
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

class TapToPolylineController(
    private val context: Context,
    private val mapView: MapView,
    private val onDistanceChanged: (Double) -> Unit = {}
) {

    val points: MutableList<GeoPoint> = mutableListOf()

    private val polyline: Polyline = Polyline(mapView).apply {
        outlinePaint.color = android.graphics.Color.RED
        outlinePaint.strokeWidth = 8f
    }

    private val tapOverlay = object : Overlay() {
        override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
            val p = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
            points.add(p)

            // Mettre à jour la polyline
            polyline.setPoints(points)
            if (!mapView.overlays.contains(polyline)) {
                mapView.overlays.add(polyline)
            }

            mapView.invalidate()
            onDistanceChanged(totalDistanceMeters())
            return true // on consomme l'évènement
        }
    }

    fun enable() {
        if (!mapView.overlays.contains(tapOverlay)) {
            // Mettre l'overlay en tête pour capter les taps avant d'autres overlays
            mapView.overlays.add(0, tapOverlay)
        }
    }

    fun disable() {
        clear()
        mapView.overlays.remove(tapOverlay)
    }

    fun clear() {
        points.clear()
        polyline.setPoints(points)
        mapView.invalidate()
        onDistanceChanged(0.0)
    }

    /**
     * Distance totale de la polyline (en mètres)
     */
    fun totalDistanceMeters(): Double {
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += points[i].distanceToAsDouble(points[i + 1])
        }
        return total
    }
}
