package com.example.ozareventscanner.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.ozareventscanner.network.ApiService
import com.example.ozareventscanner.network.BeneficioActivoDto
import com.example.ozareventscanner.network.EscaneoConfirmado
import com.example.ozareventscanner.network.EscaneoRequest
import com.example.ozareventscanner.network.SyncUploadRequest
import com.example.ozareventscanner.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class ScanResultado {
    object FormatoInvalido : ScanResultado()
    object NoEncontrado : ScanResultado()

    data class YaEscaneado(
        val nombre: String,
        val mensaje: String = "",
        val fecha: String? = null,
        val staff: String? = null,
        val entidad: ParticipanteEntity? = null
    ) : ScanResultado()

    data class Valido(
        val nombre: String,
        val beneficio: String = "",
        val fecha: String? = null,
        val staff: String? = null,
        val pendienteSync: Boolean = true,
        val entidad: ParticipanteEntity? = null
    ) : ScanResultado()

    data class Error(val mensaje: String) : ScanResultado()
}

data class UploadStats(
    val subidos: Int = 0,
    val conflictos: Int = 0,
    val rechazados: Int = 0
)

data class SyncStats(
    val subidos: Int = 0,
    val descargados: Int = 0,
    val rechazados: Int = 0,
    val conflictos: Int = 0
)

data class ParticipanteConBeneficios(
    val participante: ParticipanteEntity,
    val escaneos: Map<String, EscaneoEntity>
) {
    val idParticipante: String get() = participante.idParticipante
    val nombre: String get() = participante.nombre
    val documento: String? get() = participante.documento
    val correo: String? get() = participante.correo

    val beneficiosEntregados: Set<String>
        get() = escaneos.values
            .filter { it.entregado && it.syncError == null }
            .map { it.beneficioId }
            .toSet()

    val beneficioEntregado: Boolean get() = beneficiosEntregados.isNotEmpty()
    val tienePendientesSync: Boolean get() = escaneos.values.any { it.pendienteSubida }
    val tieneErroresSync: Boolean get() = escaneos.values.any { it.syncError != null }
    val tieneConflictos: Boolean get() = escaneos.values.any { it.conflicto }
}

