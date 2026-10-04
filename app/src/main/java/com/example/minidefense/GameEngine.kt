package com.example.minidefense

import kotlin.math.sqrt

class GameEngine {
    var lives = 10
    var coins = 120
    var wave = 0
    val totalWaves = 10
    var paused = false
    var speed = 1
    var status = 0            // 0 playing, 1 won, 2 lost
    var waveGap = 2.5f
    var time = 0f
    var selectedSpot = -1
    var placing: TowerType? = null
    val towers = mutableListOf<Tower>()
    val enemies = mutableListOf<Enemy>()
    val shots = mutableListOf<Projectile>()
    val pops = mutableListOf<Pop>()
    private val queue = mutableListOf<Pair<Float, Int>>()

    fun betweenWaves() = queue.isEmpty() && enemies.isEmpty() && status == 0 && wave < totalWaves

    fun tick(dtRaw: Float) {
        if (paused || status != 0) return
        val dt = dtRaw * speed
        time += dt
        if (queue.isEmpty() && enemies.isEmpty()) {
            if (wave >= totalWaves) { status = 1; return }
            waveGap -= dt
            if (waveGap <= 0) startWave(wave + 1)
        }
        while (queue.isNotEmpty() && queue[0].first <= time) {
            val kind = queue.removeAt(0).second
            enemies.add(Enemy(kind, Enemy.BASE_HP[kind] * (1f + 0.18f * (wave - 1))))
        }
        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.slow = maxOf(0f, e.slow - dt)
            e.dist += e.speed * dt
            if (e.dist >= MapData.TOTAL) {
                it.remove(); lives--
                if (lives <= 0) status = 2
                continue
            }
            val p = MapData.pointAt(e.dist)
            e.x = p.first; e.y = p.second
        }
        for (t in towers) {
            t.cooldown -= dt
            if (t.cooldown <= 0) {
                val target = enemies.filter { hyp(it.x - t.x, it.y - t.y) <= t.range }.maxByOrNull { it.dist }
                if (target != null) {
                    shots.add(Projectile(target, t))
                    t.cooldown = 1f / t.type.rate
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
    }

    private fun hit(e: Enemy, t: Tower) {
        if (t.type.slow > 0) e.slow = t.type.slow
        damage(e, t.damage)
        if (t.type.splash > 0) {
            for (o in enemies.toList()) if (o !== e && hyp(o.x - e.x, o.y - e.y) <= t.type.splash) damage(o, t.damage * 0.6f)
        }
    }

    private fun damage(e: Enemy, amount: Float) {
        if (e.hp <= 0) return
        e.hp -= amount
        if (e.hp <= 0) {
            enemies.remove(e)
            coins += e.reward
            pops.add(Pop(e.x, e.y, 0.3f))
        }
    }

    fun startWave(n: Int) {
        wave = n
        waveGap = 3f
        var t = time + 0.5f
        val count = 5 + n * 2
        for (i in 0 until count) {
            val kind = when {
                n >= 10 && i == count - 1 -> 3
                n >= 4 && i % 5 == 4 -> 2
                n >= 2 && i % 3 == 2 -> 1
                else -> 0
            }
            queue.add(t to kind)
            t += 0.75f
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
        val s = MapData.SPOTS[spot]
        towers.add(Tower(type, s.first, s.second, spot))
        coins -= type.cost
        return true
    }

    fun towerAt(spot: Int) = towers.firstOrNull { it.spot == spot }

    fun upgrade(t: Tower) {
        val c = t.upgradeCost
        if (coins >= c) { coins -= c; t.level++; t.invested += c }
    }

    fun sell(t: Tower) {
        coins += t.sellValue
        towers.remove(t)
        selectedSpot = -1
    }

    private fun hyp(a: Float, b: Float) = sqrt(a * a + b * b)
}
