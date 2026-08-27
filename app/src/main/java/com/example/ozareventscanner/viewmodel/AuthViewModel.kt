package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
// Credenciales hardcodeadas (development-only)
// En producción: reemplazar por llamada al endpoint de autenticación + JWT
// ─────────────────────────────────────────────────────────────────────────────
private object CredencialesDemo {
    const val EMAIL    = "admin@ozar.com"
    const val PASSWORD = "ozar2025"
    const val NOMBRE   = "Carlos Administrador"
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State del login
// ─────────────────────────────────────────────────────────────────────────────
sealed class LoginUiState {
    /** Pantalla recién cargada o limpiada. */
    object Idle : LoginUiState()

    /** Credenciales incorrectas. */
    data class ErrorCredenciales(val mensaje: String) : LoginUiState()

    /** Login exitoso — contiene el nombre del usuario para mostrar en el header. */
    data class Autenticado(val nombreUsuario: String) : LoginUiState()
}

/**
 * ViewModel de autenticación.
 *
 * ▸ Valida contra credenciales hardcodeadas (modo demo).
 * ▸ Cuando el backend de auth esté listo, reemplazar [iniciarSesion]
 *   por una llamada Retrofit al endpoint de login que devuelva un JWT
 *   y guardarlo con EncryptedSharedPreferences.
 */
class AuthViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    /** Nombre del usuario actualmente autenticado (vacío si no hay sesión). */
    private val _nombreUsuario = MutableStateFlow("")
    val nombreUsuario: StateFlow<String> = _nombreUsuario

    // ── Acciones ──────────────────────────────────────────────────────────────

    /**
     * Intenta autenticar al usuario con email + contraseña.
     * Actualmente valida contra [CredencialesDemo]; listo para conectar con backend.
     */
    fun iniciarSesion(email: String, password: String) {
        val emailTrim = email.trim().lowercase()

        // Validaciones de formato
        if (emailTrim.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState.ErrorCredenciales("Completa todos los campos.")
            return
        }

        // Validación contra credenciales demo
        if (emailTrim == CredencialesDemo.EMAIL && password == CredencialesDemo.PASSWORD) {
            _nombreUsuario.value = CredencialesDemo.NOMBRE
            _loginState.value    = LoginUiState.Autenticado(CredencialesDemo.NOMBRE)
        } else {
            _loginState.value = LoginUiState.ErrorCredenciales("Email o contraseña incorrectos.")
        }
    }

    /** Cierra la sesión actual y regresa al estado inicial. */
    fun cerrarSesion() {
        _nombreUsuario.value = ""
        _loginState.value    = LoginUiState.Idle
    }

    /** Limpia el mensaje de error (p.ej. cuando el usuario empieza a escribir). */
    fun limpiarError() {
        if (_loginState.value is LoginUiState.ErrorCredenciales) {
            _loginState.value = LoginUiState.Idle
        }
    }
}
