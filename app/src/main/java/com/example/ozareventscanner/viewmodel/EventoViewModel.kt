package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ozareventscanner.data.EscaneoEntity
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.ScanResultado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Procesando : ScanUiState()
    object FormatoInvalido : ScanUiState()
    data class Valido(val nombre: String) : ScanUiState()
    data class YaEscaneado(val nombre: String) : ScanUiState()
    object NoEncontrado : ScanUiState()
    data class ErrorScan(val mensaje: String) : ScanUiState()
}

data class ScanModalData(
    val estado: ScanUiState,
    val idParticipante: String,
    val nombre: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class EventoViewModel(
    private val repository: ParticipanteRepository
) : ViewModel() {

    // ── Estado: Búsqueda Reactiva Indexada en Room ────────────────────────────
    private val _busquedaQuery = MutableStateFlow("")
    val busquedaQuery: StateFlow<String> = _busquedaQuery.asStateFlow()

    // Búsqueda en streaming directo desde Room con índices en base de datos
    val listaParticipantes: StateFlow<List<ParticipanteEntity>> = _busquedaQuery
        .debounce(150) // Evita queries redundantes durante escritura rápida
        .flatMapLatest { query ->
            repository.buscarParticipantesFlow(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Estado: Total de Participantes en Room ────────────────────────────────
    val totalParticipantes: StateFlow<Int> = repository.observarTotalParticipantes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Estado: Todos los Escaneos (para métricas individuales) ───────────────
    val listaEscaneos: StateFlow<List<EscaneoEntity>> = repository.observarEscaneos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Estado: Conectividad ──────────────────────────────────────────────────
    private val _hayInternet = MutableStateFlow(repository.hayConexion())
    val hayInternet: StateFlow<Boolean> = _hayInternet.asStateFlow()

    // ── Estado: Escáner QR ────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _scanModalData = MutableStateFlow<ScanModalData?>(null)
    val scanModalData: StateFlow<ScanModalData?> = _scanModalData.asStateFlow()

    private var ultimoCodigoEscaneado: String? = null

    init {
        verificarConexion()
    }

    fun verificarConexion() {
        _hayInternet.value = repository.hayConexion()
    }

    fun actualizarBusqueda(query: String) {
        _busquedaQuery.value = query
    }

    fun validarYMarcarParticipante(rawQrValue: String, idBeneficio: String = "") {
        if (rawQrValue == ultimoCodigoEscaneado && _scanModalData.value != null) return
        ultimoCodigoEscaneado = rawQrValue

        viewModelScope.launch {
            verificarConexion()
            _scanState.value = ScanUiState.Procesando

            val resultado = repository.registrarEscaneo(rawQrValue, idBeneficio)
            val nuevoEstado = when (resultado) {
                is ScanResultado.FormatoInvalido -> ScanUiState.FormatoInvalido
                is ScanResultado.NoEncontrado    -> ScanUiState.NoEncontrado
                is ScanResultado.YaEscaneado     -> ScanUiState.YaEscaneado(resultado.nombre)
                is ScanResultado.Valido          -> ScanUiState.Valido(resultado.nombre)
                is ScanResultado.Error           -> ScanUiState.ErrorScan(resultado.mensaje)
            }

            _scanState.value = nuevoEstado
            val nombre = when (resultado) {
                is ScanResultado.Valido -> resultado.nombre
                is ScanResultado.YaEscaneado -> resultado.nombre
                else -> ""
            }

            _scanModalData.value = ScanModalData(
                estado = nuevoEstado,
                idParticipante = rawQrValue,
                nombre = nombre
            )
        }
    }

    fun entregarBeneficioManual(idParticipante: String, idBeneficio: String = "") {
        viewModelScope.launch {
            verificarConexion()
            repository.entregarBeneficioManual(idParticipante, idBeneficio)
        }
    }

    fun cerrarModalScan() {
        _scanModalData.value = null
        _scanState.value = ScanUiState.Idle
        ultimoCodigoEscaneado = null
    }
}
