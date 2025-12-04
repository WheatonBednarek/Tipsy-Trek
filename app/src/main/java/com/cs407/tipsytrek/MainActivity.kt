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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.cs407.tipsytrek.ui.LoginPage
import com.cs407.tipsytrek.ui.LoginPageId
import com.cs407.tipsytrek.ui.BarPage


// 🔹 IMPORTANT: import Beverage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        System.loadLibrary("liquidfun")
        System.loadLibrary("liquidfun_jni")

        setContent {
            val navController = rememberNavController()

            val auth = remember { FirebaseAuth.getInstance() }
            var firebaseUser by remember { mutableStateOf(auth.currentUser) }

            // original list used for SelectionScreen
            var beverageCollection by remember { mutableStateOf(listOf<Beverage>()) }

            // new User state for BAC + counts
            var user by remember {
                mutableStateOf(
                    firebaseUser?.let { firebaseUserToUser(it) } ?: User()
                )
            }

            var usersByUid by remember { mutableStateOf<Map<String, User>>(emptyMap()) }


            val context = LocalContext.current

            LaunchedEffect(rememberCoroutineScope()) {
                // for demo include one random drink
                beverageCollection += DrinkLocationManager.possibleBevs.random()
            }

            val startDestination = LoginPageId

            // Helper: whenever we change the current User, also save it in usersByUid
            fun updateUser(newUser: User) {
                user = newUser
                val uid = firebaseUser?.uid
                if (uid != null) {
                    usersByUid = usersByUid.toMutableMap().apply {
                        put(uid, newUser)
                    }
                }
            }


            TipsyTrekTheme {
                NavHost(navController, startDestination = startDestination) {

                    composable(LoginPageId) {
                        LoginPage { signedInUser ->
                            firebaseUser = signedInUser

                            val uid = signedInUser.uid
                            val existingUser = usersByUid[uid]

                            // If we've seen this uid before in this app run, reuse their User
                            val newUser = existingUser ?: firebaseUserToUser(signedInUser)
                            user = newUser

                            // Make sure map is updated
                            usersByUid = usersByUid.toMutableMap().apply {
                                put(uid, newUser)
                            }

                            navController.navigate(HomePageId) {
                                popUpTo(LoginPageId) { inclusive = true }
                            }
                        }
                    }


                    // HOME
                    composable(HomePageId) {
                        HomePage(
                            navController = navController,
                            user = user,
                            onCollectDrink = { beverage ->
                                // old behavior: still add to beverageCollection
                                beverageCollection += beverage

                                // new behavior: track in User as well
                                updateUser(user.addDrink(beverage))


                                Toast.makeText(
                                    context,
                                    beverage.name + " collected!",
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
                                updateUser(user.resetCurrentDrinks())

                            },
                            onLogout = {
                                auth.signOut()
                                firebaseUser = null
                                user = User()  // back to placeholder

                                navController.navigate(LoginPageId) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // BAR PAGE
                    composable("bar/{barId}") { backStackEntry ->
                        val barId = backStackEntry.arguments?.getString("barId")!!
                        BarPage(navController, barId)
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
                                beverageCollection -= drink

                                // and user stats
                                updateUser(user.addDrink(drink))

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
    private fun firebaseUserToUser(firebaseUser: FirebaseUser): User {
        val email = firebaseUser.email ?: "unknown@example.com"
        val baseName = email.substringBefore("@")
        val displayName = firebaseUser.displayName ?: baseName
        val username = "@$baseName"

        return User(
            uid = firebaseUser.uid,
            email = email,
            displayName = displayName,
            username = username
        )
    }

}

