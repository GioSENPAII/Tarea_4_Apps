package com.example.mapasnativosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mapasnativosapp.database.AppDatabase
import com.example.mapasnativosapp.model.PointOfInterest
import com.example.mapasnativosapp.repository.PointOfInterestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel que gestiona los datos de los puntos de interés y proporciona
 * métodos para interactuar con ellos desde la interfaz de usuario.
 */
class PointOfInterestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PointOfInterestRepository
    val allPointsOfInterest: LiveData<List<PointOfInterest>>
    private val _currentPoi = MutableLiveData<PointOfInterest?>()
    val currentPoi: LiveData<PointOfInterest?> = _currentPoi

    // Flag para indicar si se está editando un punto existente
    private val _isEditing = MutableLiveData<Boolean>(false)
    val isEditing: LiveData<Boolean> = _isEditing

    init {
        val pointOfInterestDao = AppDatabase.getDatabase(application).pointOfInterestDao()
        repository = PointOfInterestRepository(pointOfInterestDao)
        allPointsOfInterest = repository.getAllPointsOfInterest()
    }

    /**
     * Obtiene todos los puntos de interés de una categoría específica.
     * @param category La categoría a buscar.
     * @return LiveData con una lista de puntos de interés de la categoría especificada.
     */
    fun getPointsOfInterestByCategory(category: String): LiveData<List<PointOfInterest>> {
        return repository.getPointsOfInterestByCategory(category)
    }

    /**
     * Busca puntos de interés que contengan el texto de búsqueda en su nombre o descripción.
     * @param searchQuery Texto a buscar.
     * @return LiveData con una lista de puntos de interés que coinciden con la búsqueda.
     */
    fun searchPointsOfInterest(searchQuery: String): LiveData<List<PointOfInterest>> {
        return repository.searchPointsOfInterest(searchQuery)
    }

    /**
     * Inserta un nuevo punto de interés en la base de datos.
     * @param poi El punto de interés a insertar.
     */
    fun insertPointOfInterest(poi: PointOfInterest) = viewModelScope.launch {
        repository.insertPointOfInterest(poi)
    }

    /**
     * Actualiza un punto de interés existente en la base de datos.
     * @param poi El punto de interés con los datos actualizados.
     */
    fun updatePointOfInterest(poi: PointOfInterest) = viewModelScope.launch {
        repository.updatePointOfInterest(poi)
    }

    /**
     * Elimina un punto de interés de la base de datos.
     * @param poi El punto de interés a eliminar.
     */
    fun deletePointOfInterest(poi: PointOfInterest) = viewModelScope.launch {
        repository.deletePointOfInterest(poi)
    }

    /**
     * Establece el punto de interés actual para edición o visualización.
     * @param poi El punto de interés a establecer como actual.
     * @param isEditing Indica si se está editando (true) o solo visualizando (false).
     */
    fun setCurrentPoi(poi: PointOfInterest?, isEditing: Boolean = false) {
        _currentPoi.value = poi
        _isEditing.value = isEditing
    }

    /**
     * Carga un punto de interés por su ID.
     * @param id El ID del punto de interés.
     */
    fun loadPointOfInterestById(id: Long) = viewModelScope.launch {
        val poi = withContext(Dispatchers.IO) {
            repository.getPointOfInterestById(id)
        }
        _currentPoi.value = poi
    }

    /**
     * Obtiene todos los puntos de interés dentro de un radio específico.
     * @param lat Latitud del centro del círculo.
     * @param lng Longitud del centro del círculo.
     * @param radiusKm Radio en kilómetros.
     * @return Lista de puntos de interés dentro del radio especificado.
     */
    suspend fun getPointsOfInterestWithinRadius(lat: Double, lng: Double, radiusKm: Double): List<PointOfInterest> {
        return withContext(Dispatchers.IO) {
            repository.getPointsOfInterestWithinRadius(lat, lng, radiusKm)
        }
    }

    /**
     * Marca un punto de interés como visitado.
     * @param id El ID del punto de interés.
     * @param visited Estado de visita (true/false).
     */
    fun updateVisitedStatus(id: Long, visited: Boolean) = viewModelScope.launch {
        repository.updateVisitedStatus(id, visited)
    }

    /**
     * Limpia el punto de interés actual.
     */
    fun clearCurrentPoi() {
        _currentPoi.value = null
        _isEditing.value = false
    }
}