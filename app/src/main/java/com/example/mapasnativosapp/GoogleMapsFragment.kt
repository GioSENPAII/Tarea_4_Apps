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

class GoogleMapsFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_google_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar el WebView
        webView = view.findViewById(R.id.webViewGoogle)
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

            // Configuración adicional para que funcione Google Maps
            javaScriptCanOpenWindowsAutomatically = true
            useWideViewPort = true

            // Configurar el User Agent para evitar restricciones
            userAgentString = "Mozilla/5.0 (Linux; Android 10; Android SDK built for x86) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        }

        // Configurar WebViewClient para medir el tiempo de carga
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadStartTime = SystemClock.elapsedRealtime()
                Log.d("GoogleMapsFragment", "Cargando URL: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val loadTime = SystemClock.elapsedRealtime() - loadStartTime
                Log.d("MapPerformance", "Google Maps loaded in $loadTime ms")

                // Registrar el tiempo de carga en la actividad principal
                (activity as? MainTabActivity)?.registerLoadTime("google", loadTime)
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e("GoogleMapsFragment", "Error al cargar URL: $failingUrl, Código: $errorCode, Descripción: $description")
                Toast.makeText(
                    requireContext(),
                    "Error al cargar Google Maps: $description",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Evitar que los links externos abran otras apps
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Si la URL comienza con "intent:" o "market:", bloqueamos la redirección
                url?.let {
                    if (it.startsWith("intent:") || it.startsWith("market:") || it.startsWith("comgooglemaps:")) {
                        Log.d("GoogleMapsFragment", "Bloqueando redirección a: $url")
                        return true
                    }
                }
                return false
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
        try {
            // Usar JavaScript API de Google Maps para tener más control (incluyendo marcador de ubicación)
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        html, body { height: 100%; margin: 0; padding: 0; }
                        #map { width: 100%; height: 100%; }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <script>
                        // Función que inicializa el mapa cuando Google Maps API está cargada
                        function initMap() {
                            // Crear un objeto de mapa centrado en la ubicación
                            const map = new google.maps.Map(document.getElementById("map"), {
                                center: { lat: $latitude, lng: $longitude },
                                zoom: 15,
                                mapTypeControl: true,
                                streetViewControl: true,
                                zoomControl: true,
                                fullscreenControl: true
                            });
                            
                            // Agregar marcador de ubicación
                            const marker = new google.maps.Marker({
                                position: { lat: $latitude, lng: $longitude },
                                map: map,
                                title: "Mi ubicación",
                                // Usar icono de ubicación azul
                                icon: {
                                    path: google.maps.SymbolPath.CIRCLE,
                                    scale: 10,
                                    fillColor: "#4285F4",
                                    fillOpacity: 1,
                                    strokeColor: "#FFFFFF",
                                    strokeWeight: 2
                                },
                                // Añadir animación de rebote
                                animation: google.maps.Animation.DROP
                            });
                            
                            // Añadir círculo para mostrar precisión aproximada (100 metros)
                            const locationCircle = new google.maps.Circle({
                                strokeColor: "#4285F4",
                                strokeOpacity: 0.8,
                                strokeWeight: 2,
                                fillColor: "#4285F4",
                                fillOpacity: 0.1,
                                map: map,
                                center: { lat: $latitude, lng: $longitude },
                                radius: 100
                            });
                            
                            // Mostrar información al hacer clic en el marcador
                            const infowindow = new google.maps.InfoWindow({
                                content: "<div><strong>Mi ubicación actual</strong></div>"
                            });
                            
                            marker.addListener("click", () => {
                                infowindow.open(map, marker);
                            });
                            
                            // Abrir automáticamente el InfoWindow al cargar
                            infowindow.open(map, marker);
                        }
                    </script>
                    <script 
                        src="https://maps.googleapis.com/maps/api/js?key=&callback=initMap&v=weekly" 
                        async
                    ></script>
                </body>
                </html>
            """.trimIndent()

            // Cargar el HTML en el WebView
            webView.loadDataWithBaseURL(
                "https://maps.google.com/",
                html,
                "text/html",
                "UTF-8",
                null
            )

            Log.d("GoogleMapsFragment", "Cargando Google Maps con JavaScript API en: $latitude, $longitude")
        } catch (e: Exception) {
            Log.e("GoogleMapsFragment", "Error al cargar el mapa: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error al cargar Google Maps: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar solicitudes de ubicación pendientes
        cancellationTokenSource.cancel()
    }
}