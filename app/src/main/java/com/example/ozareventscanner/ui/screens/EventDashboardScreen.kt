package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.data.EscaneoEntity
import com.example.ozareventscanner.network.BeneficioDto
import com.example.ozareventscanner.ui.theme.*

/**
 * Pantalla Principal: EventDashboardScreen (Ultra Minimalista).
 *
 * Diseño:
 *  - TopAppBar: Nombre del Staff, semáforo de red y menú "Cerrar sesión".
 *  - Módulo de Beneficios: Tarjetas limpias de solo texto con "Nombre del Beneficio" y métrica "Entregados / Total" (sin íconos ni emojis).
 *  - Botón al final: "Ver Participantes".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDashboardScreen(
    staffNombre: String,
    nombreEvento: String,
    hayConexionInternet: Boolean,
    beneficios: List<BeneficioDto>,
    totalParticipantes: Int,
    escaneos: List<EscaneoEntity>,
    onBeneficioClick: (BeneficioDto) -> Unit,
    onIrAParticipantes: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var menuExpandido by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Staff: $staffNombre",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hayConexionInternet) "En línea · Sincronización activa" else "Sin conexión · Modo Offline",
                            fontSize = 11.sp,
                            color = if (hayConexionInternet) ColorSuccess else ColorError
                        )
                    }
                },
                actions = {
                    // Indicador de Red
                    SemaforoConexion(hayConexion = hayConexionInternet)

                    Spacer(modifier = Modifier.width(4.dp))

                    // Menú exclusivo "Cerrar sesión"
                    Box {
                        IconButton(onClick = { menuExpandido = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = ColorTextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpandido,
                            onDismissRequest = { menuExpandido = false },
                            modifier = Modifier.background(ColorSurface)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Cerrar sesión",
                                        color = ColorError,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = ColorError
                                    )
                                },
                                onClick = {
                                    menuExpandido = false
                                    onCerrarSesion()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Título del Evento ─────────────────────────────────────────────
            item {
                Text(
                    text = nombreEvento,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona un beneficio para iniciar el escaneo",
                    fontSize = 13.sp,
                    color = ColorTextSecondary
                )
            }

            // ── Lista de Tarjetas de Beneficios (Solo Texto, Sin Íconos/Emojis) ─
            if (beneficios.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorSurface)
                    ) {
                        Text(
                            text = "No hay beneficios habilitados para este evento.",
                            fontSize = 13.sp,
                            color = ColorTextSecondary,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(beneficios, key = { it.idBeneficio }) { beneficio ->
                    val entregados = escaneos.count {
                        it.idBeneficio == beneficio.idBeneficio || it.idBeneficio.isBlank()
                    }

                    TarjetaBeneficioTexto(
                        nombreBeneficio = beneficio.nombre,
                        entregados = entregados,
                        total = totalParticipantes,
                        onClick = { onBeneficioClick(beneficio) }
                    )
                }
            }

            // ── Botón Simple de Navegación al final ───────────────────────────
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onIrAParticipantes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Ver Participantes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de Beneficio de solo texto: Nombre y Métrica (Entregados / Total).
 * Sin íconos ni emojis.
 */
@Composable
private fun TarjetaBeneficioTexto(
    nombreBeneficio: String,
    entregados: Int,
    total: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = nombreBeneficio,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$entregados / $total",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (entregados >= total && total > 0) ColorSuccess else ColorPrimary
            )
        }
    }
}

@Composable
private fun SemaforoConexion(hayConexion: Boolean) {
    val colorSemaforo by animateColorAsState(
        targetValue = if (hayConexion) ColorSuccess else ColorError,
        animationSpec = tween(400),
        label = "colorSemaforo"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(colorSemaforo.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(colorSemaforo, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (hayConexion) "ONLINE" else "OFFLINE",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorSemaforo
        )
    }
}
