package com.jewish.calendar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = Blue60,
    onPrimary            = androidx.compose.ui.graphics.Color.White,
    primaryContainer     = androidx.compose.ui.graphics.Color(0xFFD6E4FF),
    onPrimaryContainer   = androidx.compose.ui.graphics.Color(0xFF001D45),
    secondary            = Gold80,
    onSecondary          = androidx.compose.ui.graphics.Color.White,
    secondaryContainer   = androidx.compose.ui.graphics.Color(0xFFFFF0C8),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF3D2000),
    tertiary             = PurityPurpleOnLight,
    onTertiary           = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer    = PurityPurpleBright,
    onTertiaryContainer  = PurityPurpleOnLight,
    background           = androidx.compose.ui.graphics.Color(0xFFF8F8F8),
    onBackground         = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
    surface              = androidx.compose.ui.graphics.Color.White,
    onSurface            = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
    surfaceVariant       = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
    onSurfaceVariant     = androidx.compose.ui.graphics.Color(0xFF444466),
    outline              = androidx.compose.ui.graphics.Color(0xFFAAAAAA),
    error                = HolidayRed,
    onError              = androidx.compose.ui.graphics.Color.White,
    errorContainer       = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer     = androidx.compose.ui.graphics.Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = androidx.compose.ui.graphics.Color(0xFF003259),
    primaryContainer     = androidx.compose.ui.graphics.Color(0xFF004881),
    onPrimaryContainer   = androidx.compose.ui.graphics.Color(0xFFD1E4FF),
    secondary            = Gold40,
    onSecondary          = androidx.compose.ui.graphics.Color(0xFF3D2000),
    secondaryContainer   = androidx.compose.ui.graphics.Color(0xFF5A3800),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDEA6),
    tertiary             = PurityPink,
    onTertiary           = androidx.compose.ui.graphics.Color(0xFF4A0050),
    tertiaryContainer    = androidx.compose.ui.graphics.Color(0xFF6B006E),
    onTertiaryContainer  = PurityPink,
    background           = DarkBackground,
    onBackground         = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surface              = DarkSurface,
    onSurface            = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surfaceVariant       = androidx.compose.ui.graphics.Color(0xFF49454F),
    onSurfaceVariant     = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
    outline              = androidx.compose.ui.graphics.Color(0xFF938F99),
    outlineVariant       = androidx.compose.ui.graphics.Color(0xFF49454F),
    error                = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError              = androidx.compose.ui.graphics.Color(0xFF370B1E),
    errorContainer       = androidx.compose.ui.graphics.Color(0xFF8C1D41),
    onErrorContainer     = androidx.compose.ui.graphics.Color(0xFFFFB3B3),
    scrim                = androidx.compose.ui.graphics.Color(0xFF000000)
)

@Composable
fun YehudaCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,          // disabled — keeps our custom Hebrew palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Force RTL layout direction for the entire app (Hebrew UI)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
