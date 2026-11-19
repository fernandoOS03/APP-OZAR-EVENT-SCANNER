package com.example.ozareventscanner.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "participantes",
    indices = [
        Index(value = ["nombre"], unique = false),
        Index(value = ["documento"], unique = false),
        Index(value = ["correo"], unique = false),
        Index(value = ["eventoId"], unique = false)
    ]
)
data class ParticipanteEntity(
    @PrimaryKey
    val idParticipante: String,
    val nombre: String,
    val documento: String? = null,
    val correo: String? = null,
    val eventoId: String = ""
)

@Entity(
    tableName = "beneficios",
    indices = [Index(value = ["orden"], unique = false)]
)
data class BeneficioEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val orden: Int,
    val estado: Boolean,
    val eventoId: String? = null,
    val eventoNombre: String? = null,
    val descripcion: String? = null
)

@Entity(
    tableName = "escaneos",
    primaryKeys = ["participanteId", "beneficioId"],
    indices = [
        Index(value = ["participanteId"], unique = false),
        Index(value = ["beneficioId"], unique = false),
        Index(value = ["pendienteSubida"], unique = false),
        Index(value = ["eventoId"], unique = false)
    ]
)
data class EscaneoEntity(
    val participanteId: String,
    val beneficioId: String,
    val eventoId: String = "",
    val entregado: Boolean = true,
    val timestampEscaneo: Long? = null,
    val sincronizadoConBackend: Boolean = false,
    val pendienteSubida: Boolean = false,
    val scannedByStaffId: String? = null,
    val scannedByStaffNombre: String? = null,
    val scannedByDeviceId: String? = null,
    val syncError: String? = null,
    val conflicto: Boolean = false,
    val conflictoStaffOriginal: String? = null,
    val conflictoFechaOriginal: Long? = null,
    val conflictoMensaje: String? = null
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey
    val clave: String,
    val valor: String
)

data class ParticipanteConEscaneosRelation(
    @Embedded
    val participante: ParticipanteEntity,
    @Relation(
        parentColumn = "idParticipante",
        entityColumn = "participanteId"
    )
    val escaneos: List<EscaneoEntity>
) {
    fun toParticipanteConBeneficios(): ParticipanteConBeneficios =
        ParticipanteConBeneficios(
            participante = participante,
            escaneos = escaneos.associateBy { it.beneficioId }
        )
}

data class BeneficioConteo(
    val beneficioId: String,
    val total: Int
)
