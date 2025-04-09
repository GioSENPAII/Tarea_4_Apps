package com.example.mapasnativosapp.exploration

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import com.example.mapasnativosapp.model.ExplorationZone
import com.example.mapasnativosapp.model.ZoneType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Sistema que gestiona el descubrimiento de zonas en el mapa.
 * Permite al usuario explorar nuevas áreas y registra su progreso.
 */
class ExplorationSystem(context: Context) {

    private val TAG = "ExplorationSystem"
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "exploration_prefs", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    // Lista de zonas predefinidas
    private var allZones: MutableList<ExplorationZone> = loadZones()

    // Zonas que el usuario ha descubierto
    private var discoveredZones: MutableSet<String> = loadDiscoveredZones()

    // Progreso total de exploración (0-100%)
    private var explorationProgress: Int = calculateProgress()

    /**
     * Carga las zonas guardadas o crea zonas predeterminadas si no existen.
     * @return Lista de zonas.
     */
    private fun loadZones(): MutableList<ExplorationZone> {
        val zonesJson = prefs.getString(KEY_ZONES, null)
        return if (zonesJson != null) {
            try {
                val type = object : TypeToken<List<ExplorationZone>>() {}.type
                gson.fromJson(zonesJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar zonas: ${e.message}")
                createDefaultZones()
            }
        } else {
            createDefaultZones()
        }
    }

    /**
     * Crea zonas predeterminadas para la Ciudad de México.
     * @return Lista de zonas predeterminadas.
     */
    private fun createDefaultZones(): MutableList<ExplorationZone> {
        // Zonas de ejemplo para Ciudad de México
        val zones = mutableListOf(
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Zócalo",
                centerLatitude = 19.4326,
                centerLongitude = -99.1332,
                radiusMeters = 500,
                discovered = false,
                type = ZoneType.NORMAL
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Chapultepec",
                centerLatitude = 19.4200,
                centerLongitude = -99.1807,
                radiusMeters = 1000,
                discovered = false,
                type = ZoneType.NORMAL
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Coyoacán",
                centerLatitude = 19.3500,
                centerLongitude = -99.1629,
                radiusMeters = 800,
                discovered = false,
                type = ZoneType.NORMAL
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Bellas Artes",
                centerLatitude = 19.4352,
                centerLongitude = -99.1413,
                radiusMeters = 300,
                discovered = false,
                type = ZoneType.NORMAL
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Reforma",
                centerLatitude = 19.4256,
                centerLongitude = -99.1582,
                radiusMeters = 1200,
                discovered = false,
                type = ZoneType.PREMIUM
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Polanco",
                centerLatitude = 19.4333,
                centerLongitude = -99.1992,
                radiusMeters = 900,
                discovered = false,
                type = ZoneType.NORMAL
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Condesa",
                centerLatitude = 19.4128,
                centerLongitude = -99.1737,
                radiusMeters = 700,
                discovered = false,
                type = ZoneType.NORMAL
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Roma",
                centerLatitude = 19.4178,
                centerLongitude = -99.1654,
                radiusMeters = 700,
                discovered = false,
                type = ZoneType.NORMAL
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Ciudad Universitaria",
                centerLatitude = 19.3321,
                centerLongitude = -99.1870,
                radiusMeters = 1500,
                discovered = false,
                type = ZoneType.PREMIUM
            ),
            ExplorationZone(
                id = UUID.randomUUID().toString(),
                name = "Xochimilco",
                centerLatitude = 19.2576,
                centerLongitude = -99.1044,
                radiusMeters = 2000,
                discovered = false,
                type = ZoneType.SPECIAL_EVENT
            )
        )

        // Guardar las zonas predeterminadas
        saveZones(zones)
        return zones
    }

    /**
     * Carga las IDs de las zonas descubiertas.
     * @return Conjunto de IDs de zonas descubiertas.
     */
    private fun loadDiscoveredZones(): MutableSet<String> {
        return prefs.getStringSet(KEY_DISCOVERED_ZONES, mutableSetOf()) ?: mutableSetOf()
    }

    /**
     * Calcula el progreso total de exploración.
     * @return Porcentaje de zonas descubiertas (0-100).
     */
    private fun calculateProgress(): Int {
        if (allZones.isEmpty()) return 0
        return (discoveredZones.size * 100) / allZones.size
    }

    /**
     * Guarda las zonas en las preferencias.
     * @param zones Lista de zonas a guardar.
     */
    private fun saveZones(zones: List<ExplorationZone>) {
        val zonesJson = gson.toJson(zones)
        prefs.edit().putString(KEY_ZONES, zonesJson).apply()
    }

