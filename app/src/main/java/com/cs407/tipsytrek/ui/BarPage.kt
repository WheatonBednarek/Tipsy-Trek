package com.cs407.tipsytrek.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cs407.tipsytrek.MadisonBars
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarPage(
    navController: NavController,
    barName: String?
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val bar = MadisonBars.firstOrNull { it.name == barName }

    if (bar == null || userId == null) {
        Text("Bar not found.")
        return
    }

    LaunchedEffect(barName) {
        recordBarVisit(userId, bar.name)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome to ${bar.name}") }
            )
        },
        containerColor = Color(bar.themeColor)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "You're inside ${bar.name}!",
                fontSize = 26.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Time for a drink!",
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ORDER DRINK BUTTON
            Button(
                onClick = { navController.navigate(SelectionScreenId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Order a Drink")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LEAVE BAR BUTTON
            Button(
                onClick = { navController.navigate(HomePageId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Leave ${bar.name}")
            }
        }
    }
}

fun recordBarVisit(userId: String, barName: String) {
    val visitData = mapOf(
        "bar" to barName,
        "timestamp" to System.currentTimeMillis()
    )

    Firebase.firestore
        .collection("users")
        .document(userId)
        .collection("barVisits")
        .add(visitData)
        .addOnSuccessListener {
            Log.d("BarVisit", "Visit recorded successfully")
        }
        .addOnFailureListener { e ->
            Log.e("BarVisit", "Error recording visit", e)
        }
}