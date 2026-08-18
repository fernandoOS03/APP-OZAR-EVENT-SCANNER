package com.example.ozareventscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos Room de la aplicación.
 *
 * Historial de versiones:
 *  v1 → Esquema inicial: idParticipante, nombre, beneficioEntregado.
 *  v2 → Agrega campos de sincronización: timestampEscaneo, sincronizadoConBackend, pendienteSubida.
 */
@Database(
    entities     = [ParticipanteEntity::class],
    version      = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun participante(): ParticipanteDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ── Migraciones ───────────────────────────────────────────────────────

        /**
         * Migración v1 → v2.
         * Agrega las 3 columnas de sincronización sin destruir datos existentes.
         *  - timestampEscaneo      : INTEGER nullable (null = sin escaneo)
         *  - sincronizadoConBackend: INTEGER NOT NULL DEFAULT 0 (false)
         *  - pendienteSubida       : INTEGER NOT NULL DEFAULT 0 (false)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE participantes ADD COLUMN timestampEscaneo INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE participantes ADD COLUMN sincronizadoConBackend INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE participantes ADD COLUMN pendienteSubida INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // ── Singleton ─────────────────────────────────────────────────────────

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ozar_event_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}