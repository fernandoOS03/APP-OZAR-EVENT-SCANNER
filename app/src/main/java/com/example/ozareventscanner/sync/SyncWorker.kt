package com.example.ozareventscanner.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.ozareventscanner.data.ParticipanteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker de sincronización bidireccional Room ↔ Backend.
 *
 * Ejecutado por WorkManager en segundo plano cuando:
 *  - El usuario escanea un QR sin conectividad (sync diferida).
 *  - El scheduler periódico cada 15 min detecta registros pendientes.
 *
 * Flujo interno:
 *  1. Sube todos los registros con [pendienteSubida] = true (con timestamps).
 *  2. Descarga el estado canónico del servidor.
 *  3. Actualiza Room con los datos del servidor.
 *
 * Reintentos:
 *  - Máximo [MAX_RETRY_COUNT] intentos con backoff exponencial.
 *  - Configurado en [ParticipanteRepository.programarSyncUnica].
 *
 * Inyección de dependencias:
 *  - El [ParticipanteRepository] se inyecta via [SyncWorkerFactory] (sin Hilt/Dagger).
 *  - Ver [OzarApplication] para la configuración del WorkManager.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ParticipanteRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val syncResult = repository.sincronizarCompleto()

            if (syncResult.isSuccess) {
                val stats = syncResult.getOrThrow()
                Result.success(
                    workDataOf(
                        "subidos"     to stats.subidos,
                        "descargados" to stats.descargados,
                        "rechazados"  to stats.rechazados
                    )
                )
            } else {
                val excepcion = syncResult.exceptionOrNull()
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf("error" to (excepcion?.localizedMessage ?: "Error desconocido"))
                    )
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.localizedMessage))
            }
        }
    }

    companion object {
        /** Máximo número de reintentos antes de marcar el worker como fallido. */
        const val MAX_RETRY_COUNT = 3

        /** Nombre único para el OneTimeWorkRequest de sync pendiente. */
        const val TAG_SYNC_UNICA     = "ozar_sync_unica_pendiente"

        /** Nombre único para el PeriodicWorkRequest de sync cada 15 min. */
        const val TAG_SYNC_PERIODICA = "ozar_sync_periodica"
    }
}
