package com.example.ozareventscanner

import androidx.paging.PagingSource
import com.example.ozareventscanner.data.BeneficioConteo
import com.example.ozareventscanner.data.BeneficioEntity
import com.example.ozareventscanner.data.EscaneoEntity
import com.example.ozareventscanner.data.ParticipanteConEscaneosRelation
import com.example.ozareventscanner.data.ParticipanteDao
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.ScanResultado
import com.example.ozareventscanner.data.SyncMetaEntity
import com.example.ozareventscanner.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.UUID

/**
 * Pruebas unitarias de Fase A (Integridad de datos, Offline-first, Aislamiento por sesión/evento y Sync).
 */
class ScannerLogicQaTest {

    private class FakeParticipanteDao : ParticipanteDao {
        val participantesDb = mutableMapOf<String, ParticipanteEntity>()
        val beneficiosDb = mutableMapOf<String, BeneficioEntity>()
        val escaneosDb = mutableMapOf<Pair<String, String>, EscaneoEntity>()
        val metaDb = mutableMapOf<String, String>()

        override suspend fun insertarParticipantes(participantes: List<ParticipanteEntity>) {
            participantes.forEach { participantesDb[it.idParticipante] = it }
        }

        override suspend fun insertarBeneficios(beneficios: List<BeneficioEntity>) {
            beneficios.forEach { beneficiosDb[it.id] = it }
        }

        override suspend fun upsertEscaneos(escaneos: List<EscaneoEntity>) {
            escaneos.forEach { escaneosDb[it.participanteId to it.beneficioId] = it }
        }

        override suspend fun upsertEscaneo(escaneo: EscaneoEntity) {
            escaneosDb[escaneo.participanteId to escaneo.beneficioId] = escaneo
        }

        override suspend fun upsertSyncMeta(meta: SyncMetaEntity) {
            metaDb[meta.clave] = meta.valor
        }

        override suspend fun buscarParticipante(id: String): ParticipanteEntity? {
            val limpio = id.trim()
            return participantesDb.values.find {
                it.idParticipante == limpio ||
                    (it.documento != null && it.documento == limpio) ||
                    (it.correo != null && it.correo.equals(limpio, ignoreCase = true))
            }
        }

        override suspend fun obtenerTodos(): List<ParticipanteEntity> =
            participantesDb.values.sortedBy { it.nombre }

        override fun pagingTodos(): PagingSource<Int, ParticipanteConEscaneosRelation> =
            throw UnsupportedOperationException("No requerido en unit test sin Room")

        override fun pagingBuscarPorPrefijo(prefix: String): PagingSource<Int, ParticipanteConEscaneosRelation> =
            throw UnsupportedOperationException("No requerido en unit test sin Room")

        override fun observarConteoPorBeneficio(): Flow<List<BeneficioConteo>> =
            flowOf(
                escaneosDb.values
                    .filter { it.entregado && it.syncError == null }
                    .groupBy { it.beneficioId }
                    .map { (id, list) -> BeneficioConteo(id, list.size) }
            )

        override fun observarTotalParticipantesEscaneados(): Flow<Int> =
            flowOf(
                escaneosDb.values
                    .filter { it.entregado && it.syncError == null }
                    .map { it.participanteId }
                    .distinct()
                    .size
            )

        override fun observarEscaneosEntregados(): Flow<List<EscaneoEntity>> =
            flowOf(escaneosDb.values.filter { it.entregado && it.syncError == null })

        override suspend fun contarTotalParticipantes(): Int = participantesDb.size

        override fun observarTotalParticipantes(): Flow<Int> = flowOf(participantesDb.size)

        override suspend fun buscarPorNombreOId(query: String): List<ParticipanteEntity> {
            val q = query.trim().lowercase()
            return participantesDb.values.filter {
                it.nombre.lowercase().contains(q) ||
                    it.idParticipante.lowercase().contains(q) ||
                    (it.documento?.lowercase()?.contains(q) == true) ||
                    (it.correo?.lowercase()?.contains(q) == true)
            }.sortedBy { it.nombre }
        }

        override suspend fun obtenerTodosEscaneos(): List<EscaneoEntity> = escaneosDb.values.toList()

        override fun observarEscaneos(): Flow<List<EscaneoEntity>> = flowOf(escaneosDb.values.toList())