class ParticipanteRepository(
    private val dao: ParticipanteDao,
    private val apiService: ApiService,
    private val context: Context?,
    private val sessionProvider: (() -> SessionInfo?)? = null,
    private val deviceIdProvider: (() -> String)? = null,
    private val connectivityChecker: (() -> Boolean)? = null,
    private val uploadScheduler: (() -> Unit)? = null
) {

    data class SessionInfo(
        val eventoId: String?,
        val staffId: String?,
        val staffNombre: String?
    )

    private val syncMutex = Mutex()

    private val sessionManager: SessionManager?
        get() = context?.let { SessionManager.getInstance(it) }

    private fun obtenerSesionActual(): SessionInfo? {
        sessionProvider?.let { return it() }
        val sm = sessionManager ?: return null
        val staff = sm.obtenerStaff() ?: return null
        return SessionInfo(
            eventoId = staff.evento.eventoId,
            staffId = staff.staffId,
            staffNombre = staff.nombreCompleto
        )
    }

    private val deviceId: String
        get() = deviceIdProvider?.invoke()
            ?: sessionManager?.obtenerDeviceId()
            ?: "device_unknown"

    // ─────────────────────────────────────────────────────────────────────────
    // Validación y extracción de formato QR
    // ─────────────────────────────────────────────────────────────────────────

    fun extraerValorQr(raw: String): String {
        val limpio = raw.trim()
        if (limpio.isEmpty()) return ""

        if (limpio.startsWith("{") && limpio.endsWith("}")) {
            try {
                val json = org.json.JSONObject(limpio)
                val extraido = json.optString("id")
                    .ifBlank { json.optString("participanteId") }
                    .ifBlank { json.optString("code") }
                if (extraido.isNotBlank()) return extraido.trim()
            } catch (_: Exception) {}
        }

        if (limpio.startsWith("http://") || limpio.startsWith("https://")) {
            try {
                val uri = android.net.Uri.parse(limpio)
                val codeParam = uri.getQueryParameter("code")
                    ?: uri.getQueryParameter("id")
                    ?: uri.getQueryParameter("participanteId")
                if (!codeParam.isNullOrBlank()) return codeParam.trim()
                val lastSegment = uri.lastPathSegment
                if (!lastSegment.isNullOrBlank()) return lastSegment.trim()
            } catch (_: Exception) {}
        }

        return limpio
    }

    fun esFormatoQrValido(valor: String): Boolean = normalizarUuid(valor) != null

    private fun normalizarUuid(valor: String): String? {
        val limpio = extraerValorQr(valor)
        if (limpio.isBlank()) return null
        return try {
            UUID.fromString(limpio).toString()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun fechaLegible(timestamp: Long?): String? {
        if (timestamp == null) return null
        return SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escaneo QR — flujo principal (Offline-first)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun registrarEscaneo(rawQrValue: String, beneficioId: String): ScanResultado {
        val participanteId = normalizarUuid(rawQrValue) ?: return ScanResultado.FormatoInvalido
        val beneficioUuid = try {
            UUID.fromString(beneficioId.trim()).toString()
        } catch (_: IllegalArgumentException) {
            return ScanResultado.Error("Selecciona un beneficio válido antes de escanear.")
        }

        return try {
            val participante = dao.buscarParticipante(participanteId)
                ?: return ScanResultado.NoEncontrado
            val beneficio = dao.obtenerBeneficioPorId(beneficioUuid)
                ?: return ScanResultado.Error("El beneficio seleccionado ya no está disponible.")

            val existente = dao.obtenerEscaneo(participanteId, beneficioUuid)
            if (existente != null && existente.entregado && existente.syncError == null) {
                return ScanResultado.YaEscaneado(
                    nombre = participante.nombre,
                    mensaje = "Este beneficio ya fue reclamado y validado en el sistema.",
                    fecha = fechaLegible(existente.timestampEscaneo),
                    staff = existente.scannedByStaffNombre ?: "Operador de acreditaciones",
                    entidad = participante
                )
            }

            if (existente?.syncError != null) {
                return ScanResultado.Error(
                    "Este escaneo fue rechazado por el servidor: ${existente.syncError}"
                )
            }

            val timestamp = System.currentTimeMillis()
            val sesion = obtenerSesionActual()
            val staffNombre = sesion?.staffNombre ?: "Staff Acreditación"
            val eventoId = sesion?.eventoId ?: beneficio.eventoId.orEmpty()

            dao.upsertEscaneo(
                EscaneoEntity(
                    participanteId = participanteId,
                    beneficioId = beneficioUuid,
                    eventoId = eventoId,
                    entregado = true,
                    timestampEscaneo = timestamp,
                    sincronizadoConBackend = false,
                    pendienteSubida = true,
                    scannedByStaffId = sesion?.staffId,
                    scannedByStaffNombre = staffNombre,
                    scannedByDeviceId = deviceId,
                    syncError = null,
                    conflicto = false
                )
            )

            programarSyncUnica()

            ScanResultado.Valido(
                nombre = participante.nombre,
                beneficio = beneficio.nombre,
                fecha = fechaLegible(timestamp),
                staff = staffNombre,
                pendienteSync = true,
                entidad = participante
            )
        } catch (e: Exception) {
            ScanResultado.Error("Error al procesar escaneo: ${e.localizedMessage}")
        }
    }

    suspend fun obtenerBeneficiosActivos(eventoId: String): List<BeneficioEntity> {
        val locales = dao.obtenerBeneficiosActivos()
        if (locales.isNotEmpty()) return locales

        if (!hayConexion()) return emptyList()

        return runCatching {
            val remotos = apiService.obtenerBeneficiosActivos(eventoId)
            dao.insertarBeneficios(remotos.map { it.toEntity(eventoId) })
            dao.obtenerBeneficiosActivos()
        }.getOrDefault(emptyList())
    }

    fun observarBeneficiosActivos(): Flow<List<BeneficioEntity>> = dao.observarBeneficiosActivos()

    // ─────────────────────────────────────────────────────────────────────────
    // Contingencia: búsqueda y entrega manual
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun buscarParticipantes(query: String = ""): List<ParticipanteConBeneficios> {
        val queryLimpia = query.trim()
        val participantes = if (queryLimpia.isBlank()) {
            dao.obtenerTodos()
        } else {
            dao.buscarPorNombreOId(queryLimpia)
        }
        val escaneosPorParticipante = dao.obtenerTodosEscaneos().groupBy { it.participanteId }
        return participantes.map { p ->
            ParticipanteConBeneficios(
                participante = p,
                escaneos = escaneosPorParticipante[p.idParticipante]
                    .orEmpty()
                    .associateBy { it.beneficioId }
            )
        }
    }

    fun paginarParticipantes(query: String): Flow<PagingData<ParticipanteConBeneficios>> {
        val queryLimpia = query.trim()
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 15,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                if (queryLimpia.isBlank()) {
                    dao.pagingTodos()
                } else {
                    dao.pagingBuscarPorPrefijo(queryLimpia)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { relation -> relation.toParticipanteConBeneficios() }
        }
    }

    fun observarConteoPorBeneficio(): Flow<Map<String, Int>> =
        dao.observarConteoPorBeneficio().map { lista ->
            lista.associate { it.beneficioId to it.total }
        }

    fun observarTotalParticipantesEscaneados(): Flow<Int> =
        dao.observarTotalParticipantesEscaneados()

    fun observarEscaneosEntregados(): Flow<List<EscaneoEntity>> =
        dao.observarEscaneosEntregados()

    fun observarTotalParticipantes(): Flow<Int> = dao.observarTotalParticipantes()

    fun observarPendientesSync(): Flow<Int> = dao.observarPendientesSync()

    fun observarPendientesSyncDeSesion(eventoId: String, staffId: String?): Flow<Int> =
        dao.observarPendientesSyncDeSesion(eventoId, staffId)

    fun observarPendientesOtrosUsuarios(eventoId: String, staffId: String?): Flow<Int> =
        dao.observarPendientesOtrosUsuarios(eventoId, staffId)

    fun observarConflictosCount(eventoId: String): Flow<Int> =
        dao.observarConflictosCount(eventoId)

    suspend fun obtenerConflictos(eventoId: String): List<EscaneoEntity> =
        dao.obtenerConflictos(eventoId)

    fun obtenerEventoIdActual(): String? = obtenerSesionActual()?.eventoId

    fun obtenerStaffIdActual(): String? = obtenerSesionActual()?.staffId

    suspend fun contarTotalParticipantes(): Int = dao.contarTotalParticipantes()

    suspend fun buscarParticipantePorId(id: String): ParticipanteEntity? {
        val limpio = normalizarUuid(id) ?: extraerValorQr(id)
        return dao.buscarParticipante(limpio)
    }

    suspend fun entregarBeneficioManual(
        idParticipante: String,
        beneficioId: String
    ): ScanResultado = registrarEscaneo(idParticipante, beneficioId)

    // ─────────────────────────────────────────────────────────────────────────
    // Sincronización separada: Upload (por escaneo) y Full (periódico/manual)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun inicializarEvento(eventoId: String): Result<SyncStats> {
        // Limpiar datos ya sincronizados de otros eventos sin tocar pendientes de otros usuarios
        dao.limpiarParticipantesSincronizadosDeOtrosEventos(eventoId)
        dao.limpiarEscaneosSincronizadosDeOtrosEventos(eventoId)
        dao.limpiarBeneficiosDeOtrosEventos(eventoId)

        val esPrimeraDescarga = ultimoTimestampDelta(eventoId) == null
        return sincronizarCompleto(eventoId = eventoId, descargaCompleta = esPrimeraDescarga)
    }

    /**
     * Descarga únicamente los cambios incrementales (delta) del servidor usando el cursor
     * persistido por evento. Si aún no existe cursor, realiza descarga inicial paginada.
     */
    suspend fun descargarSoloCambios(
        eventoId: String? = null,
        descargaCompleta: Boolean = false
    ): Result<Int> = runCatching {
        if (!hayConexion()) {
            throw IOException("Sin conexión a internet para descargar actualizaciones.")
        }
        syncMutex.withLock {
            val idEvento = eventoId ?: obtenerSesionActual()?.eventoId
                ?: throw IllegalStateException("No hay un evento activo en la sesión.")
            descargarYActualizar(
                eventoId = idEvento,
                since = if (descargaCompleta) null else ultimoTimestampDelta(idEvento)
            )
        }
    }

    /**
     * Sube únicamente los escaneos pendientes de la sesión activa (eventoId + staffId)
     * en bucle hasta vaciar la cola o alcanzar [MAX_VUELTAS_UPLOAD].
     */
    suspend fun subirSoloPendientes(): Result<UploadStats> = runCatching {
        if (!hayConexion()) {
            throw IOException("Sin conexión a internet para subir escaneos pendientes.")
        }
        syncMutex.withLock {
            val sesion = obtenerSesionActual()
            val idEvento = sesion?.eventoId
                ?: throw IllegalStateException("No hay un evento activo en la sesión.")
            subirPendientesInterno(eventoId = idEvento, staffId = sesion.staffId)
        }
    }

    suspend fun sincronizarCompleto(
        eventoId: String? = null,
        descargaCompleta: Boolean = false
    ): Result<SyncStats> = runCatching {
        if (!hayConexion()) {
            throw IOException("Sin conexión a internet.")
        }
        syncMutex.withLock {
            val sesion = obtenerSesionActual()
            val idEvento = eventoId ?: sesion?.eventoId
                ?: throw IllegalStateException("No hay un evento activo en la sesión.")

            val upload = subirPendientesInterno(eventoId = idEvento, staffId = sesion?.staffId)
            val descargados = descargarYActualizar(
                eventoId = idEvento,
                since = if (descargaCompleta) null else ultimoTimestampDelta(idEvento)
            )

            SyncStats(
                subidos = upload.subidos,
                rechazados = upload.rechazados,
                conflictos = upload.conflictos,
                descargados = descargados
            )
        }
    }

    private suspend fun subirPendientesInterno(eventoId: String, staffId: String?): UploadStats {
        var totalConfirmados = 0
        var totalConflictos = 0
        var totalRechazados = 0
        var vueltas = 0

        while (vueltas < MAX_VUELTAS_UPLOAD) {
            val pendientes = dao.obtenerPendientesSyncDeSesion(
                eventoId = eventoId,
                staffId = staffId,
                limit = MAX_ESCANEOS_POR_LOTE
            )
            if (pendientes.isEmpty()) break

            val validos = mutableListOf<EscaneoEntity>()
            pendientes.forEach { escaneo ->
                val participanteUuid = normalizarUuid(escaneo.participanteId)
                val beneficioUuid = try {
                    UUID.fromString(escaneo.beneficioId).toString()
                } catch (_: IllegalArgumentException) {
                    null
                }

                if (participanteUuid == null || beneficioUuid == null) {
                    dao.marcarEscaneoConError(
                        escaneo.participanteId,
                        escaneo.beneficioId,
                        "Identificador local inválido; no se puede sincronizar."
                    )
                    totalRechazados++
                } else {
                    validos.add(escaneo)
                }
            }

            if (validos.isEmpty()) {
                vueltas++
                continue
            }

            val request = SyncUploadRequest(
                escaneos = validos.map { escaneo ->
                    EscaneoRequest(
                        idParticipante = escaneo.participanteId,
                        timestampEscaneo = escaneo.timestampEscaneo ?: System.currentTimeMillis(),
                        beneficioId = escaneo.beneficioId,
                        staffId = escaneo.scannedByStaffId,
                        deviceId = escaneo.scannedByDeviceId ?: deviceId
                    )
                },
                deviceId = deviceId
            )

            val response = apiService.subirEscaneos(request)
            if (response.resultados.isEmpty()) {
                throw IllegalStateException("El servidor no confirmó ningún escaneo del lote.")
            }

            val resultados = response.resultados
            var omitidosReintentables = 0

            validos.forEach { escaneo ->
                val resultado = resultados.firstOrNull {
                    it.idParticipante == escaneo.participanteId &&
                        it.beneficioId != null &&
                        it.beneficioId.equals(escaneo.beneficioId, ignoreCase = true)
                } ?: resultados.firstOrNull {
                    it.idParticipante == escaneo.participanteId && it.beneficioId == null
                }

                when {
                    resultado == null -> {
                        // Error transitorio / omisión del servidor: NO bloquear ni marcar syncError; reintentar.
                        omitidosReintentables++
                    }
                    resultado.estado == EscaneoConfirmado.ESTADO_CONFIRMADO ||
                        (resultado.estado == null && resultado.validado) -> {
                        dao.marcarEscaneoSincronizado(
                            escaneo.participanteId,
                            escaneo.beneficioId,
                            resultado.timestampValidado ?: escaneo.timestampEscaneo
                        )
                        totalConfirmados++
                    }
                    resultado.estado == EscaneoConfirmado.ESTADO_YA_ENTREGADO -> {
                        dao.marcarEscaneoConflictoYaEntregado(
                            participanteId = escaneo.participanteId,
                            beneficioId = escaneo.beneficioId,
                            timestampOriginal = resultado.timestampValidado,
                            staffOriginalNombre = resultado.staffOriginalNombre,
                            mensaje = resultado.mensaje ?: "Este beneficio ya había sido entregado previamente."
                        )
                        totalConflictos++
                    }
                    else -> {
                        dao.marcarEscaneoConError(
                            escaneo.participanteId,
                            escaneo.beneficioId,
                            resultado.mensaje ?: "Escaneo rechazado por el servidor."
                        )
                        totalRechazados++
                    }
                }
            }

            if (omitidosReintentables > 0) {
                throw IllegalStateException(
                    "El servidor no devolvió resultado para $omitidosReintentables escaneo(s); se reintentará."
                )
            }

            vueltas++
        }

        return UploadStats(
            subidos = totalConfirmados,
            conflictos = totalConflictos,
            rechazados = totalRechazados
        )
    }

    private suspend fun descargarYActualizar(eventoId: String, since: Long?): Int {
        var page = 0
        var totalProcesados = 0
        var cursorServidor: Long? = null

        while (page < MAX_PAGINAS_DOWNLOAD) {
            val response = apiService.descargarEstado(
                eventoId = eventoId,
                since = since,
                page = page,
                size = PAGE_SIZE_DOWNLOAD
            )

            if (page == 0) {
                cursorServidor = response.timestampServidor
            }

            val beneficiosEntities = response.beneficios.map { it.toEntity(eventoId) }
            val (eliminadosDto, activosDto) = response.participantes.partition { it.eliminado }
            val idsEliminados = eliminadosDto.map { it.idParticipante }

            val participantesActivos = activosDto.map { dto ->
                ParticipanteEntity(
                    idParticipante = dto.idParticipante,
                    nombre = dto.nombre,
                    documento = dto.documento,
                    correo = dto.correo,
                    eventoId = eventoId
                )
            }

            // Carga en bloque de escaneos existentes para esta página (evita N+1)
            val idsActivos = activosDto.map { it.idParticipante }
            val escaneosLocalesPorClave = mutableMapOf<Pair<String, String>, EscaneoEntity>()
            idsActivos.chunked(500).forEach { chunk ->
                dao.obtenerEscaneosDeParticipantes(chunk).forEach { esc ->
                    escaneosLocalesPorClave[esc.participanteId to esc.beneficioId] = esc
                }
            }

            val escaneosUpsert = mutableListOf<EscaneoEntity>()
            val escaneosBorrar = mutableListOf<Pair<String, String>>()

            activosDto.forEach { dto ->
                dto.beneficiosEstado.forEach { estado ->
                    val local = escaneosLocalesPorClave[dto.idParticipante to estado.beneficioId]
                    if (local?.pendienteSubida == true) {
                        // Nunca pisar un escaneo local pendiente de subir
                        return@forEach
                    }
                    if (estado.entregado) {
                        escaneosUpsert.add(
                            EscaneoEntity(
                                participanteId = dto.idParticipante,
                                beneficioId = estado.beneficioId,
                                eventoId = eventoId,
                                entregado = true,
                                timestampEscaneo = estado.timestampEscaneo,
                                sincronizadoConBackend = true,
                                pendienteSubida = false,
                                scannedByStaffId = estado.staffId,
                                scannedByStaffNombre = estado.staffNombre,
                                scannedByDeviceId = estado.deviceId,
                                syncError = null,
                                conflicto = local?.conflicto == true,
                                conflictoStaffOriginal = local?.conflictoStaffOriginal,
                                conflictoFechaOriginal = local?.conflictoFechaOriginal,
                                conflictoMensaje = local?.conflictoMensaje
                            )
                        )
                    } else {
                        escaneosBorrar.add(dto.idParticipante to estado.beneficioId)
                    }
                }
            }

            dao.aplicarPaginaDescarga(
                beneficios = beneficiosEntities,
                participantesActivos = participantesActivos,
                idsEliminados = idsEliminados,
                escaneosUpsert = escaneosUpsert,
                escaneosBorrar = escaneosBorrar
            )

            totalProcesados += response.participantes.size

            if (!response.hasMore) break
            page++
        }

        // Guardar cursor del servidor SOLO cuando todas las páginas terminaron con éxito
        if (cursorServidor != null) {
            dao.upsertSyncMeta(SyncMetaEntity(keyDelta(eventoId), cursorServidor.toString()))
        }
        return totalProcesados
    }

    private fun keyDelta(eventoId: String): String = "${KEY_ULTIMO_DELTA_PREFIX}_$eventoId"

    private suspend fun ultimoTimestampDelta(eventoId: String): Long? =
        dao.obtenerMeta(keyDelta(eventoId))?.toLongOrNull()

    // ─────────────────────────────────────────────────────────────────────────
    // WorkManager
    // ─────────────────────────────────────────────────────────────────────────

    fun programarSyncUnica() {
        uploadScheduler?.let {
            it()
            return
        }
        val ctx = context ?: return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(SyncWorker.KEY_MODE to SyncWorker.MODE_UPLOAD))
            .addTag(SyncWorker.TAG_SYNC_UNICA)
            .build()

        WorkManager.getInstance(ctx)
            .enqueueUniqueWork(
                SyncWorker.TAG_SYNC_UNICA,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }

    fun programarSyncPeriodico() {
        val ctx = context ?: return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(workDataOf(SyncWorker.KEY_MODE to SyncWorker.MODE_FULL))
            .addTag(SyncWorker.TAG_SYNC_PERIODICA)
            .build()

        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(
                SyncWorker.TAG_SYNC_PERIODICA,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────────────────

    fun hayConexion(): Boolean {
        connectivityChecker?.let { return it() }
        val ctx = context ?: return false
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun BeneficioActivoDto.toEntity(eventoIdFallback: String): BeneficioEntity =
        BeneficioEntity(
            id = id,
            nombre = nombre,
            orden = orden,
            estado = estado,
            eventoId = eventoId.ifBlank { eventoIdFallback },
            eventoNombre = eventoNombre,
            descripcion = descripcion
        )

    companion object {
        private const val KEY_ULTIMO_DELTA_PREFIX = "ultimo_delta_timestamp"
        private const val MAX_ESCANEOS_POR_LOTE = 200
        private const val MAX_VUELTAS_UPLOAD = 20
        private const val PAGE_SIZE_DOWNLOAD = 500
        private const val MAX_PAGINAS_DOWNLOAD = 50
    }
}
