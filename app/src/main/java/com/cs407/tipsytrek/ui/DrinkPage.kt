package com.cs407.tipsytrek.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.cs407.tipsytrek.Beverage
import com.cs407.tipsytrek.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkPage(
    beverage: Beverage,
    navController: NavController,
    user: User,
    onDrinkConsumed: (Beverage) -> Unit
) {
    // Called once when this composable first appears
    LaunchedEffect(Unit) {
        onDrinkConsumed(beverage)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BAC: ${user.formattedBac}") }
            )
        }
    ) { innerPadding ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            drawRect(Color(beverage.color))
        }
    }
}




