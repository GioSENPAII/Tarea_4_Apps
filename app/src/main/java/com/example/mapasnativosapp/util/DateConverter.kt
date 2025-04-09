package com.example.mapasnativosapp.util

import androidx.room.TypeConverter
import java.util.Date

/**
 * Conversor de tipos para Room que permite guardar y recuperar objetos Date
 * en la base de datos SQLite.
 */
class DateConverter {
    /**
     * Convierte un timestamp (Long) a un objeto Date.
     * @param value El timestamp almacenado en la base de datos.
     * @return Objeto Date correspondiente al timestamp, o null si el valor es nulo.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convierte un objeto Date a un timestamp (Long) para guardarlo en la base de datos.
     * @param date El objeto Date a convertir.
     * @return El timestamp correspondiente, o null si el objeto Date es nulo.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}