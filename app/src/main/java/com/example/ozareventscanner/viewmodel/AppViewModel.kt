package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
// Modelos de datos mock (sin backend)
// ─────────────────────────────────────────────────────────────────────────────

data class EventoMock(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val inscritos: Int
)

data class BeneficioMock(
    val id: String,
    val nombre: String,
    val escaneado: Boolean = false
)

/**
 * ViewModel liviano para el flujo de navegación de las 4 pantallas.
 * Usa datos mock — no hace llamadas de red.
 * La lógica real de registro sigue en [EventoViewModel].
 */
class AppViewModel : ViewModel() {

    // ── Lista de eventos (mock) ───────────────────────────────────────────────
    private val _eventos = MutableStateFlow(
        listOf(
            EventoMock("1", "Ozar Conference 2025", "Conferencia anual de liderazgo", 120),
            EventoMock("2", "Retiro Liderazgo",     "Retiro espiritual de equipos",   45)
        )
    )
    val eventos: StateFlow<List<EventoMock>> = _eventos

    // ── Evento seleccionado actualmente ──────────────────────────────────────
    private val _eventoSeleccionado = MutableStateFlow<EventoMock?>(null)
    val eventoSeleccionado: StateFlow<EventoMock?> = _eventoSeleccionado

    // ── Lista de beneficios/participantes (mock) ──────────────────────────────
    private val _beneficios = MutableStateFlow(
        listOf(
            BeneficioMock("p1", "Juan Pérez",      escaneado = false),
            BeneficioMock("p2", "María González",  escaneado = true),
            BeneficioMock("p3", "Carlos López",    escaneado = false),
            BeneficioMock("p4", "Ana Martínez",    escaneado = false),
            BeneficioMock("p5", "Luis Fernández",  escaneado = true)
        )
    )
    val beneficios: StateFlow<List<BeneficioMock>> = _beneficios

    // ── Token ingresado en P2 ─────────────────────────────────────────────────
    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token

    // ── Resultado del último escaneo QR ──────────────────────────────────────
    private val _resultadoQr = MutableStateFlow<String?>(null)
    val resultadoQr: StateFlow<String?> = _resultadoQr

    // ─────────────────────────────────────────────────────────────────────────
    // Acciones
    // ─────────────────────────────────────────────────────────────────────────

    fun seleccionarEvento(evento: EventoMock) {
        _eventoSeleccionado.value = evento
    }

    fun actualizarToken(nuevoToken: String) {
        _token.value = nuevoToken
    }

    /** Simula validación del token: agrega un evento ficticio a la lista. */
    fun validarToken() {
        val tokenActual = _token.value.trim()
        if (tokenActual.isBlank()) return
        val nuevoEvento = EventoMock(
            id          = tokenActual,
            nombre      = "Evento — $tokenActual",
            descripcion = "Evento agregado con token",
            inscritos   = 0
        )
        _eventos.value = _eventos.value + nuevoEvento
        _eventoSeleccionado.value = nuevoEvento
        _token.value = ""
    }

    fun registrarResultadoQr(resultado: String) {
        _resultadoQr.value = resultado
    }

    fun limpiarResultadoQr() {
        _resultadoQr.value = null
    }
}
