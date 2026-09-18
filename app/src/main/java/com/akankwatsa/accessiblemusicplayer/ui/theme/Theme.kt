package com.akankwatsa.accessiblemusicplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A high-contrast dark palette. Contrast matters more than decoration here:
 * every foreground colour was chosen to sit well above the 4.5:1 WCAG AA
 * threshold against its background so the app stays readable for people with
 * low vision as well as for screen reader users.
 */
private val PlayerColors = darkColorScheme(
    primary = Color(0xFF9EC5FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF1B3A63),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFB9C8DE),
    onSecondary = Color(0xFF233246),
    secondaryContainer = Color(0xFF2C3B4F),
    onSecondaryContainer = Color(0xFFDCE6F5),
    tertiary = Color(0xFFF2B8C6),
    onTertiary = Color(0xFF4A2530),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF262B35),
    onSurfaceVariant = Color(0xFFC3CAD6),
    surfaceContainer = Color(0xFF171B22),
    surfaceContainerHigh = Color(0xFF1E232B),
    outline = Color(0xFF8B94A3),
    outlineVariant = Color(0xFF3A414D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val PlayerTypography = Typography().let { base ->
    base.copy(
        titleLarge = base.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontSize = 17.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 15.sp),
        labelLarge = base.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    )
}

private val PlayerShapes = Shapes(
    extraSmall = RoundedCornerShape(6),
    small = RoundedCornerShape(10),
    medium = RoundedCornerShape(14),
    large = RoundedCornerShape(20),
    extraLarge = RoundedCornerShape(28),
)

@Composable
fun AccessibleMusicPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PlayerColors,
        typography = PlayerTypography,
        shapes = PlayerShapes,
        content = content,
    )
}