    /**
     * Guarda las IDs de zonas descubiertas en las preferencias.
     */
    private fun saveDiscoveredZones() {
        prefs.edit().putStringSet(KEY_DISCOVERED_ZONES, discoveredZones).apply()
    }

    /**
     * Comprueba si la ubicación actual está dentro de alguna zona no descubierta.
     * Si es así, marca la zona como descubierta y actualiza el progreso.
     * @param currentLat Latitud actual.
     * @param currentLng Longitud actual.
     * @return Lista de zonas recién descubiertas.
     */
    fun checkForDiscoveries(currentLat: Double, currentLng: Double): List<ExplorationZone> {
        val newlyDiscoveredZones = mutableListOf<ExplorationZone>()

        for (zone in allZones) {
            if (!discoveredZones.contains(zone.id)) {
                // Calcular distancia a la zona
                val distance = calculateDistance(
                    currentLat, currentLng,
                    zone.centerLatitude, zone.centerLongitude
                )

                // Si está dentro del radio, marcarla como descubierta
                if (distance <= zone.radiusMeters) {
                    discoveredZones.add(zone.id)

                    // Actualizar la zona en la lista
                    val updatedZone = zone.copy(discovered = true)
                    val index = allZones.indexOf(zone)
                    if (index != -1) {
                        allZones[index] = updatedZone
                    }

                    newlyDiscoveredZones.add(updatedZone)
                }
            }
        }

        if (newlyDiscoveredZones.isNotEmpty()) {
            // Guardar cambios
            saveZones(allZones)
            saveDiscoveredZones()

            // Recalcular progreso
            explorationProgress = calculateProgress()
        }

        return newlyDiscoveredZones
    }

    /**
     * Calcula la distancia en metros entre dos coordenadas usando
     * la fórmula de Haversine.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Obtiene todas las zonas.
     * @return Lista de todas las zonas.
     */
    fun getAllZones(): List<ExplorationZone> {
        return allZones
    }

    /**
     * Obtiene las zonas descubiertas.
     * @return Lista de zonas descubiertas.
     */
    fun getDiscoveredZones(): List<ExplorationZone> {
        return allZones.filter { it.id in discoveredZones }
    }

    /**
     * Obtiene las zonas no descubiertas.
     * @return Lista de zonas no descubiertas.
     */
    fun getUndiscoveredZones(): List<ExplorationZone> {
        return allZones.filter { it.id !in discoveredZones }
    }

    /**
     * Obtiene el progreso total de exploración.
     * @return Porcentaje de progreso (0-100).
     */
    fun getExplorationProgress(): Int {
        return explorationProgress
    }

    /**
     * Sugiere zonas cercanas para explorar basándose en la ubicación actual.
     * @param currentLat Latitud actual.
     * @param currentLng Longitud actual.
     * @param maxCount Número máximo de zonas a sugerir.
     * @return Lista de zonas sugeridas, ordenadas por cercanía.
     */
    fun suggestNearbyZones(currentLat: Double, currentLng: Double, maxCount: Int = 3): List<ExplorationZone> {
        // Filtrar zonas no descubiertas
        val undiscoveredZones = getUndiscoveredZones()

        // Calcular distancia a cada zona
        val zonesWithDistance = undiscoveredZones.map { zone ->
            val distance = calculateDistance(
                currentLat, currentLng,
                zone.centerLatitude, zone.centerLongitude
            )
            Pair(zone, distance)
        }

        // Ordenar por distancia y limitar al número máximo
        return zonesWithDistance
            .sortedBy { it.second }
            .take(maxCount)
            .map { it.first }
    }

    /**
     * Añade una nueva zona personalizada.
     * @param name Nombre de la zona.
     * @param latitude Latitud central.
     * @param longitude Longitud central.
     * @param radiusMeters Radio en metros.
     * @param type Tipo de zona.
     * @return La zona creada.
     */
    fun addCustomZone(name: String, latitude: Double, longitude: Double, radiusMeters: Int, type: ZoneType = ZoneType.NORMAL): ExplorationZone {
        val newZone = ExplorationZone(
            id = UUID.randomUUID().toString(),
            name = name,
            centerLatitude = latitude,
            centerLongitude = longitude,
            radiusMeters = radiusMeters,
            discovered = false,
            type = type
        )

        allZones.add(newZone)
        saveZones(allZones)
        return newZone
    }

    companion object {
        private const val KEY_ZONES = "exploration_zones"
        private const val KEY_DISCOVERED_ZONES = "discovered_zones"
    }
}