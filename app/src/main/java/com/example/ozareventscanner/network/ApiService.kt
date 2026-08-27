package com.example.ozareventscanner.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz Retrofit que define todos los endpoints del backend.
 *
 * Endpoints implementados en el backend:
 *   POST /registrar-participante  → Registra un participante por código manual.
 *
 * Endpoints pendientes de implementación en el backend (mock activo):
 *   POST /sync/upload             → Sube escaneos pendientes en lote con timestamps.
 *   GET  /sync/download           → Descarga el estado canónico de todos los participantes.
 *
 * ⚠️ Los endpoints de sync están mockeados en [RetrofitClient.mockSyncInterceptor].
 *    Cuando el backend los implemente, simplemente eliminar el interceptor.
 */
interface ApiService {

    // ── Registro manual de participante ───────────────────────────────────────

    @POST("registrar-participante")
    suspend fun registrarParticipante(@Body request: CodigoRequest): ParticipanteResponse

    // ── Sincronización bidireccional (MOCK activo) ────────────────────────────

    /**
     * Sube en lote los escaneos pendientes.
     * El backend resuelve conflictos usando [EscaneoRequest.timestampEscaneo]:
     * el escaneo con menor timestamp (más antiguo) es el válido.
     *
     * TODO: Implementar en el backend antes de producción.
     */
    @POST("sync/upload")
    suspend fun subirEscaneos(@Body request: SyncUploadRequest): SyncUploadResponse

    /**
     * Descarga el estado canónico de todos los participantes del evento.
     * Se llama inmediatamente después de [subirEscaneos] para mantener
     * la consistencia Room ↔ Backend.
     *
     * TODO: Implementar en el backend antes de producción.
     */
    @GET("sync/download")
    suspend fun descargarEstado(): SyncDownloadResponse
}
