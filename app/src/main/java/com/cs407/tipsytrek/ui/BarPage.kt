package com.cs407.tipsytrek.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cs407.tipsytrek.MadisonBars
import com.cs407.tipsytrek.data.Achievements
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarPage(
    navController: NavController,
    barName: String?
) {
    val bar = MadisonBars.firstOrNull { it.name == barName }
    val context = LocalContext.current
    var barVisitCount by remember { mutableStateOf(0) }
    var unlockedAchievement by remember { mutableStateOf<String?>(null) }

    if (bar == null) {
        Text("Bar not found.")
        return
    }

    LaunchedEffect(barName) {
        Achievements.recordBarVisit { achievement ->
            unlockedAchievement = achievement
        }
    }

    unlockedAchievement?.let { name ->
        AlertDialog(
            onDismissRequest = { unlockedAchievement = null },
            title = { Text("Achievement Unlocked!") },
            text = { Text(name) },
            confirmButton = {
                Button(onClick = { unlockedAchievement = null }) {
                    Text("Awesome!")
                }
            }
        )
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Time for a drink!",
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { navController.navigate(SelectionScreenId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Order a Drink")
            }

            Spacer(modifier = Modifier.height(16.dp))

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