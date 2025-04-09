package com.example.mapasnativosapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainTabActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: MapViewPagerAdapter
    private var loadStartTime: Long = 0
    private var osmLoadTime: Long = 0
    private var googleLoadTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_tab)

        // Inicializar ViewPager y TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Configurar el adaptador para el ViewPager
        adapter = MapViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Configurar TabLayoutMediator para vincular las pestañas con ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "OpenStreetMap"
                1 -> tab.text = "Google Maps"
            }
        }.attach()

        // Monitorear cambios de página para métricas de rendimiento
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                loadStartTime = System.currentTimeMillis()

                // Registrar qué mapa está seleccionado para métricas
                when (position) {
                    0 -> Log.d("MapPerformance", "OSM tab selected at $loadStartTime")
                    1 -> Log.d("MapPerformance", "Google Maps tab selected at $loadStartTime")
                }
            }
        })
    }

    // Método para registrar tiempos de carga (llamado desde los fragmentos)
    fun registerLoadTime(mapType: String, loadTime: Long) {
        when (mapType) {
            "osm" -> {
                osmLoadTime = loadTime
                Log.d("MapPerformance", "OSM load time: $osmLoadTime ms")
            }
            "google" -> {
                googleLoadTime = loadTime
                Log.d("MapPerformance", "Google Maps load time: $googleLoadTime ms")
            }
        }

        // Si ambos mapas se han cargado al menos una vez, mostrar comparativa
        if (osmLoadTime > 0 && googleLoadTime > 0) {
            Log.d("MapPerformance", "Comparativa de tiempos de carga:")
            Log.d("MapPerformance", "- OpenStreetMap: $osmLoadTime ms")
            Log.d("MapPerformance", "- Google Maps: $googleLoadTime ms")
            Log.d("MapPerformance", "Diferencia: ${googleLoadTime - osmLoadTime} ms")
        }
    }
}