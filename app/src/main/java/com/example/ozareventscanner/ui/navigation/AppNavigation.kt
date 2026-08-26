package com.example.ozareventscanner.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ozareventscanner.OzarApplication
import com.example.ozareventscanner.ui.screens.*
import com.example.ozareventscanner.viewmodel.*

sealed class AppDestino(val ruta: String) {
    object Splash         : AppDestino("splash")
    object Login          : AppDestino("login")
    object Dashboard      : AppDestino("dashboard")
    object Participantes  : AppDestino("participantes")
    object Scanner        : AppDestino("scanner/{idBeneficio}") {
        fun crearRuta(idBeneficio: String = "general") = "scanner/$idBeneficio"
    }
}

/**
 * Grafo de navegación ultra simplificado (Sin BottomNavigationBar).
 * Flujo: Login -> Dashboard -> Scanner / Participantes (Contingencia).
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as OzarApplication

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val eventoViewModel: EventoViewModel = viewModel(
        factory = EventoViewModelFactory(app.repository)
    )

    NavHost(
        navController = navController,
        startDestination = AppDestino.Splash.ruta,
        modifier = Modifier.fillMaxSize()
    ) {
        // ── 0. Pantalla Splash ────────────────────────────────────────────────
        composable(AppDestino.Splash.ruta) {
            PantallaSplash(
                onSplashFinalizado = {
                    if (app.authRepository.haySesionActiva()) {
                        navController.navigate(AppDestino.Dashboard.ruta) {
                            popUpTo(AppDestino.Splash.ruta) { inclusive = true }
                        }
                    } else {
                        navController.navigate(AppDestino.Login.ruta) {
                            popUpTo(AppDestino.Splash.ruta) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── 1. Pantalla Login (Documento + PIN) ───────────────────────────────
        composable(AppDestino.Login.ruta) {
            PantallaLogin(
                viewModel = authViewModel,
                onLoginExitoso = {
                    navController.navigate(AppDestino.Dashboard.ruta) {
                        popUpTo(AppDestino.Login.ruta) { inclusive = true }
                    }
                }
            )
        }

        // ── 2. Pantalla Principal: EventDashboardScreen ───────────────────────
        composable(AppDestino.Dashboard.ruta) {
            val staffNombre by authViewModel.staffNombre.collectAsState()
            val nombreEvento by authViewModel.nombreEvento.collectAsState()
            val beneficios by authViewModel.beneficiosEvento.collectAsState()

            val totalParticipantes by eventoViewModel.totalParticipantes.collectAsState()
            val escaneos by eventoViewModel.listaEscaneos.collectAsState()
            val hayInternet by eventoViewModel.hayInternet.collectAsState()

            EventDashboardScreen(
                staffNombre = staffNombre.ifBlank { "Staff Operador" },
                nombreEvento = nombreEvento.ifBlank { "Evento Ozar" },
                hayConexionInternet = hayInternet,
                beneficios = beneficios,
                totalParticipantes = totalParticipantes,
                escaneos = escaneos,
                onBeneficioClick = { beneficio ->
                    navController.navigate(AppDestino.Scanner.crearRuta(beneficio.idBeneficio))
                },
                onIrAParticipantes = {
                    navController.navigate(AppDestino.Participantes.ruta)
                },
                onCerrarSesion = {
                    authViewModel.cerrarSesion()
                    navController.navigate(AppDestino.Login.ruta) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── 3. Pantalla de Contingencia: ParticipantListScreen ────────────────
        composable(AppDestino.Participantes.ruta) {
            val beneficios by authViewModel.beneficiosEvento.collectAsState()
            val totalParticipantes by eventoViewModel.totalParticipantes.collectAsState()
            val participantes by eventoViewModel.listaParticipantes.collectAsState()
            val escaneos by eventoViewModel.listaEscaneos.collectAsState()
            val busquedaQuery by eventoViewModel.busquedaQuery.collectAsState()

            ParticipantListScreen(
                totalParticipantesRegistrados = totalParticipantes,
                participantes = participantes,
                beneficios = beneficios,
                escaneos = escaneos,
                busquedaQuery = busquedaQuery,
                onQueryChange = { eventoViewModel.actualizarBusqueda(it) },
                onValidarEntregaBeneficio = { idParticipante, idBeneficio ->
                    eventoViewModel.entregarBeneficioManual(idParticipante, idBeneficio)
                },
                onVolver = {
                    navController.popBackStack()
                }
            )
        }

        // ── 4. Pantalla de Escáner de Cámara ──────────────────────────────────
        composable(
            route = AppDestino.Scanner.ruta,
            arguments = listOf(navArgument("idBeneficio") { type = NavType.StringType })
        ) { backStackEntry ->
            val idBeneficio = backStackEntry.arguments?.getString("idBeneficio") ?: ""
            PantallaLectorQr(
                idBeneficio = idBeneficio,
                viewModel = eventoViewModel,
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}
