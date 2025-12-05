package com.cs407.tipsytrek.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs407.tipsytrek.User
import com.cs407.tipsytrek.data.Achievements
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.random.Random

val UserPageId = "User"

// Light theme palette
private val BackgroundLight = Color(0xFFFDF9F3)
private val CardLight = Color(0xFFFFFFFF)
private val AccentGold = Color(0xFFFFC857)
private val TextPrimary = Color(0xFF222222)
private val TextSecondary = Color(0xFF555555)
private val ErrorRed = Color(0xFFB00020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPage(
    navController: NavController,
    user: User,
    onResetCurrent: () -> Unit,
    onLogout: () -> Unit,
    onUserUpdated: (User) -> Unit   // MainActivity should already pass this
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firebaseUser = auth.currentUser

    // Stats
    var barCount by remember { mutableStateOf(0) }
    var achievements by remember { mutableStateOf<List<String>>(emptyList()) }

    // Editable states
    var isEditingName by remember { mutableStateOf(false) }
    var editableDisplayName by remember { mutableStateOf(user.displayName) }

    var showPasswordSection by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isUpdatingName by remember { mutableStateOf(false) }
    var isUpdatingPassword by remember { mutableStateOf(false) }

    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        barCount = Achievements.localBarVisitCount
        achievements = Achievements.localUnlockedAchievements.toList()
    }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun updateDisplayName() {
        val current = firebaseUser
        if (current == null) {
            showToast("Not logged in.")
            return
        }
        val trimmed = editableDisplayName.trim()
        if (trimmed.isEmpty()) {
            val msg = "Display name cannot be empty."
            errorText = msg
            showToast(msg)
            return
        }

        isUpdatingName = true
        errorText = null

        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(trimmed)
            .build()

        current.updateProfile(updates)
            .addOnCompleteListener { task ->
                isUpdatingName = false
                if (!task.isSuccessful) {
                    val msg = task.exception?.localizedMessage ?: "Failed to update display name."
                    errorText = msg
                    showToast(msg)
                    return@addOnCompleteListener
                }

                val updatedUser = user.copy(displayName = trimmed)
                onUserUpdated(updatedUser)
                isEditingName = false
                showToast("Display name updated.")
            }
    }

    fun updatePassword() {
        val current = firebaseUser
        if (current == null) {
            showToast("Not logged in.")
            return
        }

        val trimmedNew = newPassword.trim()
        val trimmedConfirm = confirmPassword.trim()

        if (trimmedNew.isEmpty() || trimmedConfirm.isEmpty()) {
            val msg = "Please fill out both password fields."
            errorText = msg
            showToast(msg)
            return
        }

        if (trimmedNew != trimmedConfirm) {
            val msg = "New passwords do not match."
            errorText = msg
            showToast(msg)
            return
        }

        if (trimmedNew.length < 6) {
            val msg = "Password must be at least 6 characters."
            errorText = msg
            showToast(msg)
            return
        }

        isUpdatingPassword = true
        errorText = null

        current.updatePassword(trimmedNew)
            .addOnCompleteListener { task ->
                isUpdatingPassword = false
                if (!task.isSuccessful) {
                    val msg = task.exception?.localizedMessage ?: "Failed to update password."
                    errorText = msg
                    showToast(msg)
                    return@addOnCompleteListener
                }

                newPassword = ""
                confirmPassword = ""
                showPasswordSection = false
                showToast("Password updated.")
            }
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
            ProfileBubblesBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP CARD: profile info + stats
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // DISPLAY NAME ROW (editable)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Display name",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary
                                    )
                                )
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            TextButton(onClick = {
                                isEditingName = !isEditingName
                                editableDisplayName = user.displayName
                                errorText = null
                            }) {
                                Text(if (isEditingName) "Cancel" else "Edit")
                            }
                        }

                        if (isEditingName) {
                            OutlinedTextField(
                                value = editableDisplayName,
                                onValueChange = { editableDisplayName = it },
                                label = { Text("New display name", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary)
                            )
                            Button(
                                onClick = { updateDisplayName() },
                                enabled = !isUpdatingName,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isUpdatingName) "Updating..." else "Save name")
                            }
                        }

                        // EMAIL ROW (read-only, no edit)
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextSecondary
                                )
                            )
                            Text(
                                text = user.email ?: "Unknown",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // CHANGE PASSWORD TOGGLER
                        Button(
                            onClick = {
                                showPasswordSection = !showPasswordSection
                                if (!showPasswordSection) {
                                    newPassword = ""
                                    confirmPassword = ""
                                }
                                errorText = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showPasswordSection) "Cancel password change"
                                else "Change password"
                            )
                        }

                        if (showPasswordSection) {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New password", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                                visualTransformation = PasswordVisualTransformation()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm new password", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Button(
                                onClick = { updatePassword() },
                                enabled = !isUpdatingPassword,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isUpdatingPassword) "Updating..." else "Update password")
                            }
                        }

                        if (errorText != null) {
                            Text(
                                text = errorText!!,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // STATS
                        Text(
                            text = "Stats",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AccentGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

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

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Bars visited: $barCount",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            )
                        )

                        Spacer(Modifier.height(8.dp))

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

                // BOTTOM ACTIONS
                Column {
                    Button(
                        onClick = onResetCurrent,
                        modifier = Modifier.fillMaxWidth()
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
    bubbleColor: Color = Color(0x33FFC857),
    speedMultiplier: Float = 0.9f
) {
    val bubbles = remember {
        List(35) {
            ProfileBubbleSpec(
                x = Random.nextFloat(),
                initialY = Random.nextFloat(),
                radius = Random.nextFloat().coerceIn(0.004f, 0.018f),
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



