package com.example.minidefense

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(onPlay: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(Pal.Bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.size(130.dp)) {
            cartoonCastle(this, size.width / 2, size.height * 0.62f, size.minDimension * 6.5f, Pal.Tan, Pal.Roof, false)
        }
        Text(
            "MINI DEFENSE",
            style = TextStyle(
                fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Pal.White,
                shadow = Shadow(color = Pal.HudBrown, offset = Offset(0f, 5f), blurRadius = 2f)
            )
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkyButton("PLAY", Color(0xFFF9D65C), Color(0xFFB98A2E)) { onPlay() }
            ChunkyButton("LEVELS", Color(0xFFF4A259), Color(0xFFB96A35)) {
                Toast.makeText(ctx, "Level select comes in Step 3!", Toast.LENGTH_SHORT).show()
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkyButton("SETTINGS", Color(0xFF94C973), Color(0xFF5E8C4A)) {
                Toast.makeText(ctx, "Settings come in Step 3!", Toast.LENGTH_SHORT).show()
            }
            ChunkyButton("EXIT", Color(0xFF8ECDE8), Color(0xFF5E93B0)) {
                (ctx as? Activity)?.finish()
            }
        }
    }
}

@Composable
fun ChunkyButton(label: String, bg: Color, border: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .background(bg, RoundedCornerShape(16.dp))
            .border(3.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) { Text(label, color = Pal.HudBrown, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
}
