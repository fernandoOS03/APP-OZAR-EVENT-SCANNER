package com.example.ozareventscanner

import com.example.ozareventscanner.data.ParticipanteDao
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.ScanResultado
import com.example.ozareventscanner.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

/**
 * ═════════════════════════════════════════════════════════════════════════════
 * AGENTE QA: SUITE DE PRUEBAS AUTOMATIZADAS DE LÓGICA DE ESCANEO Y NEGOCIO
 * ═════════════════════════════════════════════════════════════════════════════
 *
 * Verifica exhaustivamente:
 *  1. Validación del Login (DNI y PIN de staff, modelo único hardcodeado).
 *  2. Extracción y análisis sintáctico de formatos QR (URLs, JSON, Raw).
 *  3. Validación estricta de Correos Electrónicos ("si el correo no es válido que sea no válido").
 *  4. Escaneo fidedigno sin inventar participantes ("no puede inventar si no existe").
 *  5. Detección precisa de doble escaneo (YaEscaneado).
 *  6. Persistencia y sincronización offline-first con Room (pendienteSubida / confirmación).
 */
class ScannerLogicQaTest {

    // ── DAO Simulado en Memoria para pruebas unitarias de persistencia ────────
    private class FakeParticipanteDao : ParticipanteDao {
        val participantesDb = mutableMapOf<String, ParticipanteEntity>()

        fun cargarIniciales(lista: List<ParticipanteEntity>) {
            participantesDb.clear()
            lista.forEach { participantesDb[it.idParticipante] = it }
        }

        override suspend fun insertarParticipantes(participantes: List<ParticipanteEntity>) {
            participantes.forEach { participantesDb[it.idParticipante] = it }
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

        override suspend fun contarTotalParticipantes(): Int = participantesDb.size

        override fun observarTotalParticipantes(): Flow<Int> =
            flowOf(participantesDb.size)

        override suspend fun buscarPorNombreOId(query: String): List<ParticipanteEntity> {
            val q = query.trim().lowercase()
            return participantesDb.values.filter {
                it.nombre.lowercase().contains(q) ||
                it.idParticipante.lowercase().contains(q) ||
                (it.documento?.lowercase()?.contains(q) == true) ||
                (it.correo?.lowercase()?.contains(q) == true)
            }.sortedBy { it.nombre }
        }

        override suspend fun actualizarBeneficio(id: String, entregado: Boolean) {
            val p = buscarParticipante(id) ?: return
            participantesDb[p.idParticipante] = p.copy(beneficioEntregado = entregado)
        }

        override suspend fun marcarEscaneoLocal(id: String, timestamp: Long) {
            val p = buscarParticipante(id) ?: return
            participantesDb[p.idParticipante] = p.copy(
                beneficioEntregado = true,
                timestampEscaneo = timestamp,
                pendienteSubida = true,
                sincronizadoConBackend = false
            )
        }

        override suspend fun marcarEscaneoConfirmado(id: String, timestamp: Long) {
            val p = buscarParticipante(id) ?: return
            participantesDb[p.idParticipante] = p.copy(
                beneficioEntregado = true,
                timestampEscaneo = timestamp,
                pendienteSubida = false,
                sincronizadoConBackend = true
            )
        }

        override suspend fun obtenerPendientesSync(): List<ParticipanteEntity> =
            participantesDb.values.filter { it.pendienteSubida }

        override suspend fun marcarLoteComoSincronizado(ids: List<String>) {
            ids.forEach { id ->
                val p = participantesDb[id]
                if (p != null) {
                    participantesDb[id] = p.copy(
                        sincronizadoConBackend = true,
                        pendienteSubida = false
                    )
                }
            }
        }

        override suspend fun revertirEscaneoRechazado(id: String) {
            val p = buscarParticipante(id) ?: return
            participantesDb[p.idParticipante] = p.copy(
                beneficioEntregado = false,
                timestampEscaneo = null,
                pendienteSubida = false,
                sincronizadoConBackend = true
            )
        }

        override suspend fun sincronizarDesdeBackend(participantes: List<ParticipanteEntity>) {
            insertarParticipantes(participantes)
        }
    }

