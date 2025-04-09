package com.example.mapasnativosapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MapViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2  // Tenemos dos pestañas: OSM y Google Maps

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OpenStreetMapFragment()  // Primera pestaña: OpenStreetMap
            1 -> GoogleMapsFragment()     // Segunda pestaña: Google Maps
            else -> OpenStreetMapFragment()
        }
    }
}