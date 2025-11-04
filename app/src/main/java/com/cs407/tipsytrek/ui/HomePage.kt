package com.cs407.tipsytrek.ui

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import kotlinx.serialization.Serializable

// lowk cursed kotlin allows these to have the same identifier
val HomePageId = "Home"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BAC: 0.??%") },
                actions = {
                    IconButton(onClick = { navController.navigate(SelectionScreenId) }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Beverage Selection"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Text("TODO MAP", Modifier.padding(innerPadding))
    }
}