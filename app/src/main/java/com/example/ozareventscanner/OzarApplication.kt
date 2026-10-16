package com.example.ozareventscanner

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.ozareventscanner.data.AppDatabase
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.SessionManager
import com.example.ozareventscanner.network.RetrofitClient
import com.example.ozareventscanner.sync.SyncWorker
import kotlinx.coroutines.launch

/**
 * Application class de Ozar Events Scanner.
 */
class OzarApplication : Application(), Configuration.Provider {

    // ── Singletons de datos ───────────────────────────────────────────────────

    val sessionManager: SessionManager by lazy {
        SessionManager.getInstance(this)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val repository: ParticipanteRepository by lazy {
        ParticipanteRepository(
            dao        = database.participante(),
            apiService = RetrofitClient.apiService,
            context    = this
        )
    }

    // ── WorkManager con factory custom ────────────────────────────────────────

    private val applicationScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory(repository))
            .build()

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        // Vincular SessionManager a RetrofitClient para inyección del JWT
        RetrofitClient.sessionManager = sessionManager

        // Programar sync periódica con WorkManager (15 min) para respaldo en background
        repository.programarSyncPeriodico()

        // 1. Monitoreo reactivo de red: sincronizar automáticamente cada vez que se recupere conexión
        iniciarMonitoreoDeRed()
    }

    private fun iniciarMonitoreoDeRed() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return

        var conexionPrevia = repository.hayConexion()

        val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                // Se detectó conexión; si venía de estar desconectado o al reconectar, sincronizar pendientes
                applicationScope.launch {
                    try {
                        kotlinx.coroutines.delay(1000) // Breve estabilización de red
                        if (repository.hayConexion()) {
                            repository.sincronizarCompleto()
                        }
                    } catch (e: Exception) {
                        // Respaldo con WorkManager si falla llamada directa
                        repository.programarSyncUnica()
                    }
                }
                conexionPrevia = true
            }

            override fun onLost(network: android.net.Network) {
                conexionPrevia = false
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Fallback: programar sync única condicionada por WorkManager
            repository.programarSyncUnica()
        }
    }
}


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
            else -> null
        }
    }
}
