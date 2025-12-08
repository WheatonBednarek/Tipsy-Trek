package com.cs407.tipsytrek

import android.os.Bundle
import android.util.Log
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
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.app
import com.google.firebase.ktx.app


// 🔹 IMPORTANT: import Beverage

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // 🔍 Log the Firebase project ID at runtime
        Log.d("FIREBASE", "Runtime projectId = ${Firebase.app.options.projectId}")

        val testDb = Firebase.firestore
        testDb.collection("debug")
            .document("testWrite")
            .set(mapOf("timestamp" to System.currentTimeMillis()))
            .addOnSuccessListener {
                Log.d("FIRESTORE", "Debug write SUCCESS")
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "Debug write FAILED: ${e.localizedMessage}", e)
            }
        setContent {
            val navController = rememberNavController()

            val auth = remember { FirebaseAuth.getInstance() }
            var firebaseUser by remember { mutableStateOf(auth.currentUser) }
            val db = remember { Firebase.firestore }

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

            // Helper: whenever we change the current User, also save it in Firestore
            fun updateUser(newUser: User) {
                user = newUser
                val uid = firebaseUser?.uid
                if (uid != null) {

                    Log.d("FIRESTORE", "Attempting to write user $uid with drinks=${newUser.allTimeDrinks.size}")

                    db.collection("users")
                        .document(uid)
                        .set(newUser)
                        .addOnSuccessListener {
                            Log.d("FIRESTORE", "SUCCESS writing user $uid")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FIRESTORE", "FAILED writing user $uid: ${e.localizedMessage}", e)
                        }
                }
            }






            TipsyTrekTheme {
                NavHost(navController, startDestination = startDestination) {

                    composable(LoginPageId) {
                        LoginPage { signedInUser ->
                            firebaseUser = signedInUser
                            val uid = signedInUser.uid

                            db.collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val storedUser = snapshot.toObject(User::class.java)

                                    val newUser = if (storedUser != null) {
                                        // 🔹 Use the stored user (with allTimeDrinks, etc.)
                                        storedUser
                                    } else {
                                        // 🔹 First login for this UID: create and save a new User
                                        val created = firebaseUserToUser(signedInUser)
                                        db.collection("users")
                                            .document(uid)
                                            .set(created)
                                        created
                                    }

                                    updateUser(newUser)   // updates state + Firestore cache

                                    navController.navigate(HomePageId) {
                                        popUpTo(LoginPageId) { inclusive = true }
                                    }
                                }
                                .addOnFailureListener {
                                    // Fallback if Firestore read fails: at least use a fresh User
                                    val fallback = firebaseUserToUser(signedInUser)
                                    updateUser(fallback)

                                    navController.navigate(HomePageId) {
                                        popUpTo(LoginPageId) { inclusive = true }
                                    }
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

                                navController.navigate(LoginPageId) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onUserUpdated = { updatedUser ->
                                updateUser(updatedUser)   // reuse your helper so everything stays in sync
                            }
                        )
                    }


                    // BAR PAGE
                    composable("bar/{barId}") { backStackEntry ->
                        val barId = backStackEntry.arguments?.getString("barId")!!
                        BarPage(
                            navController = navController,
                            barId = barId,
                            user = user,
                            onUserUpdated = { updated -> updateUser(updated) }
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