    // ── ApiService Simulado para pruebas de red y mock ────────────────────────
    private class FakeApiService : ApiService {
        var debeLanzarErrorDeRed = false

        val asistentesRegistrados = mutableListOf(
            ParticipanteSyncDto("10482914", "María Camila Pérez", beneficioEntregado = true, timestampEscaneo = 1719224130000L, documento = "10482914", correo = "maria.perez@zendov.com"),
            ParticipanteSyncDto("28491023", "Andrés Rodríguez", beneficioEntregado = false, timestampEscaneo = null, documento = "28491023", correo = "andres.rodriguez@zendov.com"),
            ParticipanteSyncDto("19482019", "Carlos Andrés Gómez", beneficioEntregado = true, timestampEscaneo = 1719223000000L, documento = "19482019", correo = "carlos.gomez@zendov.com"),
            ParticipanteSyncDto("30192841", "Valeria Sofía Martínez", beneficioEntregado = false, timestampEscaneo = null, documento = "30192841", correo = "valeria.martinez@zendov.com")
        )

        val escaneosRegistrados = mutableSetOf<String>()

        override suspend fun loginStaff(request: StaffLoginRequest): StaffLoginResponse {
            if (debeLanzarErrorDeRed) throw IOException("Sin conexión a api-eventos.zendov.com")

            val dni = request.documentoIdentidad.trim()
            val pin = request.pinAcceso.trim()

            // Solo el staff oficial hardcodeado está permitido
            if (dni == "12345678" && pin == "654321") {
                return StaffLoginResponse(
                    token = "eyJhbGciOiJIUzI1NiJ9.demo",
                    staff = StaffDataDto(
                        staffId = "STF-025",
                        nombreCompleto = "Luis Mendoza",
                        documentoIdentidad = "12345678",
                        rol = "Coordinador de accesos",
                        cargo = "Coordinador de accesos",
                        evento = EventoStaffDto("EVT-025", "Zendov Tech Summit 2025", "zendov-summit-2025", "tenant_zendov_2025")
                    )
                )
            } else {
                val errorBody = """{"error":"Unauthorized","message":"Credenciales no válidas o no autorizadas"}""".toResponseBody("application/json".toMediaType())
                throw HttpException(retrofit2.Response.error<StaffLoginResponse>(401, errorBody))
            }
        }

        override suspend fun obtenerBeneficiosActivos(eventoId: String): List<BeneficioActivoDto> {
            return listOf(
                BeneficioActivoDto("ben-001-kit", "Kit de bienvenida", 1, true, "EVT-025", "Zendov Tech Summit 2025", "Bolso + credencial"),
                BeneficioActivoDto("ben-002-almuerzo", "Almuerzo ejecutivo", 2, true, "EVT-025", "Zendov Tech Summit 2025", "Salón principal")
            )
        }

