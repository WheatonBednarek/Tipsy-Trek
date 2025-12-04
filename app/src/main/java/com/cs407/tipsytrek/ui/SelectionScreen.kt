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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cs407.tipsytrek.Beverage
import com.cs407.tipsytrek.data.DrinkLocationManager
import com.cs407.tipsytrek.User
import kotlin.math.sqrt
import kotlin.random.Random


val SelectionScreenId = "selection"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(navController: NavController, bevs: List<Beverage>, user: User) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BAC: ${user.formattedBac}") },
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
        Column(Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
        ) {
            bevs.forEach {
                SelectionRow(it, navController)
            }
            if(bevs.size == 0) {
                Text(
                    text = "Go collect beverages!",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
            val color = Color(beverage.color)
            ColorSpaces.Srgb
            val darkFactor = .9f
            drawCircle(Color(
                (color.red * darkFactor),
                (color.green * darkFactor),
                (color.blue * darkFactor)
            ))
            val radius = (size.width / 2)*.95f
            drawCircle(color, radius)
            val random = Random(beverage.color.hashCode())
            val bubbleCount = 3
            val bubbleRadius = radius / 4f
            val maxDist = radius * 0.75f
            val maxAttempts = 20

            val offsets = mutableListOf<Offset>()

            repeat(bubbleCount) {
                var placed = false

                for (attempt in 0 until maxAttempts) {
                    val angle = random.nextFloat() * (2f * Math.PI).toFloat()
                    val dist = sqrt(random.nextFloat()) * maxDist

                    val candidate = Offset(
                        x = kotlin.math.cos(angle) * dist,
                        y = kotlin.math.sin(angle) * dist
                    )

                    // Check overlap with existing bubbles
                    val overlaps = offsets.any { existing ->
                        candidate.minus(existing).getDistance() < bubbleRadius * 2f
                    }

                    if (!overlaps) {
                        offsets += candidate
                        placed = true
                        break
                    }
                }

                // fallback: if no placement worked, add it near center but still deterministic
                if (!placed) {
                    offsets += Offset.Zero
                }
            }

            offsets.forEach {
                drawCircle(
                    Color(255, 255, 255, 100),
                    radius / 4,
                    center.plus(it)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(beverage.name, fontSize = 24.sp)
    }
}

@Preview
@Composable
fun SelectionPreview() {
    SelectionScreen(rememberNavController(), DrinkLocationManager.possibleBevs, user = User())
}