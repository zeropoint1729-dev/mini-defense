package com.example.minidefense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sin

fun scaleOf(w: Float, h: Float) = minOf(w, h / MapData.WORLD_H)
fun offXOf(w: Float, h: Float) = (w - scaleOf(w, h)) / 2f
fun offYOf(w: Float, h: Float) = (h - MapData.WORLD_H * scaleOf(w, h)) / 2f

@Composable
fun GameCanvas(engine: GameEngine, onTap: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    val w = size.width.toFloat(); val h = size.height.toFloat()
                    val sc = scaleOf(w, h)
                    onTap((off.x - offXOf(w, h)) / sc, (off.y - offYOf(w, h)) / sc)
                }
            }
    ) {
        val w = size.width; val h = size.height
        val sc = scaleOf(w, h); val ox = offXOf(w, h); val oy = offYOf(w, h)
        val px: (Float) -> Float = { ox + it * sc }
        val py: (Float) -> Float = { oy + it * sc }

        drawRect(Pal.Grass, size = Size(w, h))
        for (b in listOf(0.06f to 0.40f, 0.94f to 0.30f, 0.08f to 0.95f, 0.92f to 0.06f)) {
            drawCircle(Color(0xFF7CB86C), radius = 0.045f * sc, center = Offset(px(b.first), py(b.second)))
        }
        val p = Path()
        MapData.PATH.forEachIndexed { i, pt ->
            if (i == 0) p.moveTo(px(pt.first), py(pt.second)) else p.lineTo(px(pt.first), py(pt.second))
        }
        drawPath(p, Color(0xFFD9BE8C), style = Stroke(0.13f * sc, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, Pal.Sand, style = Stroke(0.10f * sc, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color(0xFF5B4A3C), radius = 0.07f * sc, center = Offset(px(0.5f), py(-0.02f)))
        drawCastle(this, px(0.40f), py(1.02f), sc * 1.3f, Pal.Panel, Pal.Danger)
        drawHeart(this, px(0.40f), py(0.94f), 0.018f * sc, Pal.Danger)

        MapData.SPOTS.forEachIndexed { i, s ->
            if (engine.towerAt(i) == null) {
                drawCircle(
                    Color.White.copy(alpha = 0.55f), radius = 0.055f * sc,
                    center = Offset(px(s.first), py(s.second)),
                    style = Stroke(0.008f * sc, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.03f * sc, 0.02f * sc), 0f))
                )
                if (engine.placing != null) {
                    drawCircle(Pal.Primary.copy(alpha = 0.25f), radius = 0.055f * sc, center = Offset(px(s.first), py(s.second)))
                }
            }
        }

        engine.towerAt(engine.selectedSpot)?.let { t ->
            drawCircle(Pal.Ink.copy(alpha = 0.12f), radius = t.range * sc, center = Offset(px(t.x), py(t.y)))
            drawCircle(Pal.Ink.copy(alpha = 0.35f), radius = t.range * sc, center = Offset(px(t.x), py(t.y)), style = Stroke(0.006f * sc))
        }

        for (t in engine.towers) drawTower(this, t, sc, px, py)

        for (e in engine.enemies) {
            val r = e.radius * sc
            val wob = sin(engine.time * 8f + e.dist * 30f) * 0.08f
            val cx = px(e.x); val cy = py(e.y)
            drawOval(e.color, topLeft = Offset(cx - r * (1 + wob), cy - r * (1 - wob)), size = Size(2 * r * (1 + wob), 2 * r * (1 - wob)))
            drawCircle(Pal.Ink, 0.008f * sc, Offset(cx - r * 0.35f, cy - r * 0.15f))
            drawCircle(Pal.Ink, 0.008f * sc, Offset(cx + r * 0.35f, cy - r * 0.15f))
            drawCircle(Color(0xFFFF8A80), 0.010f * sc, Offset(cx - r * 0.6f, cy + r * 0.15f))
            drawCircle(Color(0xFFFF8A80), 0.010f * sc, Offset(cx + r * 0.6f, cy + r * 0.15f))
            if (e.slow > 0) drawCircle(Color(0xFF81D4FA).copy(alpha = 0.5f), r * 1.25f, Offset(cx, cy), style = Stroke(0.006f * sc))
            if (e.hp < e.hpMax) {
                drawRoundRect(Pal.Ink.copy(alpha = 0.35f), topLeft = Offset(cx - r, cy - r - 0.022f * sc), size = Size(r * 2, 0.012f * sc), cornerRadius = CornerRadius(0.006f * sc))
                drawRoundRect(Pal.Success, topLeft = Offset(cx - r, cy - r - 0.022f * sc), size = Size(r * 2 * (e.hp / e.hpMax), 0.012f * sc), cornerRadius = CornerRadius(0.006f * sc))
            }
        }

        for (s in engine.shots) {
            val cx = px(s.x); val cy = py(s.y)
            when (s.tower.type) {
                TowerType.ARROW -> drawCircle(Pal.Secondary, 0.010f * sc, Offset(cx, cy))
                TowerType.CANNON -> drawCircle(Color(0xFF37474F), 0.014f * sc, Offset(cx, cy))
                TowerType.ICE -> {
                    drawCircle(Color(0xFF81D4FA), 0.011f * sc, Offset(cx, cy))
                    drawCircle(Color.White, 0.005f * sc, Offset(cx, cy))
                }
            }
        }

        for (pp in engine.pops) {
            val k = 1f - pp.t / 0.3f
            drawCircle(Color.White.copy(alpha = pp.t / 0.3f), radius = (0.02f + k * 0.05f) * sc, center = Offset(px(pp.x), py(pp.y)), style = Stroke(0.008f * sc))
        }
    }
}

