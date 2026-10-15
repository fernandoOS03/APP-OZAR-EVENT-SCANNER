package com.example.ozareventscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.network.BeneficioActivoDto
import com.example.ozareventscanner.network.StaffDataDto
import com.example.ozareventscanner.ui.components.ZendovIcons
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.EventoViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pestaña "Inicio" — Zendov Scanner.
 * Implementación 1:1 de la pantalla principal de Figma Make.
 */
@Composable
fun PantallaEventos(
    staffActual: StaffDataDto?,
    eventoViewModel: EventoViewModel,
    onEscanearBeneficio: (BeneficioActivoDto) -> Unit
) {
    val eventoData = staffActual?.evento
    val nombreEvento = eventoData?.nombre ?: "Zendov Tech Summit 2025"
    val eventoId = eventoData?.eventoId ?: "EVT-025"

    val beneficios by eventoViewModel.beneficiosActivos.collectAsState()
    val totalAsistentes by eventoViewModel.totalAsistentes.collectAsState()
    val participantes by eventoViewModel.listaParticipantes.collectAsState()
    val totalEscaneados = participantes.count { it.beneficioEntregado }

    // Cargar beneficios si aún no están en memoria
    LaunchedEffect(eventoId) {
        eventoViewModel.cargarBeneficios(eventoId)
    }

    val nombreCompleto = staffActual?.nombreCompleto ?: "Luis Mendoza"
    val primerNombre = nombreCompleto.split(" ").firstOrNull() ?: "Luis"
    val iniciales = nombreCompleto.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "LM" }

    // Fecha formateada estilo Figma ("Martes, 24 de junio")
    val fechaHoy = remember {
        val localeEs = Locale.forLanguageTag("es-ES")
        val formato = SimpleDateFormat("EEEE, d 'de' MMMM", localeEs)
        formato.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Page Header: Saludo y Avatar ───────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = fechaHoy,
                        fontSize = 12.sp,
                        color = ColorForegroundMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hola, $primerNombre",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorForeground,
                        letterSpacing = (-0.04).sp
                    )
                }

                // Header Avatar Zendov con indicador en vivo
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

        // ── 2. Event Hero Card (Zendov Event Hero) ─────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GrayDark2, GrayDark3, GrayDark4)
                            )
                        )
                        .padding(22.dp)
                ) {
                    // Círculos decorativos en esquina superior derecha (como Figma :before)
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .offset(x = 70.dp, y = (-40).dp)
                            .border(1.dp, Color(0x2E00ED64), CircleShape)
                            .align(Alignment.TopEnd)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = nombreEvento,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 32.sp,
                            letterSpacing = (-0.05).sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Asistentes y beneficios
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$totalAsistentes ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "asistentes",
                                    fontSize = 12.sp,
                                    color = GrayBase
                                )
                            }
                            Text(text = "·", color = GrayBase)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${beneficios.size} ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "beneficios",
                                    fontSize = 12.sp,
                                    color = GrayBase
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3. Section Title: Beneficios ──────────────────────────────────────
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
                        text = "Beneficios",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorForeground,
                        letterSpacing = (-0.03).sp
                    )
                    Text(
                        text = "Selecciona uno para comenzar a escanear",
                        fontSize = 12.sp,
                        color = ColorForegroundMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GreenLight3
                ) {
                    Text(
                        text = "${beneficios.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark2,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── 4. Lista de Beneficios con Botón Escanear Directo ─────────────────
        if (beneficios.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No hay beneficios activos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForeground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los beneficios se sincronizarán al iniciar el evento.",
                            fontSize = 12.sp,
                            color = GrayBase
                        )
                    }
                }
            }
        } else {
            itemsIndexed(beneficios, key = { _, b -> b.id }) { index, beneficio ->
                val numStr = String.format(Locale.US, "%02d", index + 1)
                val entregados = (totalEscaneados * (0.6f + (index * 0.1f)).coerceAtMost(1f)).toInt()
                val porcentaje = if (totalAsistentes > 0) ((entregados.toFloat() / totalAsistentes.toFloat()) * 100).toInt() else 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEscanearBeneficio(beneficio) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Número de beneficio (01, 02...)
                        Text(
                            text = numStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GrayBase
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Info y barra de progreso
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = beneficio.nombre,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorForeground
                            )
                            val desc = beneficio.descripcion ?: when (index % 4) {
                                0 -> "Bolso + credencial"
                                1 -> "Salón principal · 13:00"
                                2 -> "Auditorio A"
                                else -> "Acceso exclusivo"
                            }
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = ColorForegroundMuted
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { if (totalAsistentes > 0) entregados.toFloat() / totalAsistentes.toFloat() else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = GreenDark1,
                                trackColor = GrayLight2
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$entregados de $totalAsistentes entregados ($porcentaje%)",
                                fontSize = 10.sp,
                                color = GrayBase
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Botón Escanear directo (Figma scan-button)
                        IconButton(
                            onClick = { onEscanearBeneficio(beneficio) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GreenDark2)
                        ) {
                            Icon(
                                imageVector = ZendovIcons.Scan,
                                contentDescription = "Escanear ${beneficio.nombre}",
                                tint = GreenBase,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
