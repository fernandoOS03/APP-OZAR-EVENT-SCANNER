package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.ScanResultado
import com.example.ozareventscanner.data.SyncStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// UI State — Registro / Vinculación de Evento (Pestaña "Agregar Evento")
// ─────────────────────────────────────────────────────────────────────────────

sealed class RegistroUiState {
    object Idle : RegistroUiState()
    object Cargando : RegistroUiState()
    data class Exito(val nombre: String) : RegistroUiState()
    data class Error(val mensaje: String) : RegistroUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State — Lector QR
// ─────────────────────────────────────────────────────────────────────────────

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Procesando : ScanUiState()
    object FormatoInvalido : ScanUiState()
    data class Valido(val nombre: String) : ScanUiState()
    data class YaEscaneado(val nombre: String) : ScanUiState()
    object NoEncontrado : ScanUiState()
    data class ErrorScan(val mensaje: String) : ScanUiState()
}

/**
 * Datos completos para el ModalBottomSheet de resultado de escaneo
 * (para verificación de identidad del participante).
 */
data class ScanModalData(
    val estado: ScanUiState,
    val idParticipante: String,
    val nombre: String,
    val timestamp: Long = System.currentTimeMillis(),
    val entidad: ParticipanteEntity? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// UI State — Sincronización Manual (Pestaña "Perfil / Sincronización")
// ─────────────────────────────────────────────────────────────────────────────

sealed class SyncUiState {
    object Idle : SyncUiState()
    object Sincronizando : SyncUiState()
    data class Exito(val stats: SyncStats) : SyncUiState()
    data class Error(val mensaje: String) : SyncUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel Principal
// ─────────────────────────────────────────────────────────────────────────────

class EventoViewModel(private val repository: ParticipanteRepository) : ViewModel() {

    // ── Estado: Registro / Agregar Evento ─────────────────────────────────────
    private val _uiState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val uiState: StateFlow<RegistroUiState> = _uiState

    // ── Estado: Escaneo QR ────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    private val _scanModalData = MutableStateFlow<ScanModalData?>(null)
    val scanModalData: StateFlow<ScanModalData?> = _scanModalData

    private var ultimoCodigoEscaneado: String? = null

    // ── Estado: Contingencia / Lista de Participantes ────────────────────────
    private val _listaParticipantes = MutableStateFlow<List<ParticipanteEntity>>(emptyList())
    val listaParticipantes: StateFlow<List<ParticipanteEntity>> = _listaParticipantes

    private val _busquedaQuery = MutableStateFlow("")
    val busquedaQuery: StateFlow<String> = _busquedaQuery

    private val _cargandoParticipantes = MutableStateFlow(false)
    val cargandoParticipantes: StateFlow<Boolean> = _cargandoParticipantes

    // ── Estado: Perfil & Sincronización ──────────────────────────────────────
    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState: StateFlow<SyncUiState> = _syncUiState

    private val _hayInternet = MutableStateFlow(true)
    val hayInternet: StateFlow<Boolean> = _hayInternet

    private val _pendientesSyncCount = MutableStateFlow(0)
    val pendientesSyncCount: StateFlow<Int> = _pendientesSyncCount

    init {
        verificarConexion()
        cargarParticipantes()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pestaña "Agregar Evento": Registro de código de evento / participante
    // ─────────────────────────────────────────────────────────────────────────

    fun registrarCodigo(codigo: String) {
        val codigoLimpio = codigo.trim()
        if (codigoLimpio.isBlank()) {
            _uiState.value = RegistroUiState.Error("El código no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegistroUiState.Cargando
            try {
                val nombre = repository.registrarCodigoRemoto(codigoLimpio)
                _uiState.value = RegistroUiState.Exito(nombre = nombre)
                cargarParticipantes()
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                _uiState.value = when (code) {
                    404  -> RegistroUiState.Error("Código no encontrado en el servidor.")
                    409  -> RegistroUiState.Error("Este código ya fue registrado anteriormente.")
                    else -> RegistroUiState.Error("Error del servidor ($code). Intenta nuevamente.")
                }
            } catch (e: java.net.UnknownHostException) {
                _uiState.value = RegistroUiState.Error("Sin conexión a internet. Verifica tu red.")
            } catch (e: java.net.SocketTimeoutException) {
                _uiState.value = RegistroUiState.Error("Tiempo de espera agotado.")
            } catch (e: Exception) {
                _uiState.value = RegistroUiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun resetearEstado() {
        _uiState.value = RegistroUiState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escaneo QR + BottomSheet de Verificación de Identidad
    // ─────────────────────────────────────────────────────────────────────────

    fun validarYMarcarParticipante(rawQrValue: String) {
        if (rawQrValue == ultimoCodigoEscaneado && _scanModalData.value != null) return
        ultimoCodigoEscaneado = rawQrValue

        viewModelScope.launch {
            _scanState.value = ScanUiState.Procesando
            val resultado = repository.registrarEscaneo(rawQrValue)
            val entidad = repository.buscarParticipantePorId(rawQrValue)

            val nuevoEstado = when (resultado) {
                is ScanResultado.FormatoInvalido -> ScanUiState.FormatoInvalido
                is ScanResultado.NoEncontrado    -> ScanUiState.NoEncontrado
                is ScanResultado.YaEscaneado     -> ScanUiState.YaEscaneado(resultado.nombre)
                is ScanResultado.Valido          -> ScanUiState.Valido(resultado.nombre)
                is ScanResultado.Error           -> ScanUiState.ErrorScan(resultado.mensaje)
            }

            _scanState.value = nuevoEstado

            val nombreAMostrar = when (resultado) {
                is ScanResultado.Valido -> resultado.nombre
                is ScanResultado.YaEscaneado -> resultado.nombre
                else -> entidad?.nombre ?: ""
            }

            _scanModalData.value = ScanModalData(
                estado         = nuevoEstado,
                idParticipante = rawQrValue,
                nombre         = nombreAMostrar,
                entidad        = entidad
            )

            // Refrescar lista local
            cargarParticipantes()
        }
    }

    fun cerrarModalScan() {
        _scanModalData.value = null
        _scanState.value = ScanUiState.Idle
        ultimoCodigoEscaneado = null
    }

    fun resetearScan() {
        _scanState.value = ScanUiState.Idle
        _scanModalData.value = null
        ultimoCodigoEscaneado = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contingencia: Búsqueda y Entrega Manual de Beneficios
    // ─────────────────────────────────────────────────────────────────────────

    fun actualizarBusqueda(query: String) {
        _busquedaQuery.value = query
        cargarParticipantes(query)
    }

    fun cargarParticipantes(query: String = _busquedaQuery.value) {
        viewModelScope.launch {
            _cargandoParticipantes.value = true
            try {
                val lista = repository.buscarParticipantes(query)
                _listaParticipantes.value = lista
            } catch (e: Exception) {
                _listaParticipantes.value = emptyList()
            } finally {
                _cargandoParticipantes.value = false
            }
        }
    }

    fun entregarBeneficioManual(idParticipante: String, onResultado: (ScanResultado) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.entregarBeneficioManual(idParticipante)
            cargarParticipantes()
            onResultado(res)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pestaña "Perfil / Sincronización": Estado de Conectividad y Sync Forzada
    // ─────────────────────────────────────────────────────────────────────────

    fun verificarConexion() {
        _hayInternet.value = repository.hayConexion()
    }

    fun forzarSincronizacion() {
        viewModelScope.launch {
            verificarConexion()
            _syncUiState.value = SyncUiState.Sincronizando

            val resultado = repository.sincronizarCompleto()
            if (resultado.isSuccess) {
                val stats = resultado.getOrThrow()
                _syncUiState.value = SyncUiState.Exito(stats)
                cargarParticipantes()
            } else {
                _syncUiState.value = SyncUiState.Error(
                    resultado.exceptionOrNull()?.localizedMessage ?: "Error de sincronización"
                )
            }
        }
    }

    fun limpiarEstadoSync() {
        _syncUiState.value = SyncUiState.Idle
    }
}
