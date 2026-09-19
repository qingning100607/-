package com.qingning.cloudrest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.UiTransit
import com.qingning.cloudrest.ui.components.AppBackdrop
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.screens.BreathingScreen
import com.qingning.cloudrest.ui.screens.BubbleWrapScreen
import com.qingning.cloudrest.ui.screens.CialloScreen
import com.qingning.cloudrest.ui.screens.FireworkScreen
import com.qingning.cloudrest.ui.screens.FortuneScreen
import com.qingning.cloudrest.ui.screens.HomeScreen
import com.qingning.cloudrest.ui.screens.IceBreakScreen
import com.qingning.cloudrest.ui.screens.KarmaCalendarScreen
import com.qingning.cloudrest.ui.screens.MoodScreen
import com.qingning.cloudrest.ui.screens.MuyuScreen
import com.qingning.cloudrest.ui.screens.RelaxScreen
import com.qingning.cloudrest.ui.screens.SettingsScreen
import com.qingning.cloudrest.ui.screens.ShredderScreen
import com.qingning.cloudrest.ui.theme.CloudTheme
import com.qingning.cloudrest.ui.theme.GlassBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.SunsetBrush
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        AppSettings.init(this)
        SoundEngine.init(applicationContext)
        CrashGuard.install(applicationContext)
        enableEdgeToEdge()
        setContent {
            CloudTheme {
                CloudApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SoundEngine.warm()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 自然声仍在播放时保持引擎存活（前台服务负责后台播放）
        if (!SoundEngine.anyOn()) SoundEngine.release()
    }
}

enum class PlayPage { NONE, BUBBLE, MUYU, FIREWORK, ICE, SHRED, KARMA, BREATH, FORTUNE }

