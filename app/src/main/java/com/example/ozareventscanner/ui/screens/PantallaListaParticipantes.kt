package com.example.ozareventscanner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.ozareventscanner.data.BeneficioEntity
import com.example.ozareventscanner.data.ParticipanteConBeneficios
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.EventoViewModel

/**
 * Pantalla de Asistentes — Zendov Scanner.
 * Lista limpia y plana con divisores sutiles, buscador y ModalBottomSheet
 * nativo de Material 3 para desglose y acreditación manual de beneficios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaParticipantes(
    viewModel: EventoViewModel,
    nombreEvento: String,
    mostrarBotonVolver: Boolean = false,
    onVolver: () -> Unit = {}
) {
    val lazyPagingItems = viewModel.participantesPaginados.collectAsLazyPagingItems()
    val totalAsistentes by viewModel.totalAsistentes.collectAsState()
    val beneficios by viewModel.beneficiosActivos.collectAsState()
    val query by viewModel.busquedaQuery.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    // Participante seleccionado para el ModalBottomSheet de beneficios
    var participanteSeleccionado by remember { mutableStateOf<ParticipanteConBeneficios?>(null) }

    val beneficiosDisponibles = beneficios
    val totalBeneficios = beneficiosDisponibles.size
    val estaCargandoInicial = lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Page Header: Título y Avatar ───────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mostrarBotonVolver) {
                        IconButton(
                            onClick = onVolver,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = ColorForeground
                            )
                        }
                    }
                    Column {
                        Text(
                            text = nombreEvento,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForegroundMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Asistentes",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorForeground,
                            letterSpacing = (-0.04).sp
                        )
                    }
                }

                // Avatar con punto verde
                Box(modifier = Modifier.size(44.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(GrayDark2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LM",
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

        // ── 2. Search Box Estilo Figma ────────────────────────────────────────
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.actualizarBusqueda(it) },
                placeholder = {
                    Text(
                        "Buscar por nombre o documento",
                        color = ColorForegroundMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GreenDark2
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { viewModel.actualizarBusqueda("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = ColorForegroundMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = GreenDark2,
                    unfocusedBorderColor = Color(0x1F00684A),
                    cursorColor = GreenDark2,
                    focusedTextColor = ColorForeground,
                    unfocusedTextColor = ColorForeground
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── 3. Summary Bar: Total de Participantes Registrados ─────────────────
        item {
            Text(
                text = if (query.isBlank()) {
                    "$totalAsistentes participantes registrados"
                } else {
                    "${lazyPagingItems.itemCount} resultado(s) cargados"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ColorForegroundMuted
            )
        }

        // ── 4. Lista Virtualizada Paginada de Asistentes (Paging 3) ───────────
        if (estaCargandoInicial) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenDark2)
                }
            }
        } else if (lazyPagingItems.itemCount == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, GrayLight2),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = ColorForegroundMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (query.isBlank()) "No hay asistentes en este momento" else "Sin resultados para \"$query\"",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForeground
                        )
                    }
                }
            }
        } else {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.idParticipante }
            ) { index ->
                val participante = lazyPagingItems[index] ?: return@items
                val cantidadEntregados = participante.beneficiosEntregados.size

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, GrayLight2),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    FilaParticipante(
                        participante = participante,
                        cantidadEntregados = cantidadEntregados,
                        totalBeneficios = totalBeneficios,
                        onClick = {
                            participanteSeleccionado = participante
                        }
                    )
                }
            }
        }
    }

    // ── 5. ModalBottomSheet Nativo Material 3: Beneficios Asignados ───────────
    if (participanteSeleccionado != null) {
        val seleccionadoId = participanteSeleccionado?.idParticipante
        val participanteActual = (0 until lazyPagingItems.itemCount)
            .asSequence()
            .mapNotNull { lazyPagingItems.peek(it) }
            .firstOrNull { it.idParticipante == seleccionadoId }
            ?: participanteSeleccionado!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val entregadosSet = participanteActual.beneficiosEntregados

        ModalBottomSheet(
            onDismissRequest = { participanteSeleccionado = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = GrayLight1)
            }
        ) {
            ModalBeneficiosParticipante(
                participante = participanteActual,
                beneficios = beneficiosDisponibles,
                beneficiosEntregados = entregadosSet,
                totalBeneficios = totalBeneficios,
                onEntregarBeneficio = { beneficio ->
                    viewModel.entregarBeneficioIndividual(
                        idParticipante = participanteActual.idParticipante,
                        beneficioId = beneficio.id
                    )
                },
                onCerrar = { participanteSeleccionado = null }
            )
        }
    }
}

/**
 * Fila sencilla y limpia para la lista plana de participantes.
 */
@Composable
private fun FilaParticipante(
    participante: ParticipanteConBeneficios,
    cantidadEntregados: Int,
    totalBeneficios: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = participante.nombre,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorForeground,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (cantidadEntregados > 0) GreenLight3 else GrayLight2
        ) {
            Text(
                text = "$cantidadEntregados/$totalBeneficios",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (cantidadEntregados > 0) GreenDark2 else ColorForegroundMuted,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Contenido del ModalBottomSheet para visualizar beneficios asignados y acreditar entrega uno a uno.
 */
@Composable
private fun ModalBeneficiosParticipante(
    participante: ParticipanteConBeneficios,
    beneficios: List<BeneficioEntity>,
    beneficiosEntregados: Set<String>,
    totalBeneficios: Int,
    onEntregarBeneficio: (BeneficioEntity) -> Unit,
    onCerrar: () -> Unit
) {
    val todosEntregados = totalBeneficios > 0 && beneficiosEntregados.size >= totalBeneficios
    val entregadosCount = beneficiosEntregados.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        // ── Header: Nombre del Asistente y Botón Cerrar (Sin códigos secundarios) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = participante.nombre,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ColorForeground,
                letterSpacing = (-0.02).sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onCerrar,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = ColorForegroundMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 1.dp, color = GrayLight2)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Título y Resumen ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Beneficios asignados",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ColorForeground
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (todosEntregados) GreenLight3 else StatusWarningSoft
            ) {
                Text(
                    text = "$entregadosCount de $totalBeneficios entregados",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (todosEntregados) GreenDark2 else StatusWarningText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Lista de Beneficios del Evento — Entrega Uno a Uno ────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            beneficios.forEach { beneficio ->
                val entregado = beneficiosEntregados.contains(beneficio.id)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FBFA), RoundedCornerShape(12.dp))
                        .border(1.dp, GrayLight2, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (entregado) GreenLight2 else GrayLight2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (entregado) Icons.Default.Check else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (entregado) GreenDark2 else GrayBase,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = beneficio.nombre,
                            fontSize = 14.sp,
                            fontWeight = if (entregado) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (entregado) ColorForeground else ColorForegroundMuted
                        )
                    }

                    if (entregado) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = GreenLight3
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = GreenDark2,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Entregado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark2
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { onEntregarBeneficio(beneficio) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenDark2,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Entregar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── Estado al pie del modal ───────────────────────────────────────────
        if (todosEntregados) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GreenLight3,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = GreenDark2,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Todos los beneficios han sido entregados",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenDark2
                    )
                }
            }
        }
    }
}
