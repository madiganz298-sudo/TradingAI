package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TradeAIProColorScheme = darkColorScheme(
    primary = PrimaryGold,
    secondary = SecondaryGold,
    tertiary = DarkGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = BearRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default
    dynamicColor: Boolean = false, // Disable dynamic colors to keep premium branding
    content: @Composable () -> Unit
) {
    // We enforce the TradeAIProColorScheme to maintain the premium gold-dark aesthetic regardless of system preferences
    MaterialTheme(
        colorScheme = TradeAIProColorScheme,
        typography = Typography,
        content = content
    )
}
