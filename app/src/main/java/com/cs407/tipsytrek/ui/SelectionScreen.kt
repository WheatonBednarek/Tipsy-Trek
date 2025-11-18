package com.cs407.tipsytrek.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cs407.tipsytrek.Beverage
import com.cs407.tipsytrek.data.DrinkManager

val SelectionScreenId = "selection"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BAC: 0.??%") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(HomePageId) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Home Page"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val bevs = DrinkManager.possibleBevs
        Column(Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
        ) {
            bevs.forEach {
                SelectionRow(it, navController)
            }
        }
    }
}

@Composable
fun SelectionRow(beverage: Beverage, navController: NavController) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(
                onClick = {
                    navController.navigate(beverage)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.size(80.dp)) {
            drawCircle(Color(beverage.color))
        }
        Spacer(Modifier.width(8.dp))
        Text(beverage.name, fontSize = 24.sp)
    }
}

@Preview
@Composable
fun SelectionPreview() {
    SelectionScreen(rememberNavController())
}