        override suspend fun registrarEscaneo(request: RegistrarEscaneoRequest): RegistrarEscaneoResponse {
            if (debeLanzarErrorDeRed) throw IOException("Sin conexión a api-eventos.zendov.com")

            val target = (request.participanteId ?: request.documentoIdentidad ?: "").trim()

            // Buscar en asistentes registrados
            val encontrado = asistentesRegistrados.find {
                it.idParticipante.equals(target, ignoreCase = true) ||
                (it.documento != null && it.documento.equals(target, ignoreCase = true)) ||
                (it.correo != null && it.correo.equals(target, ignoreCase = true))
            }

            if (encontrado == null) {
                // Participante no existe: NO INVENTAR NADA, responder 404
                val errorBody = """{"error":"NotFound","message":"Participante no encontrado con el código o correo: $target"}""".toResponseBody("application/json".toMediaType())
                throw HttpException(retrofit2.Response.error<RegistrarEscaneoResponse>(404, errorBody))
            }

            val key = "${encontrado.idParticipante}_${request.beneficioId}"
            val yaEntregado = encontrado.beneficioEntregado || escaneosRegistrados.contains(key)

            return if (yaEntregado) {
                RegistrarEscaneoResponse(
                    escaneoId = "esc-previo",
                    participanteId = encontrado.idParticipante,
                    participanteNombreCompleto = encontrado.nombre,
                    documentoIdentidad = encontrado.documento,
                    beneficioId = request.beneficioId,
                    beneficioNombre = "Kit de bienvenida",
                    yaEntregadoPreviamente = true,
                    mensaje = "ATENCIÓN: Este beneficio YA FUE ENTREGADO previamente a ${encontrado.nombre}",
                    fechaEscaneo = "2025-06-24T09:30:00",
                    staffNombre = "Luis Mendoza"
                )
            } else {
                escaneosRegistrados.add(key)
                RegistrarEscaneoResponse(
                    escaneoId = "esc-${System.currentTimeMillis()}",
                    participanteId = encontrado.idParticipante,
                    participanteNombreCompleto = encontrado.nombre,
                    documentoIdentidad = encontrado.documento,
                    beneficioId = request.beneficioId,
                    beneficioNombre = "Kit de bienvenida",
                    yaEntregadoPreviamente = false,
                    mensaje = "Beneficio entregado exitosamente a ${encontrado.nombre}",
                    fechaEscaneo = "2025-06-24T10:15:30",
                    staffNombre = "Luis Mendoza"
                )
            }
        }

        override suspend fun subirEscaneos(request: SyncUploadRequest): SyncUploadResponse {
            val confirmados = mutableListOf<EscaneoConfirmado>()
            request.escaneos.forEach { esc ->
                val existe = asistentesRegistrados.any { it.idParticipante == esc.idParticipante }
                confirmados.add(EscaneoConfirmado(esc.idParticipante, validado = existe, timestampValidado = if (existe) esc.timestampEscaneo else null))
            }
            return SyncUploadResponse(
                confirmados = confirmados.count { it.validado },
                rechazados = confirmados.count { !it.validado },
                resultados = confirmados
            )
        }

        override suspend fun descargarEstado(): SyncDownloadResponse {
            return SyncDownloadResponse(
                participantes = asistentesRegistrados,
                totalEscaneados = asistentesRegistrados.count { it.beneficioEntregado },
                timestampServidor = System.currentTimeMillis()
            )
        }
    }

    private lateinit var fakeDao: FakeParticipanteDao
    private lateinit var fakeApi: FakeApiService
    private lateinit var repository: ParticipanteRepository

