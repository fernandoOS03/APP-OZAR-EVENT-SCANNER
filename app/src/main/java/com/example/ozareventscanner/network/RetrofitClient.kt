package com.example.ozareventscanner.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton Retrofit.
 *
 * ⚠️ URL_BASE debe actualizarse cuando el backend esté disponible:
 *   - Emulador Android  → "http://10.0.2.2:8000/"
 *   - Dispositivo físico → "http://192.168.X.X:8000/"
 *   - Producción         → "https://api.tu-dominio.com/"
 */
object RetrofitClient {

    // TODO : Reemplazar esta url con el backend
    private const val URL_BASE = "http://10.0.2.2:8000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
