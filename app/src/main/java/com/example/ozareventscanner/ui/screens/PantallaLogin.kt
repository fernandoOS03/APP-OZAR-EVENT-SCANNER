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

// ─────────────────────────────────────────────────────────────────────────────
// Colores locales de la pantalla de login
// ─────────────────────────────────────────────────────────────────────────────
private val LoginFondo1   = Color(0xFF1E3A8A) // Azul marino profundo
private val LoginFondo2   = Color(0xFF2563EB) // Azul brillante
private val LoginFondo3   = Color(0xFF0F172A) // Casi negro
private val LoginCard     = Color(0xFFFFFFFF)
private val LoginAccent   = ColorPrimary

/**
 * Pantalla de Login — acceso a la aplicación.
 *
 * ▸ Credenciales de demo:
 *     Email    : admin@ozar.com
 *     Password : ozar2025
 *
 * ▸ Cuando el backend de auth esté listo, conectar [AuthViewModel.iniciarSesion]
 *   al endpoint real y eliminar las credenciales hardcodeadas.
 *
 * @param viewModel   ViewModel de autenticación.
 * @param onLoginExitoso Callback que navega a la pantalla principal tras el login exitoso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLogin(
    viewModel: AuthViewModel,
    onLoginExitoso: () -> Unit
) {
    // ── Estado local ──────────────────────────────────────────────────────────
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }
    var pantallaVisible by remember { mutableStateOf(false) }

    val loginState     by viewModel.loginState.collectAsState()
    val keyboard        = LocalSoftwareKeyboardController.current
    val focusManager    = LocalFocusManager.current

    // Trigger de animación de entrada
    LaunchedEffect(Unit) { pantallaVisible = true }

    // Reaccionar al estado de login
    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Autenticado) {
            onLoginExitoso()
        }
    }

    // ── Fondo degradado ───────────────────────────────────────────────────────
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
                .background(
                    LoginFondo2.copy(alpha = 0.15f),
                    RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    LoginAccent.copy(alpha = 0.1f),
                    RoundedCornerShape(50)
                )
        )

        // ── Contenido principal ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = pantallaVisible,
            enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // ── Logo + Nombre ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(listOf(LoginFondo2, LoginAccent)),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.QrCodeScanner,
                        contentDescription = "Logo Ozar",
                        tint               = Color.White,
                        modifier           = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text       = "Ozar Event Scanner",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    textAlign  = TextAlign.Center
                )
                Text(
                    text    = "Inicia sesión para continuar",
                    fontSize = 14.sp,
                    color   = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // ── Tarjeta del formulario ────────────────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(24.dp),
                    colors    = CardDefaults.cardColors(containerColor = LoginCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // Título de la tarjeta
                        Text(
                            text       = "Bienvenido",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = ColorTextPrimary
                        )
                        Text(
                            text    = "Ingresa tus credenciales",
                            fontSize = 13.sp,
                            color   = ColorTextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // ── Campo Email ───────────────────────────────────────
                        OutlinedTextField(
                            value          = email,
                            onValueChange  = { email = it; viewModel.limpiarError() },
                            label          = { Text("Correo electrónico") },
                            placeholder    = { Text("admin@ozar.com") },
                            leadingIcon    = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = LoginAccent
                                )
                            },
                            singleLine     = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction    = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            enabled = loginState !is LoginUiState.Autenticado,
                            colors  = outlinedFieldColors(),
                            shape   = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ── Campo Contraseña ──────────────────────────────────
                        OutlinedTextField(
                            value         = password,
                            onValueChange = { password = it; viewModel.limpiarError() },
                            label         = { Text("Contraseña") },
                            leadingIcon   = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = LoginAccent
                                )
                            },
                            trailingIcon  = {
                                IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                    Icon(
                                        imageVector = if (mostrarPassword)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                        contentDescription = if (mostrarPassword) "Ocultar" else "Mostrar",
                                        tint = ColorTextSecondary
                                    )
                                }
                            },
                            singleLine           = true,
                            visualTransformation = if (mostrarPassword)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction    = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboard?.hide()
                                    viewModel.iniciarSesion(email, password)
                                }
                            ),
                            enabled  = loginState !is LoginUiState.Autenticado,
                            colors   = outlinedFieldColors(),
                            shape    = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ── Mensaje de error ──────────────────────────────────
                        AnimatedVisibility(visible = loginState is LoginUiState.ErrorCredenciales) {
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
                                    tint     = ColorError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text     = (loginState as? LoginUiState.ErrorCredenciales)?.mensaje ?: "",
                                    color    = ColorError,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // ── Botón Iniciar Sesión ──────────────────────────────
                        Button(
                            onClick = {
                                keyboard?.hide()
                                viewModel.iniciarSesion(email, password)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginAccent,
                                contentColor   = Color.White
                            ),
                            enabled = email.isNotBlank() && password.isNotBlank()
                        ) {
                            Text(
                                text       = "Iniciar Sesión",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Hint de credenciales (solo en modo demo) ──────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint     = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text       = "Credenciales de demostración",
                                color      = Color.White.copy(alpha = 0.9f),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text    = "admin@ozar.com  /  ozar2025",
                                color   = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Colores reutilizables para los OutlinedTextField ─────────────────────────
@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = LoginAccent,
    unfocusedBorderColor  = Color(0xFFE2E8F0),
    cursorColor           = LoginAccent,
    focusedTextColor      = ColorTextPrimary,
    unfocusedTextColor    = ColorTextPrimary,
    focusedLabelColor     = LoginAccent,
    unfocusedLabelColor   = ColorTextSecondary,
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC)
)
