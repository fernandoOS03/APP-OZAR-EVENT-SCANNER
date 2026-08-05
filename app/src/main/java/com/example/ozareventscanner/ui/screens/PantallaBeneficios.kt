package com.example.ozareventscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ozareventscanner.viewmodel.BeneficioMock

/**
 * Pantalla 3 — Ozar Event Detail.
 * Réplica exacta del mockup "Ozar Event Detail".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBeneficios(
    viewModel: AppViewModel,
    nombreEvento: String,
    onIrALector: () -> Unit,
    onVolver: () -> Unit
) {
    val beneficios by viewModel.beneficios.collectAsState()
    val escaneados = beneficios.count { it.escaneado }
    val pendientes = beneficios.count { !it.escaneado }

    // Estado para la navegación inferior activa
    var activeTab by remember { mutableStateOf("eventos") }

    Scaffold(
        containerColor = ColorBackground,
        bottomBar = {
            FloatingBottomBar(
                activeTab = activeTab,
                onTabSelected = { tab ->
                    activeTab = tab
                    if (tab == "inicio") {
                        onVolver()
                    } else if (tab == "escanear") {
                        onIrALector()
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Top Bar (Back Arrow, Title, and More Button) ────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Atrás
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

                    // Botón Tres Puntos (...)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { /* Acción menú */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Más opciones",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── 3 Tarjetas de Estadísticas Horizontales ─────────────────────
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

            // ── Sección Beneficios ──────────────────────────────────────────
            item {
                Text(
                    text = "Beneficios",
                    color = ColorTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Lista de beneficios con barra de progreso verde
            item {
                // Definimos una lista mock de beneficios de ejemplo con progreso
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
                        // Extraemos porcentaje para la barra
                        val parts = progress.split("/")
                        val currentVal = parts[0].toFloatOrNull() ?: 0f
                        val totalVal = parts[1].toFloatOrNull() ?: 1f
                        val progressPercent = currentVal / totalVal

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icono en círculo
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ColorPrimary.copy(alpha = 0.08f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = ColorPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = name,
                                        color = ColorTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = progress,
                                        color = ColorTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Barra de progreso verde
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

            // ── Sección Asistentes ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Asistentes",
                        color = ColorTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$escaneados / ${beneficios.size}",
                        color = ColorPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Lista agrupada de asistentes con checkmark
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        beneficios.take(6).forEachIndexed { index, beneficio ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = beneficio.nombre,
                                    color = ColorTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                // Checkmark verde en badge circular
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(ColorSuccessBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Confirmado",
                                        tint = ColorSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            if (index < beneficios.take(6).size - 1) {
                                HorizontalDivider(
                                    color = Color(0xFFF1F5F9),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componente de Estadísticas en Caja
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatsBox(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = ColorTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
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
