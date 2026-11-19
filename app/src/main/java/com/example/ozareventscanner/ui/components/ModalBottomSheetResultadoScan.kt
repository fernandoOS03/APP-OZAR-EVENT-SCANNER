package com.example.ozareventscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.ScanModalData
import com.example.ozareventscanner.viewmodel.ScanUiState

/**
 * ModalBottomSheet compacto post-escaneo.
 * Diseño alineado a la izquierda con fondo sutil que mantiene visible el visor de cámara
 * y los puntitos del escáner, unificado para todos los estados (verde / ámbar / rojo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetResultadoScan(
    data: ScanModalData,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val config = when (data.estado) {
        is ScanUiState.Valido -> ConfigModal(
            icono = Icons.Default.Check,
            colorIcono = GreenDark2,
            colorFondoIcono = GreenLight3,
            overline = "ESCANEO EXITOSO",
            titulo = "Beneficio entregado",
            detalleError = null
        )
        is ScanUiState.YaEscaneado -> ConfigModal(
            icono = Icons.Default.PriorityHigh,
            colorIcono = StatusWarningText,
            colorFondoIcono = StatusWarningSoft,
            overline = "YA ENTREGADO",
            titulo = "Beneficio ya reclamado",
            detalleError = null
        )
        is ScanUiState.FormatoInvalido -> ConfigModal(
            icono = Icons.Default.Close,
            colorIcono = StatusDangerText,
            colorFondoIcono = StatusDangerSoft,
            overline = "CÓDIGO INVÁLIDO",
            titulo = "QR no reconocido",
            detalleError = "El formato escaneado no corresponde a un código de participante válido."
        )
        is ScanUiState.NoEncontrado -> ConfigModal(
            icono = Icons.Default.Close,
            colorIcono = StatusDangerText,
            colorFondoIcono = StatusDangerSoft,
            overline = "NO REGISTRADO",
            titulo = "Asistente no encontrado",
            detalleError = "El código escaneado no coincide con ningún participante autorizado de este evento."
        )
        is ScanUiState.ErrorScan -> ConfigModal(
            icono = Icons.Default.Close,
            colorIcono = StatusDangerText,
            colorFondoIcono = StatusDangerSoft,
            overline = "ERROR DE VALIDACIÓN",
            titulo = "No se pudo validar",
            detalleError = data.estado.mensaje
        )
        else -> ConfigModal(
            icono = Icons.Default.Info,
            colorIcono = GreenDark2,
            colorFondoIcono = GreenLight3,
            overline = "ESCANEO",
            titulo = "Procesando...",
            detalleError = null
        )
    }

    val esErrorSinParticipante = data.estado is ScanUiState.FormatoInvalido ||
        data.estado is ScanUiState.NoEncontrado ||
        data.estado is ScanUiState.ErrorScan

    val nombreMostrar = data.nombre.ifBlank { "Participante" }
    val iniciales = nombreMostrar
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "PA" }

    val documentoMostrar = data.entidad?.documento?.takeIf { it.isNotBlank() }
        ?: data.entidad?.correo?.takeIf { it.isNotBlank() }
        ?: data.idParticipante.take(12)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.22f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(GrayLight1)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Encabezado compacto: Ícono + Overline + Chip sync + Título ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(config.colorFondoIcono),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.icono,
                        contentDescription = null,
                        tint = config.colorIcono,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = config.overline,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForegroundMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )

                        if (data.estado is ScanUiState.Valido && data.pendienteSync) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = StatusWarningSoft
                            ) {
                                Text(
                                    text = "Guardado local",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusWarningText,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = config.titulo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorForeground,
                        letterSpacing = (-0.03).sp
                    )
                }
            }

            // ── 2. Tarjeta de Participante o Mensaje de Error ──────────────────
            if (!esErrorSinParticipante) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = GrayLight3
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(config.colorFondoIcono),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = iniciales,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = config.colorIcono
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nombreMostrar,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorForeground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$documentoMostrar · Participante",
                                    fontSize = 12.sp,
                                    color = ColorForegroundMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (data.estado is ScanUiState.YaEscaneado) {
                            val staffPrevio = data.staffNombre?.takeIf { it.isNotBlank() } ?: "Operador previo"
                            val fechaPrevia = data.fechaEscaneo?.takeIf { it.isNotBlank() }
                            val lineaDetalle = if (fechaPrevia != null) {
                                "Entregado por $staffPrevio · $fechaPrevia"
                            } else {
                                "Entregado por $staffPrevio"
                            }
                            Text(
                                text = lineaDetalle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatusWarningText
                            )
                        }
                    }
                }

                // ── 3. Fila Beneficio ──────────────────────────────────────────
                if (data.beneficioNombre.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Beneficio",
                            fontSize = 13.sp,
                            color = ColorForegroundMuted
                        )
                        Text(
                            text = data.beneficioNombre,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorForeground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (!config.detalleError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = StatusDangerSoft
                ) {
                    Text(
                        text = config.detalleError,
                        fontSize = 13.sp,
                        color = StatusDangerText,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // ── 4. Botón primario: Escanear otro ───────────────────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenDark2,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = ZendovIcons.Scan,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Escanear otro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class ConfigModal(
    val icono: ImageVector,
    val colorIcono: Color,
    val colorFondoIcono: Color,
    val overline: String,
    val titulo: String,
    val detalleError: String?
)
