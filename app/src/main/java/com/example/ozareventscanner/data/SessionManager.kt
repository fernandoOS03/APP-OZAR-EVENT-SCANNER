package com.example.ozareventscanner.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ozareventscanner.network.StaffDataDto
import com.google.gson.Gson
import java.util.UUID

/**
 * Gestor de sesión persistente para el Staff de Ozar Events Scanner.
 * Guarda el token JWT, datos del operador, evento asignado e ID único de dispositivo.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val devicePrefs: SharedPreferences = context.getSharedPreferences(DEVICE_PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME        = "ozar_scanner_session"
        private const val DEVICE_PREF_NAME = "ozar_scanner_device"
        private const val KEY_TOKEN        = "key_jwt_token"
        private const val KEY_STAFF_DATA   = "key_staff_data"
        private const val KEY_DEVICE_ID    = "key_device_uuid"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /** Devuelve un UUID único y persistente para este dispositivo (sobrevive al cierre de sesión). */
    fun obtenerDeviceId(): String {
        val existente = devicePrefs.getString(KEY_DEVICE_ID, null)
        if (!existente.isNullOrBlank()) return existente
        val nuevo = UUID.randomUUID().toString()
        devicePrefs.edit().putString(KEY_DEVICE_ID, nuevo).apply()
        return nuevo
    }

    /** Guarda la sesión tras un login exitoso. */
    fun guardarSesion(token: String, staff: StaffDataDto) {
        val staffJson = gson.toJson(staff)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_STAFF_DATA, staffJson)
            .apply()
    }

    /** Obtiene el token JWT actual o null si no hay sesión activa. */
    fun obtenerToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /** Obtiene los datos del Staff autenticado o null si no ha iniciado sesión. */
    fun obtenerStaff(): StaffDataDto? {
        val json = prefs.getString(KEY_STAFF_DATA, null) ?: return null
        return try {
            gson.fromJson(json, StaffDataDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** Devuelve el eventoId del evento actualmente asignado al staff. */
    fun obtenerEventoId(): String? {
        return obtenerStaff()?.evento?.eventoId
    }

    /** Devuelve el staffId del operador actualmente autenticado. */
    fun obtenerStaffId(): String? {
        return obtenerStaff()?.staffId
    }

    /** Verifica si hay una sesión válida con token. */
    fun estaAutenticado(): Boolean {
        val token = obtenerToken()
        return !token.isNullOrBlank()
    }

    /** Limpia los datos de sesión (Logout). */
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}
