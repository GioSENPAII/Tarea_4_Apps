package com.example.mapasnativosapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class OpenStreetMapFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val cancellationTokenSource = CancellationTokenSource()
    private var loadStartTime: Long = 0

    // Registro de solicitud de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(
                requireContext(),
                "Los permisos de ubicación son necesarios para mostrar tu ubicación",
                Toast.LENGTH_LONG
            ).show()

            // Cargar el mapa en una ubicación predeterminada
            loadMap(19.4326, -99.1332) // Ciudad de México como ubicación predeterminada
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_openstreetmap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar el WebView
        webView = view.findViewById(R.id.webViewOSM)
        setupWebView()

        // Inicializar el proveedor de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Verificar permisos de ubicación
        checkLocationPermission()
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

        // Configurar WebViewClient para medir el tiempo de carga
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadStartTime = SystemClock.elapsedRealtime()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val loadTime = SystemClock.elapsedRealtime() - loadStartTime
                Log.d("MapPerformance", "OSM Map loaded in $loadTime ms")

                // Registrar el tiempo de carga en la actividad principal
                (activity as? MainTabActivity)?.registerLoadTime("osm", loadTime)
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
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
                    // Cargar el mapa con la ubicación actual
                    loadMap(location.latitude, location.longitude)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo obtener la ubicación actual. Usando ubicación predeterminada.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Cargar el mapa en una ubicación predeterminada
                    loadMap(19.4326, -99.1332) // Ciudad de México como ubicación predeterminada
                }
            }.addOnFailureListener { e ->
                Log.e("LocationError", "Error obteniendo ubicación: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Error al obtener la ubicación: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Cargar el mapa en una ubicación predeterminada
                loadMap(19.4326, -99.1332) // Ciudad de México como ubicación predeterminada
            }
        } catch (e: Exception) {
            Log.e("LocationError", "Error crítico: ${e.message}")
            // Cargar el mapa en una ubicación predeterminada
            loadMap(19.4326, -99.1332) // Ciudad de México como ubicación predeterminada
        }
    }

    private fun loadMap(latitude: Double, longitude: Double) {
        // Crear HTML con Leaflet.js para mostrar el mapa
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
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([$latitude, $longitude], 15);
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
                        maxZoom: 19
                    }).addTo(map);
                    
                    var marker = L.marker([$latitude, $longitude]).addTo(map);
                    marker.bindPopup("<b>Mi ubicación</b>").openPopup();
                    
                    // Función para actualizar la ubicación del marcador
                    function updateLocation(lat, lng) {
                        marker.setLatLng([lat, lng]);
                        map.setView([lat, lng], map.getZoom());
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

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar solicitudes de ubicación pendientes
        cancellationTokenSource.cancel()
    }
}