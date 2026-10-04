package com.example.minidefense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(onPlay: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Pal.Bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏰", fontSize = 72.sp)
        Text("Mini Defense", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Pal.Ink)
        Text("A cute tower defense", color = Pal.Ink.copy(alpha = 0.6f), fontSize = 15.sp)
        Spacer(Modifier.height(36.dp))
        CartoonButton("PLAY") { onPlay() }
        Spacer(Modifier.height(20.dp))
        Text(
            "Tap a tower card, then tap a dashed spot.\nTap a placed tower to upgrade or sell.\nStop the blobs before they reach home!",
            color = Pal.Ink.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
