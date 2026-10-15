package com.example.ozareventscanner.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ozareventscanner.ui.components.ZendovBrand
import com.example.ozareventscanner.ui.components.ZendovIcons
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AuthViewModel
import com.example.ozareventscanner.viewmodel.LoginUiState

/**
 * Pantalla de Acceso — Zendov Scanner.
 * Implementación 1:1 del diseño oficial en Figma Make.
 */
@Composable
fun PantallaLogin(
    viewModel: AuthViewModel,
    onLoginExitoso: () -> Unit
) {
    var mostrarBottomSheet by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Autenticado) {
            onLoginExitoso()
        }
    }

    if (mostrarBottomSheet) {
        BackHandler {
            viewModel.limpiarError()
            mostrarBottomSheet = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayDark2)
    ) {
        // Decoración de fondo Figma: Resplandor radial y órbitas
        FondoOrbitasFigma()

        // Pantalla de Bienvenida siempre como fondo principal
        PantallaBienvenidaZendov(
            onComenzar = {
                viewModel.limpiarError()
                mostrarBottomSheet = true
            }
        )

        // Al pulsar "Comenzar", la tarjeta blanca de login emerge hasta justo debajo del logo zendov (~89% altura)
        // cubriendo la pantalla completa hacia abajo, 1:1 con Figma Make
        AnimatedVisibility(
            visible = mostrarBottomSheet,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        viewModel.limpiarError()
                        mostrarBottomSheet = false
                    }
            ) {
                // Tarjeta Blanca de Formulario: cubre exactamente el ~88% de la pantalla hasta el fondo
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .align(Alignment.BottomCenter)
                        .clickable(enabled = false) {}, // Evita dismiss al tocar dentro del formulario
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    shadowElevation = 16.dp
                ) {
                    PantallaFormularioZendov(
                        viewModel = viewModel,
                        loginState = loginState,
                        onRegresar = {
                            viewModel.limpiarError()
                            mostrarBottomSheet = false
                        }
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Pantalla 1: Bienvenida (Figma Make)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaBienvenidaZendov(
    onComenzar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Barra Superior: Brand Logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZendovBrand(mostrarProduct = true)
        }

        // Centro: Headline y Explicación
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            // Eyebrow con punto verde en vivo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(GreenBase, CircleShape)
                )
                Text(
                    text = "GESTIÓN DE EVENTOS EN VIVO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenLight1,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.8.sp
                )
            }

            // Título principal con estilo editorial Figma
            Text(
                text = "Todo listo para",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 44.sp,
                letterSpacing = (-0.05).sp
            )
            Text(
                text = "recibirlos.",
                fontSize = 44.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                color = GreenLight1,
                lineHeight = 46.sp,
                letterSpacing = (-0.04).sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Sistema de escaneo para el control de asistencia y entrega de beneficios en eventos.",
                fontSize = 14.sp,
                color = GrayBase,
                lineHeight = 22.sp
            )
        }

        // Parte Inferior: Micro-stats, Botón Comenzar y Versión
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Micro-stats de validación y sincronización
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rápido",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Validación instantánea",
                        fontSize = 11.sp,
                        color = GrayBase
                    )
                }

                // Divisor vertical sutil
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Seguro",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Datos sincronizados",
                        fontSize = 11.sp,
                        color = GrayBase
                    )
                }
            }

            // Botón Comenzar con texto centrado horizontalmente
            Button(
                onClick = onComenzar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenDark1,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Comenzar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Indicador de versión centrado
            Text(
                text = "Zendov Scanner · v1.0.0",
                fontSize = 11.sp,
                color = GrayBase.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Formulario Comenzar (Figma Make Sheet)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaFormularioZendov(
    viewModel: AuthViewModel,
    loginState: LoginUiState,
    onRegresar: () -> Unit
) {
    var dni by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var mostrarPin by remember { mutableStateOf(false) }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barra Superior del Sheet: Botón "< Regresar" (1:1 Figma Make)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRegresar() }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = ZendovIcons.ArrowLeft,
                    contentDescription = "Regresar",
                    tint = ColorForegroundMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Regresar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorForegroundMuted
                )
            }
        }

        // Títulos del Formulario: Centrados según Figma
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Comenzar",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorForeground,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.04).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ingresa tus credenciales de staff",
                fontSize = 14.sp,
                color = ColorForegroundMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Campo: Documento de identidad (sin leadingIcon)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Documento de identidad",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ColorForeground
            )
            OutlinedTextField(
                value = dni,
                onValueChange = {
                    dni = it.filter { ch -> ch.isLetterOrDigit() }
                    viewModel.limpiarError()
                },
                placeholder = {
                    Text(
                        "Ej. 12345678",
                        color = ColorTextMuted,
                        fontSize = 14.sp
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
                enabled = loginState !is LoginUiState.Cargando && loginState !is LoginUiState.Autenticado,
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Campo: PIN de acceso (sin leadingIcon)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PIN de acceso",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ColorForeground
            )
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6) {
                        pin = it.filter { ch -> ch.isDigit() }
                        viewModel.limpiarError()
                    }
                },
                placeholder = {
                    Text(
                        "••••",
                        color = ColorTextMuted,
                        fontSize = 14.sp
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
                        viewModel.iniciarSesion(dni, pin)
                    }
                ),
                enabled = loginState !is LoginUiState.Cargando && loginState !is LoginUiState.Autenticado,
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Alerta de Error de Credenciales
        AnimatedVisibility(visible = loginState is LoginUiState.ErrorCredenciales) {
            Surface(
                color = StatusDangerSoft,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = StatusDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = (loginState as? LoginUiState.ErrorCredenciales)?.mensaje
                            ?: "Documento o PIN incorrecto",
                        color = StatusDangerText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Botón Iniciar sesión con flecha derecha
        Button(
            onClick = {
                keyboard?.hide()
                viewModel.iniciarSesion(dni, pin)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenDark2,
                contentColor = Color.White
            ),
            enabled = dni.length >= 6 && pin.length >= 4 && loginState !is LoginUiState.Cargando
        ) {
            if (loginState is LoginUiState.Cargando) {
                CircularProgressIndicator(
                    color = GreenBase,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Verificando credenciales...", fontSize = 15.sp)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Iniciar sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Decoración de Fondo Figma Make (Órbitas y Resplandor Radial)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun FondoOrbitasFigma() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Resplandor radial verde esmeralda superior derecho
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-40).dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ZendovRadialGlow,
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Órbita 1: Círculo grande con borde translúcido
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 140.dp, y = (-50).dp)
                .size(380.dp)
                .border(1.dp, ZendovOrbitBorder, CircleShape)
        )

        // Órbita 2: Círculo inferior izquierdo
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = (-120).dp)
                .size(180.dp)
                .border(1.dp, ZendovOrbitBorder, CircleShape)
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenDark2,
    unfocusedBorderColor = ColorBorder,
    cursorColor = GreenDark2,
    focusedTextColor = ColorForeground,
    unfocusedTextColor = ColorForeground,
    focusedLabelColor = GreenDark2,
    unfocusedLabelColor = ColorTextSecondary,
    focusedContainerColor = GrayLight3,
    unfocusedContainerColor = GrayLight3
)
