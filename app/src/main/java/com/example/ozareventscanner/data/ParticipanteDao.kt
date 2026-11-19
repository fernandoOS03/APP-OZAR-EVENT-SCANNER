package com.example.ozareventscanner.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipanteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarParticipantes(participantes: List<ParticipanteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarBeneficios(beneficios: List<BeneficioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEscaneos(escaneos: List<EscaneoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEscaneo(escaneo: EscaneoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMeta(meta: SyncMetaEntity)

    @Query("""
        SELECT * FROM participantes
        WHERE idParticipante = :id
           OR (documento IS NOT NULL AND documento = :id)
           OR (correo IS NOT NULL AND LOWER(correo) = LOWER(:id))
        LIMIT 1
    """)
    suspend fun buscarParticipante(id: String): ParticipanteEntity?

    @Query("SELECT * FROM participantes ORDER BY nombre ASC")
    suspend fun obtenerTodos(): List<ParticipanteEntity>

    @Transaction
    @Query("SELECT * FROM participantes ORDER BY nombre ASC")
    fun pagingTodos(): PagingSource<Int, ParticipanteConEscaneosRelation>

    @Transaction
    @Query("""
        SELECT * FROM participantes
        WHERE nombre LIKE :prefix || '%' COLLATE NOCASE
           OR (documento IS NOT NULL AND documento LIKE :prefix || '%' COLLATE NOCASE)
           OR (correo IS NOT NULL AND correo LIKE :prefix || '%' COLLATE NOCASE)
           OR idParticipante LIKE :prefix || '%' COLLATE NOCASE
        ORDER BY nombre ASC
    """)
    fun pagingBuscarPorPrefijo(prefix: String): PagingSource<Int, ParticipanteConEscaneosRelation>

    @Query("""
        SELECT beneficioId, COUNT(*) AS total
        FROM escaneos
        WHERE entregado = 1 AND syncError IS NULL
        GROUP BY beneficioId
    """)
    fun observarConteoPorBeneficio(): Flow<List<BeneficioConteo>>

    @Query("SELECT COUNT(DISTINCT participanteId) FROM escaneos WHERE entregado = 1 AND syncError IS NULL")
    fun observarTotalParticipantesEscaneados(): Flow<Int>

    @Query("SELECT * FROM escaneos WHERE entregado = 1 AND syncError IS NULL")
    fun observarEscaneosEntregados(): Flow<List<EscaneoEntity>>

    @Query("SELECT COUNT(*) FROM participantes")
    suspend fun contarTotalParticipantes(): Int

    @Query("SELECT COUNT(*) FROM participantes")
    fun observarTotalParticipantes(): Flow<Int>

    @Query("""
        SELECT * FROM participantes
        WHERE LOWER(nombre) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(idParticipante) LIKE '%' || LOWER(:query) || '%'
           OR (documento IS NOT NULL AND LOWER(documento) LIKE '%' || LOWER(:query) || '%')
           OR (correo IS NOT NULL AND LOWER(correo) LIKE '%' || LOWER(:query) || '%')
        ORDER BY nombre ASC
    """)
    suspend fun buscarPorNombreOId(query: String): List<ParticipanteEntity>

    @Query("SELECT * FROM escaneos")
    suspend fun obtenerTodosEscaneos(): List<EscaneoEntity>

    @Query("SELECT * FROM escaneos")
    fun observarEscaneos(): Flow<List<EscaneoEntity>>

    @Query("SELECT * FROM escaneos WHERE participanteId = :participanteId")
    suspend fun obtenerEscaneosDeParticipante(participanteId: String): List<EscaneoEntity>

    @Query("SELECT * FROM escaneos WHERE participanteId IN (:participanteIds)")
    suspend fun obtenerEscaneosDeParticipantes(participanteIds: List<String>): List<EscaneoEntity>

    @Query("SELECT * FROM escaneos WHERE participanteId = :participanteId AND beneficioId = :beneficioId LIMIT 1")
    suspend fun obtenerEscaneo(participanteId: String, beneficioId: String): EscaneoEntity?

    @Query("""
        SELECT * FROM escaneos
        WHERE pendienteSubida = 1 AND syncError IS NULL
        ORDER BY timestampEscaneo ASC
    """)
    suspend fun obtenerPendientesSync(): List<EscaneoEntity>

    @Query("""
        SELECT * FROM escaneos
        WHERE pendienteSubida = 1
          AND syncError IS NULL
          AND (eventoId = :eventoId OR eventoId = '')
          AND (:staffId IS NULL OR scannedByStaffId IS NULL OR scannedByStaffId = :staffId)
        ORDER BY timestampEscaneo ASC
        LIMIT :limit
    """)
    suspend fun obtenerPendientesSyncDeSesion(
        eventoId: String,
        staffId: String?,
        limit: Int
    ): List<EscaneoEntity>

    @Query("""
        SELECT COUNT(*) FROM escaneos
        WHERE pendienteSubida = 1
          AND syncError IS NULL
          AND (eventoId = :eventoId OR eventoId = '')
          AND (:staffId IS NULL OR scannedByStaffId IS NULL OR scannedByStaffId = :staffId)
    """)
    suspend fun contarPendientesSyncDeSesion(eventoId: String, staffId: String?): Int

    @Query("SELECT COUNT(*) FROM escaneos WHERE pendienteSubida = 1 AND syncError IS NULL")
    fun observarPendientesSync(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM escaneos
        WHERE pendienteSubida = 1
          AND syncError IS NULL
          AND (eventoId = :eventoId OR eventoId = '')
          AND (:staffId IS NULL OR scannedByStaffId IS NULL OR scannedByStaffId = :staffId)
    """)
    fun observarPendientesSyncDeSesion(eventoId: String, staffId: String?): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM escaneos
        WHERE pendienteSubida = 1
          AND syncError IS NULL
          AND (
              (eventoId != '' AND eventoId != :eventoId)
              OR (:staffId IS NOT NULL AND scannedByStaffId IS NOT NULL AND scannedByStaffId != :staffId)
          )
    """)
    fun observarPendientesOtrosUsuarios(eventoId: String, staffId: String?): Flow<Int>

    @Query("SELECT COUNT(*) FROM escaneos WHERE conflicto = 1 AND (eventoId = :eventoId OR eventoId = '')")
    fun observarConflictosCount(eventoId: String): Flow<Int>

    @Query("SELECT * FROM escaneos WHERE conflicto = 1 AND (eventoId = :eventoId OR eventoId = '') ORDER BY timestampEscaneo DESC")
    suspend fun obtenerConflictos(eventoId: String): List<EscaneoEntity>

    @Query("""
        UPDATE escaneos
        SET sincronizadoConBackend = 1,
            pendienteSubida = 0,
            syncError = NULL,
            conflicto = 0,
            timestampEscaneo = :timestamp
        WHERE participanteId = :participanteId AND beneficioId = :beneficioId
    """)
    suspend fun marcarEscaneoSincronizado(
        participanteId: String,
        beneficioId: String,
        timestamp: Long?
    )

    @Query("""
        UPDATE escaneos
        SET entregado = 1,
            sincronizadoConBackend = 1,
            pendienteSubida = 0,
            syncError = NULL,
            timestampEscaneo = COALESCE(:timestampOriginal, timestampEscaneo),
            scannedByStaffNombre = COALESCE(:staffOriginalNombre, scannedByStaffNombre),
            conflicto = 1,
            conflictoStaffOriginal = :staffOriginalNombre,
            conflictoFechaOriginal = :timestampOriginal,
            conflictoMensaje = :mensaje
        WHERE participanteId = :participanteId AND beneficioId = :beneficioId
    """)
    suspend fun marcarEscaneoConflictoYaEntregado(
        participanteId: String,
        beneficioId: String,
        timestampOriginal: Long?,
        staffOriginalNombre: String?,
        mensaje: String?
    )

    @Query("""
        UPDATE escaneos
        SET pendienteSubida = 0,
            sincronizadoConBackend = 0,
            syncError = :error
        WHERE participanteId = :participanteId AND beneficioId = :beneficioId
    """)
    suspend fun marcarEscaneoConError(
        participanteId: String,
        beneficioId: String,
        error: String
    )

    @Query("""
        DELETE FROM escaneos
        WHERE participanteId = :participanteId
          AND beneficioId = :beneficioId
          AND pendienteSubida = 0
          AND sincronizadoConBackend = 1
    """)
    suspend fun borrarEscaneoConfirmado(participanteId: String, beneficioId: String)

    @Query("""
        DELETE FROM participantes
        WHERE eventoId != :eventoIdActual
          AND idParticipante NOT IN (
              SELECT DISTINCT participanteId FROM escaneos WHERE pendienteSubida = 1
          )
    """)
    suspend fun limpiarParticipantesSincronizadosDeOtrosEventos(eventoIdActual: String)

    @Query("DELETE FROM escaneos WHERE eventoId != :eventoIdActual AND pendienteSubida = 0")
    suspend fun limpiarEscaneosSincronizadosDeOtrosEventos(eventoIdActual: String)

    @Query("DELETE FROM beneficios WHERE eventoId IS NOT NULL AND eventoId != :eventoIdActual")
    suspend fun limpiarBeneficiosDeOtrosEventos(eventoIdActual: String)

    @Query("SELECT * FROM beneficios ORDER BY orden ASC")
    suspend fun obtenerBeneficios(): List<BeneficioEntity>

    @Query("SELECT * FROM beneficios WHERE id = :id LIMIT 1")
    suspend fun obtenerBeneficioPorId(id: String): BeneficioEntity?

    @Query("SELECT * FROM beneficios WHERE estado = 1 ORDER BY orden ASC")
    suspend fun obtenerBeneficiosActivos(): List<BeneficioEntity>

    @Query("SELECT * FROM beneficios WHERE estado = 1 ORDER BY orden ASC")
    fun observarBeneficiosActivos(): Flow<List<BeneficioEntity>>

    @Query("""
        DELETE FROM participantes
        WHERE idParticipante IN (:ids)
          AND idParticipante NOT IN (
              SELECT DISTINCT participanteId FROM escaneos WHERE pendienteSubida = 1
          )
    """)
    suspend fun eliminarParticipantesSinPendientes(ids: List<String>)

    @Transaction
    suspend fun aplicarPaginaDescarga(
        beneficios: List<BeneficioEntity>,
        participantesActivos: List<ParticipanteEntity>,
        idsEliminados: List<String>,
        escaneosUpsert: List<EscaneoEntity>,
        escaneosBorrar: List<Pair<String, String>>
    ) {
        if (beneficios.isNotEmpty()) {
            insertarBeneficios(beneficios)
        }
        if (participantesActivos.isNotEmpty()) {
            insertarParticipantes(participantesActivos)
        }
        if (idsEliminados.isNotEmpty()) {
            idsEliminados.chunked(500).forEach { eliminarParticipantesSinPendientes(it) }
        }
        if (escaneosUpsert.isNotEmpty()) {
            upsertEscaneos(escaneosUpsert)
        }
        escaneosBorrar.forEach { (participanteId, beneficioId) ->
            borrarEscaneoConfirmado(participanteId, beneficioId)
        }
    }

    @Query("SELECT valor FROM sync_meta WHERE clave = :clave LIMIT 1")
    suspend fun obtenerMeta(clave: String): String?
}
