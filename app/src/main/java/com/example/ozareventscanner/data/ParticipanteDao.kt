package com.example.ozareventscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Room para operaciones sobre las tablas `participantes` y `escaneos`.
 * Optimizado para rendimiento crítico con 10,000+ registros.
 */
@Dao
interface ParticipanteDao {

    // ── CRUD y Consultas de Participantes ──────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarParticipantes(participantes: List<ParticipanteEntity>)

    @Query("SELECT * FROM participantes WHERE idParticipante = :id")
    suspend fun buscarParticipante(id: String): ParticipanteEntity?

    @Query("SELECT * FROM participantes ORDER BY nombre ASC")
    suspend fun obtenerTodos(): List<ParticipanteEntity>

    @Query("SELECT * FROM participantes ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<ParticipanteEntity>>

    @Query("SELECT COUNT(*) FROM participantes")
    fun observarTotalParticipantes(): Flow<Int>

    /**
     * Búsqueda optimizada por índice.
     * En SQLite, 'documento LIKE :query || %' o 'idParticipante LIKE :query || %' aprovecha
     * directamente el índice B-Tree en O(log N) para búsquedas numéricas de DNI/ID.
     * 'nombre LIKE % || :query || %' filtra por coincidencia textual sobre resultados indexados.
     */
    @Query("""
        SELECT * FROM participantes 
        WHERE (:query = '' 
           OR documento LIKE :query || '%' 
           OR idParticipante LIKE :query || '%' 
           OR nombre LIKE '%' || :query || '%')
        ORDER BY nombre ASC
    """)
    fun buscarParticipantesFlow(query: String): Flow<List<ParticipanteEntity>>

    @Query("""
        SELECT * FROM participantes 
        WHERE LOWER(nombre) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(documento) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(idParticipante) LIKE '%' || LOWER(:query) || '%' 
        ORDER BY nombre ASC
    """)
    suspend fun buscarPorNombreOId(query: String): List<ParticipanteEntity>

    // ── Escaneo y Entrega Manual (Offline-First) ──────────────────────────────

    @Query("""
        UPDATE participantes
        SET beneficioEntregado    = 1,
            timestampEscaneo      = :timestamp,
            isSynced              = 0,
            pendienteSubida       = 1,
            sincronizadoConBackend = 0
        WHERE idParticipante = :id
    """)
    suspend fun marcarEscaneoLocal(id: String, timestamp: Long = System.currentTimeMillis())

    // ── Escaneos Individuales ──────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEscaneo(escaneo: EscaneoEntity): Long

    @Query("SELECT * FROM escaneos")
    fun observarTodosLosEscaneos(): Flow<List<EscaneoEntity>>

    @Query("SELECT * FROM escaneos WHERE idParticipante = :idParticipante")
    suspend fun obtenerEscaneosPorParticipante(idParticipante: String): List<EscaneoEntity>

    @Query("SELECT * FROM escaneos WHERE isSynced = 0")
    suspend fun obtenerEscaneosPendientes(): List<EscaneoEntity>

    @Query("UPDATE escaneos SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun marcarEscaneosSincronizados(ids: List<Long>)

    // ── Sincronización ────────────────────────────────────────────────────────

    @Query("SELECT * FROM participantes WHERE isSynced = 0 OR pendienteSubida = 1")
    suspend fun obtenerPendientesSync(): List<ParticipanteEntity>

    @Query("""
        UPDATE participantes
        SET isSynced               = 1,
            sincronizadoConBackend = 1,
            pendienteSubida        = 0
        WHERE idParticipante IN (:ids)
    """)
    suspend fun marcarLoteComoSincronizado(ids: List<String>)

    @Query("""
        UPDATE participantes
        SET beneficioEntregado     = 0,
            timestampEscaneo       = NULL,
            isSynced               = 1,
            pendienteSubida        = 0,
            sincronizadoConBackend = 1
        WHERE idParticipante = :id
    """)
    suspend fun revertirEscaneoRechazado(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sincronizarDesdeBackend(participantes: List<ParticipanteEntity>)

    // ── Purga Total de Datos ──────────────────────────────────────────────────

    @Query("DELETE FROM participantes")
    suspend fun vaciarParticipantes()

    @Query("DELETE FROM escaneos")
    suspend fun vaciarEscaneos()
}