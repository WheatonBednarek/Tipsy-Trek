package com.cs407.tipsytrek.ui

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.random.Random

const val LoginPageId = "login"

private enum class AuthMode { LOGIN, SIGNUP }

// Light theme colors
private val BackgroundLight = Color(0xFFFDF9F3)
private val CardLight = Color(0xFFFFFFFF)
private val AccentGold = Color(0xFFFFC857)
private val TextPrimary = Color(0xFF222222)
private val TextSecondary = Color(0xFF555555)
private val ErrorRed = Color(0xFFB00020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(
    onLoginSuccess: (FirebaseUser) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Bubble speed reacts to mode (a little more intense when toggling)
    val targetBubbleSpeed = when (authMode) {
        AuthMode.LOGIN -> 1f
        AuthMode.SIGNUP -> 1.6f
    }
    val bubbleSpeed by animateFloatAsState(
        targetValue = targetBubbleSpeed,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "bubbleSpeed"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Animated bubbles in the background (subtle gold on light)
            BubblesBackground(speedMultiplier = bubbleSpeed)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TipsyTrek",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = "🍺 Track your night, safely",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.background(Color.Transparent)
                )

                Spacer(Modifier.height(24.dp))

                // Login / Sign Up toggle
                AuthModeToggle(
                    currentMode = authMode,
                    onModeChange = { mode ->
                        authMode = mode
                        errorText = null
                    }
                )

                Spacer(Modifier.height(24.dp))

                // Card with form
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardLight
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (authMode == AuthMode.LOGIN) "Welcome back!" else "Create your account",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (authMode == AuthMode.LOGIN) {
                                "Log in to continue tracking your drinks and BAC."
                            } else {
                                "Sign up with a display name so your stats feel personal."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // EMAIL
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = TextPrimary)
                        )

                        // PASSWORD
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = LocalTextStyle.current.copy(color = TextPrimary)
                        )

                        // DISPLAY NAME (Sign Up only)
                        if (authMode == AuthMode.SIGNUP) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary)
                            )
                        }

                        if (errorText != null) {
                            Text(
                                text = errorText!!,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                errorText = null
                                when (authMode) {
                                    AuthMode.LOGIN -> {
                                        isLoading = true
                                        auth.signInWithEmailAndPassword(email.trim(), password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful && task.result?.user != null) {
                                                    onLoginSuccess(task.result!!.user!!)
                                                } else {
                                                    val msg = "Email or password not correct"
                                                    errorText = msg
                                                    Toast.makeText(
                                                        context,
                                                        msg,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    }


                                    AuthMode.SIGNUP -> {
                                        val trimmedEmail = email.trim()
                                        val trimmedDisplayName = displayName.trim()

                                        if (trimmedDisplayName.isEmpty()) {
                                            val msg = "Please enter a display name."
                                            errorText = msg
                                            Toast.makeText(
                                                context,
                                                msg,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }
                                        if (password.length < 6) {
                                            val msg =
                                                "Password should be at least 6 characters."
                                            errorText = msg
                                            Toast.makeText(
                                                context,
                                                msg,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }

                                        isLoading = true
                                        auth.createUserWithEmailAndPassword(
                                            trimmedEmail,
                                            password
                                        ).addOnCompleteListener { task ->
                                            if (!task.isSuccessful) {
                                                isLoading = false
                                                val ex = task.exception
                                                val msg = ex?.localizedMessage
                                                    ?: "Sign up failed. Please try again."
                                                errorText = msg
                                                Toast.makeText(
                                                    context,
                                                    msg,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@addOnCompleteListener
                                            }

                                            val user = task.result?.user
                                            if (user == null) {
                                                isLoading = false
                                                val msg =
                                                    "Sign up failed: user is null. Please try again."
                                                errorText = msg
                                                Toast.makeText(
                                                    context,
                                                    msg,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@addOnCompleteListener
                                            }

                                            val updates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(trimmedDisplayName)
                                                .build()

                                            user.updateProfile(updates)
                                                .addOnCompleteListener { profileTask ->
                                                    isLoading = false
                                                    if (!profileTask.isSuccessful) {
                                                        val msg =
                                                            profileTask.exception?.localizedMessage
                                                                ?: "Account created, but failed to update display name."
                                                        errorText = msg
                                                        Toast.makeText(
                                                            context,
                                                            msg,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    onLoginSuccess(user)
                                                }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text(
                                text = when {
                                    isLoading && authMode == AuthMode.LOGIN -> "Logging in..."
                                    isLoading && authMode == AuthMode.SIGNUP -> "Creating account..."
                                    authMode == AuthMode.LOGIN -> "Log In"
                                    else -> "Sign Up"
                                }
                            )
                        }

                        if (authMode == AuthMode.LOGIN) {
                            TextButton(
                                onClick = { authMode = AuthMode.SIGNUP; errorText = null },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Don’t have an account? Sign up")
                            }
                        } else {
                            TextButton(
                                onClick = { authMode = AuthMode.LOGIN; errorText = null },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Already have an account? Log in")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeToggle(
    currentMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0x11FFC857)
            ),
            shape = RoundedCornerShape(999.dp)
        ) {
            RowTwoToggle(
                currentMode = currentMode,
                onModeChange = onModeChange
            )
        }
    }
}

@Composable
private fun RowTwoToggle(
    currentMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToggleChip(
            text = "Log In",
            selected = currentMode == AuthMode.LOGIN,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .clickable { onModeChange(AuthMode.LOGIN) }
        )
        ToggleChip(
            text = "Sign Up",
            selected = currentMode == AuthMode.SIGNUP,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .clickable { onModeChange(AuthMode.SIGNUP) }
        )
    }
}

@Composable
private fun ToggleChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) AccentGold else Color.Transparent
    val fg = if (selected) Color.White else TextSecondary

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BubblesBackground(
    modifier: Modifier = Modifier,
    bubbleColor: Color = Color(0x22FFC857),
    speedMultiplier: Float = 1f
) {
    // Pre-generate a set of bubbles with random positions/sizes/speeds
    val bubbles = remember {
        List(40) {
            BubbleSpec(
                x = Random.nextFloat(),
                initialY = Random.nextFloat(),
                radius = Random.nextFloat().coerceIn(0.004f, 0.018f),
                speed = Random.nextFloat().coerceIn(0.12f, 0.35f)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")

    val anim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubblesProgress"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = size.width
        val heightPx = size.height

        bubbles.forEach { bubble ->
            val radiusPx = bubble.radius * size.minDimension * 2f

            // vertical position loops from bottom to top
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

private data class BubbleSpec(
    val x: Float,
    val initialY: Float,
    val radius: Float,
    val speed: Float
)


