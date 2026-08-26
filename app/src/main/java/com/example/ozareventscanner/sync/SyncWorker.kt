package com.example.ozareventscanner.sync

import android.content.Context
import androidx.work.*
import com.example.ozareventscanner.data.AppDatabase
import com.example.ozareventscanner.data.ParticipanteEntity
import com.example.ozareventscanner.network.EscaneoRequest
import com.example.ozareventscanner.network.RetrofitClient
import com.example.ozareventscanner.network.SyncUploadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker de sincronización 100% silenciosa y automática en segundo plano.
 *
 * Responsabilidades:
 *  1. Despierta cuando hay conexión a internet (`NetworkType.CONNECTED`).
 *  2. Toma los registros pendientes (`isSynced = false`).
 *  3. Los envía en lote (POST /sync/upload).
 *  4. Al confirmar, marca como `isSynced = true`.
 *  5. Ejecuta GET /sync/download para actualizar Room con el estado canónico del servidor.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getDatabase(appContext)
    private val dao = database.participante()
    private val apiService = RetrofitClient.apiService

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            // ── 1. SUBIDA EN LOTE (Upload de pendientes isSynced = false) ────────
            val pendientes = dao.obtenerPendientesSync()
            if (pendientes.isNotEmpty()) {
                val uploadRequest = SyncUploadRequest(
                    escaneos = pendientes.map { p ->
                        EscaneoRequest(
                            idParticipante = p.idParticipante,
                            timestampEscaneo = p.timestampEscaneo ?: System.currentTimeMillis()
                        )
                    }
                )

                val responseUpload = apiService.subirEscaneos(uploadRequest)

                val idsConfirmados = if (responseUpload.resultados.isEmpty()) {
                    pendientes.map { it.idParticipante }
                } else {
                    responseUpload.resultados
                        .filter { it.validado }
                        .map { it.idParticipante }
                }

                val idsRechazados = responseUpload.resultados
                    .filter { !it.validado }
                    .map { it.idParticipante }

                if (idsConfirmados.isNotEmpty()) {
                    dao.marcarLoteComoSincronizado(idsConfirmados)
                }
                idsRechazados.forEach { id ->
                    dao.revertirEscaneoRechazado(id)
                }
            }

            // ── 2. DESCARGA Y REFRESH (Download canónico del backend) ────────────
            val responseDownload = apiService.descargarEstado()
            if (responseDownload.participantes.isNotEmpty()) {
                val entidadesActualizadas = responseDownload.participantes.map { dto ->
                    ParticipanteEntity(
                        idParticipante = dto.idParticipante,
                        documento = dto.idParticipante,
                        nombre = dto.nombre,
                        beneficioEntregado = dto.beneficioEntregado,
                        timestampEscaneo = dto.timestampEscaneo,
                        isSynced = true
                    )
                }
                dao.sincronizarDesdeBackend(entidadesActualizadas)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (e.localizedMessage ?: "Fallo de sincronización")))
            }
        }
    }

    companion object {
        const val MAX_RETRY_COUNT = 3
        const val TAG_SYNC_SILENCIOSO = "ozar_silent_sync"

        /**
         * Encola la sincronización con WorkManager cuando hay conexión a internet.
         * Se ejecuta de forma 100% silenciosa en segundo plano.
         */
        fun encolarSyncSilencioso(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(TAG_SYNC_SILENCIOSO)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    TAG_SYNC_SILENCIOSO,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                )
        }

        /**
         * Programa la sincronización periódica cada 15 minutos en background.
         */
        fun programarSyncPeriodico(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG_SYNC_SILENCIOSO)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    TAG_SYNC_SILENCIOSO,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
        }
    }
}
