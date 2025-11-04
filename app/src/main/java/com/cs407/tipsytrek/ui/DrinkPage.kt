package com.cs407.tipsytrek.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.cs407.tipsytrek.Beverage
import kotlinx.serialization.Serializable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkPage(beverage: Beverage, navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BAC: 0.??%") }
            )
        }
    ) { innerPadding ->
        Canvas(Modifier.fillMaxSize().padding(innerPadding)) {
            drawRect(Color(beverage.color))
        }
    }
}