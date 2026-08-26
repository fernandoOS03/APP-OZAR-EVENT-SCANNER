package com.example.ozareventscanner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para transacciones de escaneo.
 * Índices en [idParticipante], [idBeneficio] e [isSynced] para consultas y joins instantáneos.
 */
@Entity(
    tableName = "escaneos",
    indices = [
        Index(value = ["idParticipante"]),
        Index(value = ["idBeneficio"]),
        Index(value = ["isSynced"])
    ]
)
data class EscaneoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val idParticipante: String,
    val idBeneficio: String = "",
    val timestampEscaneo: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
