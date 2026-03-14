package dev.mslalith.focuslauncher.feature.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import dev.mslalith.focuslauncher.core.model.Theme
import dev.mslalith.focuslauncher.core.ui.controller.setSystemBarsColor
import dev.mslalith.focuslauncher.core.ui.providers.LocalSystemUiController
import dev.mslalith.focuslauncher.feature.theme.data.Typography
import dev.mslalith.focuslauncher.feature.theme.data.darkColors
import dev.mslalith.focuslauncher.feature.theme.data.lightColors

@Composable
fun LauncherTheme(
    currentTheme: Theme,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val systemUiController = LocalSystemUiController.current
    val colorScheme = when (currentTheme) {
        Theme.NOT_WHITE -> lightColors
        Theme.SAID_DARK -> darkColors
        Theme.FOLLOW_SYSTEM -> if (useDarkTheme) darkColors else lightColors
    }

    val currentColorScheme by rememberUpdatedState(newValue = colorScheme)
    LaunchedEffect(key1 = systemUiController, key2 = currentColorScheme) {
        try {
            systemUiController.setSystemBarsColor(color = currentColorScheme.surface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}