package com.example.ozareventscanner

import android.app.Application
import com.example.ozareventscanner.data.AppDatabase
import com.example.ozareventscanner.data.AuthRepository
import com.example.ozareventscanner.data.ParticipanteRepository
import com.example.ozareventscanner.data.SessionManager
import com.example.ozareventscanner.network.RetrofitClient
import com.example.ozareventscanner.sync.SyncWorker

/**
 * Application class de Ozar Event Scanner.
 */
class OzarApplication : Application() {

    /** Base de datos Room singleton. */
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    /** Gestor de sesión persistente. */
    val sessionManager: SessionManager by lazy {
        SessionManager(this)
    }

    /** Repositorio de autenticación con purga de datos. */
    val authRepository: AuthRepository by lazy {
        AuthRepository(
            apiService     = RetrofitClient.apiService,
            database       = database,
            sessionManager = sessionManager,
            context        = this
        )
    }

    /** Repositorio central de participantes y operaciones offline-first. */
    val repository: ParticipanteRepository by lazy {
        ParticipanteRepository(
            dao        = database.participante(),
            apiService = RetrofitClient.apiService,
            context    = this
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Programa la sincronización periódica silenciosa en segundo plano
        SyncWorker.programarSyncPeriodico(this)
    }
}
