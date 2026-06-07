package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimalist Dark Color Scheme (Charcoal Slate & Soft Lavenders)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBEC2FF), // Soft Indigo Blue accent
    onPrimary = Color(0xFF1B226E),
    primaryContainer = Color(0xFF2C398E),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = Color(0xFF121115), // Deep dark lavender
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B20), // Dark lilac surface
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

// Minimalist Light Color Scheme (from Tailwind extract: #FBF8FD background, #1C1B1F text, #4355B9 buttons, etc.)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4355B9), // Royal slate blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF), // Soft periwinkle lilac
    onPrimaryContainer = Color(0xFF001453), // Dark indigo text
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADDFF), // Soft lilac badge backing
    onSecondaryContainer = Color(0xFF21005D), // Dark purple text
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    background = Color(0xFFFBF8FD), // Beautiful light lavender-white
    onBackground = Color(0xFF1C1B1F), // Dark charcoal
    surface = Color.White, // Clean white surface cards
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3EDF7), // Warm lilac elements background
    onSurfaceVariant = Color(0xFF49454F), // lilac gray subtext
    outline = Color(0xFFE7E0EC) // Lavender subtle gray borders
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
