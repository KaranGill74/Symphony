package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyWhite
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.MusicViewModel

// Screen-state navigation descriptor
sealed interface AppScreen {
    object Splash : AppScreen
    object Login : AppScreen
    object Signup : AppScreen
    data class Verification(val email: String) : AppScreen
    object UserApp : AppScreen
    object AdminApp : AppScreen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SymphonyApplication

        // Instantiate master viewmodels using Repository and Player instances inside custom Application class
        val authViewModel: AuthViewModel by viewModels { AuthViewModel.Factory(app.repository) }
        val musicViewModel: MusicViewModel by viewModels { MusicViewModel.Factory(app.repository, app.playerManager) }
        val adminViewModel: AdminViewModel by viewModels { AdminViewModel.Factory(app.repository) }

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
                var isFullPlayerOpen by remember { mutableStateOf(false) }

                val currentUser by authViewModel.currentUser.collectAsState()
                val context = LocalContext.current

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Main Navigation Switcher
                        when (currentScreen) {
                            AppScreen.Splash -> {
                                val isLoggedIn = currentUser != null
                                val userRole = currentUser?.role

                                SplashScreen(
                                    isUserLoggedIn = isLoggedIn,
                                    userRole = userRole,
                                    onNavigateToLogin = { currentScreen = AppScreen.Login },
                                    onNavigateToUserApp = { currentScreen = AppScreen.UserApp },
                                    onNavigateToAdminApp = { currentScreen = AppScreen.AdminApp }
                                )
                            }

                            AppScreen.Login -> {
                                LoginScreen(
                                    authViewModel = authViewModel,
                                    onNavigateToSignup = { currentScreen = AppScreen.Signup },
                                    onLoginSuccess = { role ->
                                        if (role == "Admin") {
                                            currentScreen = AppScreen.AdminApp
                                            Toast.makeText(context, "Welcome back, Administration!", Toast.LENGTH_SHORT).show()
                                        } else if (role == "Blocked") {
                                            // Handled automatically by security check card below
                                            Toast.makeText(context, "Access Denied. Account is currently blocked.", Toast.LENGTH_LONG).show()
                                        } else {
                                            currentScreen = AppScreen.UserApp
                                            Toast.makeText(context, "Logged in as Symphony User!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onNavigateToVerification = { email ->
                                        currentScreen = AppScreen.Verification(email)
                                    }
                                )
                            }

                            AppScreen.Signup -> {
                                SignupScreen(
                                    authViewModel = authViewModel,
                                    onNavigateToLogin = { currentScreen = AppScreen.Login },
                                    onNavigateToVerification = { email ->
                                        currentScreen = AppScreen.Verification(email)
                                        Toast.makeText(context, "Simulated verification code sent to $email!", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }

                            is AppScreen.Verification -> {
                                val emailVal = (currentScreen as AppScreen.Verification).email
                                EmailVerificationScreen(
                                    email = emailVal,
                                    authViewModel = authViewModel,
                                    onNavigateToLogin = { currentScreen = AppScreen.Login },
                                    onVerificationSuccess = {
                                        currentScreen = AppScreen.Login
                                    }
                                )
                            }

                            AppScreen.UserApp -> {
                                UserMusicApp(
                                    authViewModel = authViewModel,
                                    musicViewModel = musicViewModel,
                                    onOpenFullPlayer = { isFullPlayerOpen = true },
                                    onLogout = {
                                        currentScreen = AppScreen.Login
                                        isFullPlayerOpen = false
                                        musicViewModel.stop()
                                    }
                                )
                            }

                            AppScreen.AdminApp -> {
                                AdminDashboardScreen(
                                    adminViewModel = adminViewModel,
                                    onLogout = {
                                        authViewModel.logout()
                                        currentScreen = AppScreen.Login
                                        musicViewModel.stop()
                                    }
                                )
                            }
                        }

                        // Full Screen Music Player Overlay
                        AnimatedVisibility(
                            visible = isFullPlayerOpen,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(350)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(300)
                            ) + fadeOut()
                        ) {
                            MusicPlayerScreen(
                                musicViewModel = musicViewModel,
                                onClose = { isFullPlayerOpen = false }
                            )
                        }

                        // SECURITY CORNER: Blocked check overlay
                        if (currentUser != null && currentUser?.role == "Blocked") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SpotifyBlack)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Blocked",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "ACCOUNT SUSPENDED",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SpotifyWhite
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Your account has been administratively blocked due to site violations. Please reach out to support@symphony.com with any concerns.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Button(
                                        onClick = {
                                            authViewModel.logout()
                                            currentScreen = AppScreen.Login
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                                    ) {
                                        Text("RETURN TO LOGIN SCREEN", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
