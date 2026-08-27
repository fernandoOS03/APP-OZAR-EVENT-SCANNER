package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.EventoViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla de Lista de Participantes (Mecanismo de Contingencia).
 *
 * Permite al staff buscar participantes por Nombre o DNI/ID y
 * registrar manualmente la entrega del beneficio cuando el código QR
 * del participante esté dañado, ilegible o no disponible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaParticipantes(
    viewModel: EventoViewModel,
    nombreEvento: String,
    onVolver: () -> Unit
) {
    val participantes by viewModel.listaParticipantes.collectAsState()
    val query         by viewModel.busquedaQuery.collectAsState()
    val cargando      by viewModel.cargandoParticipantes.collectAsState()
    val keyboard      = LocalSoftwareKeyboardController.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Recargar al entrar a la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarParticipantes()
    }

    Scaffold(
        containerColor = ColorBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Fila Superior: Botón Volver + Título + Conteo
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lista de Asistentes",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Text(
                            text = nombreEvento,
                            fontSize = 12.sp,
                            color = ColorTextSecondary,
                            maxLines = 1
                        )
                    }

                    // Badge con total
                    Surface(
                        color = ColorInfoBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${participantes.size} reg.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Barra de Búsqueda ─────────────────────────────────────────
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.actualizarBusqueda(it) },
                    placeholder = { Text("Buscar por nombre o DNI / ID...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ColorPrimary
                        )
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { viewModel.actualizarBusqueda("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = ColorTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorBackground,
                        unfocusedContainerColor = ColorBackground,
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cargando && participantes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPrimary)
                }
            } else if (participantes.isEmpty()) {
                // Estado Vacío
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = ColorTextDisabled,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (query.isBlank()) "No hay participantes registrados" else "Sin resultados para \"$query\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Text(
                        text = "Puedes registrar participantes usando el código proporcionado por el backend.",
                        fontSize = 13.sp,
                        color = ColorTextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(participantes, key = { it.idParticipante }) { participante ->
                        TarjetaParticipanteItem(
                            participante = participante,
                            onEntregarManual = {
                                viewModel.entregarBeneficioManual(participante.idParticipante) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Beneficio entregado a ${participante.nombre}"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaParticipanteItem(
    participante: ParticipanteEntity,
    onEntregarManual: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Avatar con inicial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (participante.beneficioEntregado) ColorSuccessBg else ColorInfoBg
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participante.nombre.take(1).uppercase(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (participante.beneficioEntregado) ColorSuccess else ColorPrimary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participante.nombre,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Text(
                        text = "ID: ",
                        fontSize = 12.sp,
                        color = ColorTextSecondary
                    )
                    Text(
                        text = participante.idParticipante,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorTextPrimary
                    )
                }

                // Badge de estado
                Surface(
                    color = if (participante.beneficioEntregado) ColorSuccessBg else ColorWarningBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = if (participante.beneficioEntregado) "✓ Beneficio entregado" else "⏳ Pendiente de entrega",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (participante.beneficioEntregado) ColorSuccess else ColorWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Botón de Entrega Manual
            if (!participante.beneficioEntregado) {
                Button(
                    onClick = onEntregarManual,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorSuccess,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Validar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Entregado",
                    tint = ColorSuccess,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
