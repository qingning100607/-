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
    Brush.verticalGradient(
        listOf(seasonBlend(Color(0xFF201A2E)), seasonBlend(Color(0xFF2A223A)), seasonBlend(Color(0xFF362B44)))
    )
} else {
    Brush.verticalGradient(listOf(seasonBlend(top), seasonBlend(bottom)))
}

/** 顶部羽化底色：与全局 Backdrop 顶部一致（首页类滚动页用） */
val BackdropTopColor: Color get() = seasonBlend(if (isNight) Color(0xFF1D1830) else Color(0xFFFBF3EF))

/** 顶部羽化底色：与自定义页面背景顶部一致（设置/成就等页面用） */
fun screenTopColor(top: Color): Color = if (isNight) seasonBlend(Color(0xFF201A2E)) else seasonBlend(top)

/** 季节主题名（off / spring / summer / autumn / winter） */
val seasonName: String get() = AppSettings.season

private fun seasonAccent(): Color? = when (AppSettings.season) {
    "spring" -> Color(0xFFF6B7CF)
    "summer" -> Color(0xFF9A8FD8)
    "autumn" -> Color(0xFFE8B36A)
    "winter" -> Color(0xFF9FC0E0)
    else -> null
}

/** 把季节色调融进任意颜色（保持透明度），用于背景与主题渐变 */
fun seasonBlend(c: Color, amount: Float = 0.22f): Color {
    val a = seasonAccent() ?: return c
    return Color(
        red = c.red * (1f - amount) + a.red * amount,
        green = c.green * (1f - amount) + a.green * amount,
        blue = c.blue * (1f - amount) + a.blue * amount,
        alpha = c.alpha,
    )
}

val SunsetBrush: Brush get() = Brush.linearGradient(
    listOf(seasonBlend(Lilac, 0.40f), seasonBlend(RosePink, 0.40f), seasonBlend(Peach, 0.40f))
)
val SunsetBrushSoft: Brush get() = Brush.linearGradient(
    listOf(seasonBlend(LilacSoft, 0.40f), seasonBlend(RosePink, 0.40f).copy(alpha = 0.85f), seasonBlend(Peach, 0.40f))
)
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