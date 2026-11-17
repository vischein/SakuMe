package com.sakume.personalfinance.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

const val DESIGN_WIDTH_DP = 411f

private const val MIN_SCALE = 0.85f
private const val MAX_SCALE = 1.30f

@Composable
fun NormalizedDensity(content: @Composable () -> Unit) {
    val system = LocalDensity.current
    val smallestWidthDp = LocalConfiguration.current.smallestScreenWidthDp.toFloat()
    val scale = if (smallestWidthDp > 0f) {
        (smallestWidthDp / DESIGN_WIDTH_DP).coerceIn(MIN_SCALE, MAX_SCALE)
    } else 1f
        val normalized = Density(
        density = system.density * scale,
        fontScale = 1f
    )
    CompositionLocalProvider(LocalDensity provides normalized, content = content)
}
