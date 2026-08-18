package com.example.ozareventscanner

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.ozareventscanner.data.AppDatabase
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.network.RetrofitClient
import com.example.ozareventscanner.sync.SyncWorker

/**
 * Application class de Ozar Event Scanner.
 *
 * Responsabilidades:
 *  1. Mantiene singletons de [AppDatabase] y [ParticipanteRepository] (inicialización lazy).
 *  2. Implementa [Configuration.Provider] para inyectar [SyncWorkerFactory] a WorkManager
 *     sin necesidad de Hilt/Dagger.
 *  3. Activa la sincronización periódica de fondo al iniciar la app.
 *
 * ## Cuando migres a Hilt
 * Reemplazar los `by lazy` por módulos `@Provides` y eliminar [SyncWorkerFactory],
 * usando `HiltWorkerFactory` en su lugar.
 */
class OzarApplication : Application(), Configuration.Provider {

    // ── Singletons de datos ───────────────────────────────────────────────────

    /** Base de datos Room — inicialización lazy, hilo-segura. */
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    /**
     * Repositorio central — único punto de acceso a datos en toda la app.
     * Comparte la misma instancia entre [EventoViewModel] y [SyncWorker].
     */
    val repository: ParticipanteRepository by lazy {
        ParticipanteRepository(
            dao        = database.participante(),
            apiService = RetrofitClient.apiService,
            context    = this
        )
    }

    // ── WorkManager con factory custom ────────────────────────────────────────

    /**
     * WorkManager usará esta configuración para instanciar workers.
     * Al implementar [Configuration.Provider], WorkManager detecta automáticamente
     * la configuración custom sin necesitar eliminar el inicializador del Manifest.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory(repository))
            .build()

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        // Activar sync periódica en background al arrancar (cada 15 min, solo con red)
        repository.programarSyncPeriodico()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Factory de workers — inyección sin Hilt
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Factory custom que provee [ParticipanteRepository] al [SyncWorker].
 *
 * Patrón: WorkManager llama a [createWorker] para instanciar cada worker.
 * Si el worker no es conocido → devolver null (WorkManager usa su factory por defecto).
 */
class SyncWorkerFactory(
    private val repository: ParticipanteRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SyncWorker::class.java.name ->
                SyncWorker(appContext, workerParameters, repository)

            // Devolver null para que WorkManager delegue a su factory por defecto
            else -> null
        }
    }
}
