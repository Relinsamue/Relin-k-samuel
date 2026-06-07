package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimalist Dark Color Scheme using Slate dark slate and neon colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9E80FF), // Soft neon purple
    onPrimary = Color(0xFF1E0066),
    primaryContainer = Color(0xFF4820CE),
    onPrimaryContainer = Color(0xFFF0EBFF),
    secondary = Color(0xFFFF7DA7), // Soft pink
    onSecondary = Color(0xFF3F0015),
    secondaryContainer = Color(0xFF991544),
    onSecondaryContainer = Color(0xFFFFE5EE),
    tertiary = Color(0xFF33CFFF), // Soft blue
    onTertiary = Color(0xFF00354D),
    background = Color(0xFF0F172A), // Charcoal Slate dark background
    onBackground = Color(0xFFF8F9FC),
    surface = Color(0xFF1E293B), // Slate dark card surface
    onSurface = Color(0xFFF8F9FC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569)
)

// Minimalist Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6C3BFF), // Deep Purple
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0EBFF), // Soft lavender purple
    onPrimaryContainer = Color(0xFF1E0066),
    secondary = Color(0xFFFF4D8D), // Hot Pink
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5EE),
    onSecondaryContainer = Color(0xFF3F0015),
    tertiary = Color(0xFF00C2FF), // Sky Accent Blue
    onTertiary = Color.White,
    background = Color(0xFFF8F9FC), // Slate clean light background
    onBackground = Color(0xFF0F172A),
    surface = Color.White, // Clean white surface cards
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F6), // Warm grey blue
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0)
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
