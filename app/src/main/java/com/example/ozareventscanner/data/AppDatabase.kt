package com.example.ozareventscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ParticipanteEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun participante(): ParticipanteDao
    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ozar_event_database").build()
                INSTANCE = instance
                instance
            }
        }
    }
}