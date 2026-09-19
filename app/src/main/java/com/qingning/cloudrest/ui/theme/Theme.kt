package com.qingning.cloudrest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.ui.AppSettings

// 晚霞色板（基础色）
val Lilac = Color(0xFFA78BDA)
val LilacSoft = Color(0xFFC9B8F0)
val RosePink = Color(0xFFF0A8BC)
val Peach = Color(0xFFFFC9A3)
val SkyBlue = Color(0xFFA8C8F0)
val MintGreen = Color(0xFF9BD5B8)
val CardWhite = Color(0xFFFEFBFA)
val DuskBg = Color(0xFFFDF3F5)
val NightIndigo = Color(0xFF2B2340)
val NightPurple = Color(0xFF45345E)

/** 夜间模式开关（跟随设置，响应式重绘） */
val isNight: Boolean get() = AppSettings.deepNight

// ===== 主题感知色：浅色 / 夜间自动切换 =====
val Ink: Color get() = if (isNight) Color(0xFFEDE7F4) else Color(0xFF4A3F4F)
val SubInk: Color get() = if (isNight) Color(0xFFA99CBC) else Color(0xFF8A7E90)
val ChipBg: Color get() = if (isNight) Color(0x24FFFFFF) else Color(0x14000000)
val GlassBg: Color get() = if (isNight) Color(0xE8302846) else Color(0xE8FFFFFF)
val CardBorderColor: Color get() = if (isNight) Color(0x30FFFFFF) else Color(0x38FFFFFF)
val BackCircleBg: Color get() = if (isNight) Color(0xB33A3150) else Color(0xB3FFFFFF)
val PlaceholderInk: Color get() = SubInk.copy(alpha = 0.55f)

/** 卡片表面：浅色白瓷 / 夜间深紫玻璃 */
val CardSurfaceBrush: Brush get() = if (isNight) {
    Brush.linearGradient(listOf(Color(0xF23A3048), Color(0xE62E2740)))
} else {
    Brush.linearGradient(listOf(Color(0xF2FFFFFF), Color(0xE6FFFFFF)))
}

/** 页面背景：浅色沿用各页晚霞色调，夜间统一切到夜幕色 */
fun screenBg(top: Color, bottom: Color): Brush = if (isNight) {
    Brush.verticalGradient(listOf(Color(0xFF201A2E), Color(0xFF2A223A), Color(0xFF362B44)))
} else {
    Brush.verticalGradient(listOf(top, bottom))
}

val SunsetBrush = Brush.linearGradient(listOf(Lilac, RosePink, Peach))
val SunsetBrushSoft = Brush.linearGradient(listOf(LilacSoft, RosePink.copy(alpha = 0.85f), Peach))
val NightSkyBrush = Brush.verticalGradient(listOf(NightIndigo, NightPurple, Color(0xFF6E4A7A)))
val CardBrush = Brush.linearGradient(listOf(Color(0xF5FFFFFF), Color(0xE8FFFFFF)))

private val LightColors = lightColorScheme(
    primary = Lilac,
    onPrimary = Color.White,
    secondary = RosePink,
    onSecondary = Color.White,
    tertiary = Peach,
    background = DuskBg,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    onSurfaceVariant = SubInk,
)

private val DarkColors = darkColorScheme(
    primary = LilacSoft,
    onPrimary = Color(0xFF2B2340),
    secondary = RosePink,
    onSecondary = Color(0xFF3A2430),
    tertiary = Peach,
    background = Color(0xFF201A2E),
    onBackground = Color(0xFFEDE7F4),
    surface = Color(0xFF2E2740),
    onSurface = Color(0xFFEDE7F4),
    onSurfaceVariant = Color(0xFFA99CBC),
)

private val CloudShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

private val CloudType = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CloudTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isNight) DarkColors else LightColors,
        shapes = CloudShapes,
        typography = CloudType,
        content = content,
    )
}