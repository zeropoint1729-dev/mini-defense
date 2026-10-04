package com.example.minidefense

import android.content.Context
import android.content.SharedPreferences

object Save {
    private lateinit var prefs: SharedPreferences
    fun init(ctx: Context) {
        prefs = ctx.applicationContext.getSharedPreferences("minidefense", 0)
    }
    val sound: Boolean get() = prefs.getBoolean("sound", true)
    fun setSound(on: Boolean) = prefs.edit().putBoolean("sound", on).apply()
    fun stars(level: Int): Int = prefs.getInt("stars$level", 0)
    fun unlocked(): Int = prefs.getInt("unlocked", 0)
    fun win(level: Int, stars: Int) {
        val e = prefs.edit()
        if (stars > stars(level)) e.putInt("stars$level", stars)
        if (level + 1 > unlocked()) e.putInt("unlocked", level + 1)
        e.apply()
    }
}
