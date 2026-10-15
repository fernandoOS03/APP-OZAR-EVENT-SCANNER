package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.ScanResultado
import com.example.ozareventscanner.data.SyncStats
import com.example.ozareventscanner.network.BeneficioActivoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// UI State — Lector QR y Entrega de Beneficios
// ─────────────────────────────────────────────────────────────────────────────

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Procesando : ScanUiState()
    object FormatoInvalido : ScanUiState()
    data class Valido(
        val nombre: String,
        val beneficio: String = "",
        val fecha: String? = null,
        val staff: String? = null
    ) : ScanUiState()
    data class YaEscaneado(
        val nombre: String,
        val mensajeAlerta: String = "",
        val fecha: String? = null,
        val staff: String? = null
    ) : ScanUiState()
    object NoEncontrado : ScanUiState()
    data class ErrorScan(val mensaje: String) : ScanUiState()
}

/**
 * Datos completos para el ModalBottomSheet de resultado de escaneo
 * (para verificación de identidad del participante y confirmación de beneficio).
 */
data class ScanModalData(
    val estado: ScanUiState,
    val idParticipante: String,
    val nombre: String,
    val beneficioNombre: String = "",
    val fechaEscaneo: String? = null,
    val staffNombre: String? = null,
    val yaEntregadoPreviamente: Boolean = false,
    val mensajeServidor: String = "",
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
// ViewModel Principal — Ozar Events Scanner
// ─────────────────────────────────────────────────────────────────────────────

class EventoViewModel(private val repository: ParticipanteRepository) : ViewModel() {

    // ── Estado: Beneficios Activos del Evento (Endpoint #2) ───────────────────
    private val _beneficiosActivos = MutableStateFlow<List<BeneficioActivoDto>>(emptyList())
    val beneficiosActivos: StateFlow<List<BeneficioActivoDto>> = _beneficiosActivos

    private val _beneficioSeleccionado = MutableStateFlow<BeneficioActivoDto?>(null)
    val beneficioSeleccionado: StateFlow<BeneficioActivoDto?> = _beneficioSeleccionado

    private val _cargandoBeneficios = MutableStateFlow(false)
    val cargandoBeneficios: StateFlow<Boolean> = _cargandoBeneficios

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

    private val _totalAsistentes = MutableStateFlow(0)
    val totalAsistentes: StateFlow<Int> = _totalAsistentes

    // ── Estado: Entrega Individual de Beneficios por Participante ────────────
    private val _beneficiosEntregadosPorParticipante = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val beneficiosEntregadosPorParticipante: StateFlow<Map<String, Set<String>>> = _beneficiosEntregadosPorParticipante

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
        observarTotalAsistentes()
    }

    private fun observarTotalAsistentes() {
        viewModelScope.launch {
            repository.observarTotalParticipantes().collect { total ->
                _totalAsistentes.value = total
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Carga y Selección de Beneficios (Endpoint #2)
    // ─────────────────────────────────────────────────────────────────────────

    fun cargarBeneficios(eventoId: String) {
        viewModelScope.launch {
            _cargandoBeneficios.value = true
            try {
                val lista = repository.obtenerBeneficiosActivos(eventoId)
                _beneficiosActivos.value = lista
                if (_beneficioSeleccionado.value == null && lista.isNotEmpty()) {
                    _beneficioSeleccionado.value = lista.first()
                }
            } catch (e: Exception) {
                // Silencioso en caso de fallback
            } finally {
                _cargandoBeneficios.value = false
            }
        }
    }

    fun seleccionarBeneficio(beneficio: BeneficioActivoDto) {
        _beneficioSeleccionado.value = beneficio
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escaneo QR + BottomSheet de Verificación de Identidad (Endpoint #3)
    // ─────────────────────────────────────────────────────────────────────────

    fun validarYMarcarParticipante(rawQrValue: String) {
        if (rawQrValue == ultimoCodigoEscaneado && _scanModalData.value != null) return
        ultimoCodigoEscaneado = rawQrValue

        val beneficioActivo = _beneficioSeleccionado.value
        val beneficioId = beneficioActivo?.id ?: ""
        val beneficioNombre = beneficioActivo?.nombre ?: "Beneficio"

        viewModelScope.launch {
            _scanState.value = ScanUiState.Procesando
            val resultado = repository.registrarEscaneo(rawQrValue, beneficioId)
            val entidad = repository.buscarParticipantePorId(rawQrValue)

            val nuevoEstado = when (resultado) {
                is ScanResultado.FormatoInvalido -> ScanUiState.FormatoInvalido
                is ScanResultado.NoEncontrado    -> ScanUiState.NoEncontrado
                is ScanResultado.YaEscaneado     -> ScanUiState.YaEscaneado(
                    nombre        = resultado.nombre,
                    mensajeAlerta = resultado.mensaje,
                    fecha         = resultado.fecha,
                    staff         = resultado.staff
                )
                is ScanResultado.Valido          -> ScanUiState.Valido(
                    nombre    = resultado.nombre,
                    beneficio = if (resultado.beneficio.isNotBlank()) resultado.beneficio else beneficioNombre,
                    fecha     = resultado.fecha,
                    staff     = resultado.staff
                )
                is ScanResultado.Error           -> ScanUiState.ErrorScan(resultado.mensaje)
            }

            _scanState.value = nuevoEstado

            val nombreAMostrar = when (resultado) {
                is ScanResultado.Valido -> resultado.nombre
                is ScanResultado.YaEscaneado -> resultado.nombre
                else -> entidad?.nombre ?: ""
            }

            val yaEntregado = resultado is ScanResultado.YaEscaneado
            val mensajeServer = when (resultado) {
                is ScanResultado.YaEscaneado -> resultado.mensaje
                is ScanResultado.Valido -> "Beneficio entregado exitosamente a $nombreAMostrar"
                else -> ""
            }

            _scanModalData.value = ScanModalData(
                estado                 = nuevoEstado,
                idParticipante         = rawQrValue,
                nombre                 = nombreAMostrar,
                beneficioNombre        = beneficioNombre,
                fechaEscaneo           = if (resultado is ScanResultado.YaEscaneado) resultado.fecha else (resultado as? ScanResultado.Valido)?.fecha,
                staffNombre            = if (resultado is ScanResultado.YaEscaneado) resultado.staff else (resultado as? ScanResultado.Valido)?.staff,
                yaEntregadoPreviamente = yaEntregado,
                mensajeServidor        = mensajeServer,
                entidad                = entidad
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
            val beneficioId = _beneficioSeleccionado.value?.id ?: ""
            val res = repository.registrarEscaneo(idParticipante, beneficioId)
            cargarParticipantes()
            onResultado(res)
        }
    }

    /**
     * Entrega un beneficio específico individualmente ("uno a uno") a un participante.
     */
    fun entregarBeneficioIndividual(
        idParticipante: String,
        nombreBeneficio: String,
        todosLosBeneficios: List<String>
    ) {
        viewModelScope.launch {
            val actual = _beneficiosEntregadosPorParticipante.value[idParticipante]?.toMutableSet()
                ?: (if (_listaParticipantes.value.find { it.idParticipante == idParticipante }?.beneficioEntregado == true)
                    todosLosBeneficios.toMutableSet()
                else
                    mutableSetOf())

            actual.add(nombreBeneficio)
            val nuevoMapa = _beneficiosEntregadosPorParticipante.value.toMutableMap()
            nuevoMapa[idParticipante] = actual
            _beneficiosEntregadosPorParticipante.value = nuevoMapa

            // Si todos los beneficios fueron entregados, persistir en Room/backend
            if (actual.size >= todosLosBeneficios.size && todosLosBeneficios.isNotEmpty()) {
                val beneficioId = _beneficioSeleccionado.value?.id ?: ""
                repository.registrarEscaneo(idParticipante, beneficioId)
                cargarParticipantes()
            }
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
