package com.example.ozareventscanner.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Interfaz Retrofit del backend Ozar Events Scanner.
 *
 * Endpoints Oficiales (INFORME_MODULO_SCANNER.md):
 *   1. POST /api/scanner/auth                                      → Login Staff (DNI + PIN)
 *   2. GET  /api/scanner/eventos/{eventoId}/beneficios/activos      → Carga Beneficios Activos
 *   3. POST /api/scanner/escaneos                                  → Registrar Escaneo / Entrega de Beneficio (QR)
 *
 * Endpoints de Sincronización Offline (WorkManager):
 *   4. POST /sync/upload                                           → Subida diferida en lote
 *   5. GET  /sync/download                                         → Descarga estado consolidado
 */
interface ApiService {

    // ── 1. Autenticación de Staff ─────────────────────────────────────────────

    @POST("api/v1/scanner/auth")
    suspend fun loginStaff(@Body request: StaffLoginRequest): StaffLoginResponse

    // ── 2. Beneficios Activos del Evento ──────────────────────────────────────

    @GET("api/v1/scanner/eventos/{eventoId}/beneficios/activos")
    suspend fun obtenerBeneficiosActivos(
        @Path("eventoId") eventoId: String
    ): List<BeneficioActivoDto>

    // ── 3. Registrar Escaneo / Entrega de Beneficio (QR en Vivo) ──────────────

    @POST("api/v1/scanner/escaneos")
    suspend fun registrarEscaneo(
        @Body request: RegistrarEscaneoRequest
    ): RegistrarEscaneoResponse

    // ── Sincronización Offline (Room ↔ Backend) ──────────────────────────────

    @POST("sync/upload")
    suspend fun subirEscaneos(@Body request: SyncUploadRequest): SyncUploadResponse

    @GET("sync/download")
    suspend fun descargarEstado(): SyncDownloadResponse
}
