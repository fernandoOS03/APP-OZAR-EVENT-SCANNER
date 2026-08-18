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
 * ModalBottomSheet de Material 3 para mostrar el resultado inmediato
 * tras un escaneo QR y permitir la validación de identidad del participante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetResultadoScan(
    data: ScanModalData,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Configuración visual según el estado del escaneo
    val (icono, colorPrimario, colorFondoIcono, titulo, subtitulo) = when (data.estado) {
        is ScanUiState.Valido -> ConfigModal(
            icono = Icons.Default.CheckCircle,
            color = ColorSuccess,
            colorFondo = ColorSuccessBg,
            titulo = "¡Beneficio Entregado!",
            subtitulo = "El participante es válido y su beneficio fue registrado."
        )
        is ScanUiState.YaEscaneado -> ConfigModal(
            icono = Icons.Default.Warning,
            color = ColorWarning,
            colorFondo = ColorWarningBg,
            titulo = "Atención: Ya Escaneado",
            subtitulo = "Este participante ya recibió su beneficio anteriormente."
        )
        is ScanUiState.FormatoInvalido -> ConfigModal(
            icono = Icons.Default.QrCodeScanner,
            color = ColorWarning,
            colorFondo = ColorWarningBg,
            titulo = "Código QR No Válido",
            subtitulo = "El formato escaneado no corresponde a un participante del evento."
        )
        is ScanUiState.NoEncontrado -> ConfigModal(
            icono = Icons.Default.PersonOff,
            color = ColorError,
            colorFondo = ColorErrorBg,
            titulo = "Participante No Encontrado",
            subtitulo = "El ID no está registrado en la base de datos local."
        )
        is ScanUiState.ErrorScan -> ConfigModal(
            icono = Icons.Default.Error,
            color = ColorError,
            colorFondo = ColorErrorBg,
            titulo = "Error de Validación",
            subtitulo = data.estado.mensaje
        )
        else -> ConfigModal(
            icono = Icons.Default.Info,
            color = ColorPrimary,
            colorFondo = ColorInfoBg,
            titulo = "Escaneo Realizado",
            subtitulo = "Procesando información..."
        )
    }

    val fechaHoraFormateada = SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", Locale.getDefault())
        .format(Date(data.timestamp))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ColorSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = ColorTextDisabled.copy(alpha = 0.5f)
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
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colorFondoIcono),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorPrimario,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Título y Subtítulo ───────────────────────────────────────────
            Text(
                text = titulo,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitulo,
                fontSize = 13.sp,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // ── Tarjeta de Ficha de Identidad del Participante ───────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DATOS DEL PARTICIPANTE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextSecondary,
                        letterSpacing = 0.8.sp
                    )

                    // Nombre Completo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ColorPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (data.nombre.isNotBlank()) data.nombre else "No identificado",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                    }

                    // ID / Código
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ID / DNI: ",
                            fontSize = 13.sp,
                            color = ColorTextSecondary
                        )
                        Text(
                            text = data.idParticipante,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTextPrimary
                        )
                    }

                    // Fecha y Hora del Escaneo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fechaHoraFormateada,
                            fontSize = 13.sp,
                            color = ColorTextSecondary
                        )
                    }

                    // Estado de persistencia local
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = ColorSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Persistido en base local (Room)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorSuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Botón de Acción Principal ────────────────────────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPrimario,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Siguiente Escaneo",
                    fontSize = 15.sp,
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