        override suspend fun obtenerEscaneosDeParticipante(participanteId: String): List<EscaneoEntity> =
            escaneosDb.values.filter { it.participanteId == participanteId }

        override suspend fun obtenerEscaneosDeParticipantes(participanteIds: List<String>): List<EscaneoEntity> {
            val idSet = participanteIds.toSet()
            return escaneosDb.values.filter { it.participanteId in idSet }
        }

        override suspend fun obtenerEscaneo(participanteId: String, beneficioId: String): EscaneoEntity? =
            escaneosDb[participanteId to beneficioId]

        override suspend fun obtenerPendientesSync(): List<EscaneoEntity> =
            escaneosDb.values.filter { it.pendienteSubida && it.syncError == null }
                .sortedBy { it.timestampEscaneo ?: 0L }

        override suspend fun obtenerPendientesSyncDeSesion(
            eventoId: String,
            staffId: String?,
            limit: Int
        ): List<EscaneoEntity> = escaneosDb.values
            .filter {
                it.pendienteSubida &&
                    it.syncError == null &&
                    (it.eventoId == eventoId || it.eventoId.isEmpty()) &&
                    (staffId == null || it.scannedByStaffId == null || it.scannedByStaffId == staffId)
            }
            .sortedBy { it.timestampEscaneo ?: 0L }
            .take(limit)

        override suspend fun contarPendientesSyncDeSesion(eventoId: String, staffId: String?): Int =
            obtenerPendientesSyncDeSesion(eventoId, staffId, Int.MAX_VALUE).size

        override fun observarPendientesSync(): Flow<Int> =
            flowOf(escaneosDb.values.count { it.pendienteSubida && it.syncError == null })

        override fun observarPendientesSyncDeSesion(eventoId: String, staffId: String?): Flow<Int> =
            flowOf(escaneosDb.values.count {
                it.pendienteSubida &&
                    it.syncError == null &&
                    (it.eventoId == eventoId || it.eventoId.isEmpty()) &&
                    (staffId == null || it.scannedByStaffId == null || it.scannedByStaffId == staffId)
            })

        override fun observarPendientesOtrosUsuarios(eventoId: String, staffId: String?): Flow<Int> =
            flowOf(escaneosDb.values.count {
                it.pendienteSubida &&
                    it.syncError == null &&
                    ((it.eventoId.isNotEmpty() && it.eventoId != eventoId) ||
                        (staffId != null && it.scannedByStaffId != null && it.scannedByStaffId != staffId))
            })

        override fun observarConflictosCount(eventoId: String): Flow<Int> =
            flowOf(escaneosDb.values.count { it.conflicto && (it.eventoId == eventoId || it.eventoId.isEmpty()) })

        override suspend fun obtenerConflictos(eventoId: String): List<EscaneoEntity> =
            escaneosDb.values.filter { it.conflicto && (it.eventoId == eventoId || it.eventoId.isEmpty()) }

        override suspend fun marcarEscaneoSincronizado(
            participanteId: String,
            beneficioId: String,
            timestamp: Long?
        ) {
            val key = participanteId to beneficioId
            val current = escaneosDb[key] ?: return
            escaneosDb[key] = current.copy(
                sincronizadoConBackend = true,
                pendienteSubida = false,
                syncError = null,
                conflicto = false,
                timestampEscaneo = timestamp
            )
        }

        override suspend fun marcarEscaneoConflictoYaEntregado(
            participanteId: String,
            beneficioId: String,
            timestampOriginal: Long?,
            staffOriginalNombre: String?,
            mensaje: String?
        ) {
            val key = participanteId to beneficioId
            val current = escaneosDb[key] ?: return
            escaneosDb[key] = current.copy(
                entregado = true,
                sincronizadoConBackend = true,
                pendienteSubida = false,
                syncError = null,
                timestampEscaneo = timestampOriginal ?: current.timestampEscaneo,
                scannedByStaffNombre = staffOriginalNombre ?: current.scannedByStaffNombre,
                conflicto = true,
                conflictoStaffOriginal = staffOriginalNombre,
                conflictoFechaOriginal = timestampOriginal,
                conflictoMensaje = mensaje
            )
        }

