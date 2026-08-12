package com.alifeatseammo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SeaBlue80,
    secondary = SeaTeal80,
    tertiary = SandGold80,
    background = DeepSea,
    surface = DeepSea,
    onPrimary = DeepSea,
    onSecondary = DeepSea,
    onTertiary = DeepSea,
    onBackground = BeachSand,
    onSurface = BeachSand
)

private val LightColorScheme = lightColorScheme(
    primary = SeaBlue40,
    secondary = SeaTeal40,
    tertiary = SandGold40,
    background = BeachSand,
    surface = BeachSand,
    onPrimary = BeachSand,
    onSecondary = BeachSand,
    onTertiary = BeachSand,
    onBackground = DeepSea,
    onSurface = DeepSea
)

@Composable
fun ALifeAtSeaMMOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default for stronger nautical branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
