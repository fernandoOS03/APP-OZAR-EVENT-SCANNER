package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AuthViewModel
import com.example.ozareventscanner.viewmodel.EventoViewModel
import com.example.ozareventscanner.viewmodel.SyncUiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pestaña "Perfil / Sincronización".
 *
 * Muestra:
 *  1. Identificación del staff actualmente autenticado.
 *  2. Indicador visual del estado de conexión a internet (En línea / Offline).
 *  3. Panel de sincronización bidireccional (Room ↔ Backend) con botón "Sincronizar ahora".
 *  4. Botón para cerrar sesión de forma segura.
 */
@Composable
fun PantallaPerfil(
    authViewModel: AuthViewModel,
    eventoViewModel: EventoViewModel,
    onCerrarSesion: () -> Unit
) {
    val nombreStaff     by authViewModel.nombreUsuario.collectAsState()
    val syncState       by eventoViewModel.syncUiState.collectAsState()
    val hayInternet     by eventoViewModel.hayInternet.collectAsState()
    var mostrarDialogLogout by remember { mutableStateOf(false) }

    var ultimaSyncTexto by remember { mutableStateOf("No sincronizado aún") }

    LaunchedEffect(syncState) {
        if (syncState is SyncUiState.Exito) {
            ultimaSyncTexto = SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }

    LaunchedEffect(Unit) {
        eventoViewModel.verificarConexion()
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface)
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Perfil & Sincronización",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Tarjeta de Identificación del Staff ────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ColorPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nombreStaff.isNotBlank()) nombreStaff else "Carlos Administrador",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Text(
                            text = "Staff Operativo · Validador",
                            fontSize = 13.sp,
                            color = ColorPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "admin@ozar.com",
                            fontSize = 12.sp,
                            color = ColorTextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // ── 2. Tarjeta de Estado de Conexión a Internet ───────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTADO DE CONECTIVIDAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextSecondary,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // Chip de estado
                        Surface(
                            color = if (hayInternet) ColorSuccessBg else ColorErrorBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (hayInternet) ColorSuccess else ColorError)
                                )
                                Text(
                                    text = if (hayInternet) "En Línea" else "Sin Conexión",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hayInternet) ColorSuccess else ColorError
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (hayInternet)
                            "La aplicación tiene conexión a internet activa. Los escaneos se sincronizan automáticamente con el backend."
                        else
                            "Modo Fuera de Línea activo. Los escaneos se están guardando localmente en Room y se sincronizarán al recuperar la red.",
                        fontSize = 13.sp,
                        color = ColorTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── 3. Tarjeta de Sincronización Manual (Room ↔ Backend) ───────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "SINCRONIZACIÓN DE DATOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = ColorPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Última sincronización:",
                            fontSize = 13.sp,
                            color = ColorTextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = ultimaSyncTexto,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTextPrimary
                        )
                    }

                    // Feedback de sincronización
                    AnimatedVisibility(visible = syncState is SyncUiState.Exito) {
                        val stats = (syncState as? SyncUiState.Exito)?.stats
                        Surface(
                            color = ColorSuccessBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ColorSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Sincronizado: ${stats?.subidos ?: 0} subidos, ${stats?.descargados ?: 0} descargados.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorSuccess
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = syncState is SyncUiState.Error) {
                        Surface(
                            color = ColorErrorBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ColorError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = (syncState as? SyncUiState.Error)?.mensaje ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorError
                                )
                            }
                        }
                    }

                    // Botón Sincronizar Ahora
                    Button(
                        onClick = { eventoViewModel.forzarSincronizacion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = syncState !is SyncUiState.Sincronizando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (syncState is SyncUiState.Sincronizando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sincronizando...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sincronizar Ahora",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 4. Botón Cerrar Sesión ────────────────────────────────────────
            OutlinedButton(
                onClick = { mostrarDialogLogout = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorError.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = ColorError,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar Sesión",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorError
                )
            }
        }
    }

    // ── Diálogo de confirmación de Logout ─────────────────────────────────────
    if (mostrarDialogLogout) {
        AlertDialog(
            onDismissRequest = { mostrarDialogLogout = false },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas cerrar la sesión actual del staff?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogLogout = false
                        authViewModel.cerrarSesion()
                        onCerrarSesion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)
                ) {
                    Text("Cerrar Sesión", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogLogout = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
