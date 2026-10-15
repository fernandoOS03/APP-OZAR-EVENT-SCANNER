package com.example.ozareventscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.ScanModalData
import com.example.ozareventscanner.viewmodel.ScanUiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * ModalBottomSheet de Material 3 con diseño editorial de Zendov Scanner.
 * Muestra el resultado instantáneo del escaneo con confirmación visual de acreditación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetResultadoScan(
    data: ScanModalData,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (icono, colorPrimario, colorFondoIcono, titulo, subtitulo) = when (data.estado) {
        is ScanUiState.Valido -> ConfigModal(
            icono      = Icons.Default.CheckCircle,
            color      = StatusSuccess,
            colorFondo = GreenLight3,
            titulo     = "¡Beneficio Entregado!",
            subtitulo  = if (data.mensajeServidor.isNotBlank()) data.mensajeServidor else "Acreditación confirmada y entrega registrada."
        )
        is ScanUiState.YaEscaneado -> ConfigModal(
            icono      = Icons.Default.Warning,
            color      = StatusWarning,
            colorFondo = StatusWarningSoft,
            titulo     = "Atención: Ya Fue Reclamado",
            subtitulo  = if (data.mensajeServidor.isNotBlank()) data.mensajeServidor else "Este beneficio ya fue entregado previamente a este asistente."
        )
        is ScanUiState.FormatoInvalido -> ConfigModal(
            icono      = Icons.Default.QrCodeScanner,
            color      = StatusWarning,
            colorFondo = StatusWarningSoft,
            titulo     = "Código QR No Válido",
            subtitulo  = "El formato escaneado no corresponde al estándar del evento."
        )
        is ScanUiState.NoEncontrado -> ConfigModal(
            icono      = Icons.Default.PersonOff,
            color      = StatusDanger,
            colorFondo = StatusDangerSoft,
            titulo     = "Asistente No Registrado",
            subtitulo  = "El código no coincide con ningún participante autorizado."
        )
        is ScanUiState.ErrorScan -> ConfigModal(
            icono      = Icons.Default.Error,
            color      = StatusDanger,
            colorFondo = StatusDangerSoft,
            titulo     = "Error en Validación",
            subtitulo  = data.estado.mensaje
        )
        else -> ConfigModal(
            icono      = Icons.Default.Info,
            color      = GreenDark2,
            colorFondo = GreenLight3,
            titulo     = "Escaneo Registrado",
            subtitulo  = "Procesando información..."
        )
    }

    val esQrInvalido = data.estado is ScanUiState.FormatoInvalido || data.estado is ScanUiState.NoEncontrado

    val fechaMostrar = data.fechaEscaneo ?: SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", Locale.getDefault())
        .format(Date(data.timestamp))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = GrayLight1
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Ícono de Estado ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(colorFondoIcono),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icono,
                    contentDescription = null,
                    tint               = colorPrimario,
                    modifier           = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Título y Subtítulo ───────────────────────────────────────────
            Text(
                text       = titulo,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = ColorForeground,
                textAlign  = TextAlign.Center,
                letterSpacing = (-0.03).sp
            )

            Text(
                text       = subtitulo,
                fontSize   = 13.sp,
                color      = ColorForegroundMuted,
                textAlign  = TextAlign.Center,
                lineHeight = 18.sp,
                modifier   = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // ── Tarjeta con Detalles del Asistente y Beneficio ───────────────
            if (!esQrInvalido) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = GrayLight3),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text          = "DETALLE DE LA OPERACIÓN",
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = ColorForegroundMuted,
                            fontFamily    = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        // Nombre Completo
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Person,
                                contentDescription = null,
                                tint               = GreenDark2,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text       = if (data.nombre.isNotBlank()) data.nombre else "Asistente",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = ColorForeground
                            )
                        }

                        // Beneficio Entregado
                        if (data.beneficioNombre.isNotBlank()) {
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint               = GreenDark1,
                                    modifier           = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text     = "Beneficio: ",
                                    fontSize = 13.sp,
                                    color    = ColorForegroundMuted
                                )
                                Text(
                                    text       = data.beneficioNombre,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = ColorForeground
                                )
                            }
                        }


                        // Fecha y Hora
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint               = GrayBase,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text     = if (data.yaEntregadoPreviamente) "Fecha original: " else "Fecha registro: ",
                                fontSize = 13.sp,
                                color    = ColorForegroundMuted
                            )
                            Text(
                                text       = fechaMostrar,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color      = ColorForeground
                            )
                        }

                        // Staff Operador
                        data.staffNombre?.let { staff ->
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint               = GreenDark2,
                                    modifier           = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text     = if (data.yaEntregadoPreviamente) "Entregado por: " else "Staff operador: ",
                                    fontSize = 13.sp,
                                    color    = ColorForegroundMuted
                                )
                                Text(
                                    text       = staff,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = ColorForeground
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Botón Siguiente Escaneo ──────────────────────────────────────
            Button(
                onClick  = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = GreenDark2,
                    contentColor   = Color.White
                )
            ) {
                Icon(
                    imageVector        = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = "Siguiente Escaneo",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class ConfigModal(
    val icono: ImageVector,
    val color: Color,
    val colorFondo: Color,
    val titulo: String,
    val subtitulo: String
)
