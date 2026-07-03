package com.example.androidassignment4travelplannerapp.data.repository

import android.content.Context
import com.example.androidassignment4travelplannerapp.data.local.SavedPlaceEntity
import com.example.androidassignment4travelplannerapp.data.local.TripDao
import com.example.androidassignment4travelplannerapp.data.mapper.toDomain
import com.example.androidassignment4travelplannerapp.data.mapper.toEntity
import com.example.androidassignment4travelplannerapp.data.remote.*
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
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
    private val overpassApiService: OverpassApiService,
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
                .setTypesFilter(listOf("locality", "administrative_area_level_3"))
                .build()
            
            val response = placesClient.findAutocompletePredictions(request).await()
            response.autocompletePredictions.map { prediction ->
                Place.builder()
                    .setId(prediction.placeId)
                    .setName(prediction.getPrimaryText(null).toString())
                    .setAddress(prediction.getSecondaryText(null).toString())
                    .build()
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
            val location = if (pageToken == null) "$lat,$lon" else null
            val response = travelApiService.getNearbyPlaces(
                location = location,
                radius = radius ?: 50000,
                keyword = "tourist attractions|parks|museums|landmarks|temples|monuments|gardens",
                pageToken = pageToken,
                apiKey = googleKey
            )
            
            val attractions = response.results.filter { result ->
                val types = result.types ?: emptyList()
                val name = result.name.lowercase()
                
                val allowed = listOf(
                    "tourist_attraction", "museum", "park", "place_of_worship", 
                    "hindu_temple", "church", "mosque", "shrine", "art_gallery", 
                    "zoo", "aquarium", "amusement_park", "natural_feature", 
                    "stadium", "shopping_mall", "market", "historic_site", "landmark", 
                    "town_square", "monument", "garden", "library"
                )
                
                val forbidden = listOf(
                    "dentist", "doctor", "hospital", "car_repair", "bank", "atm", 
                    "gas_station", "pharmacy", "local_government_office",
                    "lawyer", "police", "post_office"
                )
                val keywords = listOf("clinic", "repair", "petrol", "cng", "service", "house", "villa")

                val isAllowed = types.any { it in allowed }
                val isForbidden = (types.any { it in forbidden } && !types.any { it in allowed }) || keywords.any { name.contains(it) }
                val isHighlyRated = (result.rating ?: 0.0) >= 4.0 && (result.userRatingsTotal ?: 0) > 5
                
                (isAllowed || isHighlyRated) && !isForbidden
            }.map { result ->
                val types = result.types ?: emptyList()
                
                val categoryGroup = when {
                    types.any { it in listOf("historic_site", "landmark", "town_square", "monument") } -> "Landmark"
                    types.any { it in listOf("place_of_worship", "hindu_temple", "church", "mosque", "shrine") } -> "Religious Site"
                    types.any { it in listOf("park", "natural_feature", "garden") } -> "Nature"
                    types.any { it in listOf("museum", "art_gallery", "library") } -> "Museum"
                    types.any { it in listOf("amusement_park", "stadium", "zoo", "aquarium") } -> "Entertainment"
                    types.any { it in listOf("shopping_mall", "market") } -> "Shopping"
                    else -> "Tourist Spot"
                }
                
                val rating = result.rating ?: 0.0
                val reviews = result.userRatingsTotal ?: 0
                
                // V1.1 Formula: weighted rating + volume bonus + recency (mocked for now)
                val ratingFactor = (rating / 5.0) * 60.0
                val volumeFactor = (if (reviews > 1000) 30.0 else if (reviews > 100) 15.0 else 5.0)
                val recencyFactor = 10.0 // Mocked since Google Places API doesn't give review dates in nearby search
                
                val rawScore = (ratingFactor + volumeFactor + recencyFactor).toInt().coerceIn(0, 100)
                
                val tier = when {
                    reviews < 10 -> TrustTier.LIMITED_DATA
                    rawScore >= 85 -> TrustTier.HIGHLY_TRUSTED
                    rawScore >= 60 -> TrustTier.RELIABLE
                    else -> TrustTier.UNVERIFIED
                }

                val reason = when (tier) {
                    TrustTier.HIGHLY_TRUSTED -> "Exceptional quality from ${reviews}+ verified visitors."
                    TrustTier.RELIABLE -> "Solid community feedback with consistent visits."
                    TrustTier.LIMITED_DATA -> "New or emerging spot with limited recent data."
                    TrustTier.UNVERIFIED -> "Mixed feedback or inconsistent visit data."
                }

                val trustObj = TrustScore(
                    score = rawScore,
                    tier = tier,
                    reason = reason,
                    ratingFactor = ratingFactor,
                    volumeFactor = volumeFactor,
                    recencyFactor = recencyFactor,
                    verificationLevel = if (reviews > 200) 3 else if (reviews > 50) 2 else 1
                )

                Attraction(
                    id = result.placeId,
                    name = result.name,
                    category = categoryGroup,
                    latitude = result.geometry.location.lat,
                    longitude = result.geometry.location.lng,
                    photoReference = result.photos?.firstOrNull()?.photoReference,
                    rating = result.rating,
                    totalRatings = result.userRatingsTotal,
                    trustScore = trustObj
                )
            }.sortedByDescending { it.trustScore?.score ?: 0 }
            
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
                val rawAddress = result.address ?: "Address not available"
                val formattedAddress = rawAddress.split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }

                Hotel(
                    id = result.placeId,
                    name = result.name,
                    address = formattedAddress,
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

    override suspend fun fetchNearbyAmenities(lat: Double, lon: Double): List<NearbyAmenity> {
        val query = """
            [out:json];
            (
              node["amenity"="toilets"](around:2000,$lat,$lon);
              node["amenity"="police"](around:2000,$lat,$lon);
              node["shop"="tea"](around:2000,$lat,$lon);
            );
            out body;
        """.trimIndent()

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
                    name = element.tags?.get("name") ?: type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
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

    override suspend fun fetchPlaceDetails(placeId: String): com.example.androidassignment4travelplannerapp.data.remote.GooglePlaceDetailModel {
        return travelApiService.getPlaceDetails(placeId, apiKey = googleKey).result
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
        if (photoReference == null) return null
        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photo_reference=$photoReference&key=$googleKey"
    }
}
