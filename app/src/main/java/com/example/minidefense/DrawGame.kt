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

val TUFTS = listOf(
    0.06f to 0.10f, 0.94f to 0.16f, 0.06f to 0.38f, 0.94f to 0.44f,
    0.06f to 0.72f, 0.94f to 0.74f, 0.24f to 0.98f, 0.60f to 0.24f,
    0.64f to 0.62f, 0.30f to 0.40f
)

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
        for (t in TUFTS) {
            val x = px(t.first); val y = py(t.second)
            for (a in -1..1) {
                drawLine(Pal.Tuft, Offset(x + a * 0.006f * sc, y), Offset(x + a * 0.013f * sc, y - 0.015f * sc), 0.0045f * sc, cap = StrokeCap.Round)
            }
        }
        val p = Path()
        engine.level.path.forEachIndexed { i, pt ->
            if (i == 0) p.moveTo(px(pt.first), py(pt.second)) else p.lineTo(px(pt.first), py(pt.second))
        }
        drawPath(p, Pal.TanDark, style = Stroke(0.13f * sc, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, Pal.Sand, style = Stroke(0.10f * sc, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, Pal.TanDark, style = Stroke(0.008f * sc, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.02f * sc, 0.06f * sc), 0f)))
        drawCircle(Pal.HudBrown, radius = 0.075f * sc, center = Offset(px(engine.level.path[0].first), py(engine.level.path[0].second)))
        drawCircle(Pal.Ink, radius = 0.05f * sc, center = Offset(px(engine.level.path[0].first), py(engine.level.path[0].second)))
        cartoonCastle(this, px(engine.level.base.first), py(engine.level.base.second), sc * 1.25f, Pal.Tan, Pal.Roof, false)
        drawHeart(this, px(engine.level.base.first), py(engine.level.base.second - 0.16f), 0.016f * sc, Pal.Danger)

        engine.level.spots.forEachIndexed { i, s ->
            if (engine.towerAt(i) == null) {
                drawCircle(
                    Color.White.copy(alpha = 0.6f), radius = 0.055f * sc,
                    center = Offset(px(s.first), py(s.second)),
                    style = Stroke(0.008f * sc, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.03f * sc, 0.02f * sc), 0f))
                )
                if (engine.placing != null) {
                    drawCircle(Pal.Primary.copy(alpha = 0.3f), radius = 0.055f * sc, center = Offset(px(s.first), py(s.second)))
                }
            }
        }

        engine.towerAt(engine.selectedSpot)?.let { t ->
            drawCircle(Pal.Ink.copy(alpha = 0.12f), radius = t.range * sc, center = Offset(px(t.x), py(t.y)))
            drawCircle(Pal.Ink.copy(alpha = 0.35f), radius = t.range * sc, center = Offset(px(t.x), py(t.y)), style = Stroke(0.006f * sc))
        }

        for (t in engine.towers) {
            val cx = px(t.x); val cy = py(t.y)
            when (t.type) {
                TowerType.ARROW -> cartoonCastle(this, cx, cy, sc, Pal.Tan, Pal.Roof, false)
                TowerType.CANNON -> cartoonCannon(this, cx, cy, sc)
                TowerType.ICE -> cartoonCastle(this, cx, cy, sc, Pal.IceBody, Pal.Roof, true)
            }
            for (i in 0 until t.level - 1) {
                drawCircle(Pal.Primary, radius = 0.008f * sc, center = Offset(cx - 0.02f * sc + i * 0.016f * sc, cy + 0.065f * sc))
            }
            if (t.level >= 3) {
                drawCircle(Pal.Secondary, radius = 0.010f * sc, center = Offset(cx + 0.030f * sc, cy + 0.065f * sc))
            }
        }

        for (e in engine.enemies) {
            val r = e.radius * sc
            val wob = sin(engine.time * 8f + e.dist * 30f) * 0.08f
            val cx = px(e.x); val cy = py(e.y)
            if (e.kind == 4) {
                for (k in 0..1) {
                    drawLine(
                        Color.White.copy(alpha = 0.6f),
                        Offset(cx - r * 1.6f, cy - r * 0.3f + k * r * 0.6f),
                        Offset(cx - r * 2.3f, cy - r * 0.3f + k * r * 0.6f),
                        r * 0.12f, cap = StrokeCap.Round
                    )
                }
            }
            cartoonBlob(this, cx, cy, r, e.color, Enemy.OUTLINES[e.kind], wob)
            if (e.kind == 5) {
                drawArc(Pal.Metal, 180f, 180f, false, topLeft = Offset(cx - r * 0.85f, cy - r * 1.02f), size = Size(r * 1.7f, r * 1.1f), style = Stroke(r * 0.28f))
                drawCircle(Pal.Outline, r * 0.10f, Offset(cx, cy - r * 1.05f))
            }
            if (e.kind == 3) {
                val cr = Path()
                cr.moveTo(cx - r * 0.6f, cy - r * 1.0f)
                cr.lineTo(cx - r * 0.6f, cy - r * 1.5f)
                cr.lineTo(cx - r * 0.3f, cy - r * 1.15f)
                cr.lineTo(cx, cy - r * 1.6f)
                cr.lineTo(cx + r * 0.3f, cy - r * 1.15f)
                cr.lineTo(cx + r * 0.6f, cy - r * 1.5f)
                cr.lineTo(cx + r * 0.6f, cy - r * 1.0f)
                cr.close()
                drawPath(cr, Pal.Primary)
                drawPath(cr, Pal.Outline, style = Stroke(r * 0.08f))
            }
            if (e.slow > 0) drawCircle(Pal.IceDark.copy(alpha = 0.6f), r * 1.3f, Offset(cx, cy), style = Stroke(0.006f * sc))
            if (e.hp < e.hpMax) {
                drawRoundRect(Pal.HudBrown.copy(alpha = 0.5f), topLeft = Offset(cx - r, cy - r - 0.024f * sc), size = Size(r * 2, 0.014f * sc), cornerRadius = CornerRadius(0.007f * sc))
                drawRoundRect(Pal.Success, topLeft = Offset(cx - r, cy - r - 0.024f * sc), size = Size(r * 2 * (e.hp / e.hpMax), 0.014f * sc), cornerRadius = CornerRadius(0.007f * sc))
            }
        }

        for (s in engine.shots) {
            val cx = px(s.x); val cy = py(s.y)
            when (s.tower.type) {
                TowerType.ARROW -> {
                    drawCircle(Pal.Outline, 0.012f * sc, Offset(cx, cy))
                    drawCircle(Pal.Secondary, 0.009f * sc, Offset(cx, cy))
                }
                TowerType.CANNON -> {
                    drawCircle(Pal.Outline, 0.016f * sc, Offset(cx, cy))
                    drawCircle(Pal.Ink, 0.012f * sc, Offset(cx, cy))
                }
                TowerType.ICE -> {
                    drawCircle(Pal.IceDark, 0.012f * sc, Offset(cx, cy))
                    drawCircle(Pal.White, 0.006f * sc, Offset(cx, cy))
                }
            }
        }

        for (pp in engine.pops) {
            val k = 1f - pp.t / 0.3f
            drawCircle(Color.White.copy(alpha = pp.t / 0.3f), radius = (0.02f + k * 0.05f) * sc, center = Offset(px(pp.x), py(pp.y)), style = Stroke(0.008f * sc))
        }

        for (pt in engine.parts) {
            val a = pt.t / 0.45f
            drawCircle(pt.color.copy(alpha = a), radius = (0.004f + 0.010f * a) * sc, center = Offset(px(pt.x), py(pt.y)))
        }
    }
}