        override suspend fun marcarEscaneoConError(
            participanteId: String,
            beneficioId: String,
            error: String
        ) {
            val key = participanteId to beneficioId
            val current = escaneosDb[key] ?: return
            escaneosDb[key] = current.copy(
                pendienteSubida = false,
                sincronizadoConBackend = false,
                syncError = error
            )
        }

        override suspend fun borrarEscaneoConfirmado(participanteId: String, beneficioId: String) {
            val key = participanteId to beneficioId
            val current = escaneosDb[key] ?: return
            if (!current.pendienteSubida && current.sincronizadoConBackend) {
                escaneosDb.remove(key)
            }
        }

        override suspend fun limpiarParticipantesSincronizadosDeOtrosEventos(eventoIdActual: String) {
            val idsConPendientes = escaneosDb.values.filter { it.pendienteSubida }.map { it.participanteId }.toSet()
            participantesDb.entries.removeIf { (_, p) ->
                p.eventoId != eventoIdActual && p.idParticipante !in idsConPendientes
            }
        }

        override suspend fun limpiarEscaneosSincronizadosDeOtrosEventos(eventoIdActual: String) {
            escaneosDb.entries.removeIf { (_, e) -> e.eventoId != eventoIdActual && !e.pendienteSubida }
        }

        override suspend fun limpiarBeneficiosDeOtrosEventos(eventoIdActual: String) {
            beneficiosDb.entries.removeIf { (_, b) -> b.eventoId != null && b.eventoId != eventoIdActual }
        }

        override suspend fun eliminarParticipantesSinPendientes(ids: List<String>) {
            val idsConPendientes = escaneosDb.values.filter { it.pendienteSubida }.map { it.participanteId }.toSet()
            val target = ids.toSet()
            participantesDb.entries.removeIf { (id, _) -> id in target && id !in idsConPendientes }
        }

        override suspend fun obtenerBeneficios(): List<BeneficioEntity> =
            beneficiosDb.values.sortedBy { it.orden }

        override suspend fun obtenerBeneficioPorId(id: String): BeneficioEntity? = beneficiosDb[id]

        override suspend fun obtenerBeneficiosActivos(): List<BeneficioEntity> =
            beneficiosDb.values.filter { it.estado }.sortedBy { it.orden }

        override fun observarBeneficiosActivos(): Flow<List<BeneficioEntity>> =
            flowOf(beneficiosDb.values.filter { it.estado }.sortedBy { it.orden })

        override suspend fun obtenerMeta(clave: String): String? = metaDb[clave]
    }

    private class FakeApiService : ApiService {
        var uploadHandler: ((SyncUploadRequest) -> SyncUploadResponse)? = null
        var downloadHandler: ((String, Long?, Int?, Int?) -> SyncDownloadResponse)? = null
        var downloadResponse: SyncDownloadResponse = SyncDownloadResponse(
            participantes = emptyList(),
            totalEscaneados = 0,
            timestampServidor = 1_700_000_000_000L
        )

        override suspend fun loginStaff(request: StaffLoginRequest): StaffLoginResponse =
            throw UnsupportedOperationException()

        override suspend fun obtenerBeneficiosActivos(eventoId: String): List<BeneficioActivoDto> = emptyList()

        override suspend fun registrarEscaneo(request: RegistrarEscaneoRequest): RegistrarEscaneoResponse =
            throw UnsupportedOperationException()

        override suspend fun subirEscaneos(request: SyncUploadRequest): SyncUploadResponse {
            return uploadHandler?.invoke(request)
                ?: SyncUploadResponse(
                    confirmados = request.escaneos.size,
                    rechazados = 0,
                    resultados = request.escaneos.map {
                        EscaneoConfirmado(
                            idParticipante = it.idParticipante,
                            beneficioId = it.beneficioId,
                            validado = true,
                            estado = EscaneoConfirmado.ESTADO_CONFIRMADO,
                            timestampValidado = it.timestampEscaneo
                        )
                    }
                )
        }

        override suspend fun descargarEstado(
            eventoId: String,
            since: Long?,
            page: Int?,
            size: Int?
        ): SyncDownloadResponse = downloadHandler?.invoke(eventoId, since, page, size) ?: downloadResponse
    }

    private lateinit var fakeDao: FakeParticipanteDao
    private lateinit var fakeApi: FakeApiService
    private lateinit var repository: ParticipanteRepository

