package com.example.ozareventscanner.network

// ─────────────────────────────────────────────────────────────────────────────
// Upload — Subida de escaneos pendientes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Un escaneo individual a enviar al backend.
 * El campo [timestampEscaneo] es el mecanismo de resolución de conflictos:
 * el backend acepta el escaneo con menor timestamp (el primero cronológicamente).
 */
data class EscaneoRequest(
    val idParticipante: String,
    val timestampEscaneo: Long          // epoch ms — menor timestamp = ganador del conflicto
)

/** Cuerpo del POST /sync/upload. */
data class SyncUploadRequest(
    val escaneos: List<EscaneoRequest>
)

/**
 * Resultado por participante tras el procesamiento del backend.
 * Si [validado] = false → otro dispositivo escaneó antes; revertir localmente.
 */
data class EscaneoConfirmado(
    val idParticipante: String,
    val validado: Boolean,
    val timestampValidado: Long?        // timestamp del escaneo que el backend aceptó
)

/** Respuesta completa del POST /sync/upload. */
data class SyncUploadResponse(
    val confirmados: Int,
    val rechazados: Int,
    val resultados: List<EscaneoConfirmado>
)

// ─────────────────────────────────────────────────────────────────────────────
// Download — Estado consolidado del servidor
// ─────────────────────────────────────────────────────────────────────────────

/** DTO de un participante descargado del servidor. */
data class ParticipanteSyncDto(
    val idParticipante: String,
    val nombre: String,
    val beneficioEntregado: Boolean,
    val timestampEscaneo: Long?         // null si no ha sido escaneado aún
)

/** Respuesta completa del GET /sync/download. */
data class SyncDownloadResponse(
    val participantes: List<ParticipanteSyncDto>,
    val totalEscaneados: Int,
    val timestampServidor: Long         // cuándo generó el servidor esta snapshot
)