fun cartoonCastle(d: DrawScope, cx: Float, cy: Float, sc: Float, body: Color, roof: Color, ice: Boolean) {
    val wTop = 0.060f * sc; val wBot = 0.080f * sc; val bh = 0.085f * sc
    val top = cy - bh / 2; val bot = cy + bh / 2
    val bodyPath = Path()
    bodyPath.moveTo(cx - wTop / 2, top); bodyPath.lineTo(cx + wTop / 2, top)
    bodyPath.lineTo(cx + wBot / 2, bot); bodyPath.lineTo(cx - wBot / 2, bot); bodyPath.close()
    d.drawPath(bodyPath, body)
    d.drawPath(bodyPath, Pal.Outline, style = Stroke(0.006f * sc))
    d.drawLine(Pal.Outline.copy(alpha = 0.3f), Offset(cx - wBot * 0.35f, cy - bh * 0.12f), Offset(cx + wBot * 0.35f, cy - bh * 0.12f), 0.004f * sc)
    d.drawLine(Pal.Outline.copy(alpha = 0.3f), Offset(cx - wBot * 0.38f, cy + bh * 0.18f), Offset(cx + wBot * 0.38f, cy + bh * 0.18f), 0.004f * sc)
    val rimY = top - 0.014f * sc
    d.drawRoundRect(body, topLeft = Offset(cx - wTop * 0.7f, rimY), size = Size(wTop * 1.4f, 0.016f * sc), cornerRadius = CornerRadius(0.004f * sc))
    d.drawRoundRect(Pal.Outline, topLeft = Offset(cx - wTop * 0.7f, rimY), size = Size(wTop * 1.4f, 0.016f * sc), cornerRadius = CornerRadius(0.004f * sc), style = Stroke(0.005f * sc))
    for (i in -1..1) {
        d.drawRoundRect(body, topLeft = Offset(cx + i * wTop * 0.55f - 0.008f * sc, rimY - 0.011f * sc), size = Size(0.016f * sc, 0.014f * sc), cornerRadius = CornerRadius(0.003f * sc))
        d.drawRoundRect(Pal.Outline, topLeft = Offset(cx + i * wTop * 0.55f - 0.008f * sc, rimY - 0.011f * sc), size = Size(0.016f * sc, 0.014f * sc), cornerRadius = CornerRadius(0.003f * sc), style = Stroke(0.004f * sc))
    }
    if (!ice) {
        val roofPath = Path()
        roofPath.moveTo(cx, rimY - 0.055f * sc)
        roofPath.lineTo(cx + wTop * 0.85f, rimY - 0.002f * sc)
        roofPath.lineTo(cx - wTop * 0.85f, rimY - 0.002f * sc)
        roofPath.close()
        d.drawPath(roofPath, roof)
        d.drawPath(roofPath, Pal.Outline, style = Stroke(0.006f * sc))
        d.drawLine(Pal.Outline, Offset(cx, rimY - 0.055f * sc), Offset(cx, rimY - 0.085f * sc), 0.005f * sc)
        val fp = Path()
        fp.moveTo(cx, rimY - 0.085f * sc); fp.lineTo(cx + 0.028f * sc, rimY - 0.077f * sc); fp.lineTo(cx, rimY - 0.069f * sc); fp.close()
        d.drawPath(fp, Pal.FlagRed)
    } else {
        val crystals = listOf(-0.018f to 0.030f, 0f to 0.046f, 0.018f to 0.028f)
        for (c in crystals) {
            val cp = Path()
            cp.moveTo(cx + c.first * sc, rimY - c.second * sc)
            cp.lineTo(cx + c.first * sc + 0.012f * sc, rimY)
            cp.lineTo(cx + c.first * sc - 0.012f * sc, rimY)
            cp.close()
            d.drawPath(cp, Pal.IceDark)
            d.drawPath(cp, Pal.Outline, style = Stroke(0.005f * sc))
        }
    }
    val dw = 0.024f * sc; val dh = 0.032f * sc
    d.drawRoundRect(Pal.HudBrown, topLeft = Offset(cx - dw / 2, bot - dh), size = Size(dw, dh), cornerRadius = CornerRadius(dw / 2))
    val eyeY = cy - 0.006f * sc
    d.drawCircle(Pal.Ink, 0.0055f * sc, Offset(cx - 0.014f * sc, eyeY))
    d.drawCircle(Pal.Ink, 0.0055f * sc, Offset(cx + 0.014f * sc, eyeY))
    d.drawArc(Pal.Ink, 20f, 140f, false, topLeft = Offset(cx - 0.008f * sc, cy - 0.002f * sc), size = Size(0.016f * sc, 0.012f * sc), style = Stroke(0.004f * sc))
    d.drawCircle(Color(0xFFFF8A80), 0.0075f * sc, Offset(cx - 0.025f * sc, cy + 0.006f * sc))
    d.drawCircle(Color(0xFFFF8A80), 0.0075f * sc, Offset(cx + 0.025f * sc, cy + 0.006f * sc))
}

