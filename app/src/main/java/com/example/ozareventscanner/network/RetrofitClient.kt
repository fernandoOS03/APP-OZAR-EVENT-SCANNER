package com.example.ozareventscanner.network

import com.example.ozareventscanner.data.SessionManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit configurado para Ozar Events Scanner.
 *
 * Incluye:
 *  - [AuthInterceptor] para adjuntar el JWT token automáticamente.
 *  - Fallback simulado para desarrollo local cuando el servidor backend esté apagado.
 */
object RetrofitClient {

    private const val URL_BASE = "https://api-eventos.zendov.com/"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    var sessionManager: SessionManager? = null

    // Cache en memoria para simular doble escaneo durante pruebas locales
    private val escaneosRealizadosMock = mutableSetOf<String>()

    data class AsistenteBackendMock(
        val id: String,
        val dni: String,
        val correo: String,
        val nombre: String,
        var entregado: Boolean = false
    )

    val mockAsistentesBackend = mutableListOf(
        AsistenteBackendMock("10482914", "10482914", "maria.perez@zendov.com", "María Camila Pérez", entregado = true),
        AsistenteBackendMock("28491023", "28491023", "andres.rodriguez@zendov.com", "Andrés Rodríguez", entregado = false),
        AsistenteBackendMock("19482019", "19482019", "carlos.gomez@zendov.com", "Carlos Andrés Gómez", entregado = true),
        AsistenteBackendMock("30192841", "30192841", "valeria.martinez@zendov.com", "Valeria Sofía Martínez", entregado = false),
        AsistenteBackendMock("12345678", "12345678", "diego.vargas@zendov.com", "Diego Fernando Vargas", entregado = true),
        AsistenteBackendMock("45910283", "45910283", "lucia.rojas@zendov.com", "Lucía Mariana Rojas", entregado = false),
        AsistenteBackendMock("50192847", "50192847", "esteban.mendoza@zendov.com", "Esteban Javier Mendoza", entregado = true),
        AsistenteBackendMock("60291834", "60291834", "alejandro.morales@zendov.com", "Alejandro Morales Rivera", entregado = false)
    )

