package com.example.mapasnativosapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mapasnativosapp.model.PointOfInterest

/**
 * Data Access Object (DAO) para la entidad PointOfInterest.
 * Proporciona métodos para interactuar con la tabla de puntos de interés en la base de datos.
 */
@Dao
interface PointOfInterestDao {

    /**
     * Inserta un nuevo punto de interés en la base de datos.
     * @param poi El punto de interés a insertar.
     * @return El ID asignado al nuevo punto de interés.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poi: PointOfInterest): Long

    /**
     * Actualiza un punto de interés existente en la base de datos.
     * @param poi El punto de interés con los datos actualizados.
     */
    @Update
    suspend fun update(poi: PointOfInterest)

    /**
     * Elimina un punto de interés de la base de datos.
     * @param poi El punto de interés a eliminar.
     */
    @Delete
    suspend fun delete(poi: PointOfInterest)

    /**
     * Obtiene todos los puntos de interés.
     * @return LiveData con una lista de todos los puntos de interés.
     */
    @Query("SELECT * FROM points_of_interest ORDER BY createdAt DESC")
    fun getAllPointsOfInterest(): LiveData<List<PointOfInterest>>

    /**
     * Obtiene un punto de interés específico por su ID.
     * @param id El ID del punto de interés.
     * @return El punto de interés solicitado.
     */
    @Query("SELECT * FROM points_of_interest WHERE id = :id")
    suspend fun getPointOfInterestById(id: Long): PointOfInterest?

    /**
     * Obtiene todos los puntos de interés de una categoría específica.
     * @param category La categoría a buscar.
     * @return LiveData con una lista de puntos de interés de la categoría especificada.
     */
    @Query("SELECT * FROM points_of_interest WHERE category = :category ORDER BY createdAt DESC")
    fun getPointsOfInterestByCategory(category: String): LiveData<List<PointOfInterest>>

    /**
     * Obtiene todos los puntos de interés dentro de un radio específico.
     * Utiliza la fórmula de Haversine para calcular la distancia.
     * @param lat Latitud del centro del círculo.
     * @param lng Longitud del centro del círculo.
     * @param radiusKm Radio en kilómetros.
     * @return Lista de puntos de interés dentro del radio especificado.
     */
    @Query("""
        SELECT * FROM points_of_interest WHERE 
        (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * 
        cos(radians(longitude) - radians(:lng)) + 
        sin(radians(:lat)) * sin(radians(latitude)))) 
        <= :radiusKm
    """)
    suspend fun getPointsOfInterestWithinRadius(lat: Double, lng: Double, radiusKm: Double): List<PointOfInterest>

    /**
     * Busca puntos de interés que contengan el texto de búsqueda en su nombre o descripción.
     * @param searchQuery Texto a buscar.
     * @return LiveData con una lista de puntos de interés que coinciden con la búsqueda.
     */
    @Query("SELECT * FROM points_of_interest WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%'")
    fun searchPointsOfInterest(searchQuery: String): LiveData<List<PointOfInterest>>

    /**
     * Marca un punto de interés como visitado.
     * @param id El ID del punto de interés.
     * @param visited Estado de visita (true/false).
     */
    @Query("UPDATE points_of_interest SET visited = :visited WHERE id = :id")
    suspend fun updateVisitedStatus(id: Long, visited: Boolean)
}