package com.qingning.cloudrest.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.UiTransit
import com.qingning.cloudrest.ui.theme.BackCircleBg
import com.qingning.cloudrest.ui.theme.CardBorderColor
import com.qingning.cloudrest.ui.theme.CardSurfaceBrush
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 点击动效：按压缩放回弹 + 触感反馈（可在设置中关闭） */
@Composable
fun Modifier.bounceClickable(onClick: () -> Unit): Modifier {
    val scale = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    return this
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    val press = coroutineScope { launch { scale.animateTo(0.93f, tween(70)) } }
                    val released = tryAwaitRelease()
                    press.cancel()
                    if (released) {
                        if (AppSettings.hapticsOn) {
                            val type = if (AppSettings.hapticsLevel == "light") HapticFeedbackType.TextHandleMove
                            else HapticFeedbackType.LongPress
                            haptic.performHapticFeedback(type)
                        }
                        onClick()
                    }
                    coroutineScope {
                        launch { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) }
                    }
                }
            )
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}

/** 采样解码，防止大图 OOM */
fun decodeSampledBitmap(path: String, maxSize: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Exception) {
        null
    }
}

/** 晚霞动态背景：柔和渐变 + 缓慢漂移的柔光斑 */
@Composable
fun Backdrop(modifier: Modifier = Modifier) {
    // 手驱 ~15fps 缓慢漂移：远低于逐帧动画的重绘开销；转场进行中自动冻结，避免掉帧
    var shift by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var phase = 0f
        while (true) {
            delay(66)
            if (!UiTransit.busy) {
                phase = (phase + 66f / 32000f) % 1f
                shift = (0.5f - 0.5f * cos(phase * 2f * PI)).toFloat()
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val night = AppSettings.deepNight
        drawRect(
            if (night) Brush.verticalGradient(listOf(Color(0xFF1D1830), Color(0xFF2A223A)))
            else Brush.verticalGradient(listOf(Color(0xFFFBF3EF), Color(0xFFF3E6F4)))
        )
        val w = size.width
        val h = size.height
        drawCircle(
            Brush.radialGradient(listOf(Color(0x4DFFC9A3), Color.Transparent)),
            radius = w * 0.65f,
            center = androidx.compose.ui.geometry.Offset(w * (0.15f + 0.35f * shift), h * 0.12f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0x40A78BDA), Color.Transparent)),
            radius = w * 0.75f,
            center = androidx.compose.ui.geometry.Offset(w * (0.85f - 0.35f * shift), h * 0.38f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0x45F0A8BC), Color.Transparent)),
            radius = w * 0.6f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * (0.82f + 0.08f * shift)),
        )
    }
}

/** 全局背景：优先自定义图片（可模糊 + 遮罩），否则默认晚霞渐变 */
@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val path = AppSettings.bgPath
    val blurV = AppSettings.blur
    val dim = AppSettings.dim
    val bmp = if (path != null && File(path).exists()) {
        remember(path) { decodeSampledBitmap(path, 1600)?.asImageBitmap() }
    } else {
        null
    }
    if (bmp != null) {
        Box(modifier.fillMaxSize()) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .let { m ->
                        if (Build.VERSION.SDK_INT >= 31 && blurV > 0.01f) m.blur((blurV * 80f).dp) else m
                    },
            )
            Box(Modifier.fillMaxSize().background((if (AppSettings.deepNight) Color(0xFF151020) else Color.White).copy(alpha = dim)))
        }
    } else {
        Backdrop(modifier)
    }
}

/** 柔和卡片 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    brush: Brush = CardSurfaceBrush,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .clip(shape)
            .background(brush)
            .border(1.dp, CardBorderColor, shape)
            .padding(20.dp),
        content = content,
    )
}

/** 区块小标题 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Ink,
        modifier = modifier.padding(start = 4.dp),
    )
}

/** 玩法页返回按钮 */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(12.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(BackCircleBg)
            .bounceClickable(onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
    }
}

/** 顶部玩法标题栏（自动避开状态栏）；icon 非空时优先使用自定义图标 */
@Composable
fun PlayHeader(title: String, emoji: String?, onBack: () -> Unit, icon: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onBack)
        Spacer(Modifier.width(8.dp))
        if (icon != null) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
        } else {
            Text(
                if (emoji.isNullOrEmpty()) title else "$emoji $title",
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
            )
        }
    }
}

/** 大段提示文案 */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 13.sp,
        color = SubInk,
        lineHeight = 20.sp,
        modifier = modifier,
    )
}