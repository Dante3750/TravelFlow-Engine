package com.example.androidassignment4travelplannerapp.domain.repository

import com.example.androidassignment4travelplannerapp.domain.model.*
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.flow.Flow

interface ITravelRepository {
    // Weather
    suspend fun fetchWeather(city: String): WeatherInfo
    suspend fun fetchForecast(city: String): ForecastInfo
    suspend fun fetchForecastJson(city: String): String

    // Search & Places
    suspend fun searchLocations(query: String): List<Place>
    
    // Updated: Supports pagination viapageToken and location bias
    suspend fun fetchNearbyAttractions(
        lat: Double, 
        lon: Double, 
        pageToken: String? = null
    ): Pair<List<Attraction>, String?> // Returns list + next page token

    suspend fun fetchNearbyHotels(
        lat: Double, 
        lon: Double
    ): List<Hotel>

    suspend fun fetchPlaceDetails(placeId: String): com.example.androidassignment4travelplannerapp.data.remote.GooglePlaceDetailModel
    fun getPhotoUrl(photoReference: String?): String?

    // Local Storage
    fun getSavedTrips(): Flow<List<Trip>>
    suspend fun saveNewTrip(trip: Trip)
    suspend fun deleteExistingTrip(tripId: Int)
    suspend fun addAttractionToTrip(tripId: Int, attraction: Attraction, day: Int)
    suspend fun getAttractionsForTrip(tripId: Int): List<Attraction>
    suspend fun updateTripWeather(tripId: Int, weatherSummary: String, forecastJson: String)
    
    // New: Hotel Persistence
    suspend fun pinHotelToTrip(tripId: Int, hotel: Hotel)
    suspend fun removeHotelFromTrip(tripId: Int)
}
