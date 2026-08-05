package com.example.ozareventscanner.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interfaz Retrofit que define los endpoints del backend.
 *
 * Endpoint implementado:
 *   POST /registrar-participante
 *   Body  → CodigoRequest  { "codigo": "ABC123" }
 *   Response → ParticipanteResponse { idParticipante, nombre, beneficioEntregado }
 */
interface ApiService {

    @POST("registrar-participante")
    suspend fun registrarParticipante( @Body request: CodigoRequest): ParticipanteResponse
}
