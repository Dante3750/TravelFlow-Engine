package com.example.androidassignment4travelplannerapp.data.remote

// This file contains shared models for location details during migration

data class PlaceDetailModel(
    val name: String,
    val rating: Double?,
    val address: String?,
    val summary: PlaceSummary?,
    val photos: List<PlacePhoto>?,
    val geometry: PlaceGeometry,
    val userRatingsTotal: Int?
)

data class PlaceSummary(val overview: String?)
data class PlacePhoto(val url: String)
data class PlaceGeometry(val location: PlaceLatLng)
data class PlaceLatLng(val lat: Double, val lng: Double)