fun cartoonCannon(d: DrawScope, cx: Float, cy: Float, sc: Float) {
    val car = Path()
    car.moveTo(cx - 0.030f * sc, cy + 0.032f * sc); car.lineTo(cx + 0.030f * sc, cy + 0.032f * sc)
    car.lineTo(cx + 0.020f * sc, cy); car.lineTo(cx - 0.020f * sc, cy); car.close()
    d.drawPath(car, Pal.Wood)
    d.drawPath(car, Pal.Outline, style = Stroke(0.006f * sc))
    d.drawCircle(Pal.HudBrown, 0.013f * sc, Offset(cx - 0.018f * sc, cy + 0.032f * sc))
    d.drawCircle(Pal.HudBrown, 0.013f * sc, Offset(cx + 0.018f * sc, cy + 0.032f * sc))
    val bx = cx + 0.045f * sc; val by = cy - 0.034f * sc
    d.drawLine(Pal.Outline, Offset(cx, cy - 0.006f * sc), Offset(bx, by - 0.006f * sc), strokeWidth = 0.030f * sc, cap = StrokeCap.Round)
    d.drawLine(Pal.Metal, Offset(cx, cy - 0.006f * sc), Offset(bx, by - 0.006f * sc), strokeWidth = 0.022f * sc, cap = StrokeCap.Round)
    d.drawCircle(Pal.Outline, 0.015f * sc, Offset(bx, by - 0.006f * sc))
    d.drawCircle(Pal.Secondary, 0.011f * sc, Offset(bx, by - 0.006f * sc))
}

