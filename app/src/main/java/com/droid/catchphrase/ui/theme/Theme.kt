package com.droid.catchphrase.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.droid.catchphrase.model.TeamColor

// High-contrast, clean light color palette
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),             // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),    // Indigo 50
    onPrimaryContainer = Color(0xFF312E81),  // Indigo 900
    secondary = Color(0xFF0D9488),           // Teal 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0FDFA),  // Teal 50
    onSecondaryContainer = Color(0xFF134E4A),// Teal 900
    background = Color(0xFFF8FAFC),          // Slate 50
    onBackground = Color(0xFF0F172A),        // Slate 900
    surface = Color(0xFFFFFFFF),             // Pure White Card
    onSurface = Color(0xFF0F172A),           // Slate 900
    surfaceVariant = Color(0xFFF1F5F9),      // Slate 100
    onSurfaceVariant = Color(0xFF334155),    // Slate 700
    outline = Color(0xFF94A3B8),             // Slate 400
    outlineVariant = Color(0xFFE2E8F0),      // Slate 200
    error = Color(0xFFDC2626),               // Red 600
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),      // Red 100
    onErrorContainer = Color(0xFF991B1B)     // Red 800
)

// High-contrast, rich dark color palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),             // Indigo 400
    onPrimary = Color(0xFF0F172A),           // Slate 900
    primaryContainer = Color(0xFF312E81),    // Indigo 900
    onPrimaryContainer = Color(0xFFE0E7FF),  // Indigo 100
    secondary = Color(0xFF2DD4BF),           // Teal 400
    onSecondary = Color(0xFF0F172A),         // Slate 900
    secondaryContainer = Color(0xFF134E4A),  // Teal 900
    onSecondaryContainer = Color(0xFFCCFBF1),// Teal 100
    background = Color(0xFF0F172A),          // Slate 900
    onBackground = Color(0xFFF8FAFC),        // Slate 50
    surface = Color(0xFF1E293B),             // Slate 800 Card
    onSurface = Color(0xFFF8FAFC),           // Slate 50
    surfaceVariant = Color(0xFF334155),      // Slate 700
    onSurfaceVariant = Color(0xFFCBD5E1),    // Slate 300
    outline = Color(0xFF64748B),             // Slate 500
    outlineVariant = Color(0xFF334155),      // Slate 700
    error = Color(0xFFEF4444),               // Red 500
    onError = Color(0xFF0F172A),
    errorContainer = Color(0xFF7F1D1D),      // Red 900
    onErrorContainer = Color(0xFFFECACA)     // Red 200
)

val CatchphraseTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun CatchphraseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CatchphraseTypography,
        content = content
    )
}

fun TeamColor.resolveColor(isDark: Boolean): Color {
    return if (isDark) Color(this.darkHex) else Color(this.lightHex)
}
