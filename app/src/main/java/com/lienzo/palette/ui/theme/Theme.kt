package com.lienzo.palette.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 莱茵蓝（仅用于 splash 品牌页与静态兜底配色，UI 高亮色默认跟随系统 Material You）
val RhineBlue = Color(0xFF16369F)
val RhineBlueLight = Color(0xFF4C63D8)

private val LightColors = lightColorScheme(
    primary = RhineBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE2FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72),
    surface = Color(0xFFFCF8FF),
    background = Color(0xFFFCF8FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF00228C),
    primaryContainer = RhineBlueLight,
    onPrimaryContainer = Color(0xFFDEE2FF),
    secondary = Color(0xFFC4C4DD),
    surface = Color(0xFF131318),
    background = Color(0xFF131318),
)

/**
 * UI 高亮色跟随系统：Android 12+ 使用 Material You 动态取色（动态色板里不含 splash 的莱茵蓝，
 * 因此 splash 不受影响，仍保持莱茵蓝）；低版本或取色失败时回退到内置莱茵蓝配色。
 */
@Composable
fun LienzoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
