package com.example.ozareventscanner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.components.ZendovIcons
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AuthViewModel
import com.example.ozareventscanner.viewmodel.EventoViewModel
import com.example.ozareventscanner.viewmodel.SyncUiState

/**
 * Pantalla de Perfil — Zendov Scanner.
 * Implementación 1:1 de Figma Make con tarjeta de staff, diagnóstico del dispositivo,
 * tarjeta de sincronización y cierre de sesión.
 */
@Composable
fun PantallaPerfil(
    authViewModel: AuthViewModel,
    eventoViewModel: EventoViewModel,
    onCerrarSesion: () -> Unit
) {
    val staffActual by authViewModel.staffActual.collectAsState()
    val hayInternet by eventoViewModel.hayInternet.collectAsState()
    val syncState by eventoViewModel.syncUiState.collectAsState()
    var mostrarDialogLogout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        eventoViewModel.verificarConexion()
    }

    val nombreStaff = staffActual?.nombreCompleto ?: "Luis Mendoza"
    val dniStaff = staffActual?.documentoIdentidad ?: "12345678"
    val rolStaff = staffActual?.rol ?: staffActual?.cargo ?: "Coordinador de accesos"
    val codigoStaff = "STAFF · ZND-${dniStaff.takeLast(3).padStart(3, '0')}"

    val iniciales = nombreStaff.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "LM" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Page Header: Cuenta de staff / Mi perfil ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cuenta de staff",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorForegroundMuted,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Mi perfil",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorForeground,
                    letterSpacing = (-0.04).sp
                )
            }

            // Avatar Zendov en Header
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

        // ── 2. Profile Card Principal Figma ───────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Grande con iniciales
                Box(modifier = Modifier.size(56.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(GrayDark2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iniciales,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenLight1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(GreenBase)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombreStaff,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorForeground
                    )
                    Text(
                        text = rolStaff,
                        fontSize = 13.sp,
                        color = ColorForegroundMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = GreenLight3
                    ) {
                        Text(
                            text = codigoStaff,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark2,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // ── 3. Section Title: Estado del dispositivo ──────────────────────────
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = "Estado del dispositivo",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorForeground,
                letterSpacing = (-0.03).sp
            )
            Text(
                text = "Diagnóstico en tiempo real",
                fontSize = 12.sp,
                color = ColorForegroundMuted
            )
        }

        // ── 4. Device List Figma ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item 1: Conexión a internet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GreenLight3),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = GreenDark2,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Conexión a internet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForeground
                        )
                        Text(
                            text = if (hayInternet) "Red estable y activa" else "Sin acceso a red (modo offline)",
                            fontSize = 11.sp,
                            color = ColorForegroundMuted
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (hayInternet) GreenLight3 else StatusDangerSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (hayInternet) GreenBase else StatusDanger, CircleShape)
                            )
                            Text(
                                text = if (hayInternet) "En línea" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hayInternet) GreenDark2 else StatusDangerText
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0x1F00684A), thickness = 0.8.dp)

                // Item 2: Sincronización
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GreenLight3),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = GreenDark2,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sincronización",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForeground
                        )
                        Text(
                            text = when (syncState) {
                                is SyncUiState.Sincronizando -> "Sincronizando con backend..."
                                else -> "Actualizado hace instantes"
                            },
                            fontSize = 11.sp,
                            color = ColorForegroundMuted
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = GreenLight3
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(GreenBase, CircleShape)
                            )
                            Text(
                                text = "Al día",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenDark2
                            )
                        }
                    }
                }
            }
        }

        // ── 5. Sync Note Figma (Todo está sincronizado) ───────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFE8F8F0)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(GreenDark2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Column {
                    Text(
                        text = "Todo está sincronizado",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark3
                    )
                    Text(
                        text = "Tus escaneos se guardaron correctamente en el servidor.",
                        fontSize = 11.sp,
                        color = ColorForegroundMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── 6. Botón Cerrar Sesión Figma (.btn.btn--danger.logout) ───────────
        OutlinedButton(
            onClick = { mostrarDialogLogout = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFF7D7D4)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFFFF4F3),
                contentColor = StatusDanger
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = ZendovIcons.LogOut,
                contentDescription = null,
                tint = StatusDanger,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cerrar sesión",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StatusDanger
            )
        }

        // ── 7. App Version Footer ─────────────────────────────────────────────
        Text(
            text = "Zendov Scanner · Versión 1.0.0",
            fontSize = 11.sp,
            color = GrayBase,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )
    }

    // ── Dialog Logout ─────────────────────────────────────────────────────────
    if (mostrarDialogLogout) {
        AlertDialog(
            onDismissRequest = { mostrarDialogLogout = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    "Cerrar sesión",
                    fontWeight = FontWeight.Bold,
                    color = ColorForeground
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas salir de la estación de escaneo de $nombreStaff?",
                    color = ColorForegroundMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogLogout = false
                        authViewModel.cerrarSesion()
                        onCerrarSesion()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD33D3D))
                ) {
                    Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogLogout = false }) {
                    Text("Cancelar", color = ColorForegroundMuted)
                }
            }
        )
    }
}
