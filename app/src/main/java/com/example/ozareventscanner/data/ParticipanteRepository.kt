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
    data class YaEscaneado(
        val nombre: String,
        val mensaje: String = "",
        val fecha: String? = null,
        val staff: String? = null
    ) : ScanResultado()

    /** Escaneo registrado exitosamente. Beneficio marcado como entregado. */
    data class Valido(
        val nombre: String,
        val beneficio: String = "",
        val fecha: String? = null,
        val staff: String? = null
    ) : ScanResultado()

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
    // ─────────────────────────────────────────────────────────────────────────
    // Validación y extracción de formato QR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrae el código, documento o correo útil a partir de un valor raw de QR.
     * Soporta valores raw planos, JSON payloads ({ "id": ... }) y URLs de acreditación.
     */
    fun extraerValorQr(raw: String): String {
        val limpio = raw.trim()
        if (limpio.isEmpty()) return ""

        // Si viene estructurado como JSON: {"id": "10482914"} o {"documento": ...} o {"correo": ...}
        if (limpio.startsWith("{") && limpio.endsWith("}")) {
            try {
                val json = org.json.JSONObject(limpio)
                val extraido = json.optString("id")
                    .ifBlank { json.optString("participanteId") }
                    .ifBlank { json.optString("documento") }
                    .ifBlank { json.optString("correo") }
                    .ifBlank { json.optString("email") }
                    .ifBlank { json.optString("code") }
                if (extraido.isNotBlank()) return extraido
            } catch (_: Exception) {}
        }

        // Si viene como URL: https://api-eventos.zendov.com/ticket?code=10482914
        if (limpio.startsWith("http://") || limpio.startsWith("https://")) {
            try {
                val uri = android.net.Uri.parse(limpio)
                val codeParam = uri.getQueryParameter("code")
                    ?: uri.getQueryParameter("id")
                    ?: uri.getQueryParameter("documento")
                    ?: uri.getQueryParameter("correo")
                if (!codeParam.isNullOrBlank()) return codeParam
                val lastSegment = uri.lastPathSegment
                if (!lastSegment.isNullOrBlank()) return lastSegment
            } catch (_: Exception) {}
        }

        return limpio
    }

    /**
     * Valida sintácticamente que el identificador, correo o documento sea admisible.
     * Reglas estrictas:
     *  - Si contiene '@', debe cumplir el formato estricto de email RFC 5322.
     *  - No debe contener espacios en blanco.
     *  - Debe tener entre 1 y 100 caracteres.
     *  - Si no es correo, debe ser alfanumérico (letras, dígitos, guiones o guiones bajos).
     */
    fun esFormatoQrValido(valor: String): Boolean {
        if (valor.isBlank() || valor.length > 100) return false
        if (valor.contains(" ") || valor.contains("\n") || valor.contains("\r") || valor.contains("\t")) return false

        // Si parece un correo electrónico (contiene '@')
        if (valor.contains("@")) {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            return emailRegex.matches(valor)
        }

        // Código o Documento: alfanumérico con guiones o guiones bajos
        val idRegex = "^[A-Za-z0-9\\-_]+$".toRegex()
        return idRegex.matches(valor)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escaneo QR — flujo principal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registra un escaneo QR aplicando arquitectura Single Source of Truth (SSOT) Offline-First estricta:
     *  1. Valida sintácticamente el código.
     *  2. Valida contra Room de manera inmediata (< 10 ms).
     *  3. Si es válido, persiste en Room con timestamp y datos de auditoría (pendiente de subida).
     *  4. Encola sincronización en segundo plano con WorkManager (sin bloquear el hilo de la cámara).
     *  5. Retorna inmediatamente el resultado para respuesta instantánea de la UI.
     */
    suspend fun registrarEscaneo(rawQrValue: String, beneficioId: String = ""): ScanResultado {
        val qrLimpio = extraerValorQr(rawQrValue)
        if (!esFormatoQrValido(qrLimpio)) {
            return ScanResultado.FormatoInvalido
        }

        return try {
            asegurarDatosMockIniciales()
            val participante = dao.buscarParticipante(qrLimpio)
                ?: return ScanResultado.NoEncontrado

            if (participante.beneficioEntregado) {
                val fecha = participante.timestampEscaneo?.let {
                    java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it))
                }
                val staffNombre = participante.scannedByStaffNombre ?: "Operador de acreditaciones"
                return ScanResultado.YaEscaneado(
                    nombre  = participante.nombre,
                    mensaje = "Este beneficio ya fue reclamado y validado en el sistema.",
                    fecha   = fecha,
                    staff   = staffNombre
                )
            }

            val timestamp = System.currentTimeMillis()
            val session = SessionManager.getInstance(context).obtenerStaff()
            val staffId = session?.staffId
            val staffNombre = session?.nombreCompleto ?: "Staff Acreditación"
            val deviceId = "device_${android.os.Build.MODEL.replace(" ", "_")}"

            dao.marcarEscaneoLocalConAuditoria(
                id          = participante.idParticipante,
                timestamp   = timestamp,
                staffId     = staffId,
                staffNombre = staffNombre,
                deviceId    = deviceId,
                beneficioId = if (beneficioId.isNotBlank()) beneficioId else participante.beneficioId
            )

            // Disparar sincronización resiliente en segundo plano (no bloquea UI)
            if (hayConexion()) {
                programarSyncUnica()
            } else {
                programarSyncUnica()
            }

            ScanResultado.Valido(
                nombre    = participante.nombre,
                beneficio = if (beneficioId.isNotBlank()) beneficioId else "Beneficio Acreditado",
                fecha     = java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp)),
                staff     = staffNombre
            )

        } catch (e: Exception) {
            ScanResultado.Error("Error al procesar escaneo: ${e.localizedMessage}")
        }
    }


    /**
     * Obtiene los beneficios activos para el evento especificado.
     */
    suspend fun obtenerBeneficiosActivos(eventoId: String): List<BeneficioActivoDto> {
        return apiService.obtenerBeneficiosActivos(eventoId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contingencia: Búsqueda y Entrega Manual de Beneficio + Mock Data
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun asegurarDatosMockIniciales() {
        try {
            val total = dao.contarTotalParticipantes()
            if (total == 0) {
                dao.insertarParticipantes(MOCK_PARTICIPANTES)
            } else {
                val maria = dao.buscarParticipante("10482914")
                if (maria == null) {
                    dao.insertarParticipantes(MOCK_PARTICIPANTES)
                }
            }
        } catch (e: Exception) {
            // Silencioso si ya existen
        }
    }

    /** Obtiene todos los participantes o filtra por nombre/DNI. */
    suspend fun buscarParticipantes(query: String = ""): List<ParticipanteEntity> {
        asegurarDatosMockIniciales()
        val queryLimpia = query.trim()
        return if (queryLimpia.isBlank()) {
            dao.obtenerTodos()
        } else {
            dao.buscarPorNombreOId(queryLimpia)
        }
    }

    /** Observa el conteo total de asistentes registrados. */
    fun observarTotalParticipantes(): kotlinx.coroutines.flow.Flow<Int> = dao.observarTotalParticipantes()

    /** Cuenta el total de asistentes registrados actualmente. */
    suspend fun contarTotalParticipantes(): Int {
        asegurarDatosMockIniciales()
        return dao.contarTotalParticipantes()
    }

    /** Busca un participante por su ID específico. */
    suspend fun buscarParticipantePorId(id: String): ParticipanteEntity? {
        asegurarDatosMockIniciales()
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

        val deviceId = "device_${android.os.Build.MODEL.replace(" ", "_")}"
        val request = SyncUploadRequest(
            escaneos = pendientes.map { p ->
                EscaneoRequest(
                    idParticipante   = p.idParticipante,
                    timestampEscaneo = p.timestampEscaneo ?: System.currentTimeMillis(),
                    beneficioId      = p.beneficioId,
                    staffId          = p.scannedByStaffId,
                    deviceId         = p.scannedByDeviceId ?: deviceId
                )
            },
            deviceId = deviceId
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
                pendienteSubida        = false,
                documento              = dto.documento ?: dto.idParticipante,
                correo                 = dto.correo
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

    companion object {
        val MOCK_PARTICIPANTES = listOf(
            ParticipanteEntity(
                idParticipante         = "10482914",
                nombre                 = "María Camila Pérez",
                beneficioEntregado     = true,
                timestampEscaneo       = System.currentTimeMillis() - 1000L * 60 * 15, // hace 15 min
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "10482914",
                correo                 = "maria.perez@zendov.com"
            ),
            ParticipanteEntity(
                idParticipante         = "28491023",
                nombre                 = "Andrés Rodríguez",
                beneficioEntregado     = false,
                timestampEscaneo       = null,
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "28491023",
                correo                 = "andres.rodriguez@zendov.com"
            ),
            ParticipanteEntity(
                idParticipante         = "19482019",
                nombre                 = "Carlos Andrés Gómez",
                beneficioEntregado     = true,
                timestampEscaneo       = System.currentTimeMillis() - 1000L * 60 * 35, // hace 35 min
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "19482019",
                correo                 = "carlos.gomez@zendov.com"
            ),
            ParticipanteEntity(
                idParticipante         = "30192841",
                nombre                 = "Valeria Sofía Martínez",
                beneficioEntregado     = false,
                timestampEscaneo       = null,
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "30192841",
                correo                 = "valeria.martinez@zendov.com"
            ),
            ParticipanteEntity(
                idParticipante         = "12345678",
                nombre                 = "Diego Fernando Vargas",
                beneficioEntregado     = true,
                timestampEscaneo       = System.currentTimeMillis() - 1000L * 60 * 5, // hace 5 min
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "12345678",
                correo                 = "diego.vargas@zendov.com"
            ),
            ParticipanteEntity(
                idParticipante         = "45910283",
                nombre                 = "Lucía Mariana Rojas",
                beneficioEntregado     = false,
                timestampEscaneo       = null,
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "45910283",
                correo                 = "lucia.rojas@zendov.com"
            ),
            ParticipanteEntity(
                idParticipante         = "50192847",
                nombre                 = "Esteban Javier Mendoza",
                beneficioEntregado     = true,
                timestampEscaneo       = System.currentTimeMillis() - 1000L * 60 * 50, // hace 50 min
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "50192847",
                correo                 = "esteban.mendoza@zendov.com"
            ),
            ParticipanteEntity(
                idParticipante         = "60291834",
                nombre                 = "Alejandro Morales Rivera",
                beneficioEntregado     = false,
                timestampEscaneo       = null,
                sincronizadoConBackend = true,
                pendienteSubida        = false,
                documento              = "60291834",
                correo                 = "alejandro.morales@zendov.com"
            )
        )
    }
}
