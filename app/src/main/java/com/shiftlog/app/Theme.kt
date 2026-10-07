package com.shiftlog.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Teal = Color(0xFF0F4C5C)
private val Orange = Color(0xFFE36414)

private val Light = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8EC), onPrimaryContainer = Color(0xFF062A33),
    secondary = Orange, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0CC), onSecondaryContainer = Color(0xFF3B1500),
    background = Color(0xFFF4F7F8), onBackground = Color(0xFF14201F),
    surface = Color.White, onSurface = Color(0xFF14201F),
    surfaceVariant = Color(0xFFE6EDEF), onSurfaceVariant = Color(0xFF44555A),
    outline = Color(0xFF8A9A9F), error = Color(0xFFC62828)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FCBD8), onPrimary = Color(0xFF00252D),
    primaryContainer = Color(0xFF0F4C5C), onPrimaryContainer = Color(0xFFD4E8EC),
    secondary = Color(0xFFFFB68A), onSecondary = Color(0xFF4A1C00),
    secondaryContainer = Color(0xFF6B2D00), onSecondaryContainer = Color(0xFFFFE0CC),
    background = Color(0xFF0E1517), onBackground = Color(0xFFE1EAEC),
    surface = Color(0xFF162023), onSurface = Color(0xFFE1EAEC),
    surfaceVariant = Color(0xFF263337), onSurfaceVariant = Color(0xFFB4C4C8),
    outline = Color(0xFF7A8A8F)
)

private val Typo = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

private val Shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ShiftLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Typo,
        shapes = Shapes,
        content = content
    )
}