private fun drawTower(d: DrawScope, t: Tower, sc: Float, px: (Float) -> Float, py: (Float) -> Float) {
    val cx = px(t.x); val cy = py(t.y)
    when (t.type) {
        TowerType.ARROW -> drawCastle(d, cx, cy, sc, Color(0xFFE8C07D), Pal.Danger)
        TowerType.CANNON -> {
            d.drawCircle(Color(0xFF8D6E63), radius = 0.045f * sc, center = Offset(cx, cy))
            d.drawCircle(Color(0xFF6D4C41), radius = 0.03f * sc, center = Offset(cx, cy))
            d.drawLine(Color(0xFF546E7A), Offset(cx, cy), Offset(cx + 0.05f * sc, cy - 0.045f * sc), strokeWidth = 0.022f * sc, cap = StrokeCap.Round)
        }
        TowerType.ICE -> {
            drawCastle(d, cx, cy, sc, Color(0xFFB3E5FC), Color(0xFF4FC3F7))
            val cr = Path()
            cr.moveTo(cx, cy - 0.085f * sc); cr.lineTo(cx - 0.02f * sc, cy - 0.05f * sc); cr.lineTo(cx + 0.02f * sc, cy - 0.05f * sc); cr.close()
            d.drawPath(cr, Color(0xFF81D4FA))
        }
    }
    for (i in 0 until t.level - 1) {
        d.drawCircle(Pal.Primary, radius = 0.008f * sc, center = Offset(cx - 0.02f * sc + i * 0.016f * sc, cy + 0.062f * sc))
    }
}

private fun drawCastle(d: DrawScope, cx: Float, cy: Float, sc: Float, body: Color, flag: Color) {
    val bw = 0.075f * sc; val bh = 0.085f * sc
    d.drawRoundRect(body, topLeft = Offset(cx - bw / 2, cy - bh / 2), size = Size(bw, bh), cornerRadius = CornerRadius(0.015f * sc))
    for (i in -1..1) {
        d.drawRoundRect(body, topLeft = Offset(cx + i * bw / 3 - 0.011f * sc, cy - bh / 2 - 0.018f * sc), size = Size(0.022f * sc, 0.022f * sc), cornerRadius = CornerRadius(0.006f * sc))
    }
    d.drawRoundRect(Color(0xFF5D4037), topLeft = Offset(cx - 0.014f * sc, cy + bh / 2 - 0.03f * sc), size = Size(0.028f * sc, 0.03f * sc), cornerRadius = CornerRadius(0.012f * sc))
    d.drawLine(Color(0xFF5D4037), Offset(cx, cy - bh / 2 - 0.018f * sc), Offset(cx, cy - bh / 2 - 0.05f * sc), strokeWidth = 0.006f * sc)
    val f = Path()
    f.moveTo(cx, cy - bh / 2 - 0.05f * sc); f.lineTo(cx + 0.03f * sc, cy - bh / 2 - 0.042f * sc); f.lineTo(cx, cy - bh / 2 - 0.034f * sc); f.close()
    d.drawPath(f, flag)
    d.drawCircle(Pal.Ink, radius = 0.006f * sc, center = Offset(cx - 0.016f * sc, cy - 0.008f * sc))
    d.drawCircle(Pal.Ink, radius = 0.006f * sc, center = Offset(cx + 0.016f * sc, cy - 0.008f * sc))
    d.drawCircle(Color(0xFFFF8A80), radius = 0.008f * sc, center = Offset(cx - 0.026f * sc, cy + 0.004f * sc))
    d.drawCircle(Color(0xFFFF8A80), radius = 0.008f * sc, center = Offset(cx + 0.026f * sc, cy + 0.004f * sc))
}

private fun drawHeart(d: DrawScope, cx: Float, cy: Float, r: Float, color: Color) {
    d.drawCircle(color, r, Offset(cx - r * 0.9f, cy - r * 0.6f))
    d.drawCircle(color, r, Offset(cx + r * 0.9f, cy - r * 0.6f))
    val p = Path()
    p.moveTo(cx - r * 1.8f, cy - r * 0.2f); p.lineTo(cx + r * 1.8f, cy - r * 0.2f); p.lineTo(cx, cy + r * 1.8f); p.close()
    d.drawPath(p, color)
}
