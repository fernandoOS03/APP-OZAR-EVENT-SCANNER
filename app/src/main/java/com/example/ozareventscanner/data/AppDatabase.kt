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
 *  v3 → Agrega columnas documento y correo para identificación flexible y escaneo sin inventar.
 *  v4 → Agrega índices en documento y correo (<10ms) y columnas de trazabilidad offline (scannedBy*, beneficioId).
 */
@Database(
    entities     = [ParticipanteEntity::class],
    version      = 4,
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
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE participantes ADD COLUMN timestampEscaneo INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE participantes ADD COLUMN sincronizadoConBackend INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE participantes ADD COLUMN pendienteSubida INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migración v2 → v3.
         * Agrega columnas documento y correo para validación fidedigna de QR.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE participantes ADD COLUMN documento TEXT")
                db.execSQL("ALTER TABLE participantes ADD COLUMN correo TEXT")
            }
        }

        /**
         * Migración v3 → v4.
         * Agrega índices en documento y correo para búsquedas O(1)/log y columnas de auditoría offline.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_participantes_documento ON participantes(documento)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_participantes_correo ON participantes(correo)")
                db.execSQL("ALTER TABLE participantes ADD COLUMN scannedByStaffId TEXT")
                db.execSQL("ALTER TABLE participantes ADD COLUMN scannedByStaffNombre TEXT")
                db.execSQL("ALTER TABLE participantes ADD COLUMN scannedByDeviceId TEXT")
                db.execSQL("ALTER TABLE participantes ADD COLUMN beneficioId TEXT")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}