    private var online = true
    private var uploadScheduledCount = 0
    private var currentSession = ParticipanteRepository.SessionInfo(
        eventoId = "evt-1",
        staffId = "staff-A",
        staffNombre = "Luis Mendoza"
    )

    private val partId1 = UUID.randomUUID().toString()
    private val benId1 = UUID.randomUUID().toString()
    private val benId2 = UUID.randomUUID().toString()

    @Before
    fun setUp() {
        fakeDao = FakeParticipanteDao()
        fakeApi = FakeApiService()
        online = true
        uploadScheduledCount = 0
        currentSession = ParticipanteRepository.SessionInfo("evt-1", "staff-A", "Luis Mendoza")

        runBlocking {
            fakeDao.insertarParticipantes(
                listOf(
                    ParticipanteEntity(
                        idParticipante = partId1,
                        nombre = "María Camila Pérez",
                        documento = "10482914",
                        correo = "maria.perez@zendov.com",
                        eventoId = "evt-1"
                    )
                )
            )
            fakeDao.insertarBeneficios(
                listOf(
                    BeneficioEntity(benId1, "Kit de bienvenida", 1, true, "evt-1"),
                    BeneficioEntity(benId2, "Almuerzo ejecutivo", 2, true, "evt-1")
                )
            )
        }

        repository = ParticipanteRepository(
            dao = fakeDao,
            apiService = fakeApi,
            context = null,
            sessionProvider = { currentSession },
            deviceIdProvider = { "uuid-device-001" },
            connectivityChecker = { online },
            uploadScheduler = { uploadScheduledCount++ }
        )
    }

    @Test
    fun qrFormatoEstricto_soloAceptaUuid() {
        assertTrue(repository.esFormatoQrValido(partId1))
        assertFalse(repository.esFormatoQrValido("10482914"))
        assertFalse(repository.esFormatoQrValido("maria.perez@zendov.com"))
        assertFalse(repository.esFormatoQrValido("   "))
    }

    @Test
    fun registrarEscaneo_offlinePersisteConEventoStaffYDeviceIdYProgramaUpload() = runBlocking {
        online = false
        val res = repository.registrarEscaneo(partId1, benId1)

        assertTrue(res is ScanResultado.Valido)
        assertEquals(1, uploadScheduledCount)

        val guardado = fakeDao.obtenerEscaneo(partId1, benId1)
        assertNotNull(guardado)
        assertEquals("evt-1", guardado!!.eventoId)
        assertEquals("staff-A", guardado.scannedByStaffId)
        assertEquals("uuid-device-001", guardado.scannedByDeviceId)
        assertTrue(guardado.pendienteSubida)
        assertFalse(guardado.sincronizadoConBackend)

        // Segundo escaneo del mismo beneficio devuelve YaEscaneado
        val res2 = repository.registrarEscaneo(partId1, benId1)
        assertTrue(res2 is ScanResultado.YaEscaneado)
    }

    @Test
    fun subirSoloPendientes_sinConexionFallaYMantienePendiente() = runBlocking {
        repository.registrarEscaneo(partId1, benId1)
        online = false

        val result = repository.subirSoloPendientes()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)

