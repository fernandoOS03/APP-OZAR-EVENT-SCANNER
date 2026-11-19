package com.example.ozareventscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ParticipanteEntity::class,
        BeneficioEntity::class,
        EscaneoEntity::class,
        SyncMetaEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun participante(): ParticipanteDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE participantes ADD COLUMN timestampEscaneo INTEGER")
                db.execSQL("ALTER TABLE participantes ADD COLUMN sincronizadoConBackend INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE participantes ADD COLUMN pendienteSubida INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE participantes ADD COLUMN documento TEXT")
                db.execSQL("ALTER TABLE participantes ADD COLUMN correo TEXT")
            }
        }

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE participantes RENAME TO participantes_legacy")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS participantes (
                        idParticipante TEXT NOT NULL,
                        nombre TEXT NOT NULL,
                        documento TEXT,
                        correo TEXT,
                        PRIMARY KEY(idParticipante)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_participantes_documento ON participantes(documento)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_participantes_correo ON participantes(correo)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS beneficios (
                        id TEXT NOT NULL,
                        nombre TEXT NOT NULL,
                        orden INTEGER NOT NULL,
                        estado INTEGER NOT NULL,
                        eventoId TEXT,
                        eventoNombre TEXT,
                        descripcion TEXT,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_beneficios_orden ON beneficios(orden)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS escaneos (
                        participanteId TEXT NOT NULL,
                        beneficioId TEXT NOT NULL,
                        entregado INTEGER NOT NULL,
                        timestampEscaneo INTEGER,
                        sincronizadoConBackend INTEGER NOT NULL,
                        pendienteSubida INTEGER NOT NULL,
                        scannedByStaffId TEXT,
                        scannedByStaffNombre TEXT,
                        scannedByDeviceId TEXT,
                        syncError TEXT,
                        PRIMARY KEY(participanteId, beneficioId)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_escaneos_participanteId ON escaneos(participanteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_escaneos_beneficioId ON escaneos(beneficioId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_escaneos_pendienteSubida ON escaneos(pendienteSubida)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_meta (
                        clave TEXT NOT NULL,
                        valor TEXT NOT NULL,
                        PRIMARY KEY(clave)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO participantes (idParticipante, nombre, documento, correo)
                    SELECT idParticipante, nombre, COALESCE(documento, idParticipante), correo
                    FROM participantes_legacy
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO escaneos (
                        participanteId, beneficioId, entregado, timestampEscaneo,
                        sincronizadoConBackend, pendienteSubida,
                        scannedByStaffId, scannedByStaffNombre, scannedByDeviceId, syncError
                    )
                    SELECT
                        idParticipante, beneficioId, 1, timestampEscaneo,
                        COALESCE(sincronizadoConBackend, 0), COALESCE(pendienteSubida, 0),
                        scannedByStaffId, scannedByStaffNombre, scannedByDeviceId, NULL
                    FROM participantes_legacy
                    WHERE beneficioEntregado = 1
                      AND beneficioId IS NOT NULL
                      AND LENGTH(beneficioId) = 36
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE participantes ADD COLUMN eventoId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_participantes_eventoId ON participantes(eventoId)")

                db.execSQL("ALTER TABLE escaneos ADD COLUMN eventoId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE escaneos ADD COLUMN conflicto INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE escaneos ADD COLUMN conflictoStaffOriginal TEXT")
                db.execSQL("ALTER TABLE escaneos ADD COLUMN conflictoFechaOriginal INTEGER")
                db.execSQL("ALTER TABLE escaneos ADD COLUMN conflictoMensaje TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_escaneos_eventoId ON escaneos(eventoId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_participantes_nombre ON participantes(nombre)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ozar_event_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
