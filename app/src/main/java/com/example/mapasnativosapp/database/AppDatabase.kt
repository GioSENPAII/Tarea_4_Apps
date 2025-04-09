package com.example.mapasnativosapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mapasnativosapp.model.PointOfInterest
import com.example.mapasnativosapp.util.DateConverter

/**
 * Clase principal de la base de datos Room.
 * Define las entidades y versiones de la base de datos.
 */
@Database(entities = [PointOfInterest::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Proporciona acceso al DAO de puntos de interés.
     */
    abstract fun pointOfInterestDao(): PointOfInterestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene una instancia singleton de la base de datos.
         * Si la instancia no existe, la crea.
         * @param context Contexto de la aplicación.
         * @return Instancia de la base de datos.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "maps_app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}