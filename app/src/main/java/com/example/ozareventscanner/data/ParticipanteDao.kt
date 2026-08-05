package com.example.ozareventscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ParticipanteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarParticipantes(participantes:List<ParticipanteEntity>)

    @Query("SELECT * FROM participantes WHERE idParticipante = :id")
    suspend fun buscarParticipante(id: String): ParticipanteEntity?


}