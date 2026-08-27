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
 *
 * ⚠️ URL_BASE debe actualizarse según el entorno:
 *   - Emulador Android  → "http://10.0.2.2:8000/"
 *   - Dispositivo físico → "http://192.168.X.X:8000/"
 *   - Producción         → "https://api.tu-dominio.com/"
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * MOCK INTERCEPTOR (temporal)
 * ──────────────────────────────────────────────────────────────────────────────
 * Los endpoints /sync/upload y /sync/download no existen en el backend aún.
 * El interceptor los simula con respuestas locales:
 *
 *   /sync/upload  → Responde con resultados vacíos (el repositorio trata esto como
 *                   confirmación de todos los pendientes — ver [ParticipanteRepository]).
 *   /sync/download → Lista vacía (el servidor aún no provee datos).
 *
 * PARA PRODUCCIÓN: eliminar [mockSyncInterceptor] del OkHttpClient builder.
 */
object RetrofitClient {

    private const val URL_BASE = "http://10.0.2.2:8000/"

    // ── Tipo de contenido compartido ──────────────────────────────────────────
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // ─────────────────────────────────────────────────────────────────────────
    // Mock interceptor para los endpoints de sincronización
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Intercepta /sync/upload y /sync/download y devuelve respuestas simuladas.
     * El resto de llamadas pasan directamente al servidor real.
     *
     * Nota: el mock de upload devuelve [resultados] vacío.
     * El [ParticipanteRepository] detecta esto y marca todos los pendientes
     * como confirmados (comportamiento offline-first optimista para pruebas locales).
     *
     * TODO: Eliminar este interceptor cuando el backend implemente ambos endpoints.
     */
    private val mockSyncInterceptor = Interceptor { chain ->
        val request = chain.request()
        val path    = request.url.encodedPath

        when {
            // ── Mock: POST /sync/upload ───────────────────────────────────────
            path.contains("sync/upload") -> {
                // Devuelve resultados vacíos; el repositorio confirma todos los pendientes.
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

            // ── Resto → servidor real ─────────────────────────────────────────
            else -> chain.proceed(request)
        }
    }

    private fun buildMockResponse(request: okhttp3.Request, json: String): Response =
        Response.Builder()
            .code(200)
            .message("OK [MOCK — backend pendiente]")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(json.toResponseBody(JSON_MEDIA_TYPE))
            .build()

    // ─────────────────────────────────────────────────────────────────────────
    // ApiService singleton
    // ─────────────────────────────────────────────────────────────────────────

    val apiService: ApiService by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(mockSyncInterceptor) // TODO: eliminar en producción
            .build()

        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
