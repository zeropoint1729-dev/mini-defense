package com.example.minidefense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun BattleScreen(onMenu: () -> Unit) {
    var engine by remember { mutableStateOf(GameEngine()) }
    var frame by remember { mutableStateOf(0) }
    LaunchedEffect(engine) {
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
            GameCanvas(engine) { x, y -> tapGame(engine, x, y) }
            if (engine.betweenWaves()) {
                Text(
                    "Wave ${engine.wave + 1} in ${(engine.waveGap + 0.999f).toInt()}s",
                    color = Pal.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                        .background(Pal.Panel, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            if (engine.paused && engine.status == 0) OverlayCard("Paused") {
                CartoonButton("Resume") { engine.paused = false }
                Spacer(Modifier.height(8.dp))
                CartoonButton("Restart") { engine = GameEngine() }
                Spacer(Modifier.height(8.dp))
                CartoonButton("Menu") { onMenu() }
            }
            if (engine.status == 1) OverlayCard("Victory!") {
                CartoonButton("Play Again") { engine = GameEngine() }
                Spacer(Modifier.height(8.dp))
                CartoonButton("Menu") { onMenu() }
            }
            if (engine.status == 2) OverlayCard("Defeat") {
                CartoonButton("Retry") { engine = GameEngine() }
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
    val i = MapData.SPOTS.indices.firstOrNull { hypot(MapData.SPOTS[it].first - x, MapData.SPOTS[it].second - y) < 0.075f }
    if (i == null) { e.selectedSpot = -1; return }
    val t = e.towerAt(i)
    if (t != null) { e.selectedSpot = if (e.selectedSpot == i) -1 else i; e.placing = null }
    else if (e.placing != null) { if (e.place(i, e.placing!!)) e.placing = null }
    else e.selectedSpot = -1
}

@Composable
fun HudBar(e: GameEngine) {
    Row(
        Modifier.fillMaxWidth().background(Pal.Panel).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("♥ ${e.lives}", color = Pal.Danger, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Spacer(Modifier.width(14.dp))
        Text("◉ ${e.coins}", color = Pal.Secondary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        Text("Wave ${e.wave}/${e.totalWaves}", color = Pal.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(34.dp).background(Pal.Bg, RoundedCornerShape(10.dp)).clickable { e.paused = !e.paused }, contentAlignment = Alignment.Center) {
            Text(if (e.paused) "▶" else "II", color = Pal.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(34.dp).background(Pal.Bg, RoundedCornerShape(10.dp)).clickable { e.speed = if (e.speed == 1) 2 else 1 }, contentAlignment = Alignment.Center) {
            Text("»${e.speed}", color = Pal.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TowerPopup(e: GameEngine) {
    val t = e.towerAt(e.selectedSpot) ?: return
    Row(
        Modifier.fillMaxWidth().background(Pal.Bg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${t.type.label} Lv.${t.level}", color = Pal.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("DMG ${t.damage.toInt()}   RNG ${t.range}", color = Pal.Ink.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        CartoonButton("Up ◉${t.upgradeCost}", enabled = e.coins >= t.upgradeCost) { e.upgrade(t) }
        Spacer(Modifier.width(8.dp))
        CartoonButton("Sell ◉${t.sellValue}", color = Pal.Danger) { e.sell(t) }
    }
}

@Composable
fun TowerBar(e: GameEngine) {
    Row(
        Modifier.fillMaxWidth().background(Pal.Panel).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TowerType.entries.forEach { t ->
            val sel = e.placing == t
            val afford = e.coins >= t.cost
            Column(
                Modifier.weight(1f)
                    .alpha(if (afford || sel) 1f else 0.45f)
                    .background(if (sel) Pal.Primary else Pal.Bg, RoundedCornerShape(18.dp))
                    .border(2.dp, if (sel) Pal.Secondary else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable { if (afford || sel) { e.placing = if (sel) null else t; e.selectedSpot = -1 } }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TowerIcon(t)
                Text(t.label, color = Pal.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("◉${t.cost}", color = if (afford) Pal.Secondary else Pal.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            Modifier.weight(1f)
                .alpha(if (e.betweenWaves()) 1f else 0.45f)
                .background(Pal.Bg, RoundedCornerShape(18.dp))
                .clickable { e.callNextWave() }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("▶▶", color = Pal.Success, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text("Next", color = Pal.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("+◉${10 + e.wave}", color = Pal.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TowerIcon(t: TowerType) {
    Canvas(Modifier.size(30.dp)) {
        val c = size.minDimension / 2
        val cx = size.width / 2; val cy = size.height / 2
        when (t) {
            TowerType.ARROW -> {
                drawLine(Pal.Secondary, Offset(cx, cy + c * 0.7f), Offset(cx, cy - c * 0.4f), strokeWidth = 3f)
                val p = Path()
                p.moveTo(cx, cy - c * 0.8f); p.lineTo(cx - c * 0.35f, cy - c * 0.15f); p.lineTo(cx + c * 0.35f, cy - c * 0.15f); p.close()
                drawPath(p, Pal.Secondary)
            }
            TowerType.CANNON -> {
                drawCircle(Color(0xFF8D6E63), c * 0.55f, Offset(cx, cy + c * 0.25f))
                drawLine(Color(0xFF546E7A), Offset(cx, cy + c * 0.2f), Offset(cx + c * 0.6f, cy - c * 0.55f), strokeWidth = 5f, cap = StrokeCap.Round)
            }
            TowerType.ICE -> {
                for (a in 0..2) {
                    val ang = (a * 60 + 90) * PI / 180
                    drawLine(
                        Color(0xFF4FC3F7),
                        Offset(cx - cos(ang).toFloat() * c * 0.8f, cy - sin(ang).toFloat() * c * 0.8f),
                        Offset(cx + cos(ang).toFloat() * c * 0.8f, cy + sin(ang).toFloat() * c * 0.8f),
                        strokeWidth = 3f
                    )
                }
            }
        }
    }
}

@Composable
fun CartoonButton(text: String, enabled: Boolean = true, color: Color = Pal.Primary, onClick: () -> Unit) {
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(color, RoundedCornerShape(24.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Pal.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
}

@Composable
fun OverlayCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Pal.Ink.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.background(Pal.Bg, RoundedCornerShape(28.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Pal.Ink)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
