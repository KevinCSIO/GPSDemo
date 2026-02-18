package fr.caensup.gpsdemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationViewModel(
    private val repo: FusedLocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    fun onPermission(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted, message = null) }
        if (granted) refreshOnce()
    }

    fun refreshOnce() = viewModelScope.launch {
        val loc = repo.getLastLocationOrNull()
        _state.update {
            it.copy(
                latitude = loc?.latitude,
                longitude = loc?.longitude,
                accuracyMeters = loc?.accuracy,
                provider = loc?.provider,
                message = if (loc == null) "Aucune position disponible (encore) : réessayez après quelques secondes." else null
            )
        }
    }
}