package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SpotifyGreen,
    secondary = SpotifyGreen,
    tertiary = SpotifyGrayText,
    background = SpotifyBlack,
    surface = SpotifyDarkGray,
    onPrimary = SpotifyBlack,
    onSecondary = SpotifyWhite,
    onBackground = SpotifyWhite,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifyLightGray,
    onSurfaceVariant = SpotifyGrayText
  )

private val LightColorScheme = DarkColorScheme // Always force elegant Dark Mode for the Music App as requested

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark Theme as requested by the user
  dynamicColor: Boolean = false, // Disable dynamic colors to maintain Spotify theme fidelity
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
