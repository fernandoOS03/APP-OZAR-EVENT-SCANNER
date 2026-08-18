package com.example.ozareventscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AppViewModel

/**
 * Pantalla de Detalle del Evento (KPIs & Lista de Beneficios).
 *
 * Cada tarjeta de beneficio cuenta con su propio botón de escaneo individual.
 * El acceso a la lista completa de asistentes para entrega manual se encuentra
 * centralizado en un botón de acción.
 *
 * @param onIrALector         Navega a la pantalla de escaneo QR (CameraX).
 * @param onIrAParticipantes  Navega a la pantalla de lista de participantes con búsqueda.
 * @param onVolver            Regresa a la pantalla anterior (Inicio / Lista de Eventos).
 */
@Composable
fun PantallaBeneficios(
    viewModel: AppViewModel,
    nombreEvento: String,
    onIrALector: () -> Unit,
    onIrAParticipantes: () -> Unit,
    onVolver: () -> Unit
) {
    val beneficios by viewModel.beneficios.collectAsState()
    val escaneados = beneficios.count { it.escaneado }
    val pendientes = beneficios.count { !it.escaneado }

    Scaffold(
        containerColor = ColorBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ── 1. Top Bar (Botón Atrás, Título y Botón Asistentes) ───────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onVolver() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = nombreEvento,
                        color = ColorTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── 2. KPIs Horizontales (Total, Escaneados, Pendientes) ───────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatsBox(
                        icon = Icons.Default.Groups,
                        value = beneficios.size.toString(),
                        label = "Total",
                        iconColor = ColorTextSecondary,
                        iconBg = Color(0xFFF1F5F9),
                        modifier = Modifier.weight(1f)
                    )
                    StatsBox(
                        icon = Icons.Default.CropFree,
                        value = escaneados.toString(),
                        label = "Escaneados",
                        iconColor = ColorPrimary,
                        iconBg = ColorPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    )
                    StatsBox(
                        icon = Icons.Default.Schedule,
                        value = pendientes.toString(),
                        label = "Pendientes",
                        iconColor = ColorWarning,
                        iconBg = ColorWarning.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── 3. Botón de Acceso a Lista de Asistentes (Contingencia QR) ───
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIrAParticipantes() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(ColorPrimary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PeopleAlt,
                                contentDescription = null,
                                tint = ColorPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lista de Asistentes (${beneficios.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary
                            )
                            Text(
                                text = "Búsqueda por Nombre o DNI · Entrega manual",
                                fontSize = 12.sp,
                                color = ColorTextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = ColorTextSecondary
                        )
                    }
                }
            }

            // ── 4. Cabecera Sección Beneficios ────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Beneficios Activos",
                        color = ColorTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Selecciona para escanear",
                        color = ColorTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // ── 5. Lista de Beneficios con Ícono de Escaneo en Cada Tarjeta ───
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val listBeneficiosMock = listOf(
                        Triple("Registro y Entrega Camiseta", "190/320", Icons.Default.Checkroom),
                        Triple("Desayuno 10/01", "165/320", Icons.Default.LocalCafe),
                        Triple("Almuerzo 10/01", "210/320", Icons.Default.Restaurant),
                        Triple("Cena 10/01", "87/215", Icons.Default.WineBar)
                    )

                    listBeneficiosMock.forEach { (name, progress, icon) ->
                        val parts = progress.split("/")
                        val currentVal = parts[0].toFloatOrNull() ?: 0f
                        val totalVal = parts[1].toFloatOrNull() ?: 1f
                        val progressPercent = currentVal / totalVal

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIrALector() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ícono del beneficio
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(ColorPrimary.copy(alpha = 0.08f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = ColorPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            color = ColorTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = progress,
                                            color = ColorTextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }

                                    // Botón / Ícono de Escaneo en cada Beneficio
                                    IconButton(
                                        onClick = onIrALector,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(ColorPrimary, RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Escanear beneficio",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Barra de progreso
                                LinearProgressIndicator(
                                    progress = { progressPercent },
                                    color = ColorSuccess,
                                    trackColor = Color(0xFFF1F5F9),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Componente Reutilizable: Caja de Estadísticas ───────────────────────────
@Composable
private fun StatsBox(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = ColorTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = ColorTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
