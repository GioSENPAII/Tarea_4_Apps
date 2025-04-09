package com.example.mapasnativosapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mapasnativosapp.model.PointOfInterest
import com.example.mapasnativosapp.viewmodel.PointOfInterestViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class RouteGeneratorActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var transportToggleGroup: MaterialButtonToggleGroup
    private lateinit var btnStartRoute: Button
    private lateinit var btnEndRoute: Button
    private lateinit var tvRouteInfo: TextView
    private lateinit var fabMyLocation: FloatingActionButton

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val cancellationTokenSource = CancellationTokenSource()

    private lateinit var poiViewModel: PointOfInterestViewModel

    private var currentLatitude: Double = 19.4326  // Default to CDMX
    private var currentLongitude: Double = -99.1332

    private var startLatitude: Double? = null
    private var startLongitude: Double? = null
    private var endLatitude: Double? = null
    private var endLongitude: Double? = null

    private var selectedTransportMode: String = "walking"

    // Registro de solicitud de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(
                this,
                "Los permisos de ubicación son necesarios para la navegación",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_generator)

        // Inicializar vistas
        webView = findViewById(R.id.webViewRoute)
        transportToggleGroup = findViewById(R.id.transportToggleGroup)
        btnStartRoute = findViewById(R.id.btnStartRoute)
        btnEndRoute = findViewById(R.id.btnEndRoute)
        tvRouteInfo = findViewById(R.id.tvRouteInfo)
        fabMyLocation = findViewById(R.id.fabMyLocation)

        // Inicializar ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar ViewModel
        poiViewModel = ViewModelProvider(this)[PointOfInterestViewModel::class.java]

        // Configurar WebView
        setupWebView()

        // Configurar botones y eventos
        setupUI()

        // Verificar permisos
        checkLocationPermission()
    }

    private fun setupUI() {
        // Configurar toggle de tipo de transporte
        transportToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedTransportMode = when (checkedId) {
                    R.id.btnWalking -> "walking"
                    R.id.btnBicycling -> "bicycling"
                    R.id.btnDriving -> "driving"
                    else -> "walking"
                }

                // Si ya hay una ruta, actualizarla con el nuevo modo
                if (startLatitude != null && endLatitude != null) {
                    calculateAndShowRoute()
                }
            }
        }

        // Botón para seleccionar punto de inicio
        btnStartRoute.setOnClickListener {
            showPointSelectionDialog(true)
        }

        // Botón para seleccionar punto de fin
        btnEndRoute.setOnClickListener {
            showPointSelectionDialog(false)
        }

        // Botón para centrar en ubicación actual
        fabMyLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }
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

                // Centrar en la ubicación actual
                centerMapOnCurrentLocation()
            }
        }

        // Cargar el mapa inicial
        loadMap()
    }

    private fun loadMap() {
        // Crear HTML con Leaflet.js para mostrar el mapa con funcionalidades de rutas
        val mapHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet-routing-machine@3.2.12/dist/leaflet-routing-machine.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script src="https://unpkg.com/leaflet-routing-machine@3.2.12/dist/leaflet-routing-machine.js"></script>
                <style>
                    body { margin: 0; padding: 0; }
                    #map { position: absolute; top: 0; bottom: 0; width: 100%; height: 100%; }
                    .custom-popup .leaflet-popup-content-wrapper {
                        background: rgba(255, 255, 255, 0.9);
                        border-radius: 12px;
                    }
                    .leaflet-routing-alt {
                        max-height: 0;
                        overflow: hidden;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    // Inicializar mapa
                    var map = L.map('map').setView([${currentLatitude}, ${currentLongitude}], 15);
                    
                    // Capa base de OpenStreetMap
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
                        maxZoom: 19
                    }).addTo(map);
                    
                    // Marcador de posición actual
                    var currentPositionMarker = L.marker([${currentLatitude}, ${currentLongitude}], {
                        icon: L.icon({
                            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
                            iconSize: [25, 41],
                            iconAnchor: [12, 41],
                            popupAnchor: [1, -34]
                        })
                    }).addTo(map);
                    currentPositionMarker.bindPopup("<b>Mi ubicación</b>").openPopup();
                    
                    // Marcadores para puntos de inicio y fin
                    var startMarker = null;
                    var endMarker = null;
                    
                    // Variable para el control de ruta
                    var routingControl = null;
                    
                    // Capas para diferentes tipos de POIs
                    var poisLayer = L.layerGroup().addTo(map);
                    
                    // Función para actualizar la ubicación actual
                    function updateCurrentPosition(lat, lng) {
                        currentPositionMarker.setLatLng([lat, lng]);
                    }
                    
                    // Función para centrar la vista en la ubicación actual
                    function centerOnCurrentPosition(lat, lng) {
                        map.setView([lat, lng], 15);
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
                        popupContent += '<p><button onclick="Android.selectAsStart(' + poi.latitude + ', ' + poi.longitude + ', \'' + poi.name + '\')">Usar como inicio</button> ';
                        popupContent += '<button onclick="Android.selectAsEnd(' + poi.latitude + ', ' + poi.longitude + ', \'' + poi.name + '\')">Usar como destino</button></p>';
                        popupContent += '</div>';
                        
                        marker.bindPopup(popupContent);
                        marker.addTo(poisLayer);
                        
                        return marker;
                    }
                    
                    // Función para establecer el punto de inicio
                    function setStartPoint(lat, lng, name) {
                        if (startMarker) {
                            map.removeLayer(startMarker);
                        }
                        
                        startMarker = L.marker([lat, lng], {
                            icon: L.icon({
                                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
                                iconSize: [25, 41],
                                iconAnchor: [12, 41],
                                popupAnchor: [1, -34]
                            })
                        }).addTo(map);
                        
                        startMarker.bindPopup("<b>Inicio: " + name + "</b>").openPopup();
                        
                        // Si también hay un punto final, calcular la ruta
                        if (endMarker) {
                            calculateRoute();
                        }
                    }
                    
                    // Función para establecer el punto final
                    function setEndPoint(lat, lng, name) {
                        if (endMarker) {
                            map.removeLayer(endMarker);
                        }
                        
                        endMarker = L.marker([lat, lng], {
                            icon: L.icon({
                                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-black.png',
                                iconSize: [25, 41],
                                iconAnchor: [12, 41],
                                popupAnchor: [1, -34]
                            })
                        }).addTo(map);
                        
                        endMarker.bindPopup("<b>Destino: " + name + "</b>").openPopup();
                        
                        // Si también hay un punto de inicio, calcular la ruta
                        if (startMarker) {
                            calculateRoute();
                        }
                    }
                    
                    // Función para calcular y mostrar la ruta
                    function calculateRoute(transportMode) {
                        if (!startMarker || !endMarker) return;
                        
                        // Eliminar ruta anterior si existe
                        if (routingControl) {
                            map.removeControl(routingControl);
                        }
                        
                        // Crear nuevo control de ruta
                        routingControl = L.Routing.control({
                            waypoints: [
                                L.latLng(startMarker.getLatLng().lat, startMarker.getLatLng().lng),
                                L.latLng(endMarker.getLatLng().lat, endMarker.getLatLng().lng)
                            ],
                            routeWhileDragging: false,
                            showAlternatives: true,
                            fitSelectedRoute: true,
                            lineOptions: {
                                styles: [
                                    {color: '#0000FF', opacity: 0.8, weight: 6}
                                ]
                            },
                            router: L.Routing.osrmv1({
                                serviceUrl: 'https://router.project-osrm.org/route/v1',
                                profile: transportMode || 'walking'
                            })
                        }).addTo(map);
                        
                        // Capturar evento de ruta calculada para obtener detalles
                        routingControl.on('routesfound', function(e) {
                            var routes = e.routes;
                            var summary = routes[0].summary;
                            
                            // Enviar información de la ruta a Android
                            Android.onRouteCalculated(
                                summary.totalDistance,
                                summary.totalTime,
                                transportMode || 'walking'
                            );
                        });
                    }
                    
                    // Función para limpiar las capas
                    function clearLayers() {
                        poisLayer.clearLayers();
                    }
                    
                    // Detectar clic en el mapa
                    map.on('click', function(e) {
                        var latlng = e.latlng;
                        showMapClickOptions(latlng.lat, latlng.lng);
                    });
                    
                    // Mostrar opciones al hacer clic en el mapa
                    function showMapClickOptions(lat, lng) {
                        var popup = L.popup()
                            .setLatLng([lat, lng])
                            .setContent(
                                '<div class="custom-popup">' +
                                '<p><button onclick="Android.selectAsStart(' + lat + ', ' + lng + ', \'Punto personalizado\')">Usar como inicio</button></p>' +
                                '<p><button onclick="Android.selectAsEnd(' + lat + ', ' + lng + ', \'Punto personalizado\')">Usar como destino</button></p>' +
                                '</div>'
                            )
                            .openOn(map);
                    }
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
            val poiList = StringBuilder("[")
            pois.forEachIndexed { index, poi ->
                poiList.append("""
                    {
                        "id": ${poi.id},
                        "name": "${poi.name.replace("\"", "\\\"").replace("\n", " ")}",
                        "description": "${poi.description.replace("\"", "\\\"").replace("\n", " ")}",
                        "latitude": ${poi.latitude},
                        "longitude": ${poi.longitude},
                        "category": "${poi.category}"
                    }
                """.trimIndent())

                if (index < pois.size - 1) {
                    poiList.append(",")
                }
            }
            poiList.append("]")

            val jsCode = """
                poisLayer.clearLayers();
                var pois = ${poiList.toString()};
                for (var i = 0; i < pois.length; i++) {
                    addPointOfInterest(pois[i]);
                }
            """.trimIndent()

            webView.evaluateJavascript(jsCode, null)
        }
    }

    private fun showPointSelectionDialog(isStart: Boolean) {
        val actions = arrayOf(
            "Usar mi ubicación actual",
            "Seleccionar de mis puntos guardados",
            "Seleccionar en el mapa"
        )

        AlertDialog.Builder(this)
            .setTitle(if (isStart) "Seleccionar punto de inicio" else "Seleccionar punto de destino")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> useCurrentLocationAsPoint(isStart)
                    1 -> showSavedPointsDialog(isStart)
                    2 -> {
                        // Solo mostrar instrucciones para seleccionar en el mapa
                        Toast.makeText(
                            this,
                            "Toca en el mapa para seleccionar un punto",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun useCurrentLocationAsPoint(isStart: Boolean) {
        if (isStart) {
            startLatitude = currentLatitude
            startLongitude = currentLongitude
            setStartPoint(currentLatitude, currentLongitude, "Mi ubicación")
        } else {
            endLatitude = currentLatitude
            endLongitude = currentLongitude
            setEndPoint(currentLatitude, currentLongitude, "Mi ubicación")
        }
    }

    private fun showSavedPointsDialog(isStart: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            // Obtener los puntos guardados
            val pois = poiViewModel.allPointsOfInterest.value ?: listOf()

            if (pois.isEmpty()) {
                Toast.makeText(
                    this@RouteGeneratorActivity,
                    "No tienes puntos guardados",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val poiNames = pois.map { it.name }.toTypedArray()

            AlertDialog.Builder(this@RouteGeneratorActivity)
                .setTitle(if (isStart) "Seleccionar punto de inicio" else "Seleccionar punto de destino")
                .setItems(poiNames) { _, which ->
                    val selectedPoi = pois[which]
                    if (isStart) {
                        startLatitude = selectedPoi.latitude
                        startLongitude = selectedPoi.longitude
                        setStartPoint(selectedPoi.latitude, selectedPoi.longitude, selectedPoi.name)
                    } else {
                        endLatitude = selectedPoi.latitude
                        endLongitude = selectedPoi.longitude
                        setEndPoint(selectedPoi.latitude, selectedPoi.longitude, selectedPoi.name)
                    }
                }
                .show()
        }
    }

    private fun setStartPoint(latitude: Double, longitude: Double, name: String) {
        startLatitude = latitude
        startLongitude = longitude

        val jsCode = "setStartPoint($latitude, $longitude, '$name');"
        webView.evaluateJavascript(jsCode, null)

        btnStartRoute.text = "Inicio: $name"

        if (endLatitude != null) {
            calculateAndShowRoute()
        }
    }

    private fun setEndPoint(latitude: Double, longitude: Double, name: String) {
        endLatitude = latitude
        endLongitude = longitude

        val jsCode = "setEndPoint($latitude, $longitude, '$name');"
        webView.evaluateJavascript(jsCode, null)

        btnEndRoute.text = "Destino: $name"

        if (startLatitude != null) {
            calculateAndShowRoute()
        }
    }

    private fun calculateAndShowRoute() {
        if (startLatitude != null && startLongitude != null && endLatitude != null && endLongitude != null) {
            val jsCode = "calculateRoute('$selectedTransportMode');"
            webView.evaluateJavascript(jsCode, null)
        }
    }

    private fun updateRouteInfo(distanceMeters: Double, durationSeconds: Double, transportMode: String) {
        val distanceKm = distanceMeters / 1000.0
        val formattedDistance = String.format("%.2f", distanceKm)

        val hours = (durationSeconds / 3600).toInt()
        val minutes = ((durationSeconds % 3600) / 60).toInt()

        val transportText = when (transportMode) {
            "walking" -> "a pie"
            "bicycling" -> "en bicicleta"
            "driving" -> "en auto"
            else -> transportMode
        }

        val timeText = when {
            hours > 0 -> "$hours h $minutes min"
            else -> "$minutes min"
        }

        val routeInfoText = """
            Distancia: $formattedDistance km
            Tiempo estimado $transportText: $timeText
        """.trimIndent()

        tvRouteInfo.text = routeInfoText
        tvRouteInfo.visibility = View.VISIBLE
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                getCurrentLocation()
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
    private fun getCurrentLocation() {
        try {
            // Obtener ubicación utilizando FusedLocationProviderClient
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    // Actualizar marcador de posición actual
                    updateCurrentPositionMarker()
                }
            }.addOnFailureListener { e ->
                Log.e("LocationError", "Error obteniendo ubicación: ${e.message}")
                Toast.makeText(
                    this,
                    "Error al obtener la ubicación: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("LocationError", "Error crítico: ${e.message}")
        }
    }

    private fun updateCurrentPositionMarker() {
        val jsCode = "updateCurrentPosition($currentLatitude, $currentLongitude);"
        webView.evaluateJavascript(jsCode, null)
    }

    private fun centerMapOnCurrentLocation() {
        val jsCode = "centerOnCurrentPosition($currentLatitude, $currentLongitude);"
        webView.evaluateJavascript(jsCode, null)
    }

    // Interfaz para comunicación JavaScript-Android
    inner class WebAppInterface {
        @JavascriptInterface
        fun selectAsStart(latitude: Double, longitude: Double, name: String) {
            runOnUiThread {
                setStartPoint(latitude, longitude, name)
            }
        }

        @JavascriptInterface
        fun selectAsEnd(latitude: Double, longitude: Double, name: String) {
            runOnUiThread {
                setEndPoint(latitude, longitude, name)
            }
        }

        @JavascriptInterface
        fun onRouteCalculated(distance: Double, duration: Double, transportMode: String) {
            runOnUiThread {
                updateRouteInfo(distance, duration, transportMode)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar solicitudes de ubicación pendientes
        cancellationTokenSource.cancel()
    }
}