package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AuthState
import com.example.viewmodel.AuthViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: (String) -> Unit, // returns user role
    onNavigateToVerification: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                onLoginSuccess(state.user.role)
                authViewModel.resetState()
            }
            is AuthState.VerificationRequired -> {
                onNavigateToVerification(state.email)
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                showErrorDialog = state.message
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("OK", color = SpotifyGreen)
                }
            },
            title = { Text("Authentication Issue", fontWeight = FontWeight.Bold) },
            text = { Text(showErrorDialog ?: "Invalid email or password.") }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Logo",
                tint = SpotifyGreen,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Log in to Symphony",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SpotifyWhite
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "EmailIcon") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "LockIcon") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = SpotifyGreen)
                    )
                    Text("Remember Me", color = SpotifyWhite, fontSize = 14.sp)
                }

                TextButton(onClick = { showErrorDialog = "For demo logins: admin@symphony.com with password (admin) and user@symphony.com with (user123)." }) {
                    Text("Forgot Password?", color = SpotifyGreen, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { authViewModel.login(email.trim(), password.trim()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                shape = RoundedCornerShape(25.dp),
                enabled = authState != AuthState.Loading
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(color = SpotifyBlack, modifier = Modifier.size(24.dp))
                } else {
                    Text("LOG IN", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SpotifyBlack)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = SpotifyLightGray)
                Text(" OR ", color = SpotifyGrayText, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = SpotifyLightGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showGooglePicker by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showGooglePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_login_button"),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.dp, SpotifyGrayText),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyWhite)
            ) {
                GoogleIcon()
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (showGooglePicker) {
                GoogleAccountChooserDialog(
                    onDismiss = { showGooglePicker = false },
                    onAccountSelected = { gmail, name, image ->
                        authViewModel.loginWithGoogle(gmail, name, image)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", color = SpotifyGrayText)
                TextButton(onClick = onNavigateToSignup) {
                    Text("Sign up", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerification: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var profileUrl by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.VerificationRequired -> {
                onNavigateToVerification(state.email)
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                showErrorDialog = state.message
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("OK", color = SpotifyGreen)
                }
            },
            title = { Text("Registration Issue", fontWeight = FontWeight.Bold) },
            text = { Text(showErrorDialog ?: "Please verify fields.") }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Logo",
                tint = SpotifyGreen,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Sign up for Free",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = SpotifyWhite
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Person") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = "UserTag") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = profileUrl,
                onValueChange = { profileUrl = it },
                label = { Text("Profile Pic Web URL (Optional)") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = "Avatar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_avatar_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Create Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "ConfirmLock") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_confirm_password"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        showErrorDialog = "Passwords do not match."
                    } else if (password.isBlank()) {
                        showErrorDialog = "Password is required."
                    } else {
                        authViewModel.signup(name, username, email, profileUrl, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                shape = RoundedCornerShape(25.dp),
                enabled = authState != AuthState.Loading
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(color = SpotifyBlack, modifier = Modifier.size(24.dp))
                } else {
                    Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SpotifyBlack)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = SpotifyLightGray)
                Text(" OR ", color = SpotifyGrayText, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = SpotifyLightGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showGooglePicker by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showGooglePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_signup_button"),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.dp, SpotifyGrayText),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyWhite)
            ) {
                GoogleIcon()
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (showGooglePicker) {
                GoogleAccountChooserDialog(
                    onDismiss = { showGooglePicker = false },
                    onAccountSelected = { gmail, name, image ->
                        authViewModel.loginWithGoogle(gmail, name, image)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already registered?", color = SpotifyGrayText)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Log in", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Custom remember state helper to bypass mutableStateOf inside compose warning in build
@Composable
fun <T> rememberStateFlowOf(initialValue: T): MutableState<T> = remember { mutableStateOf(initialValue) }

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            color = Color(0xFF4285F4),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun GoogleAccountChooserDialog(
    onDismiss: () -> Unit,
    onAccountSelected: (email: String, name: String, imageUrl: String) -> Unit
) {
    var customEmailMode by remember { mutableStateOf(false) }
    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF282828), // Sleek Google Material dark theme color
        shape = RoundedCornerShape(28.dp),
        confirmButton = {},
        dismissButton = {},
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (customEmailMode) "Sign in with Google" else "Choose an account",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "to continue to Symphony",
                    color = SpotifyGrayText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!customEmailMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Karan Gill (gillkarangill23@gmail.com)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAccountSelected("gillkarangill23@gmail.com", "Karan Gill", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150")
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(
                                name = "Karan Gill",
                                imageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                modifier = Modifier.size(40.dp),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Karan Gill", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("gillkarangill23@gmail.com", color = SpotifyGrayText, fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(color = SpotifyLightGray)

                        // Option 2: Demo User (symphonydemo@gmail.com)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAccountSelected("symphonydemo@gmail.com", "Demo Symphonian", "")
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(
                                name = "Demo Symphonian",
                                imageUrl = "", // empty to test first letter avatar!
                                modifier = Modifier.size(40.dp),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Demo Symphonian", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("symphonydemo@gmail.com", color = SpotifyGrayText, fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(color = SpotifyLightGray)

                        // Option 3: Add new account
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { customEmailMode = true }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyDarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Use another account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    // Custom Gmail entry mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                focusedLabelColor = SpotifyGreen,
                                unfocusedBorderColor = SpotifyGrayText
                            )
                        )

                        OutlinedTextField(
                            value = customEmail,
                            onValueChange = { customEmail = it },
                            label = { Text("Gmail Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                focusedLabelColor = SpotifyGreen,
                                unfocusedBorderColor = SpotifyGrayText
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { customEmailMode = false }) {
                                Text("Back", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customEmail.isNotBlank()) {
                                        val display = if (customName.isNotBlank()) customName else customEmail.substringBefore("@")
                                        onAccountSelected(customEmail.trim(), display, "")
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                enabled = customEmail.contains("@")
                            ) {
                                Text("Sign In", color = SpotifyBlack)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ProfileAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    email: String? = null
) {
    if (!imageUrl.isNullOrBlank() && (imageUrl.startsWith("http") || imageUrl.startsWith("content") || imageUrl.startsWith("file"))) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Profile Avatar",
            modifier = modifier
                .clip(CircleShape)
                .background(SpotifyDarkGray),
            contentScale = ContentScale.Crop
        )
    } else {
        val firstLetter = if (name.isNotBlank()) {
            name.trim().first().uppercaseChar().toString()
        } else if (!email.isNullOrBlank()) {
            email.trim().first().uppercaseChar().toString()
        } else {
            "U"
        }
        val seedText = if (name.isNotBlank()) name else (email ?: "U")
        val colors = listOf(
            Color(0xFF1DB954), // Spotify Green
            Color(0xFF1E88E5), // Blue
            Color(0xFFE53935), // Red
            Color(0xFF8E24AA), // Purple
            Color(0xFF00ACC1), // Cyan
            Color(0xFFFB8C00), // Orange
            Color(0xFF43A047), // Green
            Color(0xFF3949AB)  // Indigo
        )
        val backgroundColor = colors[java.lang.Math.abs(seedText.hashCode()) % colors.size]

        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstLetter,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    email: String,
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onVerificationSuccess: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val simulatedOtp by authViewModel.simulatedOtp.collectAsState()

    // Query database to fetch the simulated code they can use to verify
    LaunchedEffect(email) {
        authViewModel.loadSimulatedOtp(email)
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.VerificationSuccess -> {
                showSuccessDialog = true
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                showErrorDialog = state.message
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("OK", color = SpotifyGreen)
                }
            },
            title = { Text("Verification Issue", fontWeight = FontWeight.Bold) },
            text = { Text(showErrorDialog ?: "Please check the code and try again.") }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                onVerificationSuccess()
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("success_confirm_button"),
                    onClick = { 
                        showSuccessDialog = false
                        onVerificationSuccess()
                    }
                ) {
                    Text("Proceed to Login", color = SpotifyGreen)
                }
            },
            title = { Text("Account Activated!", fontWeight = FontWeight.Bold) },
            text = { Text("Your email address has been successfully verified, and your registration is complete! You can now securely log in.") }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success Verification Logo",
                tint = SpotifyGreen,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Verify Your Email",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = SpotifyWhite
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "We have simulated sending a 6-digit verification code to:",
                fontSize = 14.sp,
                color = SpotifyGrayText,
                textAlign = TextAlign.Center
            )
            Text(
                text = email,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SpotifyGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Please input the code below to fully activate your account.",
                fontSize = 14.sp,
                color = SpotifyGrayText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Beautiful Simulated OTP Banner so they know what code to use
            Card(
                colors = CardDefaults.cardColors(containerColor = SpotifyLightGray.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = SpotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simulated Inbox Check",
                            color = SpotifyWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Because emails are simulated locally, your code is:",
                        color = SpotifyGrayText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = simulatedOtp ?: "Generating...",
                        color = SpotifyGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(top = 8.dp).testTag("simulated_otp_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = { 
                    if (it.length <= 6) otpCode = it.filter { char -> char.isDigit() }
                },
                label = { Text("6-Digit Code") },
                placeholder = { Text("123456") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "OTP Key") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("verification_code_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedBorderColor = SpotifyGrayText
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (otpCode.length < 6) {
                        showErrorDialog = "Please enter the full 6-digit verification code."
                    } else {
                        authViewModel.verifyOtp(email, otpCode)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verify_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = SpotifyBlack, modifier = Modifier.size(24.dp))
                } else {
                    Text("Verify & Activate", color = SpotifyBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    // Re-generate OTP code
                    authViewModel.loadSimulatedOtp(email)
                },
                modifier = Modifier.testTag("resend_button")
            ) {
                Text("Resend Verification Code", color = SpotifyGreen, fontWeight = FontWeight.SemiBold)
            }

            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.testTag("back_to_login_button")
            ) {
                Text("Back to Login", color = SpotifyGrayText)
            }
        }
    }
}
