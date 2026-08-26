package com.example.ozareventscanner.network

/**
 * Petición de login para el staff.
 * Solo requiere documento (DNI) y PIN numérico.
 */
data class LoginRequest(
    val documento: String,
    val pin: String
)

/**
 * Beneficio configurado para el evento.
 */
data class BeneficioDto(
    val idBeneficio: String,
    val nombre: String,
    val progreso: String = "0/0",
    val icono: String = "DEFAULT"
)

/**
 * Configuración del evento asignado al staff.
 */
data class EventoConfigDto(
    val idEvento: String,
    val nombreEvento: String,
    val beneficios: List<BeneficioDto> = emptyList(),
    val participantesIniciales: List<ParticipanteSyncDto> = emptyList()
)

/**
 * Respuesta de autenticación devuelta por el backend.
 */
data class LoginResponse(
    val token: String,
    val staffNombre: String,
    val evento: EventoConfigDto
)
