package Themes

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Define custom colors
private val DarkPrimary = Color(0xFF90CAF9)      // Light Blue
private val DarkOnPrimary = Color(0xFF1A1C1E)    // Near Black
private val DarkPrimaryContainer = Color(0xFF2E4355)  // Dark Blue
private val DarkOnPrimaryContainer = Color(0xFFD6E4FF)  // Light Blue

private val DarkSecondary = Color(0xFFBB86FC)    // Lavender
private val DarkOnSecondary = Color(0xFF1A1C1E)  // Near Black
private val DarkSecondaryContainer = Color(0xFF3F3458)  // Dark Purple
private val DarkOnSecondaryContainer = Color(0xFFE9DDFF)  // Light Purple

private val DarkTertiary = Color(0xFF80DEEA)     // Cyan
private val DarkOnTertiary = Color(0xFF1A1C1E)   // Near Black
private val DarkTertiaryContainer = Color(0xFF1C4752)  // Dark Cyan
private val DarkOnTertiaryContainer = Color(0xFFAEEBF0)  // Light Cyan

private val DarkBackground = Color(0xFF121212)   // Dark Gray
private val DarkOnBackground = Color(0xFFE3E3E3) // Light Gray
private val DarkSurface = Color(0xFF1E1E1E)      // Slightly Lighter Gray
private val DarkOnSurface = Color(0xFFE3E3E3)    // Light Gray
private val DarkSurfaceVariant = Color(0xFF252525)  // Variant Gray
private val DarkOnSurfaceVariant = Color(0xFFC4C7C5)  // Muted Light Gray

private val DarkOutline = Color(0xFF8B9198)      // Medium Gray
private val DarkOutlineVariant = Color(0xFF3F484A)  // Dark Gray

// Create the dark color scheme
val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)