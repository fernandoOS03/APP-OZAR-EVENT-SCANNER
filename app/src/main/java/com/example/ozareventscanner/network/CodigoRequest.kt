package com.example.ozareventscanner.network

/**
 * Modela el cuerpo (body) del POST que se envía al backend.
 *
 * Ejemplo de JSON enviado:
 * { "codigo": "ABC123" }
 */
data class CodigoRequest(
    val codigo: String
)
