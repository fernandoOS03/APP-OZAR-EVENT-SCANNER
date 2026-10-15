package com.example.ozareventscanner.network

import com.google.gson.annotations.SerializedName

// ═════════════════════════════════════════════════════════════════════════════
// DTOs para los 3 Endpoints Móviles (INFORME_MODULO_SCANNER.md)
// ═════════════════════════════════════════════════════════════════════════════

// ── Endpoint #1: POST /api/scanner/auth ────────────────────────────────────────

data class StaffLoginRequest(
    @SerializedName("documentoIdentidad")
    val documentoIdentidad: String,
    @SerializedName("pinAcceso")
    val pinAcceso: String
)

data class StaffLoginResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("staff")
    val staff: StaffDataDto
)

data class StaffDataDto(
    @SerializedName("staffId")
    val staffId: String,
    @SerializedName("nombreCompleto")
    val nombreCompleto: String,
    @SerializedName("documentoIdentidad")
    val documentoIdentidad: String,
    @SerializedName("rol")
    val rol: String? = null,
    @SerializedName("cargo")
    val cargo: String? = null,
    @SerializedName("evento")
    val evento: EventoStaffDto
)

data class EventoStaffDto(
    @SerializedName("eventoId")
    val eventoId: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("tenantId")
    val tenantId: String
)

// ── Endpoint #2: GET /api/scanner/eventos/{eventoId}/beneficios/activos ───────

data class BeneficioActivoDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("orden")
    val orden: Int,
    @SerializedName("estado")
    val estado: Boolean,
    @SerializedName("eventoId")
    val eventoId: String,
    @SerializedName("eventoNombre")
    val eventoNombre: String,
    @SerializedName("descripcion")
    val descripcion: String? = null
)

// ── Endpoint #3: POST /api/scanner/escaneos ───────────────────────────────────

data class RegistrarEscaneoRequest(
    @SerializedName("beneficioId")
    val beneficioId: String,
    @SerializedName("documentoIdentidad")
    val documentoIdentidad: String? = null,
    @SerializedName("codigoViaje")
    val codigoViaje: String? = null,
    @SerializedName("participanteId")
    val participanteId: String? = null
)

data class RegistrarEscaneoResponse(
    @SerializedName("escaneoId")
    val escaneoId: String? = null,
    @SerializedName("participanteId")
    val participanteId: String? = null,
    @SerializedName("participanteNombreCompleto")
    val participanteNombreCompleto: String? = null,
    @SerializedName("documentoIdentidad")
    val documentoIdentidad: String? = null,
    @SerializedName("beneficioId")
    val beneficioId: String? = null,
    @SerializedName("beneficioNombre")
    val beneficioNombre: String? = null,
    @SerializedName("yaEntregadoPreviamente")
    val yaEntregadoPreviamente: Boolean = false,
    @SerializedName("mensaje")
    val mensaje: String = "",
    @SerializedName("fechaEscaneo")
    val fechaEscaneo: String? = null,
    @SerializedName("staffNombre")
    val staffNombre: String? = null
)