@Composable
fun CloudApp() {
    var tab by remember { mutableStateOf(0) }
    var play by remember { mutableStateOf(PlayPage.NONE) }
    var settingsOpen by remember { mutableStateOf(false) }
    val overlayOpen = settingsOpen || play != PlayPage.NONE

    val scope = rememberCoroutineScope()
    // 图层横向偏移（px）：0 = 完全显示，宽 = 滑出屏幕右侧
    val offsetX = remember { Animatable(0f) }
    var widthPx by remember { mutableStateOf(0f) }

    fun width(): Float = widthPx.coerceAtLeast(1f)

    fun closeOverlay() {
        scope.launch {
            UiTransit.busy = true
            try {
                offsetX.animateTo(width(), tween(280, easing = FastOutSlowInEasing))
                settingsOpen = false
                play = PlayPage.NONE
                offsetX.snapTo(0f)
            } finally {
                UiTransit.busy = false
            }
        }
    }

    fun openOverlay(open: () -> Unit) {
        SoundEngine.warm()
        scope.launch {
            UiTransit.busy = true
            try {
                offsetX.snapTo(width())
                open()
                offsetX.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
            } finally {
                UiTransit.busy = false
            }
        }
    }

    // 普通返回：非首页 Tab → 回休息厅（不带滑动层的场景）
    BackHandler(enabled = !overlayOpen && tab != 0) { tab = 0 }

    // 预测性返回：设置/玩法页手势跟手滑动，松手取消自动回弹
    PredictiveBackHandler(enabled = overlayOpen) { progress ->
        UiTransit.busy = true
        try {
            progress.collect { e ->
                if (e.progress < 0.995f) {
                    offsetX.snapTo(e.progress * width())
                }
            }
            // 手势完成：若已随手指滑出大半，直接收尾；否则补一段动画
            if (offsetX.value < width() * 0.95f) {
                closeOverlay()
            } else {
                settingsOpen = false
                play = PlayPage.NONE
                scope.launch { offsetX.snapTo(0f) }
            }
        } catch (e: CancellationException) {
            // 手势取消：平滑归位
            scope.launch { offsetX.animateTo(0f, tween(220, easing = FastOutSlowInEasing)) }
            throw e
        } finally {
            UiTransit.busy = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        // ===== 主界面（底层） =====
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = if (!overlayOpen) {
                        0f
                    } else {
                        (1f - offsetX.value / widthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    }
                    translationX = -widthPx * 0.16f * p
                }
        ) {
            AppBackdrop()
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(tween(280)) { it / 5 } + fadeIn(tween(280))) togetherWith
                                    (slideOutHorizontally(tween(280)) { -it / 5 } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally(tween(280)) { -it / 5 } + fadeIn(tween(280))) togetherWith
                                    (slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(200)))
                            }
                        },
                        label = "tabContent",
                    ) { t ->
                        when (t) {
                            0 -> HomeScreen(
                                onOpenKarmaCal = { openOverlay { play = PlayPage.KARMA } },
                                onOpenFortune = { openOverlay { play = PlayPage.FORTUNE } },
                            )
                            1 -> RelaxScreen { page -> openOverlay { play = page } }
                            2 -> CialloScreen()
                            else -> MoodScreen()
                        }
                    }
                }
                BottomBar(tab) { tab = it; SoundEngine.warm() }
            }
            // 右上角设置入口
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GlassBg)
                    .bounceClickable { openOverlay { settingsOpen = true } },
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙️", fontSize = 19.sp)
            }
        }

        // ===== 遮罩（随图层进度渐变） =====
        if (overlayOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.22f * (1f - offsetX.value / widthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    }
                    .background(Color.Black)
            )
        }

        // ===== 图层：设置 / 玩法页 =====
        if (overlayOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = offsetX.value }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { },
            ) {
                when {
                    settingsOpen -> SettingsScreen(onBack = { closeOverlay() })
                    play == PlayPage.BUBBLE -> BubbleWrapScreen { closeOverlay() }
                    play == PlayPage.MUYU -> MuyuScreen { closeOverlay() }
                    play == PlayPage.FIREWORK -> FireworkScreen { closeOverlay() }
                    play == PlayPage.ICE -> IceBreakScreen { closeOverlay() }
                    play == PlayPage.SHRED -> ShredderScreen { closeOverlay() }
                    play == PlayPage.KARMA -> KarmaCalendarScreen { closeOverlay() }
                    play == PlayPage.BREATH -> BreathingScreen { closeOverlay() }
                    play == PlayPage.FORTUNE -> FortuneScreen { closeOverlay() }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("☁️", "休息厅", 0),
        Triple("🫧", "解压铺", 1),
        Triple("⭐", "CIALLO", 2),
        Triple("💗", "心情站", 3),
    )
    val density = LocalDensity.current
    var barW by remember { mutableStateOf(0f) }
    var barH by remember { mutableStateOf(0f) }
    var dragX by remember { mutableStateOf<Float?>(null) }
    var visualSel by remember { mutableStateOf(selected) }
    var settleTick by remember { mutableStateOf(0) }
    var pendingEnd by remember { mutableStateOf<Float?>(null) }
    val slider = remember { Animatable(0f) } // 滑块左缘 x（栏内坐标, px）

    val padPx = with(density) { 6.dp.toPx() }
    val slotW = if (barW > 0f) (barW - padPx * 2f) / 4f else 0f
    fun slotLeft(i: Int) = padPx + slotW * i
    fun nearest(centerX: Float): Int =
        if (slotW <= 0f) 0 else ((centerX - padPx) / slotW).toInt().coerceIn(0, 3)

    // selected 变化 / 松手吸附：滑块动画归位
    LaunchedEffect(selected, slotW, settleTick) {
        if (slotW <= 0f) return@LaunchedEffect
        visualSel = selected
        pendingEnd?.let { slider.snapTo(it); pendingEnd = null }
        slider.animateTo(slotLeft(selected), tween(260, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(GlassBg)
            .onSizeChanged { barW = it.width.toFloat(); barH = it.height.toFloat() }
            .pointerInput(slotW) {
                if (slotW <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { pos ->
                        val x = (pos.x - slotW / 2f).coerceIn(padPx, padPx + slotW * 3f)
                        dragX = x
                        visualSel = nearest(x + slotW / 2f)
                    },
                    onDragEnd = {
                        pendingEnd = dragX
                        dragX = null
                        onSelect(visualSel)
                        settleTick++
                    },
                    onDragCancel = {
                        pendingEnd = dragX
                        dragX = null
                        settleTick++
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val cur = dragX ?: slider.value
                    val x = (cur + dragAmount).coerceIn(padPx, padPx + slotW * 3f)
                    dragX = x
                    visualSel = nearest(x + slotW / 2f)
                }
            },
    ) {
        // 滑块指示器
        if (slotW > 0f) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .width(with(density) { slotW.toDp() })
                    .height(with(density) { (barH - padPx * 2f).coerceAtLeast(0f).toDp() })
                    .graphicsLayer {
                        translationX = (dragX ?: slider.value) - padPx
                        val dragging = dragX != null
                        scaleX = if (dragging) 1.03f else 1f
                        scaleY = scaleX
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(SunsetBrush),
            )
        }
        // 选项
        Row(
            Modifier
                .fillMaxWidth()
                .padding(6.dp),
        ) {
            items.forEach { (emoji, label, idx) ->
                val sel = idx == visualSel
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .bounceClickable { onSelect(idx) }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(emoji, fontSize = 18.sp)
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = if (sel) Color.White else SubInk,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}