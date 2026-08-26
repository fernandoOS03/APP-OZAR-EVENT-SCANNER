package com.example.ozareventscanner.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton Retrofit con cliente OkHttp configurado.
 */
object RetrofitClient {

    private const val URL_BASE = "http://10.0.2.2:8000/"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Interceptor mock para desarrollo/demo local.
     * Simula login y sincronización si el servidor no está disponible.
     */
    private val mockSyncInterceptor = Interceptor { chain ->
        val request = chain.request()
        val path = request.url.encodedPath

        when {
            // ── Mock: POST /auth/login ───────────────────────────────────────
            path.contains("auth/login") -> {
                val json = """
                {
                    "token": "jwt_token_session_ozar_2026",
                    "staffNombre": "Carlos Staff",
                    "evento": {
                        "idEvento": "EVT-2026-01",
                        "nombreEvento": "Tech Summit Ozar 2026",
                        "beneficios": [
                            { "idBeneficio": "BEN-01", "nombre": "Acreditación y Credencial", "progreso": "180/300", "icono": "BADGE" },
                            { "idBeneficio": "BEN-02", "nombre": "Kit de Bienvenida", "progreso": "145/300", "icono": "GIFT" },
                            { "idBeneficio": "BEN-03", "nombre": "Almuerzo Ejecutivo", "progreso": "92/300", "icono": "FOOD" },
                            { "idBeneficio": "BEN-04", "nombre": "Acceso Sala VIP", "progreso": "40/100", "icono": "STAR" }
                        ],
                        "participantesIniciales": [
                            { "idParticipante": "74839201", "nombre": "Ana García Torres", "beneficioEntregado": false, "timestampEscaneo": null },
                            { "idParticipante": "45920183", "nombre": "Juan Pérez Mendoza", "beneficioEntregado": false, "timestampEscaneo": null },
                            { "idParticipante": "10293847", "nombre": "Lucía Morales Vega", "beneficioEntregado": true, "timestampEscaneo": 1740520000000 },
                            { "idParticipante": "83920194", "nombre": "Roberto Silva Castro", "beneficioEntregado": false, "timestampEscaneo": null },
                            { "idParticipante": "99281726", "nombre": "Elena Ramos Paredes", "beneficioEntregado": false, "timestampEscaneo": null }
                        ]
                    }
                }
                """.trimIndent()
                buildMockResponse(request, json)
            }

            // ── Mock: POST /sync/upload ───────────────────────────────────────
            path.contains("sync/upload") -> {
                val json = """{"confirmados":0,"rechazados":0,"resultados":[]}"""
                buildMockResponse(request, json)
            }

            // ── Mock: GET /sync/download ──────────────────────────────────────
            path.contains("sync/download") -> {
                val json = buildString {
                    append("""{"participantes":[],"totalEscaneados":0,""")
                    append(""""timestampServidor":${System.currentTimeMillis()}}""")
                }
                buildMockResponse(request, json)
            }

            else -> chain.proceed(request)
        }
    }

    private fun buildMockResponse(request: okhttp3.Request, json: String): Response =
        Response.Builder()
            .code(200)
            .message("OK [MOCK]")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(json.toResponseBody(JSON_MEDIA_TYPE))
            .build()

    val apiService: ApiService by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(mockSyncInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
