package com.example.ozareventscanner.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ozareventscanner.network.*
import com.example.ozareventscanner.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Resultado de un intento de escaneo QR.
 */
sealed class ScanResultado {
    object FormatoInvalido : ScanResultado()
    object NoEncontrado : ScanResultado()
    data class YaEscaneado(val nombre: String) : ScanResultado()
    data class Valido(val nombre: String) : ScanResultado()
    data class Error(val mensaje: String) : ScanResultado()
}

/** Estadísticas de sincronización. */
data class SyncStats(
    val subidos: Int = 0,
    val descargados: Int = 0,
    val rechazados: Int = 0
)

/**
 * Repositorio central de participantes y operaciones offline-first.
 * Optimizado para streaming asíncrono con Flow y consultas indexadas para 10k+ registros.
 */
class ParticipanteRepository(
    private val dao: ParticipanteDao,
    private val apiService: ApiService,
    private val context: Context
) {

    fun observarParticipantes(): Flow<List<ParticipanteEntity>> = dao.observarTodos()

    fun buscarParticipantesFlow(query: String): Flow<List<ParticipanteEntity>> = dao.buscarParticipantesFlow(query)

    fun observarTotalParticipantes(): Flow<Int> = dao.observarTotalParticipantes()

    fun observarEscaneos(): Flow<List<EscaneoEntity>> = dao.observarTodosLosEscaneos()

    suspend fun obtenerEscaneosDeParticipante(id: String): List<EscaneoEntity> = withContext(Dispatchers.IO) {
        dao.obtenerEscaneosPorParticipante(id)
    }

    private fun esIdParticipanteValido(rawQr: String): Boolean {
        if (rawQr.isBlank() || rawQr.length > 100) return false
        if (rawQr.startsWith("http://") || rawQr.startsWith("https://")) return false
        if (rawQr.startsWith("{") || rawQr.startsWith("[")) return false
        if (rawQr.contains(" ")) return false
        return true
    }

    /**
     * Registra un escaneo QR aplicando la regla Offline-First:
     * 1. Persistencia inmediata en Room con isSynced = false (éxito instantáneo en UI).
     * 2. Encola el SyncWorker silencioso con restricción de red conectada.
     */
    suspend fun registrarEscaneo(rawQrValue: String, idBeneficio: String = ""): ScanResultado = withContext(Dispatchers.IO) {
        if (!esIdParticipanteValido(rawQrValue)) {
            return@withContext ScanResultado.FormatoInvalido
        }

        return@withContext try {
            val participante = dao.buscarParticipante(rawQrValue)
                ?: return@withContext ScanResultado.NoEncontrado

            if (participante.beneficioEntregado) {
                return@withContext ScanResultado.YaEscaneado(participante.nombre)
            }

            val timestamp = System.currentTimeMillis()
            dao.marcarEscaneoLocal(rawQrValue, timestamp)
            dao.insertarEscaneo(
                EscaneoEntity(
                    idParticipante = rawQrValue,
                    idBeneficio = idBeneficio,
                    timestampEscaneo = timestamp,
                    isSynced = false
                )
            )

            // Encola sincronización automática silenciosa
            SyncWorker.encolarSyncSilencioso(context)

            ScanResultado.Valido(participante.nombre)
        } catch (e: Exception) {
            ScanResultado.Error("Error al procesar escaneo: ${e.localizedMessage}")
        }
    }

    /** Busca participantes localmente en Room por Nombre o DNI. */
    suspend fun buscarParticipantes(query: String = ""): List<ParticipanteEntity> = withContext(Dispatchers.IO) {
        val queryLimpia = query.trim()
        if (queryLimpia.isBlank()) {
            dao.obtenerTodos()
        } else {
            dao.buscarPorNombreOId(queryLimpia)
        }
    }

    /**
     * Entrega manual de beneficio (Contingencia para QR dañado).
     * Guarda inmediatamente en Room con isSynced = false y encola SyncWorker.
     */
    suspend fun entregarBeneficioManual(idParticipante: String, idBeneficio: String = ""): ScanResultado = withContext(Dispatchers.IO) {
        return@withContext try {
            val participante = dao.buscarParticipante(idParticipante)
                ?: return@withContext ScanResultado.NoEncontrado

            val timestamp = System.currentTimeMillis()
            dao.marcarEscaneoLocal(idParticipante, timestamp)
            dao.insertarEscaneo(
                EscaneoEntity(
                    idParticipante = idParticipante,
                    idBeneficio = idBeneficio,
                    timestampEscaneo = timestamp,
                    isSynced = false
                )
            )

            // Encola sincronización automática silenciosa
            SyncWorker.encolarSyncSilencioso(context)

            ScanResultado.Valido(participante.nombre)
        } catch (e: Exception) {
            ScanResultado.Error("Error al validar entrega: ${e.localizedMessage}")
        }
    }

    suspend fun registrarCodigoRemoto(codigo: String): String = withContext(Dispatchers.IO) {
        val respuesta = apiService.registrarParticipante(CodigoRequest(codigo.trim()))
        val entidad = ParticipanteEntity(
            idParticipante = respuesta.idParticipante,
            documento = respuesta.idParticipante,
            nombre = respuesta.nombre,
            beneficioEntregado = respuesta.beneficioEntregado,
            timestampEscaneo = null,
            isSynced = true
        )
        dao.insertarParticipantes(listOf(entidad))
        return@withContext respuesta.nombre
    }

    /**
     * Comprueba la conectividad de red activa.
     */
    fun hayConexion(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
