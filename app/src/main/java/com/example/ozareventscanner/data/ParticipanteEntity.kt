package com.example.ozareventscanner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room optimizada para alto volumen (10,000+ registros).
 * Incluye índices B-Tree en [documento], [nombre] e [isSynced] para búsquedas ultra rápidas O(log N).
 */
@Entity(
    tableName = "participantes",
    indices = [
        Index(value = ["documento"]),
        Index(value = ["nombre"]),
        Index(value = ["isSynced"])
    ]
)
data class ParticipanteEntity(
    @PrimaryKey
    val idParticipante: String,
    val documento: String = idParticipante,
    val nombre: String,
    val beneficioEntregado: Boolean = false,
    val timestampEscaneo: Long? = null,
    val isSynced: Boolean = false,
    val sincronizadoConBackend: Boolean = isSynced,
    val pendienteSubida: Boolean = !isSynced
)
