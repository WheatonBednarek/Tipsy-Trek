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
import kotlin.math.*
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.material3.ModalBottomSheetDefaults.properties
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.MapProperties

// (Maps integrated in Home Screen and Bar locations added)

// lowk cursed kotlin allows these to have the same identifier
val HomePageId = "Home"

fun distanceMeters(a: LatLng, b: LatLng): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)

    val h = sin(dLat / 2).pow(2) + sin(dLng / 2).pow(2) * cos(lat1) * cos(lat2)
    return 2 * R * asin(sqrt(h))
}

val drinkLocationManager = DrinkLocationManager()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController, user: User, mapViewModel: MapViewModel = viewModel(), onCollectDrink: (Beverage) -> Unit) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    var hasLocationPermission by remember { mutableStateOf(false) }
    var nearbyBar by remember { mutableStateOf<Bar?>(null) }
    mapViewModel.initializeLocationClient(context)


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
            uiState.currentLocation ?: LatLng(43.0731, -89.4012),
            15f
        )
    }

    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { loc ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(loc, 15f)
        }
    }

    val drinks by drinkLocationManager.drinksFlow.collectAsState()
    // launch a thread to tick the drink manager every second to have it check for changes
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(coroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            while(true) {
                val myLat = uiState.currentLocation?.latitude ?: 0.0
                val myLong = uiState.currentLocation?.longitude ?: 0.0
                drinkLocationManager.tick(myLat, myLong)
                val collectedDrinks = drinkLocationManager.collectNearbyDrinks(myLat, myLong)
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
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true
            ),
        ) {
            MadisonBars.forEach { bar ->
                Marker(
                    state = MarkerState(position = LatLng(bar.latitude, bar.longitude)),
                    title = bar.name
                )
            }

            uiState.currentLocation?.let { userLoc ->
                MadisonBars.forEach { bar ->
                    val closest = MadisonBars.minByOrNull { bar ->
                        distanceMeters(userLoc, LatLng(bar.latitude, bar.longitude))
                    }

                    val closestDist = closest?.let {
                        distanceMeters(userLoc, LatLng(it.latitude, it.longitude))
                    } ?: Double.MAX_VALUE

                    nearbyBar = if (closestDist < 30) closest else null
                }
                drinks.forEach {
                    Marker(
                        state = MarkerState(position = LatLng(it.lat, it.long)),
                        icon = createColoredCircle(it.beverage.color.toInt(), 64.dp.value.toInt())
                    )
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
        nearbyBar?.let { bar ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "You're near ${bar.name}!",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { navController.navigate("bar/${bar.name}") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enter ${bar.name}")
                        }
                    }
                }
            }
            }
        }
    }
}

fun createColoredCircle(color: Int, size: Int = 64): BitmapDescriptor {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }

    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
