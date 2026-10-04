package com.example.minidefense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.hypot

@Composable
fun BattleScreen(levelIdx: Int, onMenu: () -> Unit) {
    var engine by remember { mutableStateOf(GameEngine(levelIdx)) }
    var frame by remember { mutableStateOf(0) }
    LaunchedEffect(engine) {
        engine.onSfx = { name -> if (Save.sound) Sfx.play(name); Haptics.buzz(name) }
        var last = System.nanoTime()
        while (true) {
            delay(16)
            val now = System.nanoTime()
            engine.tick(((now - last) / 1e9).toFloat().coerceIn(0.001f, 0.05f))
            last = now
            frame++
        }
    }
    val tick = frame
    Column(Modifier.fillMaxSize().background(Pal.Bg)) {
        HudBar(engine)
        Box(Modifier.weight(1f)) {
            GameCanvas(engine, onTap = { x, y -> tapGame(engine, x, y) })
            if (engine.betweenWaves()) {
                Text(
                    "Wave ${engine.wave + 1} in ${(engine.waveGap + 0.999f).toInt()}s",
                    color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                        .background(Pal.White, RoundedCornerShape(50)).border(2.dp, Pal.Outline, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            if (engine.paused && engine.status == 0) OverlayCard("Paused") {
                CartoonButton("Resume") { engine.paused = false }
                Spacer(Modifier.height(8.dp))
                CartoonButton("Restart") { engine = GameEngine(levelIdx) }
                Spacer(Modifier.height(8.dp))
                CartoonButton("Menu") { onMenu() }
            }
            if (engine.status == 1) OverlayCard("Victory! ${"★".repeat(if (engine.lives >= 10) 3 else if (engine.lives >= 6) 2 else 1)}") {
                CartoonButton("Next Level") { onMenu() }
                Spacer(Modifier.height(8.dp))
                CartoonButton("Play Again") { engine = GameEngine(levelIdx) }
            }
            if (engine.status == 2) OverlayCard("Defeat") {
                CartoonButton("Retry") { engine = GameEngine(levelIdx) }
                Spacer(Modifier.height(8.dp))
                CartoonButton("Menu") { onMenu() }
            }
        }
        TowerPopup(engine)
        TowerBar(engine)
    }
}

fun tapGame(e: GameEngine, x: Float, y: Float) {
    if (e.paused || e.status != 0) return
    val i = e.level.spots.indices.firstOrNull { hypot(e.level.spots[it].first - x, e.level.spots[it].second - y) < 0.075f }
    if (i == null) { e.selectedSpot = -1; return }
    val t = e.towerAt(i)
    if (t != null) { e.selectedSpot = if (e.selectedSpot == i) -1 else i; e.placing = null }
    else if (e.placing != null) { if (e.place(i, e.placing!!)) e.placing = null }
    else e.selectedSpot = -1
}

@Composable
fun HudBar(e: GameEngine) {
    Row(
        Modifier.fillMaxWidth().background(Pal.Grass).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Pill {
            HeartIcon()
            Text("${e.lives}", color = Pal.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Spacer(Modifier.width(8.dp))
        Pill {
            CoinIcon()
            Text("${e.coins}", color = Pal.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Spacer(Modifier.weight(1f))
        Text("L${e.levelIdx + 1}", color = Pal.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
            modifier = Modifier.background(Pal.HudBrown, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        Spacer(Modifier.width(6.dp))
        Text("Wave ${e.wave}/${e.totalWaves}", color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        CircleButton(onClick = { e.paused = !e.paused }) {
            Text(if (e.paused) "▶" else "❚❚", color = Pal.HudBrown, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(8.dp))
        CircleButton(onClick = { e.speed = if (e.speed == 1) 2 else 1 }) {
            Text("»${e.speed}", color = Pal.HudBrown, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun Pill(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.background(Pal.HudBrown, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) { content() }
}

@Composable
fun CircleButton(onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.size(36.dp)
            .background(Pal.White, RoundedCornerShape(50))
            .border(2.dp, Pal.HudBrown, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun HeartIcon() {
    Canvas(Modifier.size(16.dp)) {
        drawHeart(this, size.width / 2, size.height / 2, size.minDimension * 0.30f, Pal.Danger)
    }
}

@Composable
fun CoinIcon() {
    Canvas(Modifier.size(16.dp)) {
        drawCircle(Pal.Primary, radius = size.minDimension * 0.5f)
        drawCircle(Pal.Secondary, radius = size.minDimension * 0.44f, style = Stroke(size.minDimension * 0.12f))
        drawCircle(Pal.Secondary, radius = size.minDimension * 0.18f)
    }
}

@Composable
fun TowerPopup(e: GameEngine) {
    val t = e.towerAt(e.selectedSpot) ?: return
    Row(
        Modifier.fillMaxWidth().background(Pal.Panel).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${t.type.label} Lv.${t.level}", color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("DMG ${t.damage.toInt()}  RNG ${t.range}" + (if (t.level >= 3) "  ★${t.type.trait}" else ""), color = Pal.HudBrown.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        CartoonButton("Up ◉${t.upgradeCost}", enabled = e.coins >= t.upgradeCost) { e.upgrade(t) }
        Spacer(Modifier.width(8.dp))
        CartoonButton("Sell ◉${t.sellValue}", color = Pal.Danger) { e.sell(t) }
    }
}

@Composable
fun TowerBar(e: GameEngine) {
    Row(
        Modifier.fillMaxWidth().background(Pal.Grass).padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TowerType.entries.forEach { t ->
            val sel = e.placing == t
            val afford = e.coins >= t.cost
            Column(
                Modifier.weight(1f)
                    .alpha(if (afford || sel) 1f else 0.5f)
                    .background(if (sel) Color(0xFFFFF3C4) else Pal.White, RoundedCornerShape(14.dp))
                    .border(2.dp, Pal.Outline, RoundedCornerShape(14.dp))
                    .clickable { if (afford || sel) { e.placing = if (sel) null else t; e.selectedSpot = -1 } }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TowerIcon(t)
                Text(t.label.uppercase(), color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                Text("◉${t.cost}", color = if (afford) Pal.Secondary else Pal.Danger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            Modifier.weight(1f)
                .alpha(if (e.betweenWaves()) 1f else 0.5f)
                .background(Pal.White, RoundedCornerShape(14.dp))
                .border(2.dp, Pal.Outline, RoundedCornerShape(14.dp))
                .clickable { e.callNextWave() }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("▶▶", color = Pal.Success, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("NEXT", color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            Text("+◉${10 + e.wave}", color = Pal.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TowerIcon(t: TowerType) {
    Canvas(Modifier.size(34.dp)) {
        val s = size.minDimension * 7f
        when (t) {
            TowerType.ARROW -> cartoonCastle(this, size.width / 2, size.height * 0.60f, s, Pal.Tan, Pal.Roof, false)
            TowerType.CANNON -> cartoonCannon(this, size.width / 2, size.height * 0.55f, s)
            TowerType.ICE -> cartoonCastle(this, size.width / 2, size.height * 0.60f, s, Pal.IceBody, Pal.Roof, true)
        }
    }
}

@Composable
fun CartoonButton(text: String, enabled: Boolean = true, color: Color = Pal.Primary, onClick: () -> Unit) {
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(color, RoundedCornerShape(24.dp))
            .border(2.dp, Pal.Outline, RoundedCornerShape(24.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
}

@Composable
fun OverlayCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Pal.Ink.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.background(Pal.Bg, RoundedCornerShape(28.dp)).border(3.dp, Pal.Outline, RoundedCornerShape(28.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Pal.HudBrown)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
