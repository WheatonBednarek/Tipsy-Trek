package com.cs407.tipsytrek.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.cs407.tipsytrek.Beverage
import com.cs407.tipsytrek.data.DrinkLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Person
import com.cs407.tipsytrek.User


// lowk cursed kotlin allows these to have the same identifier
val HomePageId = "Home"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController,
             user: User,
             onCollectDrink: (Beverage) -> Unit) {
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
        Text("TODO MAP", Modifier.padding(innerPadding))
        Column {
            for (drink in drinks) {
                Text(drink.toString())
            }
        }
    }

}