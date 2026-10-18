package com.pocketdev.ank_shabd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = AmberCodeLight,
    onPrimary = DarkBg,
    primaryContainer = IndigoPrimary,
    onPrimaryContainer = Color.White,
    secondary = AmberCode,
    onSecondary = Color.Black,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = IndigoDark,
    secondary = AmberCode,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary
)

private val DevanagariPlatformStyle = PlatformTextStyle(
    includeFontPadding = true
)

private val DevanagariLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Proportional,
    trim = LineHeightStyle.Trim.None
)

val AnkShabdTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 36.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.ExtraBold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    displayMedium = TextStyle(
        fontSize = 30.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    displaySmall = TextStyle(
        fontSize = 26.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        platformStyle = DevanagariPlatformStyle,
        lineHeightStyle = DevanagariLineHeightStyle
    )
)

@Composable
fun AnkShabdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AnkShabdTypography,
        content = content
    )
}
