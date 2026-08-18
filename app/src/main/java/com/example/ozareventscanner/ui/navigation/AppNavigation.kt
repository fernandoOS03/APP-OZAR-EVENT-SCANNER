package com.example.ozareventscanner.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ozareventscanner.OzarApplication
import com.example.ozareventscanner.ui.screens.*
import com.example.ozareventscanner.ui.theme.ColorBackground
import com.example.ozareventscanner.ui.theme.ColorPrimary
import com.example.ozareventscanner.ui.theme.ColorSurface
import com.example.ozareventscanner.ui.theme.ColorTextSecondary
import com.example.ozareventscanner.viewmodel.AppViewModel
import com.example.ozareventscanner.viewmodel.AuthViewModel
import com.example.ozareventscanner.viewmodel.EventoViewModel
import com.example.ozareventscanner.viewmodel.EventoViewModelFactory

// ─────────────────────────────────────────────────────────────────────────────
// Estructura de Rutas con Sealed Class
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Representa todos los destinos navegables de la aplicación.
 */
sealed class Destino(
    val ruta: String,
    val titulo: String = "",
    val iconoActivo: ImageVector? = null,
    val iconoInactivo: ImageVector? = null
) {
    // ── Autenticación y Bienvenida ────────────────────────────────────────────
    object Splash : Destino(ruta = "splash")
    object Login  : Destino(ruta = "login")

    // ── Pestañas Principales (BottomNavigationBar) ────────────────────────────
    object Inicio : Destino(
        ruta = "inicio",
        titulo = "Inicio",
        iconoActivo = Icons.Filled.CalendarMonth,
        iconoInactivo = Icons.Outlined.CalendarMonth
    )

    object AgregarEvento : Destino(
        ruta = "agregar_evento",
        titulo = "Agregar",
        iconoActivo = Icons.Filled.AddCircle,
        iconoInactivo = Icons.Outlined.AddCircleOutline
    )

    object Perfil : Destino(
        ruta = "perfil",
        titulo = "Perfil / Sync",
        iconoActivo = Icons.Filled.Person,
        iconoInactivo = Icons.Outlined.PersonOutline
    )

    // ── Vistas Operativas Secundarias ─────────────────────────────────────────
    object DetalleEvento : Destino(ruta = "detalle_evento/{nombreEvento}") {
        fun crearRuta(nombreEvento: String) = "detalle_evento/$nombreEvento"
    }

    object LectorQr : Destino(ruta = "lector_qr")

    object ListaParticipantes : Destino(ruta = "lista_participantes/{nombreEvento}") {
        fun crearRuta(nombreEvento: String) = "lista_participantes/$nombreEvento"
    }

    companion object {
        val pestañasPrincipales = listOf(Inicio, AgregarEvento, Perfil)
    }
}

/**
 * NavHost principal con flujo completo de autenticación y BottomNavigationBar.
 *
 * Flujo:
 *  1. Splash -> Login (Autenticación requerida)
 *  2. Al iniciar sesión exitosamente -> Entra al flujo principal ([Destino.Inicio]) con BottomNavigationBar.
 *  3. Al cerrar sesión -> Regresa a Login limpiando el backstack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ── ViewModels Compartidos ────────────────────────────────────────────────
    val authViewModel: AuthViewModel = viewModel()
    val appViewModel:  AppViewModel  = viewModel()

    val app = LocalContext.current.applicationContext as OzarApplication
    val eventoViewModel: EventoViewModel = viewModel(
        factory = EventoViewModelFactory(app.repository)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // La BottomNavigationBar solo se muestra en las 3 pestañas principales
    val mostrarBottomBar = currentRoute in Destino.pestañasPrincipales.map { it.ruta }

    Scaffold(
        containerColor = ColorBackground,
        bottomBar = {
            AnimatedVisibility(
                visible = mostrarBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = ColorSurface,
                    tonalElevation = 8.dp
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
                                Icon(
                                    imageVector = if (selected)
                                        destino.iconoActivo ?: Icons.Default.Circle
                                    else
                                        destino.iconoInactivo ?: Icons.Default.Circle,
                                    contentDescription = destino.titulo
                                )
                            },
                            label = {
                                Text(
                                    text = destino.titulo,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorPrimary,
                                selectedTextColor = ColorPrimary,
                                unselectedIconColor = ColorTextSecondary,
                                unselectedTextColor = ColorTextSecondary,
                                indicatorColor = ColorPrimary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destino.Splash.ruta,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Pantalla 0: Splash ────────────────────────────────────────────
            composable(Destino.Splash.ruta) {
                PantallaSplash(
                    onSplashFinalizado = {
                        navController.navigate(Destino.Login.ruta) {
                            popUpTo(Destino.Splash.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // ── Pantalla 1: Login ─────────────────────────────────────────────
            composable(Destino.Login.ruta) {
                PantallaLogin(
                    viewModel = authViewModel,
                    onLoginExitoso = {
                        navController.navigate(Destino.Inicio.ruta) {
                            popUpTo(Destino.Login.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // ── 2. Pestaña "Inicio" (Lista de Eventos) ────────────────────────
            composable(Destino.Inicio.ruta) {
                PantallaEventos(
                    viewModel = appViewModel,
                    onEventoSeleccionado = { evento ->
                        navController.navigate(Destino.DetalleEvento.crearRuta(evento.nombre))
                    }
                )
            }

            // ── 3. Pestaña "Agregar Evento" ───────────────────────────────────
            composable(Destino.AgregarEvento.ruta) {
                PantallaAgregarEvento(
                    viewModel = eventoViewModel,
                    onEventoAgregadoExitoso = {
                        navController.navigate(Destino.Inicio.ruta)
                    }
                )
            }

            // ── 4. Pestaña "Perfil / Sincronización" ───────────────────────────
            composable(Destino.Perfil.ruta) {
                PantallaPerfil(
                    authViewModel = authViewModel,
                    eventoViewModel = eventoViewModel,
                    onCerrarSesion = {
                        navController.navigate(Destino.Login.ruta) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── 5. Pantalla de Detalle del Evento (KPIs & Beneficios) ─────────
            composable(
                route = Destino.DetalleEvento.ruta,
                arguments = listOf(navArgument("nombreEvento") { type = NavType.StringType })
            ) { backStackEntry ->
                val nombreEvento = backStackEntry.arguments?.getString("nombreEvento") ?: "Detalle Evento"
                PantallaBeneficios(
                    viewModel = appViewModel,
                    nombreEvento = nombreEvento,
                    onIrALector = {
                        navController.navigate(Destino.LectorQr.ruta)
                    },
                    onIrAParticipantes = {
                        navController.navigate(Destino.ListaParticipantes.crearRuta(nombreEvento))
                    },
                    onVolver = {
                        navController.popBackStack()
                    }
                )
            }

            // ── 6. Pantalla de Escaneo QR (CameraX + ModalBottomSheet) ────────
            composable(Destino.LectorQr.ruta) {
                PantallaLectorQr(
                    viewModel = eventoViewModel,
                    onVolver = {
                        navController.popBackStack()
                    }
                )
            }

            // ── 7. Pantalla de Lista de Participantes (Contingencia QR dañado) ─
            composable(
                route = Destino.ListaParticipantes.ruta,
                arguments = listOf(navArgument("nombreEvento") { type = NavType.StringType })
            ) { backStackEntry ->
                val nombreEvento = backStackEntry.arguments?.getString("nombreEvento") ?: "Participantes"
                PantallaListaParticipantes(
                    viewModel = eventoViewModel,
                    nombreEvento = nombreEvento,
                    onVolver = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
