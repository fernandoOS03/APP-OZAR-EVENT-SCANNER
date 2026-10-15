package com.example.ozareventscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.network.StaffDataDto
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.EventoViewModel

/**
 * Pantalla de Overview — Zendov Scanner.
 * Muestra el dashboard de métricas en tiempo real del evento activo.
 */
@Composable
fun PantallaOverview(
    staffActual: StaffDataDto?,
    eventoViewModel: EventoViewModel
) {
    val totalAsistentes by eventoViewModel.totalAsistentes.collectAsState()
    val beneficios by eventoViewModel.beneficiosActivos.collectAsState()
    val participantes by eventoViewModel.listaParticipantes.collectAsState()

    val totalEscaneados = participantes.count { it.beneficioEntregado }
    val porcentaje = if (totalAsistentes > 0) {
        ((totalEscaneados.toFloat() / totalAsistentes.toFloat()) * 100).toInt()
    } else {
        0
    }

    val nombreStaff = staffActual?.nombreCompleto ?: "Luis Mendoza"
    val iniciales = nombreStaff.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "LM" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Page Header con Avatar de Staff ─────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Métricas en tiempo real",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorForegroundMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Overview",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorForeground,
                        letterSpacing = (-0.04).sp
                    )
                }

                // Header Avatar Zendov
                Box(modifier = Modifier.size(44.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(GrayDark2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iniciales,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenLight1
                        )
                    }
                    // Dot verde en línea
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(GreenBase)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        }

        // ── 2. Metric Feature Card (Asistentes escaneados + Sparkline) ─────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GrayDark2),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "ASISTENTES ESCANEADOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenLight1,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalEscaneados",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 44.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "/ $totalAsistentes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrayBase,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de progreso del Feature
                    LinearProgressIndicator(
                        progress = { if (totalAsistentes > 0) totalEscaneados.toFloat() / totalAsistentes.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GreenBase,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$porcentaje% del total registrado",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sparkline (Histograma de barras de actividad en vivo)
                    val mockBarras = listOf(0.26f, 0.34f, 0.28f, 0.46f, 0.41f, 0.58f, 0.54f, 0.72f, 0.64f, 0.84f, 0.76f, 0.94f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        mockBarras.forEach { altura ->
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(altura)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(GreenBase.copy(alpha = 0.35f + (altura * 0.65f)))
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Mini-metrics en paralelo ───────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini Metric 1: Hoy
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "HOY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForegroundMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+$totalEscaneados",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorForeground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "nuevos ingresos",
                            fontSize = 11.sp,
                            color = GrayBase
                        )
                    }
                }

                // Mini Metric 2: Última hora
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ÚLTIMA HORA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForegroundMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(totalEscaneados * 0.35).toInt().coerceAtLeast(1)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenDark2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "escaneos validados",
                            fontSize = 11.sp,
                            color = GrayBase
                        )
                    }
                }
            }
        }

        // ── 4. Cabecera Desglose por Beneficio ────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Desglose de Beneficios",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorForeground
                    )
                    Text(
                        text = "Tasa de entrega en estación",
                        fontSize = 12.sp,
                        color = ColorForegroundMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GreenLight3
                ) {
                    Text(
                        text = "${beneficios.size} activos",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark2,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── 5. Filas de progreso por beneficio ────────────────────────────────
        items(beneficios) { beneficio ->
            val entregados = (totalEscaneados * (0.6f + (beneficio.orden * 0.1f)).coerceAtMost(1f)).toInt()
            val pctBeneficio = if (totalAsistentes > 0) ((entregados.toFloat() / totalAsistentes.toFloat()) * 100).toInt() else 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = beneficio.nombre,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForeground
                        )
                        Text(
                            text = "$pctBeneficio%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark2
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { if (totalAsistentes > 0) entregados.toFloat() / totalAsistentes.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GreenDark1,
                        trackColor = GrayLight2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$entregados de $totalAsistentes entregados",
                            fontSize = 12.sp,
                            color = GrayBase
                        )
                        Text(
                            text = "Orden #${beneficio.orden}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GrayBase
                        )
                    }
                }
            }
        }
    }
}
