package com.example.minidefense

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(val levelIdx: Int = 0) {
    val level = Levels.ALL[levelIdx]
    var lives = 10
    var coins = 120
    var wave = 0
    val totalWaves = 10
    var paused = false
    var speed = 1
    var status = 0
    var waveGap = 2.5f
    var time = 0f
    var selectedSpot = -1
    var placing: TowerType? = null
    var onSfx: (String) -> Unit = {}
    val towers = mutableListOf<Tower>()
    val enemies = mutableListOf<Enemy>()
    val shots = mutableListOf<Projectile>()
    val pops = mutableListOf<Pop>()
    val parts = mutableListOf<Particle>()
    private val queue = mutableListOf<Pair<Float, Int>>()

    fun betweenWaves() = queue.isEmpty() && enemies.isEmpty() && status == 0 && wave < totalWaves

    fun tick(dtRaw: Float) {
        if (paused || status != 0) return
        val dt = dtRaw * speed
        time += dt
        if (queue.isEmpty() && enemies.isEmpty()) {
            if (wave >= totalWaves) {
                status = 1
                val stars = if (lives >= 10) 3 else if (lives >= 6) 2 else 1
                Save.win(levelIdx, stars)
                onSfx("win")
                return
            }
            waveGap -= dt
            if (waveGap <= 0) startWave(wave + 1)
        }
        while (queue.isNotEmpty() && queue[0].first <= time) {
            val kind = queue.removeAt(0).second
            enemies.add(Enemy(kind, Enemy.BASE_HP[kind] * level.diff * (1f + 0.18f * (wave - 1))))
        }
        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.slow = maxOf(0f, e.slow - dt)
            e.dist += e.speed * dt
            if (e.dist >= level.total) {
                it.remove(); lives--
                burst(level.base.first, level.base.second, Pal.Danger, 8)
                onSfx("leak")
                if (lives <= 0) { status = 2; onSfx("lose") }
                continue
            }
            val p = level.pointAt(e.dist)
            e.x = p.first; e.y = p.second
        }
        for (t in towers) {
            t.cooldown -= dt
            if (t.cooldown <= 0) {
                val target = enemies.filter { hyp(it.x - t.x, it.y - t.y) <= t.range }.maxByOrNull { it.dist }
                if (target != null) {
                    shots.add(Projectile(target, t))
                    if (t.type == TowerType.ARROW && t.level >= 3) shots.add(Projectile(target, t))
                    t.cooldown = 1f / t.type.rate
                    onSfx("shoot")
                }
            }
        }
        val ps = shots.iterator()
        while (ps.hasNext()) {
            val s = ps.next()
            val tgt = s.target
            if (tgt.hp <= 0 || !enemies.contains(tgt)) { ps.remove(); continue }
            val dx = tgt.x - s.x; val dy = tgt.y - s.y
            val d = hyp(dx, dy)
            val step = when (s.tower.type) {
                TowerType.ARROW -> 1.4f; TowerType.CANNON -> 0.9f; TowerType.ICE -> 1.1f
            } * dt
            if (d <= step + tgt.radius) { hit(tgt, s.tower); ps.remove() }
            else { s.x += dx / d * step; s.y += dy / d * step }
        }
        val pp = pops.iterator()
        while (pp.hasNext()) { val p = pp.next(); p.t -= dt; if (p.t <= 0) pp.remove() }
        val pt = parts.iterator()
        while (pt.hasNext()) {
            val p = pt.next()
            p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 1.2f * dt; p.t -= dt
            if (p.t <= 0) pt.remove()
        }
    }

    fun burst(x: Float, y: Float, color: androidx.compose.ui.graphics.Color, n: Int) {
        for (i in 0 until n) {
            val ang = Random.nextFloat() * 6.283f
            val sp = 0.12f + Random.nextFloat() * 0.22f
            parts.add(Particle(x, y, cos(ang) * sp, sin(ang) * sp - 0.12f, 0.45f, color))
        }
    }

    private fun hit(e: Enemy, t: Tower) {
        if (t.type == TowerType.ICE) {
            e.slow = if (t.level >= 3) 2.2f else 1.5f
            e.slowMul = if (t.level >= 3) 0.3f else 0.5f
        }
        damage(e, t.damage)
        if (t.type.splash > 0) {
            val r = t.type.splash * (if (t.level >= 3) 1.5f else 1f)
            burst(e.x, e.y, Pal.Secondary, 8)
            for (o in enemies.toList()) if (o !== e && hyp(o.x - e.x, o.y - e.y) <= r) damage(o, t.damage * 0.6f)
        }
    }

    private fun damage(e: Enemy, amount: Float) {
        if (e.hp <= 0) return
        e.hp -= maxOf(1f, amount - Enemy.ARMOR[e.kind])
        if (e.hp <= 0) {
            enemies.remove(e)
            coins += e.reward
            pops.add(Pop(e.x, e.y, 0.3f))
            burst(e.x, e.y, e.color, 6)
            onSfx("pop")
        }
    }

    fun startWave(n: Int) {
        wave = n
        waveGap = 3f
        var t = time + 0.5f
        val count = 5 + n * 2
        val tier = levelIdx / 3
        for (i in 0 until count) {
            val kind = pickKind(n, i, tier, count)
            queue.add(t to kind)
            t += if (kind == 4) 0.45f else 0.75f
        }
        onSfx("wave")
    }

    private fun pickKind(n: Int, i: Int, tier: Int, count: Int): Int {
        if (n == totalWaves && i == count - 1) return 3
        val r = (i * 31 + n * 17 + levelIdx * 7) % 10
        return when {
            tier >= 2 && r == 0 -> 5
            tier >= 1 && r == 1 -> 2
            tier >= 0 && r == 2 && n >= 2 -> 1
            r == 3 || r == 4 -> 4
            tier >= 2 && r == 5 -> 5
            tier >= 1 && r == 6 -> 2
            else -> 0
        }
    }

    fun callNextWave(): Boolean {
        if (!betweenWaves()) return false
        coins += 10 + wave
        startWave(wave + 1)
        return true
    }

    fun place(spot: Int, type: TowerType): Boolean {
        if (coins < type.cost || towers.any { it.spot == spot }) return false
        val s = level.spots[spot]
        towers.add(Tower(type, s.first, s.second, spot))
        coins -= type.cost
        burst(s.first, s.second, Pal.White, 5)
        onSfx("place")
        return true
    }

    fun towerAt(spot: Int) = towers.firstOrNull { it.spot == spot }

    fun upgrade(t: Tower) {
        val c = t.upgradeCost
        if (coins >= c) {
            coins -= c; t.level++; t.invested += c
            burst(t.x, t.y, Pal.Primary, 6)
            onSfx("place")
        }
    }

    fun sell(t: Tower) {
        coins += t.sellValue
        towers.remove(t)
        selectedSpot = -1
        burst(t.x, t.y, Pal.TanDark, 5)
        onSfx("pop")
    }

    private fun hyp(a: Float, b: Float) = sqrt(a * a + b * b)
}
