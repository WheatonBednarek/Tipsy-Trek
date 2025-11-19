package com.cs407.tipsytrek

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import com.cs407.tipsytrek.ui.UserPage
import com.cs407.tipsytrek.ui.UserPageId
import com.cs407.tipsytrek.ui.theme.TipsyTrekTheme

// 🔹 IMPORTANT: import Beverage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            // original list used for SelectionScreen
            var beverageCollection by remember { mutableStateOf(listOf<Beverage>()) }

            // new User state for BAC + counts
            var user by remember { mutableStateOf(User()) }

            val context = LocalContext.current

            // keep your original debug init of beverages
            LaunchedEffect(rememberCoroutineScope()) {
                beverageCollection += DrinkLocationManager.possibleBevs
            }

            TipsyTrekTheme {
                NavHost(navController, startDestination = HomePageId) {

                    // HOME
                    composable(HomePageId) {
                        HomePage(
                            navController = navController,
                            user = user,
                            onCollectDrink = { beverage ->
                                // old behavior: still add to beverageCollection
                                beverageCollection += beverage

                                // new behavior: track in User as well
                                user = user.addDrink(beverage)

                                Toast.makeText(
                                    context,
                                    "Drink consumed!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    // SELECTION SCREEN (unchanged from your original logic)
                    composable(SelectionScreenId) {
                        SelectionScreen(navController, beverageCollection, user = user)
                    }

                    // USER PAGE
                    composable(UserPageId) {
                        UserPage(
                            navController = navController,
                            user = user,
                            onResetCurrent = {
                                user = user.resetCurrentDrinks()
                            }
                        )
                    }

                    // DRINK DETAIL
                    composable<Beverage> { backStack ->
                        val beverage = backStack.toRoute<Beverage>()

                        DrinkPage(
                            beverage = beverage,
                            navController = navController,
                            user = user,                         // 🔹 pass user in
                            onDrinkConsumed = { drink ->
                                // increment main drink list
                                beverageCollection += drink

                                // and user stats
                                user = user.addDrink(drink)

                                Toast.makeText(
                                    context,
                                    "Drink consumed!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

