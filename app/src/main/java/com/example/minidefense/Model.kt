package com.example.minidefense

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.math.sqrt

enum class TowerType(val cost: Int, val range: Float, val damage: Float, val rate: Float, val label: String) {
    ARROW(50, 0.20f, 12f, 2.0f, "Arrow"),
    CANNON(80, 0.17f, 30f, 0.7f, "Cannon"),
    ICE(60, 0.16f, 6f, 1.2f, "Ice");
    val splash: Float get() = if (this == CANNON) 0.07f else 0f
    val slow: Float get() = if (this == ICE) 1.5f else 0f
}

class Tower(val type: TowerType, val x: Float, val y: Float, val spot: Int) {
    var level = 1
    var cooldown = 0f
    var invested = type.cost
    val damage get() = type.damage * 1.35.pow(level - 1).toFloat()
    val range get() = type.range * (1f + 0.10f * (level - 1))
    val upgradeCost get() = (type.cost * 0.8 * level).toInt()
    val sellValue get() = (invested * 0.7).toInt()
}

class Enemy(val kind: Int, val hpMax: Float) {
    var hp = hpMax
    var dist = 0f
    var slow = 0f
    var x = 0f; var y = 0f
    val speed get() = BASE_SPEED[kind] * (if (slow > 0) 0.5f else 1f)
    val reward get() = REWARD[kind]
    val radius get() = RADIUS[kind]
    val color get() = COLORS[kind]
    companion object {
        val BASE_SPEED = floatArrayOf(0.075f, 0.06f, 0.045f, 0.032f)
        val REWARD = intArrayOf(8, 10, 14, 60)
        val RADIUS = floatArrayOf(0.030f, 0.032f, 0.036f, 0.050f)
        val BASE_HP = floatArrayOf(40f, 75f, 130f, 650f)
        val COLORS = listOf(Color(0xFFFFD93D), Color(0xFFFF6B6B), Color(0xFF8ECDE8), Color(0xFFFF8FAB))
        val OUTLINES = listOf(Color(0xFFC9A227), Color(0xFFC94C4C), Color(0xFF5E93B0), Color(0xFFD96A8A))
    }
}

class Projectile(val target: Enemy, val tower: Tower) {
    var x = tower.x; var y = tower.y - 0.02f
}

class Pop(var x: Float, var y: Float, var t: Float)

object MapData {
    const val WORLD_H = 1.15f
    val PATH = listOf(
        0.50f to -0.05f, 0.50f to 0.16f, 0.18f to 0.20f, 0.18f to 0.50f,
        0.82f to 0.54f, 0.82f to 0.82f, 0.40f to 0.86f, 0.40f to 1.06f
    )
    val SPOTS = listOf(
        0.33f to 0.07f, 0.70f to 0.10f, 0.50f to 0.36f,
        0.55f to 0.68f, 0.18f to 0.68f, 0.63f to 0.94f
    )
    private val lens: List<Float>
    val TOTAL: Float
    init {
        var acc = 0f
        lens = PATH.zipWithNext { a, b ->
            val l = sqrt((b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second))
            acc += l; l
        }
        TOTAL = acc
    }
    fun pointAt(dist: Float): Pair<Float, Float> {
        var d = dist
        for (i in lens.indices) {
            val l = lens[i]
            if (d <= l) {
                val a = PATH[i]; val b = PATH[i + 1]
                val t = if (l == 0f) 0f else d / l
                return (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
            }
            d -= l
        }
        return PATH.last()
    }
}
