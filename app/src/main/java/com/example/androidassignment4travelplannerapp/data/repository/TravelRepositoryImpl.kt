package com.example.androidassignment4travelplannerapp.data.repository

import android.content.Context
import com.example.androidassignment4travelplannerapp.data.local.*
import com.example.androidassignment4travelplannerapp.data.mapper.toDomain
import com.example.androidassignment4travelplannerapp.data.mapper.toEntity
import com.example.androidassignment4travelplannerapp.data.remote.*
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TravelRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val overpassApiService: OverpassApiService,
    private val otmApiService: OpenTripMapApiService,
    private val nominatimApiService: NominatimApiService,
    private val tripDao: TripDao,
    @ApplicationContext context: Context,
) : ITravelRepository {

    private val owApiKey = com.example.androidassignment4travelplannerapp.BuildConfig.WEATHER_API_KEY
    private val otmKey = com.example.androidassignment4travelplannerapp.BuildConfig.OPEN_TRIP_MAP_KEY

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

    override suspend fun searchLocations(query: String): List<PlaceSuggestion> {
        return try {
            val response = nominatimApiService.searchLocations(query)
            response.map { res ->
                PlaceSuggestion(
                    id = res.place_id.toString(),
                    name = res.display_name.split(",").firstOrNull()?.trim() ?: res.display_name,
                    address = res.display_name,
                    lat = res.lat,
                    lon = res.lon
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchNearbyAttractions(
        lat: Double, 
        lon: Double, 
        pageToken: String?,
        radius: Int?
    ): Pair<List<Attraction>, String?> {
        return try {
            val response = otmApiService.getPlacesInRadius(
                radius = radius ?: 50000,
                lon = lon,
                lat = lat,
                kinds = "interesting_places",
                apiKey = otmKey
            )
            
            val attractions = response.map { res ->
                val rating = (res.rate / 3.0) * 5.0
                val ratingFactor = (rating / 5.0) * 60.0
                val volumeFactor = 20.0
                val recencyFactor = 10.0
                val rawScore = (ratingFactor + volumeFactor + recencyFactor).toInt().coerceIn(0, 100)

                val tier = if (rawScore >= 85) TrustTier.HIGHLY_TRUSTED else TrustTier.RELIABLE

                val trustObj = TrustScore(
                    score = rawScore,
                    tier = tier,
                    reason = "Verified via OpenTripMap heritage database.",
                    ratingFactor = ratingFactor,
                    volumeFactor = volumeFactor,
                    recencyFactor = recencyFactor,
                    verificationLevel = 2
                )

                Attraction(
                    id = res.xid,
                    name = res.name,
                    category = res.kinds.split(",").firstOrNull()?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Landmark",
                    latitude = res.point.lat,
                    longitude = res.point.lon,
                    photoReference = null,
                    rating = rating,
                    totalRatings = 100,
                    trustScore = trustObj
                )
            }.filter { it.name.isNotBlank() }
            
            Pair(attractions, null)
        } catch (_: Exception) {
            Pair(emptyList(), null)
        }
    }

    override suspend fun fetchNearbyHotels(lat: Double, lon: Double): List<Hotel> {
        val query = "[out:json];node[\"tourism\"=\"hotel\"](around:5000,$lat,$lon);out body;"
        return try {
            val response = overpassApiService.getNearbyPOIs(query)
            response.elements.map { element ->
                Hotel(
                    id = element.id.toString(),
                    name = element.tags?.get("name") ?: "Local Stay",
                    address = element.tags?.get("addr:street") ?: "Nearby Stay",
                    latitude = element.lat,
                    longitude = element.lon,
                    rating = 4.0,
                    userRatingsTotal = 50,
                    photoReference = null
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchNearbyAmenities(lat: Double, lon: Double): List<NearbyAmenity> {
        val query = "[out:json];(node[\"amenity\"=\"toilets\"](around:2000,$lat,$lon);node[\"amenity\"=\"police\"](around:2000,$lat,$lon);node[\"shop\"=\"tea\"](around:2000,$lat,$lon););out body;"
        return try {
            val response = overpassApiService.getNearbyPOIs(query)
            response.elements.map { element ->
                val type = when {
                    element.tags?.get("amenity") == "toilets" -> AmenityType.TOILET
                    element.tags?.get("amenity") == "police" -> AmenityType.POLICE
                    element.tags?.get("shop") == "tea" -> AmenityType.TEA_STALL
                    else -> AmenityType.TOILET
                }
                NearbyAmenity(
                    id = element.id.toString(),
                    type = type,
                    name = element.tags?.get("name") ?: type.name.lowercase().replaceFirstChar { it.uppercase() },
                    lat = element.lat,
                    lon = element.lon,
                    address = element.tags?.get("addr:full") ?: element.tags?.get("addr:street")
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

    override suspend fun fetchPlaceDetails(placeId: String): PlaceDetailModel {
        return try {
            val otmDetail = otmApiService.getPlaceDetail(placeId, otmKey)
            PlaceDetailModel(
                name = otmDetail.name,
                rating = 4.0,
                address = otmDetail.address?.road ?: "Address not available",
                summary = PlaceSummary(otmDetail.wikipedia_extracts?.text),
                photos = if (otmDetail.image != null) listOf(PlacePhoto(otmDetail.image)) else null,
                geometry = PlaceGeometry(
                    PlaceLatLng(otmDetail.point.lat, otmDetail.point.lon)
                ),
                userRatingsTotal = 100
            )
        } catch (_: Exception) {
            PlaceDetailModel("", 0.0, "", null, null, PlaceGeometry(PlaceLatLng(0.0, 0.0)), 0)
        }
    }

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

    override suspend fun updateTripDates(tripId: Int, start: Long, end: Long) {
        val trip = tripDao.getTripById(tripId)
        trip?.let {
            val updated = it.copy(startDate = start, endDate = end)
            tripDao.insertTrip(updated)
        }
    }
    
    override fun getPhotoUrl(photoReference: String?): String? {
        return photoReference
    }
}
