package com.cs407.tipsytrek.ui

import androidx.compose.foundation.layout.Column
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cs407.tipsytrek.Beverage
import com.cs407.tipsytrek.data.DrinkLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Person
import com.cs407.tipsytrek.User
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.serialization.Serializable
import com.cs407.tipsytrek.MadisonBars
import com.cs407.tipsytrek.Bar
import com.google.android.gms.location.LocationServices
import android.Manifest
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
// (Maps integrated in Home Screen and Bar locations added)

// lowk cursed kotlin allows these to have the same identifier
val HomePageId = "Home"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController, user: User, mapViewModel: MapViewModel = viewModel(), onCollectDrink: (Beverage) -> Unit) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        mapViewModel.updateLocationPermission(granted)

        if (granted) {
            mapViewModel.getCurrentLocation()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(43.0731, -89.4012),
            15f
        )
    }

    val drinkLocationManager by remember { mutableStateOf(DrinkLocationManager()) }
    val drinks by drinkLocationManager.drinksFlow.collectAsState()
    // launch a thread to tick the drink manager every second to have it check for changes
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(coroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            while(true) {
                drinkLocationManager.tick(0.0, 0.0)
                val collectedDrinks = drinkLocationManager.collectNearbyDrinks(0.0, 0.0)
                collectedDrinks.forEach(onCollectDrink)
                delay(1000)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BAC: ${user.formattedBac}") },
                actions = {
                    // Button to go to User page
                    IconButton(onClick = { navController.navigate(UserPageId) }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User Page"
                        )
                    }
                    // Existing button to go to beverage selection
                    IconButton(onClick = { navController.navigate(SelectionScreenId) }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Beverage Selection"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            cameraPositionState = cameraPositionState
        ) {
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(location),
                    title = "My Location"
                )
            }
            MadisonBars.forEach { bar ->
                Marker(
                    state = MarkerState(position = LatLng(bar.latitude, bar.longitude)),
                    title = bar.name
                )
            }
        }
        Column {
            for (drink in drinks) {
                Text(drink.toString())
            }
        }
    }

}