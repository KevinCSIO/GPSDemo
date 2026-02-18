package fr.caensup.gpsdemo

data class LocationUiState(
    val permissionGranted: Boolean = false,
    val isTracking: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val provider: String? = null,
    val message: String? = null
)