package com.qingning.cloudrest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** 迷你木鱼图标：标题栏、卡片等场景复用 */
@Composable
fun MuyuGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 100f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val bodyCy = cy + 4f * u
        val rx = 39f * u
        val ry = 34f * u
        val ringR = 13.5f * u
        val ringCy = bodyCy - ry + 3f * u

        // 顶部提环（下半会被鱼身盖住）
        drawArc(
            Color(0xFF4E2F18),
            160f, 260f, false,
            topLeft = Offset(cx - ringR, ringCy - ringR),
            size = Size(ringR * 2f, ringR * 2f),
            style = Stroke(width = 7.5f * u, cap = StrokeCap.Round),
        )
        drawArc(
            Color(0xFF8A5A3B),
            160f, 260f, false,
            topLeft = Offset(cx - ringR, ringCy - ringR),
            size = Size(ringR * 2f, ringR * 2f),
            style = Stroke(width = 4.8f * u, cap = StrokeCap.Round),
        )

        // 鱼身
        drawOval(
            Brush.radialGradient(
                listOf(Color(0xFFB5834F), Color(0xFF82522B), Color(0xFF573219)),
                center = Offset(cx - rx * 0.30f, bodyCy - ry * 0.34f),
                radius = rx * 1.55f,
            ),
            topLeft = Offset(cx - rx, bodyCy - ry),
            size = Size(rx * 2f, ry * 2f),
        )
        drawOval(
            Color(0x4D3E2A18),
            topLeft = Offset(cx - rx, bodyCy - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = 2.2f * u),
        )

        // 正面开口
        drawArc(
            Color(0xFF2B1809),
            46.5f, 87f, false,
            topLeft = Offset(cx - rx * 0.85f, bodyCy - ry * 1.08f),
            size = Size(rx * 1.7f, ry * 1.6f),
            style = Stroke(width = ry * 0.20f, cap = StrokeCap.Round),
        )

        // 高光
        drawArc(
            Color(0x30FFFFFF),
            205f, 60f, false,
            topLeft = Offset(cx - rx * 0.70f, bodyCy - ry * 0.62f),
            size = Size(rx * 1.40f, ry * 1.06f),
            style = Stroke(width = 3f * u, cap = StrokeCap.Round),
        )
    }
}