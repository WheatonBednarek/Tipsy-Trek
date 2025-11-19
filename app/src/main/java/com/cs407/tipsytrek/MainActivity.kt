package com.cs407.tipsytrek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cs407.tipsytrek.data.DrinkLocationManager
import com.cs407.tipsytrek.ui.DrinkPage
import com.cs407.tipsytrek.ui.HomePage
import com.cs407.tipsytrek.ui.HomePageId
import com.cs407.tipsytrek.ui.SelectionScreen
import com.cs407.tipsytrek.ui.SelectionScreenId
import com.cs407.tipsytrek.ui.theme.TipsyTrekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var beverageCollection by remember { mutableStateOf(listOf<Beverage>()) }
            // debug code to list out each beverage
            LaunchedEffect(rememberCoroutineScope()) {
                beverageCollection += DrinkLocationManager.possibleBevs
            }
            TipsyTrekTheme {
                NavHost(navController, startDestination = HomePageId) {
                    composable(HomePageId) { HomePage(navController, {
                        beverageCollection += it
                    }) }
                    composable(SelectionScreenId) { SelectionScreen(navController, beverageCollection) }
                    composable<Beverage> { backStack ->
                        DrinkPage(backStack.toRoute(), navController)
                    }
                }
            }
        }
    }
}