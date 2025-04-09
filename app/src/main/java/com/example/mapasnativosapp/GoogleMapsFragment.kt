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

            // Configuración importante para Google Maps
            userAgentString = "Mozilla/5.0 (Linux; Android 10; Android SDK built for x86) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
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
                Log.d("MapPerformance", "Google Maps loaded in $loadTime ms")

                // Registrar el tiempo de carga en la actividad principal
                (activity as? MainTabActivity)?.registerLoadTime("google", loadTime)
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
        // Cargar Google Maps web
        val googleMapsUrl = "https://www.google.com/maps?q=$latitude,$longitude&z=15"
        webView.loadUrl(googleMapsUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar solicitudes de ubicación pendientes
        cancellationTokenSource.cancel()
    }
}