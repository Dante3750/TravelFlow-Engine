package com.example.androidassignment4travelplannerapp.data.remote

// This file is kept for backward compatibility during migration
// All active travel data is now served via OpenTripMap and OpenStreetMap (Overpass/Nominatim)

data class GooglePlaceDetailModel(
    val name: String,
    val rating: Double?,
    val address: String?,
    val summary: GoogleSummary?,
    val photos: List<GooglePhoto>?,
    val geometry: GoogleGeometry,
    val userRatingsTotal: Int?
)

data class GoogleSummary(val overview: String?)
data class GooglePhoto(val photo_reference: String)
data class GoogleGeometry(val location: GoogleLatLng)
data class GoogleLatLng(val lat: Double, val lng: Double)
