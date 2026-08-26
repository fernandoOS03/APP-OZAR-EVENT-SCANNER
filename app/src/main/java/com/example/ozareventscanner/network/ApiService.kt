package com.example.ozareventscanner.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz Retrofit que define los endpoints del backend.
 */
interface ApiService {

    // ── Autenticación de Staff ───────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // ── Registro manual de participante ───────────────────────────────────────

    @POST("registrar-participante")
    suspend fun registrarParticipante(@Body request: CodigoRequest): ParticipanteResponse

    // ── Sincronización bidireccional en segundo plano ────────────────────────

    /**
     * Sube en lote los escaneos pendientes con timestamps.
     */
    @POST("sync/upload")
    suspend fun subirEscaneos(@Body request: SyncUploadRequest): SyncUploadResponse

    /**
     * Descarga el estado canónico de todos los participantes del evento.
     */
    @GET("sync/download")
    suspend fun descargarEstado(): SyncDownloadResponse
}
