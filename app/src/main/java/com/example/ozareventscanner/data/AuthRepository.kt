package com.example.ozareventscanner.data

import android.content.Context
import androidx.room.withTransaction
import com.example.ozareventscanner.network.ApiService
import com.example.ozareventscanner.network.LoginRequest
import com.example.ozareventscanner.network.LoginResponse
import com.example.ozareventscanner.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de autenticación y gestión de sesión 1-a-1.
 *
 * Responsabilidad clave:
 *  Antes de guardar una nueva sesión de Staff, ejecuta una PURGA TOTAL de Room
 *  para garantizar que no quede ningún rastro o dato de eventos anteriores.
 */
class AuthRepository(
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val context: Context
) {
    private val dao = database.participante()

    /**
     * Autentica con Número de Documento y PIN numérico.
     * Ejecuta purga total de Room, guarda la nueva sesión y puebla la configuración inicial del evento.
     */
    suspend fun iniciarSesion(documento: String, pin: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val docLimpio = documento.trim()
            val pinLimpio = pin.trim()

            if (docLimpio.isBlank() || pinLimpio.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Documento y PIN son requeridos."))
            }

            // 1. Petición al Backend
            val response = apiService.login(LoginRequest(docLimpio, pinLimpio))

            // 2. PURGA TOTAL DE ROOM: Elimina datos de eventos previos
            database.withTransaction {
                database.clearAllTables()
            }

            // 3. Guardar la nueva sesión localmente
            sessionManager.guardarSesion(
                token = response.token,
                staffNombre = response.staffNombre,
                evento = response.evento
            )

            // 4. Si el backend incluyó participantes iniciales, poblarlos en Room
            if (response.evento.participantesIniciales.isNotEmpty()) {
                val entidades = response.evento.participantesIniciales.map { dto ->
                    ParticipanteEntity(
                        idParticipante = dto.idParticipante,
                        documento = dto.idParticipante,
                        nombre = dto.nombre,
                        beneficioEntregado = dto.beneficioEntregado,
                        timestampEscaneo = dto.timestampEscaneo,
                        isSynced = true
                    )
                }
                dao.insertarParticipantes(entidades)
            }

            // 5. Encolar sincronización silenciosa
            SyncWorker.encolarSyncSilencioso(context)

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión activa y purga la base de datos local.
     */
    suspend fun cerrarSesion(): Unit = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                database.clearAllTables()
            }
        } catch (_: Exception) {}
        sessionManager.cerrarSesion()
    }

    fun obtenerStaffNombre(): String = sessionManager.obtenerStaffNombre()

    fun obtenerEventoConfig() = sessionManager.obtenerEventoConfig()

    fun haySesionActiva(): Boolean = sessionManager.haySesionActiva()
}