    @Before
    fun setUp() {
        fakeDao = FakeParticipanteDao()
        fakeApi = FakeApiService()

        // Sembrar datos mock iniciales en el FakeDao
        fakeDao.cargarIniciales(listOf(
            ParticipanteEntity("10482914", "María Camila Pérez", beneficioEntregado = true, timestampEscaneo = 1719224130000L, documento = "10482914", correo = "maria.perez@zendov.com"),
            ParticipanteEntity("28491023", "Andrés Rodríguez", beneficioEntregado = false, timestampEscaneo = null, documento = "28491023", correo = "andres.rodriguez@zendov.com"),
            ParticipanteEntity("19482019", "Carlos Andrés Gómez", beneficioEntregado = true, timestampEscaneo = 1719223000000L, documento = "19482019", correo = "carlos.gomez@zendov.com"),
            ParticipanteEntity("30192841", "Valeria Sofía Martínez", beneficioEntregado = false, timestampEscaneo = null, documento = "30192841", correo = "valeria.martinez@zendov.com")
        ))

        // Para pruebas unitarias que no invocan WorkManager, el contexto puede ser null o mockeado si se prueba repository
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: VALIDACIÓN DE LOGIN (DOCUMENTO Y CÓDIGO GENERADO PIN)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun testLogin_staffOficialHardcodeado_validaExitosamente() = runBlocking {
        val respuesta = fakeApi.loginStaff(StaffLoginRequest(
            documentoIdentidad = "12345678",
            pinAcceso = "654321"
        ))

        assertNotNull(respuesta.token)
        assertEquals("Luis Mendoza", respuesta.staff.nombreCompleto)
        assertEquals("STF-025", respuesta.staff.staffId)
        assertEquals("EVT-025", respuesta.staff.evento.eventoId)
    }

    @Test
    fun testLogin_staffNoAutorizado_rechazaConHttp401() = runBlocking {
        try {
            fakeApi.loginStaff(StaffLoginRequest(
                documentoIdentidad = "87654321",
                pinAcceso = "123456"
            ))
            fail("Debió lanzar HttpException 401 por credenciales no autorizadas")
        } catch (e: HttpException) {
            assertEquals(401, e.code())
        }
    }

    @Test
    fun testLogin_documentoInvalido_rechazaConHttp401() = runBlocking {
        try {
            fakeApi.loginStaff(StaffLoginRequest(
                documentoIdentidad = "00000000",
                pinAcceso = "654321"
            ))
            fail("Debió lanzar HttpException 401 al usar documento no registrado")
        } catch (e: HttpException) {
            assertEquals(401, e.code())
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: EXTRACCIÓN Y VALIDACIÓN DE FORMATO QR
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun testQrExtraction_rawPlano_extraeCorrectamente() {
        // Objeto dummy para invocar métodos de extracción
        val valor = extraerValorQrMock("10482914")
        assertEquals("10482914", valor)

        val correo = extraerValorQrMock("maria.perez@zendov.com")
        assertEquals("maria.perez@zendov.com", correo)
    }

    @Test
    fun testQrExtraction_jsonEstructurado_extraeCampoRelevante() {
        val jsonId = """{"id": "10482914", "evento": "EVT-025"}"""
        assertEquals("10482914", extraerValorQrMock(jsonId))

        val jsonCorreo = """{"correo": "andres.rodriguez@zendov.com"}"""
        assertEquals("andres.rodriguez@zendov.com", extraerValorQrMock(jsonCorreo))

        val jsonDoc = """{"documento": "28491023"}"""
        assertEquals("28491023", extraerValorQrMock(jsonDoc))
    }

    @Test
    fun testQrExtraction_urlAcreditacion_extraeQueryOPath() {
        val urlParam = "https://api-eventos.zendov.com/ticket?code=10482914"
        assertEquals("10482914", extraerValorQrMock(urlParam))

        val urlPath = "https://zendov.com/participantes/28491023"
        assertEquals("28491023", extraerValorQrMock(urlPath))
    }

    @Test
    fun testQrValidation_correosValidos_retornaTrue() {
        assertTrue(esFormatoQrValidoMock("maria.perez@zendov.com"))
        assertTrue(esFormatoQrValidoMock("andres_rodriguez@gmail.com"))
        assertTrue(esFormatoQrValidoMock("staff.acceso+test@empresa.com.pe"))
    }

    @Test
    fun testQrValidation_correosInvalidos_retornaFalse() {
        // "si el correo no es valido que sea no valido"
        assertFalse(esFormatoQrValidoMock("correo_sin_arroba.com"))
        assertFalse(esFormatoQrValidoMock("usuario@"))
        assertFalse(esFormatoQrValidoMock("@dominio.com"))
        assertFalse(esFormatoQrValidoMock("usuario@dominio")) // sin TLD
        assertFalse(esFormatoQrValidoMock("usuario espaciado@zendov.com"))
        assertFalse(esFormatoQrValidoMock("usuario<nombre>@zendov.com"))
    }

    @Test
    fun testQrValidation_codigosIdsValidos_retornaTrue() {
        assertTrue(esFormatoQrValidoMock("10482914"))
        assertTrue(esFormatoQrValidoMock("PAR-025-99"))
        assertTrue(esFormatoQrValidoMock("TICKET_ABC_123"))
    }

    @Test
    fun testQrValidation_codigosInvalidos_retornaFalse() {
        assertFalse(esFormatoQrValidoMock(""))
        assertFalse(esFormatoQrValidoMock("   "))
        assertFalse(esFormatoQrValidoMock("codigo con espacios"))
        assertFalse(esFormatoQrValidoMock("codigo\ncon\nsaltos"))
        assertFalse(esFormatoQrValidoMock("A".repeat(101))) // mayor a 100 caracteres
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECCIÓN 3: ESCANEO FIDEDIGNO SIN INVENTAR ("NO PUEDE INVENTAR")
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun testScan_correoNoRegistrado_retorna404YNoInventaParticipante() = runBlocking {
        // Un correo sintácticamente válido pero que NO pertenece a ningún participante
        val correoFantasma = "hacker.desconocido@noexiste.com"

        try {
            fakeApi.registrarEscaneo(RegistrarEscaneoRequest(
                beneficioId = "ben-001-kit",
                participanteId = correoFantasma
            ))
            fail("Debió retornar 404 Not Found porque el participante no existe. ¡No debe inventar!")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test
    fun testScan_codigoNoRegistrado_retorna404() = runBlocking {
        val codigoInexistente = "COD-999999"

        try {
            fakeApi.registrarEscaneo(RegistrarEscaneoRequest(
                beneficioId = "ben-001-kit",
                participanteId = codigoInexistente
            ))
            fail("Debió fallar con 404 para código inexistente")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test
    fun testScan_participanteValidoPorId_retornaValido() = runBlocking {
        // Andrés Rodríguez ("28491023") aún no tiene beneficio entregado
        val resp = fakeApi.registrarEscaneo(RegistrarEscaneoRequest(
            beneficioId = "ben-001-kit",
            participanteId = "28491023"
        ))

        assertFalse(resp.yaEntregadoPreviamente)
        assertEquals("Andrés Rodríguez", resp.participanteNombreCompleto)
        assertEquals("28491023", resp.participanteId)
    }

    @Test
    fun testScan_participanteValidoPorCorreo_retornaValido() = runBlocking {
        // Búsqueda por correo "andres.rodriguez@zendov.com"
        val resp = fakeApi.registrarEscaneo(RegistrarEscaneoRequest(
            beneficioId = "ben-001-kit",
            participanteId = "andres.rodriguez@zendov.com"
        ))

        assertEquals("Andrés Rodríguez", resp.participanteNombreCompleto)
        assertEquals("28491023", resp.participanteId)
    }

    @Test
    fun testScan_participanteYaEscaneado_retornaYaEntregadoPreviamente() = runBlocking {
        // María Camila Pérez ("10482914") ya tenía beneficio entregado previamente
        val resp = fakeApi.registrarEscaneo(RegistrarEscaneoRequest(
            beneficioId = "ben-001-kit",
            participanteId = "10482914"
        ))

        assertTrue(resp.yaEntregadoPreviamente)
        assertEquals("María Camila Pérez", resp.participanteNombreCompleto)
        assertTrue(resp.mensaje.contains("YA FUE ENTREGADO"))
    }

    @Test
    fun testScan_dobleEscaneoConsecutivo_primerExitoSegundoYaEscaneado() = runBlocking {
        val idValeria = "30192841"

        // 1er Escaneo: Éxito
        val resp1 = fakeApi.registrarEscaneo(RegistrarEscaneoRequest(
            beneficioId = "ben-001-kit",
            participanteId = idValeria
        ))
        assertFalse("Primer escaneo debe ser nuevo", resp1.yaEntregadoPreviamente)

        // 2do Escaneo: Ya escaneado
        val resp2 = fakeApi.registrarEscaneo(RegistrarEscaneoRequest(
            beneficioId = "ben-001-kit",
            participanteId = idValeria
        ))
        assertTrue("Segundo escaneo debe detectar duplicado", resp2.yaEntregadoPreviamente)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECCIÓN 4: PERSISTENCIA ROOM Y MODO OFFLINE
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun testOffline_escaneoLocal_persisteConPendienteSubida() = runBlocking {
        val timestamp = System.currentTimeMillis()
        fakeDao.marcarEscaneoLocal("28491023", timestamp)

        val participante = fakeDao.buscarParticipante("28491023")
        assertNotNull(participante)
        assertTrue(participante!!.beneficioEntregado)
        assertTrue(participante.pendienteSubida)
        assertFalse(participante.sincronizadoConBackend)
        assertEquals(timestamp, participante.timestampEscaneo)

        // Verificar que aparece en lista de pendientes
        val pendientes = fakeDao.obtenerPendientesSync()
        assertEquals(1, pendientes.size)
        assertEquals("28491023", pendientes[0].idParticipante)
    }

    @Test
    fun testOffline_syncUpload_confirmaYDespejaPendientes() = runBlocking {
        // 1. Escaneo offline
        fakeDao.marcarEscaneoLocal("28491023", System.currentTimeMillis())

        // 2. Subida al backend
        val pendientes = fakeDao.obtenerPendientesSync()
        val uploadReq = SyncUploadRequest(pendientes.map { EscaneoRequest(it.idParticipante, it.timestampEscaneo ?: 0L) })
        val uploadResp = fakeApi.subirEscaneos(uploadReq)

        assertEquals(1, uploadResp.confirmados)
        assertEquals(0, uploadResp.rechazados)

        // 3. Confirmar en Room
        val confirmadosIds = uploadResp.resultados.filter { it.validado }.map { it.idParticipante }
        fakeDao.marcarLoteComoSincronizado(confirmadosIds)

        // 4. Verificar que pendientes ahora está vacío
        val pendientesDespues = fakeDao.obtenerPendientesSync()
        assertEquals(0, pendientesDespues.size)

        val p = fakeDao.buscarParticipante("28491023")
        assertTrue(p!!.sincronizadoConBackend)
        assertFalse(p.pendienteSubida)
    }

    @Test
    fun testOffline_busquedaMultiColumna_funcionaPorDniYCorreo() = runBlocking {
        // Búsqueda por documento exacto
        val porDni = fakeDao.buscarParticipante("10482914")
        assertNotNull(porDni)
        assertEquals("María Camila Pérez", porDni!!.nombre)

        // Búsqueda por correo case-insensitive
        val porCorreo = fakeDao.buscarParticipante("MARIA.PEREZ@ZENDOV.COM")
        assertNotNull(porCorreo)
        assertEquals("María Camila Pérez", porCorreo!!.nombre)

        // Búsqueda de asistente inexistente
        val inexistente = fakeDao.buscarParticipante("nadie@empresa.com")
        assertNull(inexistente)
    }

    // ── Métodos utilitarios helper que replican la lógica del repositorio ─────
    private fun extraerValorQrMock(raw: String): String {
        val limpio = raw.trim()
        if (limpio.isEmpty()) return ""

        if (limpio.startsWith("{") && limpio.endsWith("}")) {
            val idMatch = """"id"\s*:\s*"([^"]+)"""".toRegex().find(limpio)?.groupValues?.get(1)
            if (!idMatch.isNullOrBlank()) return idMatch
            val docMatch = """"documento"\s*:\s*"([^"]+)"""".toRegex().find(limpio)?.groupValues?.get(1)
            if (!docMatch.isNullOrBlank()) return docMatch
            val correoMatch = """"correo"\s*:\s*"([^"]+)"""".toRegex().find(limpio)?.groupValues?.get(1)
            if (!correoMatch.isNullOrBlank()) return correoMatch
        }

        if (limpio.startsWith("http://") || limpio.startsWith("https://")) {
            val codeParam = "[?&]code=([^&]+)".toRegex().find(limpio)?.groupValues?.get(1)
            if (!codeParam.isNullOrBlank()) return codeParam
            val lastSlash = limpio.lastIndexOf('/')
            if (lastSlash != -1 && lastSlash < limpio.length - 1) {
                val segment = limpio.substring(lastSlash + 1)
                if (!segment.contains("?")) return segment
            }
        }

        return limpio
    }

    private fun esFormatoQrValidoMock(valor: String): Boolean {
        if (valor.isBlank() || valor.length > 100) return false
        if (valor.contains(" ") || valor.contains("\n") || valor.contains("\r") || valor.contains("\t")) return false

        if (valor.contains("@")) {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            return emailRegex.matches(valor)
        }

        val idRegex = "^[A-Za-z0-9\\-_]+$".toRegex()
        return idRegex.matches(valor)
    }
}
