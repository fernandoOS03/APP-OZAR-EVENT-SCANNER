package com.example.ozareventscanner.network

/**
 * Modela la respuesta JSON del backend cuando se registra un código.
 *
 * Ejemplo de respuesta esperada:
 * {
 *   "idParticipante": "uuid-1234-abcd",
 *   "nombre": "Juan Pérez",
 *   "beneficioEntregado": false
 * }
 */
data class ParticipanteResponse(
    val idParticipante: String,
    val nombre: String,
    val beneficioEntregado: Boolean
)
