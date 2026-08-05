package com.example.ozareventscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ozareventscanner.data.ParticipanteDao
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.network.CodigoRequest
import com.example.ozareventscanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la UI para la Pantalla 2.
 * El ViewModel expone este sealed class para que la UI reaccione de forma declarativa.
 */
sealed class RegistroUiState {
    /** Estado inicial — el usuario aún no ha enviado ningún código. */
    object Idle : RegistroUiState()

    /** El POST está en vuelo (mostramos loading indicator). */
    object Cargando : RegistroUiState()

    /**
     * El backend respondió exitosamente y el participante ya fue guardado en Room.
     * @param nombre Nombre del participante devuelto por el backend.
     */
    data class Exito(val nombre: String) : RegistroUiState()

    /**
     * Algo salió mal (sin red, backend caído, código inválido…).
     * @param mensaje Texto legible para mostrar en la UI.
     */
    data class Error(val mensaje: String) : RegistroUiState()
}

/**
 * ViewModel de la Pantalla 2 (Ingreso de Código).
 *
 * Flujo:
 *  1. La UI llama a [registrarCodigo] con el código ingresado por el usuario.
 *  2. Se hace POST al backend vía Retrofit.
 *  3. Si tiene éxito → se guarda el ParticipanteEntity en Room y se emite [RegistroUiState.Exito].
 *  4. Si falla       → se emite [RegistroUiState.Error] con el mensaje correspondiente.
 *
 * @param participanteDao DAO de Room para persistencia local.
 */
class EventoViewModel(private val participanteDao: ParticipanteDao) : ViewModel() {

    // -------------------------------------------------------------------------
    // Estado reactivo (StateFlow → la UI observa con collectAsState())
    // -------------------------------------------------------------------------

    private val _uiState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val uiState: StateFlow<RegistroUiState> = _uiState

    // -------------------------------------------------------------------------
    // Lógica principal
    // -------------------------------------------------------------------------

    /**
     * Registra un código enviándolo al backend y guardando la respuesta en Room.
     *
     * @param codigo Código alfanumérico ingresado/escaneado por el usuario.
     */
    fun registrarCodigo(codigo: String) {
        // Validación básica antes de hacer la llamada de red
        if (codigo.isBlank()) {
            _uiState.value = RegistroUiState.Error("El código no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegistroUiState.Cargando

            try {
                // ── Paso 1: Llamada POST al backend ─────────────────────────
                val respuesta = RetrofitClient.apiService.registrarParticipante(
                    CodigoRequest(codigo = codigo.trim())
                )

                // ── Paso 2: Guardar en Room ──────────────────────────────────
                val entidad = ParticipanteEntity(
                    idParticipante   = respuesta.idParticipante,
                    nombre           = respuesta.nombre,
                    beneficioEntregado = respuesta.beneficioEntregado   // siempre false al registrar
                )
                participanteDao.insertarParticipantes(listOf(entidad))

                // ── Paso 3: Notificar éxito a la UI ─────────────────────────
                _uiState.value = RegistroUiState.Exito(nombre = respuesta.nombre)

            } catch (e: retrofit2.HttpException) {
                // El servidor respondió con 4xx / 5xx
                val codigoHttp = e.code()
                _uiState.value = when (codigoHttp) {
                    404 -> RegistroUiState.Error("Código no encontrado en el sistema.")
                    409 -> RegistroUiState.Error("Este participante ya fue registrado.")
                    else -> RegistroUiState.Error("Error del servidor ($codigoHttp). Intenta de nuevo.")
                }
            } catch (e: java.net.UnknownHostException) {
                // Sin conexión a internet o DNS no resuelve
                _uiState.value = RegistroUiState.Error("Sin conexión a internet. Verifica tu red.")
            } catch (e: java.net.SocketTimeoutException) {
                // El backend tardó demasiado
                _uiState.value = RegistroUiState.Error("Tiempo de espera agotado. Intenta de nuevo.")
            } catch (e: Exception) {
                // Cualquier otro error inesperado
                _uiState.value = RegistroUiState.Error("Error inesperado: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Reinicia el estado a Idle (útil para limpiar el mensaje de error/éxito).
     */
    fun resetearEstado() {
        _uiState.value = RegistroUiState.Idle
    }

    // -------------------------------------------------------------------------
    // Función heredada (compatibilidad con código anterior si se usa en otro lado)
    // -------------------------------------------------------------------------

    /** @deprecated Usar [registrarCodigo] en su lugar. */
    fun procesarRequestBackend(uuid: String, nombreBackend: String) {
        val participante = ParticipanteEntity(
            idParticipante    = uuid,
            nombre            = nombreBackend,
            beneficioEntregado = false
        )
        viewModelScope.launch {
            try {
                participanteDao.insertarParticipantes(listOf(participante))
            } catch (e: Exception) {
                // Manejo de error pendiente
            }
        }
    }
}
