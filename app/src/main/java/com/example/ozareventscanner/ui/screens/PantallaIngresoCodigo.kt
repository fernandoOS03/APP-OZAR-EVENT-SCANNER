package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.EventoViewModel
import com.example.ozareventscanner.viewmodel.RegistroUiState

// Alias semánticos locales alineados a la nueva paleta clara
private val ColorFondo1     = ColorDark
private val ColorFondo2     = GradientFondo[1]
private val ColorAccentLocal = ColorPrimary
private val VerdeExito       = ColorSuccess
private val RojoError        = ColorError
private val TextoSecundario  = ColorTextSecondary
private val Superficie       = ColorSurface

/**
 * Pantalla 2 — Ingreso de Código de Evento.
 *
 * Responsabilidades:
 *  - Mostrar el nombre del evento activo.
 *  - Campo de texto para ingresar el código del participante.
 *  - Botón que llama a [EventoViewModel.registrarCodigo].
 *  - Feedback visual según [RegistroUiState] (loading, éxito, error).
 *  - Guardado automático en Room a través del ViewModel.
 *
 * @param viewModel  ViewModel que contiene toda la lógica de negocio.
 * @param nombreEvento Nombre del evento activo a mostrar en pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaIngresoCodigo(
    viewModel: EventoViewModel,
    nombreEvento: String = "Evento Ozar 2025"
) {
    // ── Estado local del campo de texto ─────────────────────────────────────
    var codigoIngresado by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ── Estado reactivo del ViewModel ────────────────────────────────────────
    val uiState by viewModel.uiState.collectAsState()

    // Cuando llega Exito, vaciamos el campo automáticamente
    LaunchedEffect(uiState) {
        if (uiState is RegistroUiState.Exito) {
            codigoIngresado = ""
        }
    }

    // ── Layout principal ─────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ColorFondo1, ColorFondo2)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Ícono decorativo ─────────────────────────────────────────────
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = "Ícono código QR",
                tint = ColorAccentLocal,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Título del evento ─────────────────────────────────────────────
            Text(
                text = nombreEvento,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Ingresa el código del participante para registrarlo",
                fontSize = 14.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 32.dp)
            )

            // ── Card contenedora del formulario ──────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Superficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ── Campo de texto ────────────────────────────────────────
                    OutlinedTextField(
                        value = codigoIngresado,
                        onValueChange = { nuevoValor ->
                            // Mientras el usuario edita, volvemos a Idle para limpiar mensajes
                            if (uiState !is RegistroUiState.Idle) viewModel.resetearEstado()
                            codigoIngresado = nuevoValor.uppercase()
                        },
                        label = { Text("Código del participante") },
                        placeholder = { Text("Ej: ABC123") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization  = KeyboardCapitalization.Characters,
                            keyboardType    = KeyboardType.Text,
                            imeAction       = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                viewModel.registrarCodigo(codigoIngresado)
                            }
                        ),
                        enabled = uiState !is RegistroUiState.Cargando,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = ColorAccentLocal,
                            unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                            cursorColor          = ColorAccentLocal,
                            focusedTextColor     = ColorTextPrimary,
                            unfocusedTextColor   = ColorTextPrimary,
                            focusedLabelColor    = ColorAccentLocal,
                            unfocusedLabelColor  = TextoSecundario
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Botón de registro ─────────────────────────────────────
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.registrarCodigo(codigoIngresado)
                        },
                        enabled = codigoIngresado.isNotBlank() && uiState !is RegistroUiState.Cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccentLocal, contentColor = Color.White)
                    ) {
                        if (uiState is RegistroUiState.Cargando) {
                            // Indicador de carga dentro del botón
                            CircularProgressIndicator(
                                color     = Color.White,
                                modifier  = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text       = "Registrar Participante",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Feedback de resultado (animado) ───────────────────────────────
            AnimatedVisibility(
                visible = uiState is RegistroUiState.Exito || uiState is RegistroUiState.Error,
                enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                exit    = fadeOut(tween(200))
            ) {
                when (val estado = uiState) {

                    is RegistroUiState.Exito -> ResultadoCard(
                        icono     = Icons.Default.CheckCircle,
                        color     = VerdeExito,
                        bgColor   = ColorSuccessBg,
                        titulo    = "¡Registrado exitosamente!",
                        subtitulo = "Bienvenido, ${estado.nombre}",
                        onDismiss = { viewModel.resetearEstado() }
                    )

                    is RegistroUiState.Error -> ResultadoCard(
                        icono     = Icons.Default.Error,
                        color     = RojoError,
                        bgColor   = ColorErrorBg,
                        titulo    = "Error al registrar",
                        subtitulo = estado.mensaje,
                        onDismiss = { viewModel.resetearEstado() }
                    )

                    else -> Unit
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componente auxiliar: tarjeta de resultado (éxito / error)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultadoCard(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    titulo: String,
    subtitulo: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = titulo,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = titulo,
                    fontWeight = FontWeight.Bold,
                    color      = color,
                    fontSize   = 15.sp
                )
                Text(
                    text     = subtitulo,
                    color    = ColorTextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            TextButton(onClick = onDismiss) {
                Text("OK", color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}
