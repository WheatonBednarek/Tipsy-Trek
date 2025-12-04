package com.cs407.tipsytrek.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs407.tipsytrek.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

val UserPageId = "User"

fun getBarVisitCount(userId: String, onResult: (Int) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(userId)
        .collection("barVisits")
        .get()
        .addOnSuccessListener { query ->
            onResult(query.size())
        }
        .addOnFailureListener {
            onResult(0)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPage(navController: NavController,
             user: User,
             onResetCurrent: () -> Unit,
             onLogout: () -> Unit
) {
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val userId = firebaseUser?.uid
    var barCount by remember { mutableStateOf(0) }

    LaunchedEffect(userId) {
        userId?.let {
            getBarVisitCount(it) { count ->
                barCount = count
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BAC: ${user.formattedBac}") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Display name: ${user.displayName}")
                Text("Email: ${user.email ?: "Unknown"}")

                // Text("Username: ${user.username}")

                Spacer(Modifier.padding(top = 16.dp))

                Text("Current drinks: ${user.currentDrinks.size}")
                Text("All-time drinks: ${user.allTimeDrinks.size}")

                Spacer(Modifier.padding(top = 16.dp))

                Text("Bars visited: $barCount")
            }

            Column {
                Button(
                    onClick = onResetCurrent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset drink count")
                }

                Spacer(Modifier.padding(top = 8.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log out")
                }

                Spacer(Modifier.padding(top = 8.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}



