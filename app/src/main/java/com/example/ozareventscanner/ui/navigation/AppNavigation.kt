package com.example.ozareventscanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ozareventscanner.ui.screens.*
import com.example.ozareventscanner.viewmodel.AppViewModel

// ── Rutas de navegación ───────────────────────────────────────────────────────
object Rutas {
    const val SPLASH      = "splash"
    const val EVENTOS     = "eventos"
    const val BENEFICIOS  = "beneficios/{nombreEvento}"
    const val LECTOR      = "lector"

    fun beneficios(nombreEvento: String) = "beneficios/$nombreEvento"
}

/**
 * NavHost principal de la aplicación.
 * Conecta las 4 pantallas del flujo completo.
 *
 * Flujo: Splash → Eventos → Beneficios → Lector QR
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Rutas.SPLASH
    ) {

        // ── Pantalla 1: Splash ────────────────────────────────────────────────
        composable(Rutas.SPLASH) {
            PantallaSplash(
                onSplashFinalizado = {
                    navController.navigate(Rutas.EVENTOS) {
                        popUpTo(Rutas.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Pantalla 2: Lista de Eventos ──────────────────────────────────────
        composable(Rutas.EVENTOS) {
            PantallaEventos(
                viewModel          = appViewModel,
                onEventoSeleccionado = { evento ->
                    navController.navigate(Rutas.beneficios(evento.nombre))
                }
            )
        }

        // ── Pantalla 3: Lista de Beneficios ───────────────────────────────────
        composable(
            route     = Rutas.BENEFICIOS,
            arguments = listOf(
                navArgument("nombreEvento") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val nombreEvento = backStackEntry.arguments?.getString("nombreEvento") ?: ""
            PantallaBeneficios(
                viewModel    = appViewModel,
                nombreEvento = nombreEvento,
                onIrALector  = { navController.navigate(Rutas.LECTOR) },
                onVolver     = { navController.popBackStack() }
            )
        }

        // ── Pantalla 4: Lector QR ─────────────────────────────────────────────
        composable(Rutas.LECTOR) {
            PantallaLectorQr(
                viewModel = appViewModel,
                onVolver  = { navController.popBackStack() }
            )
        }
    }
}
