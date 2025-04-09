package com.example.mapasnativosapp.repository

import androidx.lifecycle.LiveData
import com.example.mapasnativosapp.database.PointOfInterestDao
import com.example.mapasnativosapp.model.PointOfInterest

/**
 * Repositorio que proporciona una capa de abstracción para acceder
 * a los datos de puntos de interés desde diferentes fuentes (Room, API, etc.)
 */
class PointOfInterestRepository(private val pointOfInterestDao: PointOfInterestDao) {

    /**
     * Obtiene todos los puntos de interés almacenados en la base de datos.
     * @return LiveData con una lista de todos los puntos de interés.
     */
    fun getAllPointsOfInterest(): LiveData<List<PointOfInterest>> {
        return pointOfInterestDao.getAllPointsOfInterest()
    }

    /**
     * Inserta un nuevo punto de interés en la base de datos.
     * @param poi El punto de interés a insertar.
     * @return El ID asignado al nuevo punto de interés.
     */
    suspend fun insertPointOfInterest(poi: PointOfInterest): Long {
        return pointOfInterestDao.insert(poi)
    }

    /**
     * Actualiza un punto de interés existente en la base de datos.
     * @param poi El punto de interés con los datos actualizados.
     */
    suspend fun updatePointOfInterest(poi: PointOfInterest) {
        pointOfInterestDao.update(poi)
    }

    /**
     * Elimina un punto de interés de la base de datos.
     * @param poi El punto de interés a eliminar.
     */
    suspend fun deletePointOfInterest(poi: PointOfInterest) {
        pointOfInterestDao.delete(poi)
    }

    /**
     * Obtiene un punto de interés específico por su ID.
     * @param id El ID del punto de interés.
     * @return El punto de interés solicitado.
     */
    suspend fun getPointOfInterestById(id: Long): PointOfInterest? {
        return pointOfInterestDao.getPointOfInterestById(id)
    }

    /**
     * Obtiene todos los puntos de interés de una categoría específica.
     * @param category La categoría a buscar.
     * @return LiveData con una lista de puntos de interés de la categoría especificada.
     */
    fun getPointsOfInterestByCategory(category: String): LiveData<List<PointOfInterest>> {
        return pointOfInterestDao.getPointsOfInterestByCategory(category)
    }

    /**
     * Obtiene todos los puntos de interés dentro de un radio específico.
     * @param lat Latitud del centro del círculo.
     * @param lng Longitud del centro del círculo.
     * @param radiusKm Radio en kilómetros.
     * @return Lista de puntos de interés dentro del radio especificado.
     */
    suspend fun getPointsOfInterestWithinRadius(lat: Double, lng: Double, radiusKm: Double): List<PointOfInterest> {
        return pointOfInterestDao.getPointsOfInterestWithinRadius(lat, lng, radiusKm)
    }

    /**
     * Busca puntos de interés que contengan el texto de búsqueda en su nombre o descripción.
     * @param searchQuery Texto a buscar.
     * @return LiveData con una lista de puntos de interés que coinciden con la búsqueda.
     */
    fun searchPointsOfInterest(searchQuery: String): LiveData<List<PointOfInterest>> {
        return pointOfInterestDao.searchPointsOfInterest(searchQuery)
    }

    /**
     * Marca un punto de interés como visitado.
     * @param id El ID del punto de interés.
     * @param visited Estado de visita (true/false).
     */
    suspend fun updateVisitedStatus(id: Long, visited: Boolean) {
        pointOfInterestDao.updateVisitedStatus(id, visited)
    }
}