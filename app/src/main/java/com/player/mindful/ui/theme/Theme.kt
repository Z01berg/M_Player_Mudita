package com.player.mindful.ui.theme

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.eInkColorScheme
import com.mudita.mmd.eInkTypography

@Composable
fun PlayerAppTheme(content: @Composable () -> Unit) {
    ThemeMMD(colorScheme = eInkColorScheme, typography = eInkTypography, content = content)
}
