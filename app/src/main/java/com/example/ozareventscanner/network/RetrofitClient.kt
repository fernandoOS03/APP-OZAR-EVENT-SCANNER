package com.example.ozareventscanner.network

import com.example.ozareventscanner.data.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit configurado para Ozar Events Scanner.
 *
 * Incluye:
 *  - [AuthInterceptor] para adjuntar el JWT token automáticamente.
 *  - Timeouts explícitos para evitar bloqueos en redes inestables de eventos.
 */
object RetrofitClient {

    private const val URL_BASE = "https://api-eventos.zendov.com/"

    var sessionManager: SessionManager? = null

    val apiService: ApiService by lazy {
        val authInterceptor = AuthInterceptor { sessionManager }

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
