package com.example.ozareventscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ozareventscanner.OzarApplication
import com.example.ozareventscanner.network.EventoStaffDto
import com.example.ozareventscanner.network.RetrofitClient
import com.example.ozareventscanner.network.StaffDataDto
import com.example.ozareventscanner.network.StaffLoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// ─────────────────────────────────────────────────────────────────────────────
// UI State del Login de Staff
// ─────────────────────────────────────────────────────────────────────────────
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Cargando : LoginUiState()
    data class ErrorCredenciales(val mensaje: String) : LoginUiState()
    data class Autenticado(val staff: StaffDataDto) : LoginUiState()
}

/**
 * ViewModel de autenticación de Staff para Ozar Events Scanner.
 *
 * Implementa el Endpoint Móvil #1: POST /api/scanner/auth (DNI + PIN de 6 dígitos).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = (application as OzarApplication).sessionManager
    private val apiService     = RetrofitClient.apiService

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    private val _staffActual = MutableStateFlow<StaffDataDto?>(null)
    val staffActual: StateFlow<StaffDataDto?> = _staffActual

    init {
        verificarSesionExistente()
    }

    /** Verifica si ya hay un token y datos de staff guardados localmente. */
    fun verificarSesionExistente() {
        if (sessionManager.estaAutenticado()) {
            val staff = sessionManager.obtenerStaff()
            if (staff != null) {
                _staffActual.value = staff
                _loginState.value  = LoginUiState.Autenticado(staff)
            }
        }
    }

    /**
     * Autentica al operador del staff validando el documento y el código generado (PIN de 4-6 dígitos).
     */
    fun iniciarSesion(dni: String, pin: String) {
        val dniLimpio = dni.trim()
        val pinLimpio = pin.trim()

        // Validaciones estrictas de formato de Documento y Código Generado (PIN)
        if (dniLimpio.isBlank()) {
            _loginState.value = LoginUiState.ErrorCredenciales("Por favor ingresa tu documento de identidad.")
            return
        }

        if (dniLimpio.length < 6) {
            _loginState.value = LoginUiState.ErrorCredenciales("El documento de identidad debe tener al menos 6 caracteres.")
            return
        }

        if (pinLimpio.isBlank()) {
            _loginState.value = LoginUiState.ErrorCredenciales("Por favor ingresa tu código generado (PIN de acceso).")
            return
        }

        if (pinLimpio.length < 4 || pinLimpio.length > 6 || !pinLimpio.all { it.isDigit() }) {
            _loginState.value = LoginUiState.ErrorCredenciales("El código generado (PIN) debe tener entre 4 y 6 dígitos numéricos.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginUiState.Cargando
            try {
                val response = apiService.loginStaff(
                    StaffLoginRequest(
                        documentoIdentidad = dniLimpio,
                        pinAcceso          = pinLimpio
                    )
                )

                // Guardar la sesión persistente con JWT
                sessionManager.guardarSesion(response.token, response.staff)
                _staffActual.value = response.staff
                _loginState.value  = LoginUiState.Autenticado(response.staff)

            } catch (e: HttpException) {
                val serverMsg = try {
                    val body = e.response()?.errorBody()?.string()
                    if (!body.isNullOrBlank()) {
                        org.json.JSONObject(body).optString("message").takeIf { it.isNotBlank() }
                    } else null
                } catch (_: Exception) {
                    null
                }

                val errorMsg = when (e.code()) {
                    401 -> serverMsg ?: "Credenciales no válidas o no autorizadas"
                    403 -> serverMsg ?: "Acceso denegado. El staff se encuentra inactivo para este evento."
                    404 -> serverMsg ?: "Operador no encontrado en el sistema."
                    else -> serverMsg ?: "Error del servidor (${e.code()}). Intenta nuevamente."
                }
                _loginState.value = LoginUiState.ErrorCredenciales(errorMsg)

            } catch (e: IOException) {
                // Servidor backend offline: solo permitir el login hardcodeado si las credenciales coinciden exactamente
                if (dniLimpio == "12345678" && pinLimpio == "654321") {
                    val staffDev = StaffDataDto(
                        staffId = "STF-025",
                        nombreCompleto = "Luis Mendoza",
                        documentoIdentidad = "12345678",
                        rol = "Coordinador de accesos",
                        cargo = "Coordinador de accesos",
                        evento = EventoStaffDto(
                            eventoId = "EVT-025",
                            nombre = "Zendov Tech Summit 2025",
                            slug = "zendov-summit-2025",
                            tenantId = "tenant_zendov_2025"
                        )
                    )
                    val tokenDev = "eyJhbGciOiJIUzI1NiJ9.demo-offline"
                    sessionManager.guardarSesion(tokenDev, staffDev)
                    _staffActual.value = staffDev
                    _loginState.value  = LoginUiState.Autenticado(staffDev)
                } else {
                    _loginState.value = LoginUiState.ErrorCredenciales("No fue posible validar tus credenciales en el servidor (api-eventos.zendov.com).")
                }

            } catch (e: Exception) {
                _loginState.value = LoginUiState.ErrorCredenciales("Error inesperado: ${e.localizedMessage}")
            }
        }
    }

    /** Cierra la sesión activa del Staff. */
    fun cerrarSesion() {
        sessionManager.cerrarSesion()
        _staffActual.value = null
        _loginState.value  = LoginUiState.Idle
    }

    /** Limpia mensajes de error al editar los campos. */
    fun limpiarError() {
        if (_loginState.value is LoginUiState.ErrorCredenciales) {
            _loginState.value = LoginUiState.Idle
        }
    }
}