        val guardado = fakeDao.obtenerEscaneo(partId1, benId1)!!
        assertTrue(guardado.pendienteSubida)
        assertFalse(guardado.sincronizadoConBackend)
    }

    @Test
    fun subirSoloPendientes_soloSubeEscaneosDeLaSesionActiva() = runBlocking {
        // Staff A escanea benId1 en evt-1
        repository.registrarEscaneo(partId1, benId1)

        // Cambia de usuario a Staff B en evt-2
        currentSession = ParticipanteRepository.SessionInfo("evt-2", "staff-B", "Ana Torres")
        var llamadosUpload = 0
        fakeApi.uploadHandler = { req ->
            llamadosUpload++
            SyncUploadResponse(0, 0, emptyList())
        }

        val res = repository.subirSoloPendientes()
        assertTrue(res.isSuccess)
        assertEquals(0, llamadosUpload)
        // El escaneo de Staff A sigue intacto y pendiente en Room
        assertTrue(fakeDao.obtenerEscaneo(partId1, benId1)!!.pendienteSubida)
    }

    @Test
    fun subirSoloPendientes_emparejaPorBeneficioIdYMarcaConflictoYaEntregado() = runBlocking {
        repository.registrarEscaneo(partId1, benId1)
        repository.registrarEscaneo(partId1, benId2)

        val tsOriginal = 1_700_000_000_000L
        fakeApi.uploadHandler = { req ->
            assertEquals(2, req.escaneos.size)
            SyncUploadResponse(
                confirmados = 1,
                rechazados = 1,
                resultados = listOf(
                    EscaneoConfirmado(
                        idParticipante = partId1,
                        beneficioId = benId1,
                        validado = false,
                        estado = EscaneoConfirmado.ESTADO_YA_ENTREGADO,
                        timestampValidado = tsOriginal,
                        staffOriginalNombre = "Operador Previo",
                        mensaje = "Ya entregado previamente"
                    ),
                    EscaneoConfirmado(
                        idParticipante = partId1,
                        beneficioId = benId2,
                        validado = true,
                        estado = EscaneoConfirmado.ESTADO_CONFIRMADO,
                        timestampValidado = 1_700_000_500_000L
                    )
                )
            )
        }

        val result = repository.subirSoloPendientes()
        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(1, stats.subidos)
        assertEquals(1, stats.conflictos)
        assertEquals(0, stats.rechazados)

        val esc1 = fakeDao.obtenerEscaneo(partId1, benId1)!!
        assertTrue(esc1.entregado)
        assertTrue(esc1.sincronizadoConBackend)
        assertFalse(esc1.pendienteSubida)
        assertNull(esc1.syncError)
        assertTrue(esc1.conflicto)
        assertEquals("Operador Previo", esc1.conflictoStaffOriginal)
        assertEquals(tsOriginal, esc1.conflictoFechaOriginal)

        val esc2 = fakeDao.obtenerEscaneo(partId1, benId2)!!
        assertTrue(esc2.sincronizadoConBackend)
        assertFalse(esc2.pendienteSubida)
        assertFalse(esc2.conflicto)
    }

    @Test
    fun subirSoloPendientes_resultadoOmitidoPorServidorEsReintentableYNoBloquea() = runBlocking {
        repository.registrarEscaneo(partId1, benId1)
        repository.registrarEscaneo(partId1, benId2)

        // El servidor solo devuelve resultado para benId1 y omite benId2
        fakeApi.uploadHandler = {
            SyncUploadResponse(
                confirmados = 1,
                rechazados = 0,
                resultados = listOf(
                    EscaneoConfirmado(
                        idParticipante = partId1,
                        beneficioId = benId1,
                        validado = true,
                        estado = EscaneoConfirmado.ESTADO_CONFIRMADO,
                        timestampValidado = System.currentTimeMillis()
                    )
                )
            )
        }

        val result = repository.subirSoloPendientes()
        assertTrue(result.isFailure)

        val esc1 = fakeDao.obtenerEscaneo(partId1, benId1)!!
        assertTrue(esc1.sincronizadoConBackend)
        assertFalse(esc1.pendienteSubida)

        val esc2 = fakeDao.obtenerEscaneo(partId1, benId2)!!
        assertTrue("El escaneo omitido debe seguir pendiente para reintentarse", esc2.pendienteSubida)
        assertNull("No debe marcarse con syncError permanente", esc2.syncError)
    }

    @Test
    fun inicializarEvento_limpiaSincronizadosDeOtroEventoPeroConservaPendientesAjenos() = runBlocking {
        // Escaneo pendiente de staff-A en evt-1
        repository.registrarEscaneo(partId1, benId1)
        // Otro participante ya sincronizado en evt-1
        val partSyncId = UUID.randomUUID().toString()
        fakeDao.insertarParticipantes(
            listOf(ParticipanteEntity(partSyncId, "Sync User", eventoId = "evt-1"))
        )
        fakeDao.upsertEscaneo(
            EscaneoEntity(
                participanteId = partSyncId,
                beneficioId = benId1,
                eventoId = "evt-1",
                entregado = true,
                sincronizadoConBackend = true,
                pendienteSubida = false
            )
        )

        // Entra staff-B en evt-2
        currentSession = ParticipanteRepository.SessionInfo("evt-2", "staff-B", "Ana Torres")
        repository.inicializarEvento("evt-2")

        // El participante sincronizado de evt-1 fue borrado
        assertNull(fakeDao.buscarParticipante(partSyncId))
        assertNull(fakeDao.obtenerEscaneo(partSyncId, benId1))
        // El participante y escaneo pendiente de staff-A en evt-1 se conservan
        assertNotNull(fakeDao.buscarParticipante(partId1))
        assertNotNull(fakeDao.obtenerEscaneo(partId1, benId1))
    }

    @Test
    fun descargarSoloCambios_paginadoGuardaCursorDePagina0YAplicaBeneficiosEstadoSinPisarPendientes() = runBlocking {
        // Escaneo local pendiente para benId1
        repository.registrarEscaneo(partId1, benId1)

        val partId2 = UUID.randomUUID().toString()
        val partEliminado = UUID.randomUUID().toString()
        fakeDao.insertarParticipantes(
            listOf(ParticipanteEntity(partEliminado, "Por Eliminar", eventoId = "evt-1"))
        )

        val paginasLlamadas = mutableListOf<Pair<Long?, Int?>>()
        fakeApi.downloadHandler = { _, since, page, _ ->
            paginasLlamadas.add(since to page)
            when (page) {
                0 -> SyncDownloadResponse(
                    participantes = listOf(
                        ParticipanteSyncDto(
                            idParticipante = partId1,
                            nombre = "María Camila Pérez",
                            beneficiosEstado = listOf(
                                // El servidor aún dice entregado=false para benId1, pero está pendiente local -> NO pisar
                                BeneficioEstadoDto(beneficioId = benId1, entregado = false),
                                // Otro dispositivo entregó benId2 -> debe guardarse en Room
                                BeneficioEstadoDto(
                                    beneficioId = benId2,
                                    entregado = true,
                                    timestampEscaneo = 1_700_000_123_000L,
                                    staffNombre = "Otro Staff"
                                )
                            )
                        )
                    ),
                    totalEscaneados = 1,
                    timestampServidor = 1_700_000_500_000L,
                    page = 0,
                    size = 500,
                    hasMore = true
                )
                else -> SyncDownloadResponse(
                    participantes = listOf(
                        ParticipanteSyncDto(idParticipante = partId2, nombre = "Nuevo Asistente"),
                        ParticipanteSyncDto(idParticipante = partEliminado, nombre = "Por Eliminar", eliminado = true)
                    ),
                    totalEscaneados = 0,
                    timestampServidor = 1_700_000_505_000L,
                    page = 1,
                    size = 500,
                    hasMore = false
                )
            }
        }

        val res = repository.descargarSoloCambios("evt-1")
        assertTrue(res.isSuccess)
        assertEquals(3, res.getOrThrow())
        assertEquals(listOf(null to 0, null to 1), paginasLlamadas)

        // Cursor guardado es el de page 0 (inicio de la consulta del servidor)
        assertEquals("1700000500000", fakeDao.obtenerMeta("ultimo_delta_timestamp_evt-1"))

        // benId1 sigue pendiente local (no fue borrado ni pisado)
        val esc1 = fakeDao.obtenerEscaneo(partId1, benId1)!!
        assertTrue(esc1.pendienteSubida)
        assertTrue(esc1.entregado)

        // benId2 entregado por otro dispositivo ahora existe en Room
        val esc2 = fakeDao.obtenerEscaneo(partId1, benId2)!!
        assertTrue(esc2.entregado)
        assertTrue(esc2.sincronizadoConBackend)
        assertEquals("Otro Staff", esc2.scannedByStaffNombre)

        // Tombstone eliminado y nuevo asistente insertado
        assertNotNull(fakeDao.buscarParticipante(partId2))
        assertNull(fakeDao.buscarParticipante(partEliminado))

        // Siguiente llamada usa el cursor guardado como since
        paginasLlamadas.clear()
        fakeApi.downloadHandler = { _, since, page, _ ->
            paginasLlamadas.add(since to page)
            SyncDownloadResponse(emptyList(), 0, 1_700_000_600_000L, delta = true, hasMore = false)
        }
        val resDelta = repository.descargarSoloCambios("evt-1")
        assertTrue(resDelta.isSuccess)
        assertEquals(listOf(1_700_000_500_000L to 0), paginasLlamadas)
        assertEquals("1700000600000", fakeDao.obtenerMeta("ultimo_delta_timestamp_evt-1"))
    }
}
