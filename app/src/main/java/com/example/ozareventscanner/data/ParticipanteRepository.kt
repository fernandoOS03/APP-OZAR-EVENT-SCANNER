package com.example.ozareventscanner.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.example.ozareventscanner.network.*
import com.example.ozareventscanner.sync.SyncWorker
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// Tipos de resultado
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resultado de un intento de escaneo QR.
 * El ViewModel mapea esto a [ScanUiState] para la UI.
 */
sealed class ScanResultado {
    /** El QR no tiene el formato esperado (no es un ID de participante). */
    object FormatoInvalido : ScanResultado()

    /** El ID del QR no existe en la base de datos local. */
    object NoEncontrado : ScanResultado()

    /** El participante ya tenía su beneficio entregado. */
    data class YaEscaneado(val nombre: String) : ScanResultado()

    /** Escaneo registrado exitosamente. Beneficio marcado como entregado. */
    data class Valido(val nombre: String) : ScanResultado()

    /** Error inesperado durante el procesamiento. */
    data class Error(val mensaje: String) : ScanResultado()
}

/** Estadísticas de una operación de sincronización completa. */
data class SyncStats(
    val subidos: Int     = 0,
    val descargados: Int = 0,
    val rechazados: Int  = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// Repositorio
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Repositorio central de datos — única fuente de verdad (Single Source of Truth).
 *
 * Orquesta:
 *  - Acceso local a Room ([ParticipanteDao]).
 *  - Llamadas de red al backend ([ApiService]).
 *  - Sincronización en segundo plano (WorkManager).
 *
 * ## Estrategia offline-first
 *
 * Cada escaneo QR se persiste inmediatamente en Room con timestamp.
 * Si hay conectividad → sincronización inmediata (upload + download).
 * Si no hay red → WorkManager encola la sincronización hasta recuperar conexión.
 *
 * ## Resolución de conflictos
 *
 * Doble escaneo desde dos dispositivos offline:
 *  1. Ambos suben su escaneo al backend con su respectivo timestamp.
 *  2. El backend acepta el de menor timestamp (el más antiguo = el primero).
 *  3. El dispositivo perdedor recibe `validado: false` y revierte localmente.
 */
class ParticipanteRepository(
    private val dao: ParticipanteDao,
    private val apiService: ApiService,
    private val context: Context
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Validación de formato QR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica que el valor raw del QR corresponde a un ID de participante.
     *
     * Reglas:
     *  - No debe estar vacío ni superar 100 caracteres.
     *  - No debe ser una URL (http/https).
     *  - No debe ser JSON u otro formato estructurado ({, [).
     *  - No debe contener espacios (los IDs son alfanuméricos con guiones).
     *
     * Si el QR no cumple estas condiciones → [ScanResultado.FormatoInvalido].
     */
    private fun esIdParticipanteValido(rawQr: String): Boolean {
        if (rawQr.isBlank() || rawQr.length > 100) return false
        if (rawQr.startsWith("http://") || rawQr.startsWith("https://")) return false
        if (rawQr.startsWith("{") || rawQr.startsWith("[")) return false
        if (rawQr.contains(" ")) return false
        return true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escaneo QR — flujo principal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registra un escaneo QR aplicando la estrategia offline-first.
     *
     * Flujo:
     *  1. Valida el formato del QR → [ScanResultado.FormatoInvalido] si no es un ID.
     *  2. Busca el participante en Room → [ScanResultado.NoEncontrado] si no existe.
     *  3. Verifica si ya fue escaneado → [ScanResultado.YaEscaneado].
     *  4. Persiste el escaneo en Room con timestamp.
     *  5a. Con red → [sincronizarCompleto] inmediatamente.
     *  5b. Sin red → [programarSyncUnica] con WorkManager.
     *
     * @param rawQrValue Valor raw devuelto por ML Kit al escanear el código.
     * @return [ScanResultado] con el resultado de la operación.
     */
    suspend fun registrarEscaneo(rawQrValue: String): ScanResultado {
        // Paso 1: Validar formato del QR
        if (!esIdParticipanteValido(rawQrValue)) {
            return ScanResultado.FormatoInvalido
        }

        return try {
            // Paso 2: Buscar en Room
            val participante = dao.buscarParticipante(rawQrValue)
                ?: return ScanResultado.NoEncontrado

            // Paso 3: Verificar si ya fue escaneado
            if (participante.beneficioEntregado) {
                return ScanResultado.YaEscaneado(participante.nombre)
            }

            // Paso 4: Persistir escaneo local con timestamp
            val timestamp = System.currentTimeMillis()
            dao.marcarEscaneoLocal(rawQrValue, timestamp)

            // Paso 5: Sincronizar (inmediato o diferido)
            if (hayConexion()) {
                // Fire-and-forget: la UI no espera el resultado del sync
                sincronizarCompleto()
            } else {
                programarSyncUnica()
            }

            ScanResultado.Valido(participante.nombre)

        } catch (e: Exception) {
            ScanResultado.Error("Error al procesar escaneo: ${e.localizedMessage}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contingencia: Búsqueda y Entrega Manual de Beneficio
    // ─────────────────────────────────────────────────────────────────────────

    /** Obtiene todos los participantes o filtra por nombre/DNI. */
    suspend fun buscarParticipantes(query: String = ""): List<ParticipanteEntity> {
        val queryLimpia = query.trim()
        return if (queryLimpia.isBlank()) {
            dao.obtenerTodos()
        } else {
            dao.buscarPorNombreOId(queryLimpia)
        }
    }

    /** Busca un participante por su ID específico. */
    suspend fun buscarParticipantePorId(id: String): ParticipanteEntity? {
        return dao.buscarParticipante(id.trim())
    }

    /**
     * Entrega manualmente el beneficio a un participante (contingencia por QR dañado).
     * Aplica la misma persistencia con timestamp y sincronización diferida/inmediata.
     */
    suspend fun entregarBeneficioManual(idParticipante: String): ScanResultado {
        return try {
            val participante = dao.buscarParticipante(idParticipante)
                ?: return ScanResultado.NoEncontrado

            if (participante.beneficioEntregado) {
                return ScanResultado.YaEscaneado(participante.nombre)
            }

            val timestamp = System.currentTimeMillis()
            dao.marcarEscaneoLocal(idParticipante, timestamp)

            if (hayConexion()) {
                sincronizarCompleto()
            } else {
                programarSyncUnica()
            }

            ScanResultado.Valido(participante.nombre)
        } catch (e: Exception) {
            ScanResultado.Error("Error al entregar beneficio manual: ${e.localizedMessage}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Registro manual por código (POST → Room)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registra un participante enviando un código alfanumérico al backend.
     * El registro queda en Room con [sincronizadoConBackend] = true desde el inicio.
     *
     * @param codigo Código ingresado manualmente en [PantallaIngresoCodigo].
     * @return Nombre del participante registrado.
     * @throws Exception si el backend responde con error o no hay conectividad.
     */
    suspend fun registrarCodigoRemoto(codigo: String): String {
        val respuesta = apiService.registrarParticipante(CodigoRequest(codigo.trim()))
        val entidad   = ParticipanteEntity(
            idParticipante         = respuesta.idParticipante,
            nombre                 = respuesta.nombre,
            beneficioEntregado     = respuesta.beneficioEntregado,
            timestampEscaneo       = null,
            sincronizadoConBackend = true,
            pendienteSubida        = false
        )
        dao.insertarParticipantes(listOf(entidad))
        return respuesta.nombre
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sincronización completa (upload → download)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sincronización bidireccional completa.
     *
     * Fase 1 — Upload:
     *   - Obtiene todos los registros con `pendienteSubida = true`.
     *   - POST /sync/upload con IDs y timestamps.
     *   - Backend confirma (gana el menor timestamp) o rechaza (conflicto).
     *   - Confirmados: marca `sincronizadoConBackend = true`.
     *   - Rechazados:  revierte `beneficioEntregado = false` (otro dispositivo fue primero).
     *
     * Fase 2 — Download:
     *   - GET /sync/download → estado canónico del servidor.
     *   - REPLACE en Room con los datos del servidor.
     *
     * @return [Result<SyncStats>] con estadísticas o excepción en caso de fallo de red.
     */
    suspend fun sincronizarCompleto(): Result<SyncStats> = runCatching {
        val statsUpload   = subirPendientes()
        val countDownload = descargarYActualizar()
        SyncStats(
            subidos     = statsUpload.first,
            rechazados  = statsUpload.second,
            descargados = countDownload
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload (privado)
    // ─────────────────────────────────────────────────────────────────────────

    /** @return Par (confirmados, rechazados). */
    private suspend fun subirPendientes(): Pair<Int, Int> {
        val pendientes = dao.obtenerPendientesSync()
        if (pendientes.isEmpty()) return Pair(0, 0)

        val request = SyncUploadRequest(
            escaneos = pendientes.map { p ->
                EscaneoRequest(
                    idParticipante   = p.idParticipante,
                    timestampEscaneo = p.timestampEscaneo ?: System.currentTimeMillis()
                )
            }
        )

        val response = apiService.subirEscaneos(request)

        val idsConfirmados = if (response.resultados.isEmpty()) {
            // Si el backend (o mock) no devuelve detalle por item, confirmamos todo el lote enviado
            request.escaneos.map { it.idParticipante }
        } else {
            response.resultados
                .filter  { it.validado }
                .map     { it.idParticipante }
        }

        val idsRechazados = response.resultados
            .filter  { !it.validado }
            .map     { it.idParticipante }

        if (idsConfirmados.isNotEmpty()) {
            dao.marcarLoteComoSincronizado(idsConfirmados)
        }
        idsRechazados.forEach { id ->
            dao.revertirEscaneoRechazado(id)
        }

        return Pair(idsConfirmados.size, idsRechazados.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download (privado)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Descarga el estado canónico del servidor y reemplaza Room.
     * @return Número de participantes procesados.
     */
    private suspend fun descargarYActualizar(): Int {
        val response = apiService.descargarEstado()

        if (response.participantes.isEmpty()) return 0

        val entities = response.participantes.map { dto ->
            ParticipanteEntity(
                idParticipante         = dto.idParticipante,
                nombre                 = dto.nombre,
                beneficioEntregado     = dto.beneficioEntregado,
                timestampEscaneo       = dto.timestampEscaneo,
                sincronizadoConBackend = true,
                pendienteSubida        = false
            )
        }

        dao.sincronizarDesdeBackend(entities)
        return entities.size
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WorkManager — programación de sincronización
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encola una sincronización única que se ejecutará en cuanto haya red.
     * Usa backoff exponencial (30 s → 60 s → 120 s → falla a los 3 intentos).
     * Si ya existe un trabajo con el mismo nombre → lo reemplaza.
     */
    fun programarSyncUnica() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(SyncWorker.TAG_SYNC_UNICA)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.TAG_SYNC_UNICA,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    /**
     * Registra la sincronización periódica cada 15 minutos (mínimo de WorkManager).
     * Solo se ejecuta si hay conectividad.
     * [ExistingPeriodicWorkPolicy.KEEP] → no resetea el intervalo si ya existe.
     *
     * Llamar una vez al inicio de la app (en [OzarApplication.onCreate]).
     */
    fun programarSyncPeriodico() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval         = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.TAG_SYNC_PERIODICA)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SyncWorker.TAG_SYNC_PERIODICA,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades de red
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Devuelve true si el dispositivo tiene conectividad a internet activa y validada.
     * Requiere permiso android.permission.ACCESS_NETWORK_STATE en el Manifest.
     */
    fun hayConexion(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network      = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
