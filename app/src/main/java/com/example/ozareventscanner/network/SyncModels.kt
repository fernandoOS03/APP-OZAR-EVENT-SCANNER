package com.example.ozareventscanner.network

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// Upload — Subida de escaneos pendientes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Un escaneo individual a enviar al backend.
 * [beneficioId] es obligatorio: el backend lo valida como UUID.
 * La subida es idempotente: un escaneo ya activo se confirma sin duplicarse.
 */
data class EscaneoRequest(
    @SerializedName("idParticipante")
    val idParticipante: String,
    @SerializedName("timestampEscaneo")
    val timestampEscaneo: Long,
    @SerializedName("beneficioId")
    val beneficioId: String,
    @SerializedName("staffId")
    val staffId: String? = null,
    @SerializedName("deviceId")
    val deviceId: String? = null,
    @SerializedName("documentoIdentidad")
    val documentoIdentidad: String? = null,
    @SerializedName("codigoViaje")
    val codigoViaje: String? = null
)

/** Cuerpo del POST /api/v1/scanner/sync/upload. */
data class SyncUploadRequest(
    @SerializedName("escaneos")
    val escaneos: List<EscaneoRequest>,
    @SerializedName("deviceId")
    val deviceId: String? = null
)

/**
 * Resultado por escaneo tras el procesamiento del backend.
 * [estado] = "CONFIRMADO" | "YA_ENTREGADO" | "RECHAZADO".
 * [validado] = true → confirmado (incluye el caso idempotente).
 */
data class EscaneoConfirmado(
    @SerializedName("idParticipante")
    val idParticipante: String,
    @SerializedName("beneficioId")
    val beneficioId: String? = null,
    @SerializedName("validado")
    val validado: Boolean,
    @SerializedName("estado")
    val estado: String? = null,
    @SerializedName("timestampValidado")
    val timestampValidado: Long? = null,
    @SerializedName("staffOriginalNombre")
    val staffOriginalNombre: String? = null,
    @SerializedName("mensaje")
    val mensaje: String? = null
) {
    companion object {
        const val ESTADO_CONFIRMADO = "CONFIRMADO"
        const val ESTADO_YA_ENTREGADO = "YA_ENTREGADO"
        const val ESTADO_RECHAZADO = "RECHAZADO"
    }
}

/** Respuesta completa del POST /sync/upload. */
data class SyncUploadResponse(
    @SerializedName("confirmados")
    val confirmados: Int,
    @SerializedName("rechazados")
    val rechazados: Int,
    @SerializedName("resultados")
    val resultados: List<EscaneoConfirmado> = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
// Download — Estado consolidado del servidor (completo o delta)
// ─────────────────────────────────────────────────────────────────────────────

/** Estado de un beneficio para un participante. */
data class BeneficioEstadoDto(
    @SerializedName("beneficioId")
    val beneficioId: String,
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("orden")
    val orden: Int? = null,
    @SerializedName("entregado")
    val entregado: Boolean = false,
    @SerializedName("fechaEscaneo")
    val fechaEscaneo: String? = null,
    @SerializedName("timestampEscaneo")
    val timestampEscaneo: Long? = null,
    @SerializedName("staffId")
    val staffId: String? = null,
    @SerializedName("staffNombre")
    val staffNombre: String? = null,
    @SerializedName("deviceId")
    val deviceId: String? = null
)

/** DTO de un participante descargado del servidor. */
data class ParticipanteSyncDto(
    @SerializedName("idParticipante")
    val idParticipante: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("beneficioEntregado")
    val beneficioEntregado: Boolean = false,
    @SerializedName("timestampEscaneo")
    val timestampEscaneo: Long? = null,
    @SerializedName("documento")
    val documento: String? = null,
    @SerializedName("correo")
    val correo: String? = null,
    @SerializedName("beneficiosEstado")
    val beneficiosEstado: List<BeneficioEstadoDto> = emptyList(),
    @SerializedName("eliminado")
    val eliminado: Boolean = false
)

/** Respuesta completa del GET /sync/download/{eventoId}. */
data class SyncDownloadResponse(
    @SerializedName("participantes")
    val participantes: List<ParticipanteSyncDto>,
    @SerializedName("totalEscaneados")
    val totalEscaneados: Int,
    @SerializedName("timestampServidor")
    val timestampServidor: Long,
    @SerializedName("eventoId")
    val eventoId: String? = null,
    @SerializedName("beneficios")
    val beneficios: List<BeneficioActivoDto> = emptyList(),
    @SerializedName("delta")
    val delta: Boolean = false,
    @SerializedName("page")
    val page: Int = 0,
    @SerializedName("size")
    val size: Int = 500,
    @SerializedName("hasMore")
    val hasMore: Boolean = false
)
