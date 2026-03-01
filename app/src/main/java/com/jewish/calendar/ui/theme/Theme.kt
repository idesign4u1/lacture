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
    primary = DarkPrimary,
    onPrimary = Blue80,
    primaryContainer = Blue80,
    onPrimaryContainer = Blue40,
    secondary = Gold40,
    onSecondary = Gold80,
    secondaryContainer = Gold80,
    onSecondaryContainer = Gold40,
    tertiary = PurityPink,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5)
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
