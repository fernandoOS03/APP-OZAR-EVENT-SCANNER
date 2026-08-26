package com.example.ozareventscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.data.EscaneoEntity
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.network.BeneficioDto
import com.example.ozareventscanner.ui.theme.*

/**
 * Pantalla de Lista de Participantes (Contingencia para 10,000+ registros).
 *
 * Rendimiento Crítico:
 *  - Recibe un `Flow` indexado desde Room vía ViewModel (`LazyColumn` reactivo).
 *  - Claves estables `key = { it.idParticipante }` para reciclaje instantáneo en Compose.
 *  - Fila de solo texto: Nombre, Documento y "Beneficios: X / Total".
 *  - Estrictamente sin avatares, ni círculos con iniciales, ni emojis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantListScreen(
    totalParticipantesRegistrados: Int,
    participantes: List<ParticipanteEntity>,
    beneficios: List<BeneficioDto>,
    escaneos: List<EscaneoEntity>,
    busquedaQuery: String,
    onQueryChange: (String) -> Unit,
    onValidarEntregaBeneficio: (idParticipante: String, idBeneficio: String) -> Unit,
    onVolver: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var participanteSeleccionado by remember { mutableStateOf<ParticipanteEntity?>(null) }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Participantes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ColorTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Cabecera: KPI Global y Campo de Búsqueda ───────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // KPI Global en la Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total de Participantes Registrados",
                        fontSize = 13.sp,
                        color = ColorTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$totalParticipantesRegistrados",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorPrimary
                    )
                }

                // TextField de Búsqueda por DNI o Nombre
                OutlinedTextField(
                    value = busquedaQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Buscar por DNI o Nombre...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ColorPrimary
                        )
                    },
                    trailingIcon = {
                        if (busquedaQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = ColorTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorBackground,
                        unfocusedContainerColor = ColorBackground,
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── LazyColumn de Alto Rendimiento (10,000+ registros) ────────────
            if (participantes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (busquedaQuery.isBlank()) "No hay participantes registrados" else "Sin resultados para \"$busquedaQuery\"",
                        fontSize = 14.sp,
                        color = ColorTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = participantes,
                        key = { it.idParticipante }
                    ) { participante ->
                        val escaneosDeEste = escaneos.filter { it.idParticipante == participante.idParticipante }
                        val entregadosCount = escaneosDeEste.size

                        FilaParticipanteTexto(
                            nombre = participante.nombre,
                            documento = participante.documento,
                            entregados = entregadosCount,
                            totalBeneficios = beneficios.size,
                            beneficioEntregadoFallback = participante.beneficioEntregado,
                            onClick = { participanteSeleccionado = participante }
                        )
                    }
                }
            }
        }
    }

    // ── BottomSheet de Beneficios al Seleccionar un Participante ──────────────
    participanteSeleccionado?.let { participante ->
        ModalBottomSheetBeneficios(
            participante = participante,
            beneficios = beneficios,
            escaneos = escaneos.filter { it.idParticipante == participante.idParticipante },
            onDismiss = { participanteSeleccionado = null },
            onEntregar = { idBeneficio ->
                onValidarEntregaBeneficio(participante.idParticipante, idBeneficio)
            }
        )
    }
}

/**
 * Fila de participante de solo texto: Nombre, Documento y "Beneficios: X / Total".
 * Estrictamente sin avatares ni círculos con iniciales.
 */
@Composable
private fun FilaParticipanteTexto(
    nombre: String,
    documento: String,
    entregados: Int,
    totalBeneficios: Int,
    beneficioEntregadoFallback: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombre,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = "Documento: $documento",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            val textoBeneficios = if (totalBeneficios > 0) {
                "Beneficios: $entregados / $totalBeneficios"
            } else if (beneficioEntregadoFallback) {
                "Beneficios: 1 / 1"
            } else {
                "Beneficios: 0 / 1"
            }

            Text(
                text = textoBeneficios,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (entregados > 0 || beneficioEntregadoFallback) ColorSuccess else ColorTextSecondary
            )
        }
    }
}

/**
 * BottomSheet con desglose de beneficios para entrega manual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalBottomSheetBeneficios(
    participante: ParticipanteEntity,
    beneficios: List<BeneficioDto>,
    escaneos: List<EscaneoEntity>,
    onDismiss: () -> Unit,
    onEntregar: (idBeneficio: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ColorSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera del Participante
            Column {
                Text(
                    text = participante.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = "Documento: ${participante.documento}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            Text(
                text = "Beneficios del Participante",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextSecondary
            )

            if (beneficios.isEmpty()) {
                val entregado = participante.beneficioEntregado || escaneos.isNotEmpty()
                FilaBeneficioSimple(
                    nombre = "Entrega General",
                    estaEntregado = entregado,
                    onEntregar = { onEntregar("general") }
                )
            } else {
                beneficios.forEach { beneficio ->
                    val entregado = escaneos.any { it.idBeneficio == beneficio.idBeneficio } ||
                                    (escaneos.isNotEmpty() && escaneos.any { it.idBeneficio.isBlank() })

                    FilaBeneficioSimple(
                        nombre = beneficio.nombre,
                        estaEntregado = entregado,
                        onEntregar = { onEntregar(beneficio.idBeneficio) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaBeneficioSimple(
    nombre: String,
    estaEntregado: Boolean,
    onEntregar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (estaEntregado) ColorSuccessBg.copy(alpha = 0.4f) else ColorBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = if (estaEntregado) "Entregado" else "Pendiente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (estaEntregado) ColorSuccess else ColorWarning
                )
            }

            if (!estaEntregado) {
                Button(
                    onClick = onEntregar,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorSuccess,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Validar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "✓ Listo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSuccess
                )
            }
        }
    }
}
