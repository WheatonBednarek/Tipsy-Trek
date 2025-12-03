package com.cs407.tipsytrek.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

const val LoginPageId = "login"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(
    onLoginSuccess: (FirebaseUser) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TipsyTrek - Login / Sign Up") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // EMAIL (used for both login & signup)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // PASSWORD (used for both)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            // DISPLAY NAME (used only when creating an account)
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name (for new accounts)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorText?.let {
                Text(it)
            }

            // LOGIN BUTTON: just email + password
            Button(
                onClick = {
                    isLoading = true
                    errorText = null
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                task.result?.user?.let(onLoginSuccess)
                            } else {
                                errorText = task.exception?.localizedMessage ?: "Login failed"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Logging in..." else "Login")
            }

            // SIGN-UP BUTTON: email + password + displayName
            Button(
                onClick = {
                    isLoading = true
                    errorText = null

                    val trimmedEmail = email.trim()
                    val trimmedDisplayName = displayName.trim()

                    if (trimmedDisplayName.isEmpty()) {
                        isLoading = false
                        errorText = "Please enter a display name for new accounts."
                        return@Button
                    }

                    auth.createUserWithEmailAndPassword(trimmedEmail, password)
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                isLoading = false
                                errorText = task.exception?.localizedMessage ?: "Sign up failed"
                                return@addOnCompleteListener
                            }

                            val user = task.result?.user
                            if (user == null) {
                                isLoading = false
                                errorText = "Sign up failed: user is null"
                                return@addOnCompleteListener
                            }

                            // Set the display name on the Firebase user profile
                            val updates = UserProfileChangeRequest.Builder()
                                .setDisplayName(trimmedDisplayName)
                                .build()

                            user.updateProfile(updates)
                                .addOnCompleteListener { profileTask ->
                                    isLoading = false
                                    if (profileTask.isSuccessful) {
                                        onLoginSuccess(user)
                                    } else {
                                        // Account is created but profile update failed
                                        errorText = profileTask.exception?.localizedMessage
                                            ?: "Account created, but failed to update display name."
                                        onLoginSuccess(user)
                                    }
                                }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Creating account..." else "Create account")
            }
        }
    }
}

