package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ozareventscanner.data.ParticipanteRepository

/**
 * Factory para [EventoViewModel].
 *
 * Inyecta [ParticipanteRepository] (que a su vez encapsula el DAO y el ApiService).
 * El repositorio se obtiene del singleton en [OzarApplication.repository].
 *
 * ## Ejemplo de uso en un Composable
 * ```kotlin
 * val app = LocalContext.current.applicationContext as OzarApplication
 * val eventoViewModel: EventoViewModel = viewModel(
 *     factory = EventoViewModelFactory(app.repository)
 * )
 * ```
 */
class EventoViewModelFactory(
    private val repository: ParticipanteRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventoViewModel::class.java)) {
            return EventoViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
