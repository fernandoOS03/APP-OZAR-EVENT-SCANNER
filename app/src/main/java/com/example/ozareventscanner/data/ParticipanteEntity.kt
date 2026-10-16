package com.example.ozareventscanner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un participante del evento.
 *
 * Campos de sincronización (v2):
 *  - [timestampEscaneo]       : Momento del escaneo local (epoch ms). Usado por el backend
 *                               para resolver conflictos: gana el menor timestamp (el primero).
 *  - [sincronizadoConBackend] : true cuando el backend confirmó este registro.
 *  - [pendienteSubida]        : true cuando hay un escaneo local aún no sincronizado.
 *
 * Campos de trazabilidad y auditoría (v4):
 *  - [scannedByStaffId], [scannedByStaffNombre], [scannedByDeviceId], [beneficioId]
 */
@Entity(
    tableName = "participantes",
    indices = [
        Index(value = ["documento"], unique = false),
        Index(value = ["correo"], unique = false)
    ]
)
data class ParticipanteEntity(
    @PrimaryKey
    val idParticipante: String,
    val nombre: String,
    val beneficioEntregado: Boolean,

    // ── Campos de sincronización (Room v2) ────────────────────────────────────
    /** Epoch ms en que se realizó el escaneo en este dispositivo. null = no escaneado. */
    val timestampEscaneo: Long? = null,

    /** true cuando el backend confirmó este escaneo como válido. */
    val sincronizadoConBackend: Boolean = false,

    /** true cuando el escaneo está pendiente de subir al backend. */
    val pendienteSubida: Boolean = false,

    // ── Identificación complementaria para búsqueda y QR ─────────────────────
    val documento: String? = null,
    val correo: String? = null,

    // ── Trazabilidad y Auditoría Offline-First (Room v4) ──────────────────────
    val scannedByStaffId: String? = null,
    val scannedByStaffNombre: String? = null,
    val scannedByDeviceId: String? = null,
    val beneficioId: String? = null
)