fun cartoonBlob(d: DrawScope, cx: Float, cy: Float, r: Float, fill: Color, outline: Color, wob: Float) {
    d.drawOval(outline, topLeft = Offset(cx - r * 1.14f * (1 + wob), cy - r * 1.14f * (1 - wob)), size = Size(2 * r * 1.14f * (1 + wob), 2 * r * 1.14f * (1 - wob)))
    d.drawCircle(outline, r * 0.42f, Offset(cx - r * 0.72f, cy - r * 0.62f))
    d.drawCircle(outline, r * 0.42f, Offset(cx + r * 0.72f, cy - r * 0.62f))
    d.drawOval(fill, topLeft = Offset(cx - r * (1 + wob), cy - r * (1 - wob)), size = Size(2 * r * (1 + wob), 2 * r * (1 - wob)))
    d.drawCircle(fill, r * 0.30f, Offset(cx - r * 0.72f, cy - r * 0.62f))
    d.drawCircle(fill, r * 0.30f, Offset(cx + r * 0.72f, cy - r * 0.62f))
    d.drawCircle(Pal.Ink, r * 0.14f, Offset(cx - r * 0.32f, cy - r * 0.08f))
    d.drawCircle(Pal.Ink, r * 0.14f, Offset(cx + r * 0.32f, cy - r * 0.08f))
    d.drawArc(Pal.Ink, 20f, 140f, false, topLeft = Offset(cx - r * 0.15f, cy + r * 0.10f), size = Size(r * 0.30f, r * 0.22f), style = Stroke(r * 0.09f))
    d.drawCircle(Color(0xFFFF8A80), r * 0.16f, Offset(cx - r * 0.58f, cy + r * 0.16f))
    d.drawCircle(Color(0xFFFF8A80), r * 0.16f, Offset(cx + r * 0.58f, cy + r * 0.16f))
}

fun drawHeart(d: DrawScope, cx: Float, cy: Float, r: Float, color: Color) {
    d.drawCircle(color, r, Offset(cx - r * 0.9f, cy - r * 0.6f))
    d.drawCircle(color, r, Offset(cx + r * 0.9f, cy - r * 0.6f))
    val p = Path()
    p.moveTo(cx - r * 1.8f, cy - r * 0.2f); p.lineTo(cx + r * 1.8f, cy - r * 0.2f); p.lineTo(cx, cy + r * 1.8f); p.close()
    d.drawPath(p, color)
}
