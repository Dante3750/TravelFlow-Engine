package com.example.androidassignment4travelplannerapp.data.repository

import android.content.Context
import com.example.androidassignment4travelplannerapp.data.local.SavedPlaceEntity
import com.example.androidassignment4travelplannerapp.data.local.TripDao
import com.example.androidassignment4travelplannerapp.data.mapper.toDomain
import com.example.androidassignment4travelplannerapp.data.mapper.toEntity
import com.example.androidassignment4travelplannerapp.data.remote.TravelApiService
import com.example.androidassignment4travelplannerapp.data.remote.WeatherApiService
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class TravelRepositoryImpl @Inject constructor(
    private val travelApiService: TravelApiService,
    private val weatherApiService: WeatherApiService,
    private val tripDao: TripDao,
    @ApplicationContext context: Context,
) : ITravelRepository {

    private val owApiKey = com.example.androidassignment4travelplannerapp.BuildConfig.WEATHER_API_KEY
    private val googleKey = com.example.androidassignment4travelplannerapp.BuildConfig.GOOGLE_MAPS_KEY
    private val placesClient: PlacesClient = Places.createClient(context)

    override suspend fun fetchWeather(city: String): WeatherInfo {
        return weatherApiService.getWeather(city, owApiKey).toDomain()
    }

    override suspend fun fetchForecast(city: String): ForecastInfo {
        return weatherApiService.getForecast(city, owApiKey).toDomain()
    }

    override suspend fun fetchForecastJson(city: String): String {
        val forecast = weatherApiService.getForecast(city, owApiKey)
        return Gson().toJson(forecast)
    }

    override suspend fun searchLocations(query: String): List<Place> {
        return try {
            val token = AutocompleteSessionToken.newInstance()
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .build()
            
            val response = placesClient.findAutocompletePredictions(request).await()
            response.autocompletePredictions.map { prediction ->
                val placeRequest = FetchPlaceRequest.builder(prediction.placeId, listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ID)).build()
                placesClient.fetchPlace(placeRequest).await().place
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchNearbyAttractions(
        lat: Double, 
        lon: Double, 
        pageToken: String?
    ): Pair<List<Attraction>, String?> {
        return try {
            val location = if (pageToken == null) "$lat,$lon" else null
            val response = travelApiService.getNearbyPlaces(
                location = location,
                radius = 10000, // Increased radius to find more places
                type = "tourist_attraction",
                pageToken = pageToken,
                apiKey = googleKey
            )
            
            val attractions = response.results.map { result ->
                val rawType = result.types?.firstOrNull() ?: "attraction"
                val displayType = rawType.replace("_", " ").lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                
                Attraction(
                    id = result.placeId,
                    name = result.name,
                    category = if (rawType == "tourist_attraction") "Nearby Place" else displayType,
                    latitude = result.geometry.location.lat,
                    longitude = result.geometry.location.lng,
                    photoReference = result.photos?.firstOrNull()?.photoReference,
                    rating = result.rating,
                    totalRatings = result.userRatingsTotal
                )
            }.sortedByDescending { it.rating ?: 0.0 } // Sort by rating

            Pair(attractions, response.nextPageToken)
        } catch (_: Exception) {
            Pair(emptyList(), null)
        }
    }

    override suspend fun fetchNearbyHotels(lat: Double, lon: Double): List<Hotel> {
        return try {
            val response = travelApiService.getNearbyPlaces(
                location = "$lat,$lon",
                radius = 5000,
                type = "lodging",
                apiKey = googleKey
            )
            
            response.results.map { result ->
                Hotel(
                    id = result.placeId,
                    name = result.name,
                    address = result.address ?: "Address not available",
                    latitude = result.geometry.location.lat,
                    longitude = result.geometry.location.lng,
                    rating = result.rating,
                    userRatingsTotal = result.userRatingsTotal,
                    photoReference = result.photos?.firstOrNull()?.photoReference
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun pinHotelToTrip(tripId: Int, hotel: Hotel) {
        val trip = tripDao.getTripById(tripId)
        trip?.let {
            val updated = it.copy(pinnedHotelJson = Gson().toJson(hotel))
            tripDao.insertTrip(updated)
        }
    }

    override suspend fun removeHotelFromTrip(tripId: Int) {
        val trip = tripDao.getTripById(tripId)
        trip?.let {
            val updated = it.copy(pinnedHotelJson = null)
            tripDao.insertTrip(updated)
        }
    }

    override suspend fun fetchPlaceDetails(placeId: String) = 
        travelApiService.getPlaceDetails(placeId, apiKey = googleKey).result

    override fun getSavedTrips(): Flow<List<Trip>> {
        return tripDao.getAllTrips().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveNewTrip(trip: Trip) {
        tripDao.insertTrip(trip.toEntity())
    }

    override suspend fun deleteExistingTrip(tripId: Int) {
        val trip = tripDao.getTripById(tripId)
        trip?.let {
            tripDao.deletePlacesForTrip(it.id)
            tripDao.deleteTrip(it)
        }
    }

    override suspend fun addAttractionToTrip(tripId: Int, attraction: Attraction, day: Int) {
        val entity = SavedPlaceEntity(
            tripId = tripId,
            name = attraction.name,
            kinds = attraction.category,
            lat = attraction.latitude,
            lon = attraction.longitude,
            xid = attraction.id,
            dayNumber = day
        )
        tripDao.insertPlace(entity)
    }

    override suspend fun getAttractionsForTrip(tripId: Int): List<Attraction> {
        return tripDao.getPlacesForTrip(tripId).map { it.toDomain() }
    }

    override suspend fun updateTripWeather(tripId: Int, weatherSummary: String, forecastJson: String) {
        val trip = tripDao.getTripById(tripId)
        trip?.let {
            val updated = it.copy(weatherInfo = weatherSummary, forecastJson = forecastJson)
            tripDao.insertTrip(updated)
        }
    }
    
    override fun getPhotoUrl(photoReference: String?): String? {
        if (photoReference == null) return null
        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photo_reference=$photoReference&key=$googleKey"
    }
}
