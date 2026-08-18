package com.example.ozareventscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un participante del evento.
 *
 * Campos de sincronización (nuevos en v2):
 *  - [timestampEscaneo]       : Momento del escaneo local (epoch ms). Usado por el backend
 *                               para resolver conflictos: gana el menor timestamp (el primero).
 *  - [sincronizadoConBackend] : true cuando el backend confirmó este registro.
 *  - [pendienteSubida]        : true cuando hay un escaneo local aún no sincronizado.
 */
@Entity(tableName = "participantes")
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
    val pendienteSubida: Boolean = false
)
