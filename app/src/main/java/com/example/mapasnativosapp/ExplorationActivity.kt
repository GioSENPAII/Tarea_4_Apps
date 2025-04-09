package com.example.mapasnativosapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mapasnativosapp.exploration.ExplorationSystem
import com.example.mapasnativosapp.model.ExplorationZone
import com.example.mapasnativosapp.model.PoiCategory
import com.example.mapasnativosapp.model.PointOfInterest
import com.example.mapasnativosapp.viewmodel.PointOfInterestViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

class ExplorationActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var fabAddPoi: FloatingActionButton
    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var btnAddPoi: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var explorationSystem: ExplorationSystem
    private lateinit var poiViewModel: PointOfInterestViewModel

    private var currentLatitude: Double = 19.4326  // Default to CDMX
    private var currentLongitude: Double = -99.1332
    private var isTrackingLocation = false

    private val gson = Gson()

    // Registro de solicitud de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startLocationTracking()
        } else {
            Toast.makeText(
                this,
                "Los permisos de ubicación son necesarios para la exploración",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exploration)

        // Inicializar vistas
        webView = findViewById(R.id.webViewExploration)
        progressBar = findViewById(R.id.progressBarExploration)
        progressText = findViewById(R.id.textViewProgress)
        fabAddPoi = findViewById(R.id.fabAddPoi)
        bottomSheet = findViewById(R.id.bottomSheet)
        btnAddPoi = findViewById(R.id.btnAddPoi)

        // Configurar BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Inicializar sistemas
        explorationSystem = ExplorationSystem(this)
        poiViewModel = ViewModelProvider(this)[PointOfInterestViewModel::class.java]

        // Configurar ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationRequest()

        // Configurar WebView
        setupWebView()

        // Actualizar progreso inicial
        updateProgressDisplay()

        // Configurar botones
        setupButtons()

        // Verificar permisos
        checkLocationPermission()
    }

    private fun setupButtons() {
        fabAddPoi.setOnClickListener {
            showBottomSheet()
        }

        btnAddPoi.setOnClickListener {
            addCurrentLocationAsPoi()
        }
    }

    private fun showBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun addCurrentLocationAsPoi() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poi, null)
        val etName = dialogView.findViewById<EditText>(R.id.etPoiName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etPoiDescription)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar punto de interés")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString()
                val description = etDescription.text.toString()

                if (name.isNotEmpty()) {
                    val newPoi = PointOfInterest(
                        name = name,
                        description = description,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        category = PoiCategory.FAVORITE.name,
                        visited = true
                    )

                    poiViewModel.insertPointOfInterest(newPoi)
                    Toast.makeText(this, "Punto de interés guardado", Toast.LENGTH_SHORT).show()
                    hideBottomSheet()

                    // Actualizar el mapa con el nuevo punto
                    addPoiToMap(newPoi)
                } else {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                hideBottomSheet()
            }
            .create()

        dialog.show()
    }

    private fun setupWebView() {
        // Configurar WebView con JavaScript habilitado
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Añadir interfaz JavaScript
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Configurar WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Cargar puntos de interés en el mapa después de que cargue
                loadPointsOfInterestToMap()

                // Cargar zonas de exploración
                loadExplorationZonesToMap()
            }
        }

        // Cargar el mapa inicial
        loadMap(currentLatitude, currentLongitude)
    }

    private fun loadMap(latitude: Double, longitude: Double) {
        // Crear HTML con Leaflet.js para mostrar el mapa con funcionalidades avanzadas
        val mapHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin: 0; padding: 0; }
                    #map { position: absolute; top: 0; bottom: 0; width: 100%; height: 100%; }
                    .custom-popup .leaflet-popup-content-wrapper {
                        background: rgba(255, 255, 255, 0.9);
                        border-radius: 12px;
                    }
                    .zone-circle {
                        stroke-dasharray: 10, 10;
                        animation: dash 20s linear infinite;
                    }
                    @keyframes dash {
                        to {
                            stroke-dashoffset: 1000;
                        }
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    // Inicializar mapa
                    var map = L.map('map').setView([$latitude, $longitude], 15);
                    
                    // Capa base de OpenStreetMap
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
                        maxZoom: 19
                    }).addTo(map);
                    
                    // Marcador de posición actual
                    var currentPositionMarker = L.marker([$latitude, $longitude], {
                        icon: L.icon({
                            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
                            iconSize: [25, 41],
                            iconAnchor: [12, 41],
                            popupAnchor: [1, -34]
                        })
                    }).addTo(map);
                    currentPositionMarker.bindPopup("<b>Mi ubicación</b>").openPopup();
                    
                    // Capas para diferentes tipos de POIs
                    var poisLayer = L.layerGroup().addTo(map);
                    var zonesLayer = L.layerGroup().addTo(map);
                    
                    // Función para actualizar la ubicación actual
                    function updateCurrentPosition(lat, lng) {
                        currentPositionMarker.setLatLng([lat, lng]);
                        map.setView([lat, lng], map.getZoom());
                    }
                    
                    // Función para agregar un POI al mapa
                    function addPointOfInterest(poi) {
                        var marker = L.marker([poi.latitude, poi.longitude], {
                            icon: L.icon({
                                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
                                iconSize: [25, 41],
                                iconAnchor: [12, 41],
                                popupAnchor: [1, -34]
                            })
                        });
                        
                        var popupContent = '<div class="custom-popup"><h3>' + poi.name + '</h3>';
                        if (poi.description) {
                            popupContent += '<p>' + poi.description + '</p>';
                        }
                        popupContent += '<p><strong>Categoría:</strong> ' + poi.category + '</p>';
                        if (poi.visited) {
                            popupContent += '<p><span style="color: green;">✓ Visitado</span></p>';
                        }
                        popupContent += '</div>';
                        
                        marker.bindPopup(popupContent);
                        marker.addTo(poisLayer);
                        
                        return marker;
                    }
                    
                    // Función para agregar una zona de exploración
                    function addExplorationZone(zone) {
                        var color = zone.discovered ? 'green' : 'orange';
                        if (zone.type === 'PREMIUM') {
                            color = zone.discovered ? 'blue' : 'purple';
                        } else if (zone.type === 'SPECIAL_EVENT') {
                            color = zone.discovered ? 'gold' : 'red';
                        }
                        
                        var circle = L.circle([zone.centerLatitude, zone.centerLongitude], {
                            radius: zone.radiusMeters,
                            color: color,
                            fillColor: color,
                            fillOpacity: 0.2,
                            weight: 2,
                            className: 'zone-circle'
                        });
                        
                        var popupContent = '<div class="custom-popup"><h3>' + zone.name + '</h3>';
                        popupContent += '<p><strong>Estado:</strong> ' + (zone.discovered ? 'Descubierta' : 'Por descubrir') + '</p>';
                        popupContent += '<p><strong>Tipo:</strong> ' + zone.type + '</p>';
                        popupContent += '</div>';
                        
                        circle.bindPopup(popupContent);
                        circle.addTo(zonesLayer);
                        
                        return circle;
                    }
                    
                    // Función para limpiar las capas
                    function clearLayers() {
                        poisLayer.clearLayers();
                        zonesLayer.clearLayers();
                    }
                    
                    // Detectar clic largo en el mapa para añadir POIs
                    var pressTimer;
                    var startLatLng;
                    
                    map.on('mousedown', function(e) {
                        startLatLng = e.latlng;
                        pressTimer = window.setTimeout(function() {
                            Android.onMapLongClick(startLatLng.lat, startLatLng.lng);
                        }, 1000);
                    });
                    
                    map.on('mouseup', function() {
                        clearTimeout(pressTimer);
                    });
                    
                    map.on('click', function() {
                        clearTimeout(pressTimer);
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        // Cargar el HTML en el WebView
        webView.loadDataWithBaseURL(
            "https://openstreetmap.org",
            mapHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun loadPointsOfInterestToMap() {
        // Observar los POIs del ViewModel y agregarlos al mapa
        poiViewModel.allPointsOfInterest.observe(this) { pois ->
            val poisJson = gson.toJson(pois)
            val jsCode = """
                poisLayer.clearLayers();
                var pois = $poisJson;
                for (var i = 0; i < pois.length; i++) {
                    addPointOfInterest(pois[i]);
                }
            """.trimIndent()

            webView.evaluateJavascript(jsCode, null)
        }
    }

    private fun loadExplorationZonesToMap() {
        val zones = explorationSystem.getAllZones()
        val zonesJson = gson.toJson(zones)
        val jsCode = """
            zonesLayer.clearLayers();
            var zones = $zonesJson;
            for (var i = 0; i < zones.length; i++) {
                addExplorationZone(zones[i]);
            }
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }

    private fun addPoiToMap(poi: PointOfInterest) {
        val poiJson = gson.toJson(poi)
        val jsCode = "addPointOfInterest($poiJson);"
        webView.evaluateJavascript(jsCode, null)
    }

    private fun updateCurrentPositionOnMap() {
        val jsCode = "updateCurrentPosition($currentLatitude, $currentLongitude);"
        webView.evaluateJavascript(jsCode, null)
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    // Actualizar la posición en el mapa
                    updateCurrentPositionOnMap()

                    // Comprobar si se ha descubierto alguna zona
                    checkForNewDiscoveries()
                }
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                startLocationTracking()
            }
            else -> {
                // Solicitar permisos
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        if (!isTrackingLocation) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            isTrackingLocation = true

            // Obtener la posición inicial
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                    updateCurrentPositionOnMap()
                }
            }
        }
    }

    private fun stopLocationTracking() {
        if (isTrackingLocation) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTrackingLocation = false
        }
    }

    private fun checkForNewDiscoveries() {
        val newDiscoveries = explorationSystem.checkForDiscoveries(currentLatitude, currentLongitude)

        if (newDiscoveries.isNotEmpty()) {
            // Actualizar el mapa con las nuevas zonas descubiertas
            loadExplorationZonesToMap()

            // Actualizar la barra de progreso
            updateProgressDisplay()

            // Mostrar notificación de descubrimiento
            showDiscoveryNotification(newDiscoveries)
        }
    }

    private fun updateProgressDisplay() {
        val progress = explorationSystem.getExplorationProgress()
        progressBar.progress = progress
        progressText.text = "$progress% explorado"
    }

    private fun showDiscoveryNotification(discoveries: List<ExplorationZone>) {
        val zoneNames = discoveries.joinToString(", ") { it.name }
        val message = "¡Has descubierto ${discoveries.size} nueva(s) zona(s): $zoneNames!"

        val dialog = AlertDialog.Builder(this)
            .setTitle("¡Nueva zona descubierta!")
            .setMessage(message)
            .setPositiveButton("¡Genial!") { _, _ -> }
            .create()

        dialog.show()
    }

    // Interfaz para comunicación JavaScript-Android
    inner class WebAppInterface {
        @JavascriptInterface
        fun onMapLongClick(latitude: Double, longitude: Double) {
            runOnUiThread {
                showAddPoiDialog(latitude, longitude)
            }
        }
    }

    private fun showAddPoiDialog(latitude: Double, longitude: Double) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poi, null)
        val etName = dialogView.findViewById<EditText>(R.id.etPoiName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etPoiDescription)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar punto de interés")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString()
                val description = etDescription.text.toString()

                if (name.isNotEmpty()) {
                    val newPoi = PointOfInterest(
                        name = name,
                        description = description,
                        latitude = latitude,
                        longitude = longitude,
                        category = PoiCategory.TO_VISIT.name
                    )

                    poiViewModel.insertPointOfInterest(newPoi)
                    Toast.makeText(this, "Punto de interés guardado", Toast.LENGTH_SHORT).show()

                    // Actualizar el mapa con el nuevo punto
                    addPoiToMap(newPoi)
                } else {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> }
            .create()

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && !isTrackingLocation) {
            startLocationTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationTracking()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
    }
}