    /**
     * Interceptor que intercepta llamadas y provee respuestas simuladas
     * si el servidor local no está disponible o para endpoints pendientes.
     */
    private val devMockInterceptor = Interceptor { chain ->
        val request = chain.request()
        val path    = request.url.encodedPath

        // Intentar conectar con el backend real primero
        try {
            val response = chain.proceed(request)
            if (response.isSuccessful || response.code in 400..499) {
                return@Interceptor response
            }
        } catch (e: IOException) {
            // Servidor apagado o inalcanzable: procedemos con mock para no bloquear desarrollo
        }

        // Mock exacto según INFORME_MODULO_SCANNER.md y especificaciones Zendov
        when {
            // ── 1. POST /api/scanner/auth ─────────────────────────────────────
            path.contains("scanner/auth") -> {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val bodyStr = buffer.readUtf8()

                val loginRequest = try {
                    Gson().fromJson(bodyStr, StaffLoginRequest::class.java)
                } catch (e: Exception) {
                    null
                }

                val dni = loginRequest?.documentoIdentidad?.trim() ?: ""
                val pin = loginRequest?.pinAcceso?.trim() ?: ""

                // Solo acepta estrictamente como modelo hardcodeado el usuario staff: DNI "12345678" y PIN "654321"
                val esCredencialOficial = (dni == "12345678" && pin == "654321")

                if (esCredencialOficial) {
                    val staffData = StaffDataDto(
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
                    val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWJqZWN0IjoiMTIzNDU2NzgiLCJzdGFmZklkIjoiU1RGLTAyNSIsImV2ZW50b0lkIjoiRVZULTAyNSIsInRlbmFudElkIjoidGVuYW50X3plbmRvdl8yMDI1Iiwicm9sIjoiQ29vcmRpbmFkb3IgZGUgYWNjZXNvcyJ9.demo"

                    // Guardar sesión en SessionManager si está disponible
                    sessionManager?.guardarSesion(token, staffData)

                    val json = """
                    {
                      "token": "$token",
                      "staff": {
                        "staffId": "STF-025",
                        "nombreCompleto": "Luis Mendoza",
                        "documentoIdentidad": "12345678",
                        "rol": "Coordinador de accesos",
                        "cargo": "Coordinador de accesos",
                        "evento": {
                          "eventoId": "EVT-025",
                          "nombre": "Zendov Tech Summit 2025",
                          "slug": "zendov-summit-2025",
                          "tenantId": "tenant_zendov_2025"
                        }
                      }
                    }
                    """.trimIndent()
                    buildMockResponse(request, 200, json)
                } else {
                    val errorJson = """
                    {
                      "error": "Unauthorized",
                      "message": "Credenciales no válidas o no autorizadas"
                    }
                    """.trimIndent()
                    buildMockResponse(request, 401, errorJson)
                }
            }

            // ── 2. GET /api/scanner/eventos/{eventoId}/beneficios/activos ──────
            path.contains("beneficios/activos") -> {
                val json = """
                [
                  {
                    "id": "ben-001-kit",
                    "nombre": "Kit de bienvenida",
                    "orden": 1,
                    "estado": true,
                    "eventoId": "EVT-025",
                    "eventoNombre": "Zendov Tech Summit 2025",
                    "descripcion": "Bolso + credencial"
                  },
                  {
                    "id": "ben-002-almuerzo",
                    "nombre": "Almuerzo ejecutivo",
                    "orden": 2,
                    "estado": true,
                    "eventoId": "EVT-025",
                    "eventoNombre": "Zendov Tech Summit 2025",
                    "descripcion": "Salón principal · 13:00"
                  },
                  {
                    "id": "ben-003-conferencia",
                    "nombre": "Acceso conferencia",
                    "orden": 3,
                    "estado": true,
                    "eventoId": "EVT-025",
                    "eventoNombre": "Zendov Tech Summit 2025",
                    "descripcion": "Auditorio A"
                  },
                  {
                    "id": "ben-004-merch",
                    "nombre": "Merch oficial",
                    "orden": 4,
                    "estado": true,
                    "eventoId": "EVT-025",
                    "eventoNombre": "Zendov Tech Summit 2025",
                    "descripcion": "Acceso exclusivo"
                  }
                ]
                """.trimIndent()
                buildMockResponse(request, 200, json)
            }

            // ── 3. POST /api/scanner/escaneos ─────────────────────────────────
            path.contains("scanner/escaneos") -> {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val bodyStr = buffer.readUtf8()

                val scanReq = try {
                    Gson().fromJson(bodyStr, RegistrarEscaneoRequest::class.java)
                } catch (e: Exception) {
                    null
                }
                val rawTarget = (scanReq?.participanteId ?: scanReq?.documentoIdentidad ?: "").trim()
                val benId = scanReq?.beneficioId ?: "ben-001-kit"

                // 1. Validar formato sintáctico
                val regexEmail = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
                val formatoInvalido = rawTarget.isBlank() ||
                        rawTarget.contains(" ") ||
                        (rawTarget.contains("@") && !regexEmail.matches(rawTarget))

                if (formatoInvalido) {
                    val errJson = """{"error":"BadRequest","message":"Formato de código o correo inválido"}"""
                    return@Interceptor buildMockResponse(request, 400, errJson)
                }

                // 2. Buscar en la lista oficial de asistentes del evento (sin inventar participantes)
                val asistente = mockAsistentesBackend.find {
                    it.id.equals(rawTarget, ignoreCase = true) ||
                    it.dni.equals(rawTarget, ignoreCase = true) ||
                    it.correo.equals(rawTarget, ignoreCase = true)
                }

                if (asistente == null) {
                    val errJson = """{"error":"NotFound","message":"Participante no encontrado con el código o correo escaneado: $rawTarget"}"""
                    return@Interceptor buildMockResponse(request, 404, errJson)
                }

                val benNombre = when (benId) {
                    "ben-002-almuerzo" -> "Almuerzo ejecutivo"
                    "ben-003-conferencia" -> "Acceso conferencia"
                    "ben-004-merch" -> "Merch oficial"
                    else -> "Kit de bienvenida"
                }

                val keyEntrega = "${asistente.id}_$benId"
                val yaEscaneado = asistente.entregado || escaneosRealizadosMock.contains(keyEntrega)

                val json = if (!yaEscaneado) {
                    escaneosRealizadosMock.add(keyEntrega)
                    asistente.entregado = true
                    """
                    {
                      "escaneoId": "esc-${System.currentTimeMillis()}",
                      "participanteId": "${asistente.id}",
                      "participanteNombreCompleto": "${asistente.nombre}",
                      "documentoIdentidad": "${asistente.dni}",
                      "beneficioId": "$benId",
                      "beneficioNombre": "$benNombre",
                      "yaEntregadoPreviamente": false,
                      "mensaje": "Beneficio entregado exitosamente a ${asistente.nombre}",
                      "fechaEscaneo": "2025-06-24T10:15:30",
                      "staffNombre": "Luis Mendoza"
                    }
                    """.trimIndent()
                } else {
                    """
                    {
                      "escaneoId": "esc-${System.currentTimeMillis()}",
                      "participanteId": "${asistente.id}",
                      "participanteNombreCompleto": "${asistente.nombre}",
                      "documentoIdentidad": "${asistente.dni}",
                      "beneficioId": "$benId",
                      "beneficioNombre": "$benNombre",
                      "yaEntregadoPreviamente": true,
                      "mensaje": "ATENCIÓN: Este beneficio YA FUE ENTREGADO previamente a ${asistente.nombre}",
                      "fechaEscaneo": "2025-06-24T09:30:00",
                      "staffNombre": "Luis Mendoza"
                    }
                    """.trimIndent()
                }
                buildMockResponse(request, 200, json)
            }

            // ── Sincronización Room <-> Backend ──────────────────────────────
            path.contains("sync/upload") -> {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val bodyStr = buffer.readUtf8()
                val uploadReq = try {
                    Gson().fromJson(bodyStr, SyncUploadRequest::class.java)
                } catch (e: Exception) {
                    null
                }

                var confirmados = 0
                var rechazados = 0
                val resultados = mutableListOf<String>()

                uploadReq?.escaneos?.forEach { esc ->
                    val asistente = mockAsistentesBackend.find {
                        it.id.equals(esc.idParticipante, ignoreCase = true) ||
                        it.dni.equals(esc.idParticipante, ignoreCase = true) ||
                        it.correo.equals(esc.idParticipante, ignoreCase = true)
                    }
                    if (asistente != null) {
                        confirmados++
                        asistente.entregado = true
                        resultados.add("""{"idParticipante":"${esc.idParticipante}","validado":true,"timestampValidado":${esc.timestampEscaneo}}""")
                    } else {
                        rechazados++
                        resultados.add("""{"idParticipante":"${esc.idParticipante}","validado":false,"timestampValidado":null}""")
                    }
                }

                val json = """
                {
                  "confirmados": $confirmados,
                  "rechazados": $rechazados,
                  "resultados": [${resultados.joinToString(",")}]
                }
                """.trimIndent()
                buildMockResponse(request, 200, json)
            }

            path.contains("sync/download") -> {
                val dtos = mockAsistentesBackend.map { a ->
                    """
                    {
                      "idParticipante": "${a.id}",
                      "nombre": "${a.nombre}",
                      "beneficioEntregado": ${a.entregado},
                      "timestampEscaneo": ${if (a.entregado) "1719224130000" else "null"},
                      "documento": "${a.dni}",
                      "correo": "${a.correo}"
                    }
                    """.trimIndent()
                }
                val totalEscaneados = mockAsistentesBackend.count { it.entregado }
                val json = """
                {
                  "participantes": [${dtos.joinToString(",")}],
                  "totalEscaneados": $totalEscaneados,
                  "timestampServidor": ${System.currentTimeMillis()}
                }
                """.trimIndent()
                buildMockResponse(request, 200, json)
            }

            else -> throw IOException("No se pudo conectar al servidor y no hay mock para la ruta: $path")
        }
    }

    private fun buildMockResponse(request: okhttp3.Request, code: Int, json: String): Response =
        Response.Builder()
            .code(code)
            .message(if (code == 401) "Credenciales no válidas o no autorizadas" else "OK [MOCK DEVELOPMENT]")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(json.toResponseBody(JSON_MEDIA_TYPE))
            .build()

    val apiService: ApiService by lazy {
        val authInterceptor = AuthInterceptor { sessionManager }

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(devMockInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
