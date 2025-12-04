package com.cs407.tipsytrek.ui

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MapState(
    val markers: List<LatLng> = emptyList(),
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapState())
    val uiState = _uiState.asStateFlow()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun initializeLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        viewModelScope.launch {

            if (!_uiState.value.locationPermissionGranted) {
                _uiState.value = _uiState.value.copy(
                    error = "Location permission not granted."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            _uiState.value = _uiState.value.copy(
                                currentLocation = latLng,
                                isLoading = false
                            )
                            getCurrentLocation()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                error = "Unable to get location.",
                                isLoading = false
                            )
                        }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Location error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}