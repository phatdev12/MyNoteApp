package com.phatdev.noteapp.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimal Black & White Theme từ colors.xml
val PrimaryColor = Color(0xFF1A1A1A)
val PrimaryDark = Color(0xFF000000)
val PrimaryLight = Color(0xFFF5F5F5)

val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF666666)
val TextTertiary = Color(0xFF999999)

val BackgroundWhite = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFFAFAFA)
val BackgroundSecondary = Color(0xFFF5F5F5)
val BackgroundInput = Color(0xFFF5F5F5)

val BorderLight = Color(0xFFE5E5E5)
val ErrorColor = Color(0xFFD32F2F)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    secondary = TextSecondary,
    onSecondary = Color.White,
    background = BackgroundWhite,
    surface = BackgroundWhite,
    surfaceVariant = BackgroundInput,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    outlineVariant = BorderLight,
    error = ErrorColor,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = PrimaryColor,
    primaryContainer = Color(0xFF2A2A2A),
    secondary = Color(0xFFAAAAAA),
    onSecondary = PrimaryColor,
    background = PrimaryColor,
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF333333),
    error = ErrorColor,
    onError = Color.White,
)

@Composable
fun NoteAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
