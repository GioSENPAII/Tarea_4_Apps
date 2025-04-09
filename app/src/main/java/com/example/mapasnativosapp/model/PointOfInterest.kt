package com.example.mapasnativosapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Clase que representa un punto de interés personalizado
 */
@Entity(tableName = "points_of_interest")
data class PointOfInterest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val category: String = "general",
    val imageUri: String? = null,
    val visited: Boolean = false,
    val createdAt: Date = Date(),
    val notes: String = ""
)

/**
 * Enumeración con las categorías predefinidas para los puntos de interés
 */
enum class PoiCategory(val displayName: String) {
    FAVORITE("Favorito"),
    TO_VISIT("Por visitar"),
    RESTAURANT("Restaurante"),
    MONUMENT("Monumento"),
    PARK("Parque"),
    SHOPPING("Compras"),
    EVENT("Evento"),
    RECOMMENDED("Recomendado"),
    OTHER("Otro")
}

/**
 * Clase que representa una zona explorable en el mapa
 */
data class ExplorationZone(
    val id: String,
    val name: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Int,
    val discovered: Boolean = false,
    val type: ZoneType = ZoneType.NORMAL
)

/**
 * Tipos de zonas explorables
 */
enum class ZoneType {
    NORMAL,
    PREMIUM,
    SPECIAL_EVENT
}