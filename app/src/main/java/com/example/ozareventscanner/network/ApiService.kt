package com.example.ozareventscanner.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfaz Retrofit del backend Ozar Events Scanner.
 *
 * Endpoints Oficiales:
 *   1. POST /api/v1/scanner/auth
 *   2. GET  /api/v1/scanner/eventos/{eventoId}/beneficios/activos
 *   3. POST /api/v1/scanner/escaneos
 *   4. POST /api/v1/scanner/sync/upload
 *   5. GET  /api/v1/scanner/sync/download/{eventoId}?since=<epochMs>
 */
interface ApiService {

    @POST("api/v1/scanner/auth")
    suspend fun loginStaff(@Body request: StaffLoginRequest): StaffLoginResponse

    @GET("api/v1/scanner/eventos/{eventoId}/beneficios/activos")
    suspend fun obtenerBeneficiosActivos(
        @Path("eventoId") eventoId: String
    ): List<BeneficioActivoDto>

    @POST("api/v1/scanner/escaneos")
    suspend fun registrarEscaneo(
        @Body request: RegistrarEscaneoRequest
    ): RegistrarEscaneoResponse

    @POST("api/v1/scanner/sync/upload")
    suspend fun subirEscaneos(@Body request: SyncUploadRequest): SyncUploadResponse

    @GET("api/v1/scanner/sync/download/{eventoId}")
    suspend fun descargarEstado(
        @Path("eventoId") eventoId: String,
        @Query("since") since: Long? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): SyncDownloadResponse
}
