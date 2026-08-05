package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ozareventscanner.data.ParticipanteDao

/**
 * Factory necesaria para inyectar [ParticipanteDao] en [EventoViewModel].
 *
 * Uso desde Compose:
 *   val viewModel: EventoViewModel = viewModel(
 *       factory = EventoViewModelFactory(participanteDao)
 *   )
 */
class EventoViewModelFactory(
    private val participanteDao: ParticipanteDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventoViewModel::class.java)) {
            return EventoViewModel(participanteDao) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
