package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AppViewModel
import com.example.ozareventscanner.viewmodel.EventoMock

/**
 * Pantalla 2 — Ozar Dashboard.
 * Réplica exacta del mockup "Ozar Dashboard".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEventos(
    viewModel: AppViewModel,
    onEventoSeleccionado: (EventoMock) -> Unit
) {
    val eventos by viewModel.eventos.collectAsState()
    val token   by viewModel.token.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    // Estado para la navegación inferior activa
    var activeTab by remember { mutableStateOf("inicio") }

    Scaffold(
        containerColor = ColorBackground,
        bottomBar = {
            FloatingBottomBar(
                activeTab = activeTab,
                onTabSelected = { tab ->
                    activeTab = tab
                    // Si se selecciona escanear, ejecutamos la acción correspondiente
                    if (tab == "escanear") {
                        // Simulación/Navegación al lector
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ── Header (Avatar, Bienvenida y Campana) ────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar del Usuario (Carlos)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        // En lugar de una imagen pesada, usamos un ícono de persona estilizado
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Bienvenido,",
                            color = ColorTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Hola, Carlos",
                            color = ColorTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Botón de Notificación con campana
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { /* Acción de notificaciones */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notificaciones",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        // Pequeño indicador verde
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ColorSuccess, CircleShape)
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .offset(x = (-8).dp, y = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Sección Mis Eventos ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis Eventos",
                        color = ColorTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Ver todos",
                        color = ColorPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Ver todos */ }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lista de eventos del usuario
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(eventos, key = { it.id }) { evento ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.seleccionarEvento(evento)
                                        onEventoSeleccionado(evento)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ColorSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = evento.nombre,
                                        color = ColorTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    // Punto azul del mockup
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(ColorPrimary, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    // Sección REGISTRAR TOKEN
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "REGISTRAR TOKEN",
                                    color = ColorTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = token,
                                        onValueChange = viewModel::actualizarToken,
                                        placeholder = { Text("Ej: EVT-2025-XXXX", color = ColorTextSecondary.copy(alpha = 0.6f)) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8FAFC),
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedBorderColor = Color(0xFFE2E8F0),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedTextColor = ColorTextPrimary,
                                            unfocusedTextColor = ColorTextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Button(
                                        onClick = {
                                            keyboard?.hide()
                                            viewModel.validarToken()
                                        },
                                        enabled = token.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ColorSuccess,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text(
                                            text = "Validar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componente de Barra de Navegación Inferior Flotante
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FloatingBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                icon = Icons.Default.Home,
                label = "Inicio",
                isActive = activeTab == "inicio",
                onClick = { onTabSelected("inicio") }
            )
            BottomBarItem(
                icon = Icons.Default.DateRange,
                label = "Eventos",
                isActive = activeTab == "eventos",
                onClick = { onTabSelected("eventos") }
            )
            BottomBarItem(
                icon = Icons.Default.QrCodeScanner,
                label = "Escanear",
                isActive = activeTab == "escanear",
                onClick = { onTabSelected("escanear") }
            )
            BottomBarItem(
                icon = Icons.Default.Settings,
                label = "Ajustes",
                isActive = activeTab == "ajustes",
                onClick = { onTabSelected("ajustes") }
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 30.dp)
                .background(
                    color = if (isActive) ColorPrimary.copy(alpha = 0.08f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) ColorPrimary else ColorTextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) ColorPrimary else ColorTextSecondary.copy(alpha = 0.7f)
        )
    }
}
