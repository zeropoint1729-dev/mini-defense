package com.example.minidefense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LevelSelectScreen(onBack: () -> Unit, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().background(Pal.Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.width(40.dp).height(40.dp)
                    .background(Pal.White, RoundedCornerShape(50))
                    .border(2.dp, Pal.HudBrown, RoundedCornerShape(50))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("◀", color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
            Spacer(Modifier.weight(1f))
            Text("LEVELS", color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        for (row in 0..4) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                for (col in 0..1) {
                    val i = row * 2 + col
                    val unlocked = i <= Save.unlocked()
                    val st = Save.stars(i)
                    Column(
                        Modifier.weight(1f)
                            .alpha(if (unlocked) 1f else 0.55f)
                            .background(if (unlocked) Pal.White else Pal.Panel, RoundedCornerShape(16.dp))
                            .border(2.dp, Pal.Outline, RoundedCornerShape(16.dp))
                            .clickable(enabled = unlocked) { onPick(i) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (unlocked) "${i + 1}" else "🔒",
                            color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp
                        )
                        Text(
                            "★".repeat(st) + "☆".repeat(3 - st),
                            color = Pal.Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
