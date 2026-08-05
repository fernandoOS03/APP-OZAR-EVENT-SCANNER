package com.example.ozareventscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participantes")

data class ParticipanteEntity(
    @PrimaryKey
    val idParticipante : String,
    val nombre: String,
    val beneficioEntregado: Boolean
)
