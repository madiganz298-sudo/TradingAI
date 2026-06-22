package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SecondaryGold
import com.example.ui.theme.TextGray
import kotlinx.coroutines.delay

@Composable
fun SplashView(onSplashFinished: () -> Unit) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Smooth 1.5s fade-in animation
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(1500)
        )
        // Additional delay holding the gold screen
        delay(1000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            // Metallic Gold textual logo "M4DI"
            Text(
                text = "M4DI",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryGold, Color.White, SecondaryGold)
                    )
                ),
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // App branding subtitle
            Text(
                text = "TradeAI Pro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryGold,
                letterSpacing = 6.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Autonomous Analytics Engine",
                fontSize = 11.sp,
                color = TextGray,
                letterSpacing = 2.sp
            )
        }
    }
}
