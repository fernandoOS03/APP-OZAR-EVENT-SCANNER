package com.example.ozareventscanner.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ozareventscanner.OzarApplication
import com.example.ozareventscanner.ui.components.ZendovIcons
import com.example.ozareventscanner.ui.screens.*
import com.example.ozareventscanner.ui.theme.*
import com.example.ozareventscanner.viewmodel.AuthViewModel
import com.example.ozareventscanner.viewmodel.EventoViewModel
import com.example.ozareventscanner.viewmodel.EventoViewModelFactory

// ─────────────────────────────────────────────────────────────────────────────
// Estructura de Rutas con Sealed Class
// ─────────────────────────────────────────────────────────────────────────────

sealed class Destino(
    val ruta: String,
    val titulo: String = "",
    val icono: ImageVector? = null
) {
    // ── Autenticación y Bienvenida ────────────────────────────────────────────
    object Splash : Destino(ruta = "splash")
    object Login  : Destino(ruta = "login")

    // ── Pestañas Principales (BottomNavigationBar — 1:1 Figma Make) ───────────
    object Inicio : Destino(
        ruta = "inicio",
        titulo = "Inicio",
        icono = ZendovIcons.Home
    )

    object Asistentes : Destino(
        ruta = "asistentes",
        titulo = "Asistentes",
        icono = ZendovIcons.Users
    )

    object Overview : Destino(
        ruta = "overview",
        titulo = "Overview",
        icono = ZendovIcons.Chart
    )

    object Perfil : Destino(
        ruta = "perfil",
        titulo = "Perfil",
        icono = ZendovIcons.User
    )

    // ── Vistas Operativas Secundarias ─────────────────────────────────────────
    object LectorQr : Destino(ruta = "lector_qr")

    companion object {
        val pestañasPrincipales = listOf(Inicio, Asistentes, Overview, Perfil)
    }
}

/**
 * NavHost principal con flujo completo de autenticación y BottomNavigationBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ── ViewModels Compartidos ────────────────────────────────────────────────
    val authViewModel: AuthViewModel = viewModel()

    val app = LocalContext.current.applicationContext as OzarApplication
    val eventoViewModel: EventoViewModel = viewModel(
        factory = EventoViewModelFactory(app.repository)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // La BottomNavigationBar solo se muestra en las 3 pestañas principales
    val mostrarBottomBar = currentRoute in Destino.pestañasPrincipales.map { it.ruta }

    val staffActual by authViewModel.staffActual.collectAsState()

    Scaffold(
        containerColor = ColorBackground,
        bottomBar = {
            AnimatedVisibility(
                visible = mostrarBottomBar,
                enter   = slideInVertically(initialOffsetY = { it }),
                exit    = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(BorderStroke(1.dp, GrayLight2))
                ) {
                    Destino.pestañasPrincipales.forEach { destino ->
                        val selected = currentRoute == destino.ruta
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != destino.ruta) {
                                    navController.navigate(destino.ruta) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(2.dp)
                                                .background(GreenDark1, RoundedCornerShape(999.dp))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Icon(
                                        imageVector = destino.icono ?: Icons.Default.Circle,
                                        contentDescription = destino.titulo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = destino.titulo,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GreenDark2,
                                selectedTextColor = GreenDark2,
                                unselectedIconColor = Color(0xFF84958F),
                                unselectedTextColor = Color(0xFF84958F),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController    = navController,
            startDestination = Destino.Splash.ruta,
            modifier         = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Pantalla 0: Splash ────────────────────────────────────────────
            composable(Destino.Splash.ruta) {
                PantallaSplash(
                    onSplashFinalizado = {
                        val siguienteRuta = if (authViewModel.loginState.value is com.example.ozareventscanner.viewmodel.LoginUiState.Autenticado) {
                            Destino.Inicio.ruta
                        } else {
                            Destino.Login.ruta
                        }
                        navController.navigate(siguienteRuta) {
                            popUpTo(Destino.Splash.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // ── Pantalla 1: Login ─────────────────────────────────────────────
            composable(
                route = Destino.Login.ruta,
                exitTransition = {
                    fadeOut(animationSpec = tween(150))
                }
            ) {
                PantallaLogin(
                    viewModel = authViewModel,
                    onLoginExitoso = {
                        navController.navigate(Destino.Inicio.ruta) {
                            popUpTo(Destino.Login.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // ── 2. Pestaña "Inicio" (Evento del Staff y Beneficios) ──────────
            composable(
                route = Destino.Inicio.ruta,
                enterTransition = {
                    fadeIn(animationSpec = tween(150))
                }
            ) {
                PantallaEventos(
                    staffActual = staffActual,
                    eventoViewModel = eventoViewModel,
                    onEscanearBeneficio = { beneficio ->
                        eventoViewModel.seleccionarBeneficio(beneficio)
                        navController.navigate(Destino.LectorQr.ruta)
                    }
                )
            }

            // ── 3. Pestaña "Asistentes" (Búsqueda y Contingencia) ─────────────
            composable(Destino.Asistentes.ruta) {
                val nombreEvento = staffActual?.evento?.nombre ?: "Evento Activo"
                PantallaListaParticipantes(
                    viewModel          = eventoViewModel,
                    nombreEvento       = nombreEvento,
                    mostrarBotonVolver = false
                )
            }

            // ── 4. Pestaña "Overview" (Métricas en Tiempo Real) ───────────────
            composable(Destino.Overview.ruta) {
                PantallaOverview(
                    staffActual     = staffActual,
                    eventoViewModel = eventoViewModel
                )
            }

            // ── 4. Pestaña "Perfil / Sincronización" ───────────────────────────
            composable(Destino.Perfil.ruta) {
                PantallaPerfil(
                    authViewModel   = authViewModel,
                    eventoViewModel = eventoViewModel,
                    onCerrarSesion = {
                        navController.navigate(Destino.Login.ruta) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── 5. Pantalla de Escaneo QR (CameraX + ModalBottomSheet) ────────
            composable(Destino.LectorQr.ruta) {
                PantallaLectorQr(
                    viewModel = eventoViewModel,
                    onVolver = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
