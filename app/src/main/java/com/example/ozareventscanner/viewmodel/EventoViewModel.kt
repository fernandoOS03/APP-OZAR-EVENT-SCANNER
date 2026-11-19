package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.ozareventscanner.data.BeneficioEntity
import com.example.ozareventscanner.data.EscaneoEntity
import com.example.ozareventscanner.data.ParticipanteConBeneficios
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.ScanResultado
import com.example.ozareventscanner.data.SyncStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Procesando : ScanUiState()
    object FormatoInvalido : ScanUiState()
    data class Valido(
        val nombre: String,
        val beneficio: String = "",
        val fecha: String? = null,
        val staff: String? = null,
        val pendienteSync: Boolean = true
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

data class ScanModalData(
    val estado: ScanUiState,
    val idParticipante: String,
    val nombre: String,
    val beneficioNombre: String = "",
    val fechaEscaneo: String? = null,
    val staffNombre: String? = null,
    val yaEntregadoPreviamente: Boolean = false,
    val mensajeServidor: String = "",
    val pendienteSync: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val entidad: ParticipanteEntity? = null
)

sealed class SyncUiState {
    object Idle : SyncUiState()
    object Sincronizando : SyncUiState()
    data class Exito(val stats: SyncStats) : SyncUiState()
    data class Error(val mensaje: String) : SyncUiState()
}

class EventoViewModel(private val repository: ParticipanteRepository) : ViewModel() {

    private val _beneficiosActivos = MutableStateFlow<List<BeneficioEntity>>(emptyList())
    val beneficiosActivos: StateFlow<List<BeneficioEntity>> = _beneficiosActivos

    private val _beneficioSeleccionado = MutableStateFlow<BeneficioEntity?>(null)
    val beneficioSeleccionado: StateFlow<BeneficioEntity?> = _beneficioSeleccionado

    private val _cargandoBeneficios = MutableStateFlow(false)
    val cargandoBeneficios: StateFlow<Boolean> = _cargandoBeneficios

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    private val _scanModalData = MutableStateFlow<ScanModalData?>(null)
    val scanModalData: StateFlow<ScanModalData?> = _scanModalData

    private var ultimoCodigoEscaneado: String? = null

    private val _listaParticipantes = MutableStateFlow<List<ParticipanteConBeneficios>>(emptyList())
    val listaParticipantes: StateFlow<List<ParticipanteConBeneficios>> = _listaParticipantes

    private val _busquedaQuery = MutableStateFlow("")
    val busquedaQuery: StateFlow<String> = _busquedaQuery

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val participantesPaginados: Flow<PagingData<ParticipanteConBeneficios>> = _busquedaQuery
        .debounce(200L)
        .distinctUntilChanged()
        .flatMapLatest { query -> repository.paginarParticipantes(query) }
        .cachedIn(viewModelScope)

    val conteoPorBeneficio: StateFlow<Map<String, Int>> = repository
        .observarConteoPorBeneficio()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val totalParticipantesEscaneados: StateFlow<Int> = repository
        .observarTotalParticipantesEscaneados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val escaneosEntregados: StateFlow<List<EscaneoEntity>> = repository
        .observarEscaneosEntregados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cargandoParticipantes = MutableStateFlow(false)
    val cargandoParticipantes: StateFlow<Boolean> = _cargandoParticipantes

    private val _totalAsistentes = MutableStateFlow(0)
    val totalAsistentes: StateFlow<Int> = _totalAsistentes

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState: StateFlow<SyncUiState> = _syncUiState

    private val _hayInternet = MutableStateFlow(true)
    val hayInternet: StateFlow<Boolean> = _hayInternet

    private val _pendientesSyncCount = MutableStateFlow(0)
    val pendientesSyncCount: StateFlow<Int> = _pendientesSyncCount

    private val _pendientesOtrosUsuariosCount = MutableStateFlow(0)
    val pendientesOtrosUsuariosCount: StateFlow<Int> = _pendientesOtrosUsuariosCount

    private val _conflictosCount = MutableStateFlow(0)
    val conflictosCount: StateFlow<Int> = _conflictosCount

    private val _conflictosLista = MutableStateFlow<List<EscaneoEntity>>(emptyList())
    val conflictosLista: StateFlow<List<EscaneoEntity>> = _conflictosLista

    fun cargarConflictos() {
        val eventoId = repository.obtenerEventoIdActual() ?: return
        viewModelScope.launch {
            _conflictosLista.value = repository.obtenerConflictos(eventoId)
        }
    }

    private var pendientesObserverJob: Job? = null
    private var otrosPendientesObserverJob: Job? = null
    private var conflictosObserverJob: Job? = null
    private var deltaDownloadLoopJob: Job? = null

    init {
        verificarConexion()
        observarTotalAsistentes()
        observarPendientesDeSesionActual()
    }

    private fun observarTotalAsistentes() {
        viewModelScope.launch {
            repository.observarTotalParticipantes().collect { total ->
                _totalAsistentes.value = total
            }
        }
    }

    private fun observarPendientesDeSesionActual(eventoIdOverride: String? = null) {
        val eventoId = eventoIdOverride ?: repository.obtenerEventoIdActual()
        val staffId = repository.obtenerStaffIdActual()

        pendientesObserverJob?.cancel()
        otrosPendientesObserverJob?.cancel()
        conflictosObserverJob?.cancel()

        if (eventoId.isNullOrBlank()) {
            pendientesObserverJob = viewModelScope.launch {
                repository.observarPendientesSync().collect { total ->
                    _pendientesSyncCount.value = total
                }
            }
            _pendientesOtrosUsuariosCount.value = 0
            _conflictosCount.value = 0
            return
        }

        pendientesObserverJob = viewModelScope.launch {
            repository.observarPendientesSyncDeSesion(eventoId, staffId).collect { total ->
                _pendientesSyncCount.value = total
            }
        }
        otrosPendientesObserverJob = viewModelScope.launch {
            repository.observarPendientesOtrosUsuarios(eventoId, staffId).collect { total ->
                _pendientesOtrosUsuariosCount.value = total
            }
        }
        conflictosObserverJob = viewModelScope.launch {
            repository.observarConflictosCount(eventoId).collect { total ->
                _conflictosCount.value = total
            }
        }
    }

    fun inicializarEvento(eventoId: String) {
        observarPendientesDeSesionActual(eventoId)
        iniciarDescargaDeltaEnPrimerPlano(eventoId)
        viewModelScope.launch {
            verificarConexion()
            _syncUiState.value = SyncUiState.Sincronizando
            val resultado = repository.inicializarEvento(eventoId)
            if (resultado.isSuccess) {
                _syncUiState.value = SyncUiState.Exito(resultado.getOrThrow())
            } else {
                _syncUiState.value = SyncUiState.Error(
                    resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo descargar el estado del evento."
                )
            }
            recargarDatosLocales(eventoId)
        }
    }

    private fun iniciarDescargaDeltaEnPrimerPlano(eventoId: String) {
        deltaDownloadLoopJob?.cancel()
        deltaDownloadLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(150_000L) // Cada 2.5 minutos con la app activa
                verificarConexion()
                if (_hayInternet.value) {
                    val res = repository.descargarSoloCambios(eventoId)
                    if (res.isSuccess && (res.getOrNull() ?: 0) > 0) {
                        recargarDatosLocales(eventoId)
                    }
                }
            }
        }
    }

    fun cargarBeneficios(eventoId: String) {
        viewModelScope.launch {
            _cargandoBeneficios.value = true
            try {
                val lista = repository.obtenerBeneficiosActivos(eventoId)
                _beneficiosActivos.value = lista
                val seleccionado = _beneficioSeleccionado.value
                if (seleccionado == null || lista.none { it.id == seleccionado.id }) {
                    _beneficioSeleccionado.value = lista.firstOrNull()
                }
            } catch (_: Exception) {
                _beneficiosActivos.value = emptyList()
            } finally {
                _cargandoBeneficios.value = false
            }
        }
    }

    fun seleccionarBeneficio(beneficio: BeneficioEntity) {
        _beneficioSeleccionado.value = beneficio
    }

    fun validarYMarcarParticipante(rawQrValue: String) {
        if (rawQrValue == ultimoCodigoEscaneado && _scanModalData.value != null) return
        ultimoCodigoEscaneado = rawQrValue

        val beneficioActivo = _beneficioSeleccionado.value
        if (beneficioActivo == null) {
            val estado = ScanUiState.ErrorScan("Selecciona un beneficio antes de escanear.")
            _scanState.value = estado
            _scanModalData.value = ScanModalData(
                estado = estado,
                idParticipante = rawQrValue,
                nombre = "",
                mensajeServidor = "Selecciona un beneficio antes de escanear."
            )
            return
        }

        viewModelScope.launch {
            _scanState.value = ScanUiState.Procesando
            val resultado = repository.registrarEscaneo(rawQrValue, beneficioActivo.id)
            val entidad = when (resultado) {
                is ScanResultado.Valido -> resultado.entidad
                is ScanResultado.YaEscaneado -> resultado.entidad
                else -> null
            }

            val nuevoEstado = when (resultado) {
                is ScanResultado.FormatoInvalido -> ScanUiState.FormatoInvalido
                is ScanResultado.NoEncontrado -> ScanUiState.NoEncontrado
                is ScanResultado.YaEscaneado -> ScanUiState.YaEscaneado(
                    nombre = resultado.nombre,
                    mensajeAlerta = resultado.mensaje,
                    fecha = resultado.fecha,
                    staff = resultado.staff
                )
                is ScanResultado.Valido -> ScanUiState.Valido(
                    nombre = resultado.nombre,
                    beneficio = resultado.beneficio.ifBlank { beneficioActivo.nombre },
                    fecha = resultado.fecha,
                    staff = resultado.staff,
                    pendienteSync = resultado.pendienteSync
                )
                is ScanResultado.Error -> ScanUiState.ErrorScan(resultado.mensaje)
            }

            _scanState.value = nuevoEstado

            val nombreAMostrar = when (resultado) {
                is ScanResultado.Valido -> resultado.nombre
                is ScanResultado.YaEscaneado -> resultado.nombre
                else -> entidad?.nombre ?: ""
            }

            val yaEntregado = resultado is ScanResultado.YaEscaneado
            val pendiente = (resultado as? ScanResultado.Valido)?.pendienteSync == true
            val mensajeServer = when (resultado) {
                is ScanResultado.YaEscaneado -> resultado.mensaje
                is ScanResultado.Valido -> if (resultado.pendienteSync) {
                    "Beneficio registrado localmente. Se sincronizará automáticamente."
                } else {
                    "Beneficio entregado y confirmado por el servidor."
                }
                is ScanResultado.Error -> resultado.mensaje
                else -> ""
            }

            _scanModalData.value = ScanModalData(
                estado = nuevoEstado,
                idParticipante = entidad?.idParticipante ?: rawQrValue,
                nombre = nombreAMostrar,
                beneficioNombre = beneficioActivo.nombre,
                fechaEscaneo = when (resultado) {
                    is ScanResultado.YaEscaneado -> resultado.fecha
                    is ScanResultado.Valido -> resultado.fecha
                    else -> null
                },
                staffNombre = when (resultado) {
                    is ScanResultado.YaEscaneado -> resultado.staff
                    is ScanResultado.Valido -> resultado.staff
                    else -> null
                },
                yaEntregadoPreviamente = yaEntregado,
                mensajeServidor = mensajeServer,
                pendienteSync = pendiente,
                entidad = entidad
            )
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

    fun actualizarBusqueda(query: String) {
        _busquedaQuery.value = query
    }

    fun cargarParticipantes(query: String = _busquedaQuery.value) {
        viewModelScope.launch {
            _cargandoParticipantes.value = true
            try {
                _listaParticipantes.value = repository.buscarParticipantes(query)
            } catch (_: Exception) {
                _listaParticipantes.value = emptyList()
            } finally {
                _cargandoParticipantes.value = false
            }
        }
    }

    fun entregarBeneficioIndividual(
        idParticipante: String,
        beneficioId: String,
        onResultado: (ScanResultado) -> Unit = {}
    ) {
        viewModelScope.launch {
            val resultado = repository.entregarBeneficioManual(idParticipante, beneficioId)
            onResultado(resultado)
        }
    }

    fun verificarConexion() {
        _hayInternet.value = repository.hayConexion()
    }

    fun forzarSincronizacion() {
        viewModelScope.launch {
            verificarConexion()
            if (!_hayInternet.value) {
                _syncUiState.value = SyncUiState.Error(
                    "Sin conexión a internet. Los escaneos se subirán automáticamente al reconectar."
                )
                return@launch
            }
            _syncUiState.value = SyncUiState.Sincronizando

            val resultado = repository.sincronizarCompleto()
            if (resultado.isSuccess) {
                _syncUiState.value = SyncUiState.Exito(resultado.getOrThrow())
                val eventoId = repository.obtenerEventoIdActual()
                if (eventoId != null) {
                    recargarDatosLocales(eventoId)
                } else {
                    cargarParticipantes()
                }
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

    private suspend fun recargarDatosLocales(eventoId: String) {
        _beneficiosActivos.value = repository.obtenerBeneficiosActivos(eventoId)
        val seleccionado = _beneficioSeleccionado.value
        if (seleccionado == null || _beneficiosActivos.value.none { it.id == seleccionado.id }) {
            _beneficioSeleccionado.value = _beneficiosActivos.value.firstOrNull()
        }
    }
}
