package com.axveer.lancar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import lancar.composeapp.generated.resources.Res
import lancar.composeapp.generated.resources.bricolage_bold
import lancar.composeapp.generated.resources.bricolage_extrabold
import lancar.composeapp.generated.resources.instrument_bold
import lancar.composeapp.generated.resources.instrument_medium
import lancar.composeapp.generated.resources.instrument_regular
import lancar.composeapp.generated.resources.instrument_semibold
import org.jetbrains.compose.resources.Font

// ── Palette (from "Bahasa Indonesia iOS app" handoff) ────────────────────────
val LancarTerracotta = Color(0xFFC8502B)
val LancarGreen = Color(0xFF2F6B4F)
val LancarAmber = Color(0xFFE9A93D)
val LancarInk = Color(0xFF1E2B24)
val LancarCream = Color(0xFFFAF4E8)
val LancarSurface = Color(0xFFFFFDF8)
val LancarSecondaryText = Color(0xFF5A5244)
val LancarBorder = Color(0xFFE5D9C3)
val LancarPanel = Color(0xFFF1E7D4)

// Answer-state tokens
val LancarCorrectBg = Color(0xFFE4F0E8)
val LancarCorrectText = Color(0xFF1E4433)
val LancarWrongBg = Color(0xFFF9E2DB)
val LancarWrongText = Color(0xFF7A2313)
val LancarWrongBorder = Color(0xFFB3341C)

private val LancarColors = lightColorScheme(
    primary = LancarTerracotta,
    onPrimary = LancarCream,
    secondary = LancarGreen,
    onSecondary = LancarCream,
    tertiary = LancarAmber,
    onTertiary = LancarInk,
    background = LancarCream,
    onBackground = LancarInk,
    surface = LancarSurface,
    onSurface = LancarInk,
    surfaceVariant = LancarPanel,
    onSurfaceVariant = LancarSecondaryText,
    outline = LancarBorder,
    error = LancarWrongBorder,
    onError = LancarCream,
    errorContainer = LancarWrongBg,
    onErrorContainer = LancarWrongText,
)

@Composable
private fun bricolage() = FontFamily(
    Font(Res.font.bricolage_bold, FontWeight.Bold),
    Font(Res.font.bricolage_extrabold, FontWeight.ExtraBold),
)

@Composable
private fun instrument() = FontFamily(
    Font(Res.font.instrument_regular, FontWeight.Normal),
    Font(Res.font.instrument_medium, FontWeight.Medium),
    Font(Res.font.instrument_semibold, FontWeight.SemiBold),
    Font(Res.font.instrument_bold, FontWeight.Bold),
)

@Composable
private fun lancarTypography(): Typography {
    val display = bricolage()
    val body = instrument()
    fun d(size: Int, weight: FontWeight = FontWeight.ExtraBold, letter: Double = -0.5) =
        TextStyle(fontFamily = display, fontWeight = weight, fontSize = size.sp, letterSpacing = letter.sp)
    fun b(size: Int, weight: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = body, fontWeight = weight, fontSize = size.sp)
    return Typography(
        displayLarge = d(48, letter = -1.0),
        displayMedium = d(40, letter = -1.0),
        displaySmall = d(34),
        headlineLarge = d(32),
        headlineMedium = d(28),
        headlineSmall = d(24),
        titleLarge = d(22, weight = FontWeight.Bold),
        titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 18.sp),
        titleSmall = b(15, FontWeight.SemiBold),
        bodyLarge = b(17),
        bodyMedium = b(15),
        bodySmall = b(13),
        labelLarge = b(16, FontWeight.SemiBold),
        labelMedium = b(13, FontWeight.SemiBold),
        labelSmall = b(11, FontWeight.SemiBold),
    )
}

val LocalAccentColor = staticCompositionLocalOf { LancarTerracotta }

@Composable
fun LancarTheme(accent: Accent = Accent.TERRACOTTA, content: @Composable () -> Unit) {
    val colors = LancarColors.copy(primary = accent.color)
    CompositionLocalProvider(LocalAccentColor provides accent.color) {
        MaterialTheme(
            colorScheme = colors,
            typography = lancarTypography(),
            content = content,
        )
    }
}
