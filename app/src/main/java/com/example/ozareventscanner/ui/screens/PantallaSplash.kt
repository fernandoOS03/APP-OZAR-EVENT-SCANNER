package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
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

// Colores locales ajustados a la nueva paleta clara
private val SplashFondo1    = ColorDark
private val SplashFondo2    = GradientFondo[1]
private val SplashAccent    = ColorPrimary
private val SplashSecundario = ColorTextSecondary

/**
 * Pantalla 1 — Splash.
 * Muestra animación de logo + nombre y navega automáticamente a la siguiente pantalla.
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
        // Fade-in del ícono
        alpha.animateTo(1f, animationSpec = tween(800))
        // Fade-in del texto con pequeño delay
        delay(200)
        alphaTexto.animateTo(1f, animationSpec = tween(600))
        // Espera antes de navegar
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

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashFondo1, SplashFondo2))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Ícono principal
            Icon(
                imageVector        = Icons.Default.QrCodeScanner,
                contentDescription = "Logo Ozar",
                tint               = SplashAccent,
                modifier           = Modifier
                    .size(100.dp)
                    .scale(escala)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nombre de la app
            Text(
                text       = "Ozar Event Scanner",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorTextPrimary,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.alpha(alphaTexto.value)
            )

            Text(
                text     = "Gestión de eventos y beneficios",
                fontSize = 14.sp,
                color    = SplashSecundario,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .alpha(alphaTexto.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Dots de carga
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(alphaTexto.value)
            ) {
                listOf(dot1, dot2, dot3).forEach { alphaValor ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(alphaValor)
                            .background(SplashAccent, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
    }
}
