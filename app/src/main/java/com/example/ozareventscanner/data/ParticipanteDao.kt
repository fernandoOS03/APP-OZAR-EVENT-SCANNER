package com.example.ozareventscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO de Room para operaciones sobre la tabla `participantes`.
 *
 * Operaciones agrupadas:
 *  ▸ CRUD básico: insertarParticipantes, buscarParticipante, obtenerTodos.
 *  ▸ Escaneo: actualizarBeneficio, marcarEscaneoLocal.
 *  ▸ Sincronización: obtenerPendientesSync, marcarLoteComoSincronizado,
 *                    revertirEscaneoRechazado, sincronizarDesdeBackend.
 */
@Dao
interface ParticipanteDao {

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarParticipantes(participantes: List<ParticipanteEntity>)

    @Query("SELECT * FROM participantes WHERE idParticipante = :id")
    suspend fun buscarParticipante(id: String): ParticipanteEntity?

    @Query("SELECT * FROM participantes ORDER BY nombre ASC")
    suspend fun obtenerTodos(): List<ParticipanteEntity>

    /** Busca participantes por nombre o ID/DNI (case-insensitive). */
    @Query("""
        SELECT * FROM participantes 
        WHERE LOWER(nombre) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(idParticipante) LIKE '%' || LOWER(:query) || '%' 
        ORDER BY nombre ASC
    """)
    suspend fun buscarPorNombreOId(query: String): List<ParticipanteEntity>

    // ── Escaneo ───────────────────────────────────────────────────────────────

    /** Actualiza solo el flag de beneficio (usado en flujos de corrección). */
    @Query("UPDATE participantes SET beneficioEntregado = :entregado WHERE idParticipante = :id")
    suspend fun actualizarBeneficio(id: String, entregado: Boolean)

    /**
     * Registra un escaneo QR local con timestamp.
     * Establece [beneficioEntregado] = true, [pendienteSubida] = true y guarda el timestamp.
     * Este es el primer paso del flujo offline-first antes de sincronizar con el backend.
     */
    @Query("""
        UPDATE participantes
        SET beneficioEntregado    = 1,
            timestampEscaneo      = :timestamp,
            pendienteSubida       = 1,
            sincronizadoConBackend = 0
        WHERE idParticipante = :id
    """)
    suspend fun marcarEscaneoLocal(id: String, timestamp: Long)

    // ── Sincronización ────────────────────────────────────────────────────────

    /**
     * Devuelve todos los registros con [pendienteSubida] = true.
     * Son los que se enviarán en el siguiente POST /sync/upload.
     */
    @Query("SELECT * FROM participantes WHERE pendienteSubida = 1")
    suspend fun obtenerPendientesSync(): List<ParticipanteEntity>

    /**
     * Marca un lote de participantes como sincronizados tras la confirmación del backend.
     * Se llama con los IDs devueltos en [SyncUploadResponse.resultados] donde validado = true.
     */
    @Query("""
        UPDATE participantes
        SET sincronizadoConBackend = 1,
            pendienteSubida        = 0
        WHERE idParticipante IN (:ids)
    """)
    suspend fun marcarLoteComoSincronizado(ids: List<String>)

    /**
     * Revierte el escaneo de un participante rechazado por el backend.
     * Ocurre en conflictos de doble escaneo: otro dispositivo escaneó antes (menor timestamp).
     * Resultado: [beneficioEntregado] = false, [pendienteSubida] = false.
     */
    @Query("""
        UPDATE participantes
        SET beneficioEntregado    = 0,
            timestampEscaneo      = NULL,
            pendienteSubida       = 0,
            sincronizadoConBackend = 1
        WHERE idParticipante = :id
    """)
    suspend fun revertirEscaneoRechazado(id: String)

    /**
     * Reemplaza Room con el estado canónico descargado del servidor (GET /sync/download).
     * Usa REPLACE para actualizar registros existentes y agregar nuevos.
     * Los registros del servidor tienen siempre [sincronizadoConBackend] = true.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sincronizarDesdeBackend(participantes: List<ParticipanteEntity>)
}