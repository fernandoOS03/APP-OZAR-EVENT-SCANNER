package com.example.ozareventscanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AuthViewModel
import com.example.ozareventscanner.viewmodel.LoginUiState

private val LoginFondo1 = Color(0xFF1E3A8A)
private val LoginFondo2 = Color(0xFF2563EB)
private val LoginFondo3 = Color(0xFF0F172A)
private val LoginCard   = Color(0xFFFFFFFF)
private val LoginAccent = ColorPrimary

/**
 * Pantalla de Login — Acceso exclusivo para el Staff con Documento y PIN numéricos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLogin(
    viewModel: AuthViewModel,
    onLoginExitoso: () -> Unit
) {
    var documento       by remember { mutableStateOf("") }
    var pin             by remember { mutableStateOf("") }
    var mostrarPin      by remember { mutableStateOf(false) }
    var pantallaVisible by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()
    val keyboard   = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { pantallaVisible = true }

    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Autenticado) {
            onLoginExitoso()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to LoginFondo3,
                        0.5f to LoginFondo1,
                        1.0f to LoginFondo2
                    )
                )
            )
    ) {
        // Círculos decorativos de fondo
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(LoginFondo2.copy(alpha = 0.15f), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(LoginAccent.copy(alpha = 0.1f), RoundedCornerShape(50))
        )

        AnimatedVisibility(
            visible = pantallaVisible,
            enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Logo + Título ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.radialGradient(listOf(LoginFondo2, LoginAccent)),
                            RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Logo Ozar",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Ozar Event Scanner",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Control de Acceso y Beneficios",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // ── Tarjeta Formulario (Documento y PIN) ───────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = LoginCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Acceso de Operador",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Text(
                            text = "Ingresa tu número de documento y PIN",
                            fontSize = 13.sp,
                            color = ColorTextSecondary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // ── Campo Número de Documento (Teclado Numérico) ───────
                        OutlinedTextField(
                            value = documento,
                            onValueChange = {
                                if (it.length <= 15 && it.all { char -> char.isDigit() }) {
                                    documento = it
                                    viewModel.limpiarError()
                                }
                            },
                            label = { Text("Número de Documento (DNI)") },
                            placeholder = { Text("Ej: 74839201") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = LoginAccent
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            enabled = loginState !is LoginUiState.Cargando,
                            colors = outlinedFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ── Campo PIN (Teclado Numérico Password) ──────────────
                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                    pin = it
                                    viewModel.limpiarError()
                                }
                            },
                            label = { Text("PIN de Acceso") },
                            placeholder = { Text("••••") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Pin,
                                    contentDescription = null,
                                    tint = LoginAccent
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { mostrarPin = !mostrarPin }) {
                                    Icon(
                                        imageVector = if (mostrarPin)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                        contentDescription = if (mostrarPin) "Ocultar PIN" else "Mostrar PIN",
                                        tint = ColorTextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (mostrarPin)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboard?.hide()
                                    viewModel.iniciarSesion(documento, pin)
                                }
                            ),
                            enabled = loginState !is LoginUiState.Cargando,
                            colors = outlinedFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ── Mensaje de Error ──────────────────────────────────
                        AnimatedVisibility(visible = loginState is LoginUiState.Error) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ColorErrorBg)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ColorError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = (loginState as? LoginUiState.Error)?.mensaje ?: "",
                                    color = ColorError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // ── Botón Ingresar ────────────────────────────────────
                        Button(
                            onClick = {
                                keyboard?.hide()
                                viewModel.iniciarSesion(documento, pin)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginAccent,
                                contentColor = Color.White
                            ),
                            enabled = documento.isNotBlank() && pin.isNotBlank() && loginState !is LoginUiState.Cargando
                        ) {
                            if (loginState is LoginUiState.Cargando) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Ingresar al Evento",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = LoginAccent,
    unfocusedBorderColor    = Color(0xFFE2E8F0),
    cursorColor             = LoginAccent,
    focusedTextColor        = ColorTextPrimary,
    unfocusedTextColor      = ColorTextPrimary,
    focusedLabelColor       = LoginAccent,
    unfocusedLabelColor     = ColorTextSecondary,
    focusedContainerColor   = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC)
)
