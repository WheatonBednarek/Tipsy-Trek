package com.cs407.tipsytrek.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.cs407.tipsytrek.User
import com.cs407.tipsytrek.data.DrinkLocationManager
import kotlin.math.sqrt
import kotlin.random.Random

val SelectionScreenId = "selection"

// Light-ish background to match other screens
private val SelectionBackground = Color(0xFFFDF9F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(navController: NavController, bevs: List<Beverage>, user: User) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SelectionBackground,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 🔹 Bubbly background
            SelectionBubblesBackground()

            if (bevs.isEmpty()) {
                // Empty state with centered message over bubbles
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Go collect beverages!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Scrollable list over bubbles
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    bevs.forEach {
                        SelectionRow(it, navController)
                    }
                }
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
            drawCircle(
                Color(
                    (color.red * darkFactor),
                    (color.green * darkFactor),
                    (color.blue * darkFactor)
                )
            )
            val radius = (size.width / 2) * .95f
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

// 🔹 Background bubbles like login/user, but scoped to this file
@Composable
private fun SelectionBubblesBackground(
    modifier: Modifier = Modifier,
    bubbleColor: Color = Color(0x22FFC857),
    speedMultiplier: Float = 1f
) {
    val bubbles = remember {
        List(40) {
            SelectionBubbleSpec(
                x = Random.nextFloat(),
                initialY = Random.nextFloat(),
                radius = Random.nextFloat().coerceIn(0.004f, 0.018f),
                speed = Random.nextFloat().coerceIn(0.12f, 0.35f)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "selectionBubbles")
    val anim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "selectionBubblesProgress"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = size.width
        val heightPx = size.height

        bubbles.forEach { bubble ->
            val radiusPx = bubble.radius * size.minDimension * 2f

            val yProgress =
                ((bubble.initialY + anim * bubble.speed * speedMultiplier) % 1f)
            val xPos = bubble.x * widthPx
            val yPos = heightPx * (1f - yProgress)

            drawCircle(
                color = bubbleColor,
                radius = radiusPx,
                center = Offset(xPos, yPos)
            )
        }
    }
}

private data class SelectionBubbleSpec(
    val x: Float,
    val initialY: Float,
    val radius: Float,
    val speed: Float
)

@Preview
@Composable
fun SelectionPreview() {
    SelectionScreen(
        rememberNavController(),
        DrinkLocationManager.possibleBevs,
        user = User()
    )
}
