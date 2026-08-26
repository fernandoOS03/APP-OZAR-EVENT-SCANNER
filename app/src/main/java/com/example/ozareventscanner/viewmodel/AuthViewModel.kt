package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ozareventscanner.data.AuthRepository
import com.example.ozareventscanner.network.BeneficioDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Cargando : LoginUiState()
    data class Autenticado(val staffNombre: String, val nombreEvento: String) : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _staffNombre = MutableStateFlow(authRepository.obtenerStaffNombre())
    val staffNombre: StateFlow<String> = _staffNombre.asStateFlow()

    private val _nombreEvento = MutableStateFlow(authRepository.obtenerEventoConfig()?.nombreEvento ?: "Evento Ozar")
    val nombreEvento: StateFlow<String> = _nombreEvento.asStateFlow()

    private val _beneficiosEvento = MutableStateFlow<List<BeneficioDto>>(
        authRepository.obtenerEventoConfig()?.beneficios ?: emptyList()
    )
    val beneficiosEvento: StateFlow<List<BeneficioDto>> = _beneficiosEvento.asStateFlow()

    init {
        if (authRepository.haySesionActiva()) {
            val config = authRepository.obtenerEventoConfig()
            val staff = authRepository.obtenerStaffNombre()
            val evtNombre = config?.nombreEvento ?: "Evento Activo"
            _staffNombre.value = staff
            _nombreEvento.value = evtNombre
            _beneficiosEvento.value = config?.beneficios ?: emptyList()
            _loginState.value = LoginUiState.Autenticado(staff, evtNombre)
        }
    }

    /**
     * Autentica con Documento y PIN numérico.
     * Ejecuta purga total de datos de eventos previos y guarda la nueva sesión.
     */
    fun iniciarSesion(documento: String, pin: String) {
        val docTrim = documento.trim()
        val pinTrim = pin.trim()

        if (docTrim.isBlank() || pinTrim.isBlank()) {
            _loginState.value = LoginUiState.Error("Ingresa tu número de documento y PIN.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginUiState.Cargando
            val resultado = authRepository.iniciarSesion(docTrim, pinTrim)

            resultado.onSuccess { response ->
                _staffNombre.value = response.staffNombre
                _nombreEvento.value = response.evento.nombreEvento
                _beneficiosEvento.value = response.evento.beneficios
                _loginState.value = LoginUiState.Autenticado(
                    staffNombre = response.staffNombre,
                    nombreEvento = response.evento.nombreEvento
                )
            }.onFailure { error ->
                _loginState.value = LoginUiState.Error(
                    error.localizedMessage ?: "Error al autenticar. Verifica tus credenciales."
                )
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            authRepository.cerrarSesion()
            _loginState.value = LoginUiState.Idle
            _staffNombre.value = ""
            _nombreEvento.value = ""
            _beneficiosEvento.value = emptyList()
        }
    }

    fun limpiarError() {
        if (_loginState.value is LoginUiState.Error) {
            _loginState.value = LoginUiState.Idle
        }
    }
}
