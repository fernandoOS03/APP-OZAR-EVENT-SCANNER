package com.example.ozareventscanner.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.ozareventscanner.data.ParticipanteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker de sincronización Room ↔ Backend.
 *
 * Modos:
 *  - [MODE_UPLOAD]: Sube únicamente los escaneos pendientes de la sesión activa en bucle
 *    hasta vaciar la cola (disparado al escanear o al recuperar red).
 *  - [MODE_FULL]: Sube pendientes y descarga el estado del servidor (periódico cada 15 min).
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ParticipanteRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val mode = inputData.getString(KEY_MODE) ?: MODE_UPLOAD

        return@withContext try {
            when (mode) {
                MODE_UPLOAD -> {
                    val uploadResult = repository.subirSoloPendientes()
                    if (uploadResult.isSuccess) {
                        val stats = uploadResult.getOrThrow()
                        Result.success(
                            workDataOf(
                                "subidos" to stats.subidos,
                                "conflictos" to stats.conflictos,
                                "rechazados" to stats.rechazados
                            )
                        )
                    } else {
                        manejarFallo(uploadResult.exceptionOrNull())
                    }
                }
                MODE_DOWNLOAD -> {
                    val downloadResult = repository.descargarSoloCambios()
                    if (downloadResult.isSuccess) {
                        Result.success(workDataOf("descargados" to downloadResult.getOrThrow()))
                    } else {
                        manejarFallo(downloadResult.exceptionOrNull())
                    }
                }
                else -> {
                    val syncResult = repository.sincronizarCompleto()
                    if (syncResult.isSuccess) {
                        val stats = syncResult.getOrThrow()
                        Result.success(
                            workDataOf(
                                "subidos" to stats.subidos,
                                "descargados" to stats.descargados,
                                "conflictos" to stats.conflictos,
                                "rechazados" to stats.rechazados
                            )
                        )
                    } else {
                        manejarFallo(syncResult.exceptionOrNull())
                    }
                }
            }
        } catch (e: Exception) {
            manejarFallo(e)
        }
    }

    private fun manejarFallo(excepcion: Throwable?): Result {
        return if (runAttemptCount < MAX_RETRY_COUNT) {
            Result.retry()
        } else {
            Result.failure(
                workDataOf("error" to (excepcion?.localizedMessage ?: "Error desconocido"))
            )
        }
    }

    companion object {
        const val MAX_RETRY_COUNT = 3
        const val KEY_MODE = "sync_mode"
        const val MODE_UPLOAD = "upload"
        const val MODE_DOWNLOAD = "download"
        const val MODE_FULL = "full"

        const val TAG_SYNC_UNICA = "ozar_sync_unica_pendiente"
        const val TAG_SYNC_PERIODICA = "ozar_sync_periodica"
    }
}
