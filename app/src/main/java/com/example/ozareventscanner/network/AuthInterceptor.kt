package com.example.ozareventscanner.network

import com.example.ozareventscanner.data.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de OkHttp que inyecta automáticamente la cabecera
 * "Authorization: Bearer <token>" en las peticiones que lo requieran.
 */
class AuthInterceptor(private val sessionManagerProvider: () -> SessionManager?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Si la petición ya tiene Authorization o es el endpoint de login, no alteramos
        if (originalRequest.header("Authorization") != null ||
            originalRequest.url.encodedPath.contains("api/scanner/auth")) {
            return chain.proceed(originalRequest)
        }

        val token = sessionManagerProvider()?.obtenerToken()
        val requestBuilder = originalRequest.newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
