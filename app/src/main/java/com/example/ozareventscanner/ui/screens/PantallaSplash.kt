package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Pantalla 1 — Splash de Ozar Events Scanner.
 * Muestra animación de logo con los Genesis Design Tokens y navega automáticamente.
 */
@Composable
fun PantallaSplash(onSplashFinalizado: () -> Unit) {

    // ── Animaciones ───────────────────────────────────────────────────────────
    val escala by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "escala_logo"
    )

    val alpha = remember { Animatable(0f) }
    val alphaTexto = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
        delay(200)
        alphaTexto.animateTo(1f, animationSpec = tween(600))
        delay(1400)
        onSplashFinalizado()
    }

    // ── Puntos de carga animados ──────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 150), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 300), RepeatMode.Reverse),
        label = "dot3"
    )

    // ── Layout Genesis ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ColorForeground, GreenDark3, GreenDark2)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logotipo oficial de Zendov Scanner
            Box(
                modifier = Modifier
                    .scale(escala)
                    .alpha(alpha.value),
                contentAlignment = Alignment.Center
            ) {
                com.example.ozareventscanner.ui.components.ZendovBrandMark(size = 72.dp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Nombre oficial
            Text(
                text       = "zendov",
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                letterSpacing = (-0.06).sp,
                modifier   = Modifier.alpha(alphaTexto.value)
            )

            Text(
                text     = "SCANNER",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color    = GreenLight1,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(alphaTexto.value)
            )

            Text(
                text     = "Gestión de eventos en vivo",
                fontSize = 14.sp,
                color    = GreenLight2.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .alpha(alphaTexto.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Dots de carga en verde vibrante Genesis
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(alphaTexto.value)
            ) {
                listOf(dot1, dot2, dot3).forEach { alphaValor ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(alphaValor)
                            .background(GreenBase, shape = ShapeRadiusPill)
                    )
                }
            }
        }
    }
}
