package com.cs407.tipsytrek.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs407.tipsytrek.User
import com.cs407.tipsytrek.data.Achievements
import kotlin.random.Random

val UserPageId = "User"

// Same light color palette as login
private val BackgroundLight = Color(0xFFFDF9F3)
private val CardLight = Color(0xFFFFFFFF)
private val AccentGold = Color(0xFFFFC857)
private val TextPrimary = Color(0xFF222222)
private val TextSecondary = Color(0xFF555555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPage(
    navController: NavController,
    user: User,
    onResetCurrent: () -> Unit,
    onLogout: () -> Unit
) {
    var barCount by remember { mutableStateOf(0) }
    var achievements by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        barCount = Achievements.localBarVisitCount
        achievements = Achievements.localUnlockedAchievements.toList()
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Your Night",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "BAC: ${user.formattedBac}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 🔹 More visible bubbles now
            ProfileBubblesBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP: user info + stats in a card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardLight
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = user.displayName.ifBlank { "Your Profile" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "Email: ${user.email ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Current drinks: ${user.currentDrinks.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "All-time drinks: ${user.allTimeDrinks.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Bars visited: $barCount",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Achievements",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AccentGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        if (achievements.isEmpty()) {
                            Text(
                                text = "No achievements yet. Time to explore some bars 🍻",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary
                                )
                            )
                        } else {
                            achievements.forEach {
                                Text(
                                    text = "• $it",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // BOTTOM: actions
                Column {
                    Button(
                        onClick = onResetCurrent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reset drink count")
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log out")
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Back",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBubblesBackground(
    modifier: Modifier = Modifier,
    bubbleColor: Color = Color(0x33FFC857), // 🔹 more opaque so they show
    speedMultiplier: Float = 0.9f
) {
    val bubbles = remember {
        List(35) {
            ProfileBubbleSpec(
                x = Random.nextFloat(),
                initialY = Random.nextFloat(),
                radius = Random.nextFloat().coerceIn(0.004f, 0.018f), // bigger
                speed = Random.nextFloat().coerceIn(0.08f, 0.22f)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "profileBubbles")
    val anim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "profileBubblesProgress"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = size.width
        val heightPx = size.height

        bubbles.forEach { bubble ->
            val radiusPx = bubble.radius * size.minDimension * 2f
            val yProgress = ((bubble.initialY + anim * bubble.speed * speedMultiplier) % 1f)
            val xPos = bubble.x * widthPx
            val yPos = heightPx * (1f - yProgress)

            drawCircle(
                color = bubbleColor,
                radius = radiusPx,
                center = androidx.compose.ui.geometry.Offset(xPos, yPos)
            )
        }
    }
}

private data class ProfileBubbleSpec(
    val x: Float,
    val initialY: Float,
    val radius: Float,
    val speed: Float
)





