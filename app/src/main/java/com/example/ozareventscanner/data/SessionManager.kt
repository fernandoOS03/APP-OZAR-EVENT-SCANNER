package com.example.ozareventscanner.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ozareventscanner.network.EventoConfigDto
import com.google.gson.Gson

/**
 * Gestor de preferencias de sesión local.
 * Garantiza la regla de negocio 1-a-1: un operador tiene acceso a un único evento por sesión.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ozar_session_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun guardarSesion(token: String, staffNombre: String, evento: EventoConfigDto) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_STAFF_NOMBRE, staffNombre)
            .putString(KEY_EVENTO_CONFIG, gson.toJson(evento))
            .putBoolean(KEY_SESION_ACTIVA, true)
            .apply()
    }

    fun obtenerToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun obtenerStaffNombre(): String = prefs.getString(KEY_STAFF_NOMBRE, "Operador Staff") ?: "Operador Staff"

    fun obtenerEventoConfig(): EventoConfigDto? {
        val json = prefs.getString(KEY_EVENTO_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, EventoConfigDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun haySesionActiva(): Boolean = prefs.getBoolean(KEY_SESION_ACTIVA, false) && !obtenerToken().isNullOrBlank()

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "key_auth_token"
        private const val KEY_STAFF_NOMBRE = "key_staff_nombre"
        private const val KEY_EVENTO_CONFIG = "key_evento_config"
        private const val KEY_SESION_ACTIVA = "key_sesion_activa"
    }
}
