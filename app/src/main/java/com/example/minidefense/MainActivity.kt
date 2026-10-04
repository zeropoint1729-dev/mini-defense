package com.example.minidefense

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Save.init(this)
        Sfx.init()
        setContent {
            var screen by remember { mutableStateOf(0) }
            var level by remember { mutableStateOf(0) }
            when (screen) {
                0 -> MenuScreen(
                    onPlay = { level = Save.unlocked().coerceAtMost(9); screen = 2 },
                    onLevels = { screen = 1 },
                    onSettings = {
                        val now = !Save.sound
                        Save.setSound(now)
                        Toast.makeText(this, if (now) "Sound ON" else "Sound OFF", Toast.LENGTH_SHORT).show()
                    }
                )
                1 -> LevelSelectScreen(onBack = { screen = 0 }, onPick = { level = it; screen = 2 })
                else -> BattleScreen(levelIdx = level, onMenu = { screen = 1 })
            }
        }
    }
}
