package com.example.androidassignment4travelplannerapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidassignment4travelplannerapp.data.remote.ForecastResponse
import com.example.androidassignment4travelplannerapp.data.remote.WeatherResponse
import com.example.androidassignment4travelplannerapp.data.remote.GooglePlaceDetailModel
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.domain.usecase.*
import com.google.android.libraries.places.api.model.Place
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TravelViewModel @Inject constructor(
    getTripsUseCase: GetTripsUseCase,
    private val searchLocationsUseCase: SearchLocationsUseCase,
    private val getNearbyAttractionsUseCase: GetNearbyAttractionsUseCase,
    private val getNearbyHotelsUseCase: GetNearbyHotelsUseCase,
    private val saveTripUseCase: SaveTripUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val syncTripWeatherUseCase: SyncTripWeatherUseCase,
    private val addAttractionUseCase: AddAttractionUseCase,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val fetchForecastJsonUseCase: FetchForecastJsonUseCase,
    private val getPlaceDetailsUseCase: GetPlaceDetailsUseCase,
    private val getAttractionsForTripUseCase: GetAttractionsForTripUseCase,
    private val getPhotoUrlUseCase: GetPhotoUrlUseCase,
    private val pinHotelUseCase: PinHotelUseCase,
    private val removeHotelUseCase: RemoveHotelUseCase,
    private val updateTripDatesUseCase: UpdateTripDatesUseCase,
    private val fetchNearbyAmenitiesUseCase: FetchNearbyAmenitiesUseCase
) : ViewModel() {

    val savedTrips: StateFlow<List<Trip>> = getTripsUseCase().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _searchResults = MutableStateFlow<List<Attraction>>(emptyList())
    val searchResults: StateFlow<List<Attraction>> = _searchResults

    private val _nearbyHotels = MutableStateFlow<List<Hotel>>(emptyList())
    val nearbyHotels: StateFlow<List<Hotel>> = _nearbyHotels

    private val _nearStayResults = MutableStateFlow<List<Attraction>>(emptyList())
    val nearStayResults: StateFlow<List<Attraction>> = _nearStayResults

    private val _nearbyAmenities = MutableStateFlow<List<NearbyAmenity>>(emptyList())
    val nearbyAmenities: StateFlow<List<NearbyAmenity>> = _nearbyAmenities

    private var currentNextPageToken: String? = null

    private val _currentWeather = MutableStateFlow<WeatherResponse?>(null)
    val currentWeather: StateFlow<WeatherResponse?> = _currentWeather

    private val _forecast = MutableStateFlow<ForecastResponse?>(null)
    val forecast: StateFlow<ForecastResponse?> = _forecast

    private val _suggestions = MutableStateFlow<List<Place>>(emptyList())
    val suggestions: StateFlow<List<Place>> = _suggestions

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _mapFocus = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapFocus: StateFlow<Pair<Double, Double>?> = _mapFocus

    private val _selectedTripPlaces = MutableStateFlow<List<Attraction>>(emptyList())
    val selectedTripPlaces: StateFlow<List<Attraction>> = _selectedTripPlaces

    private val _selectedPlaceDetail = MutableStateFlow<GooglePlaceDetailModel?>(null)
    val selectedPlaceDetail: StateFlow<GooglePlaceDetailModel?> = _selectedPlaceDetail

    private val _activeDiscoveryCity = MutableStateFlow<String?>(null)
    val activeDiscoveryCity: StateFlow<String?> = _activeDiscoveryCity

    private var searchJob: Job? = null

    fun syncAllTripWeather() {
        viewModelScope.launch {
            syncTripWeatherUseCase(savedTrips.value)
        }
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            clearData()
            return
        }
        if (newQuery.length < 3) {
            _suggestions.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val results = searchLocationsUseCase(newQuery)
                if (_searchQuery.value == newQuery) {
                    _suggestions.value = results
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                if (_searchQuery.value == newQuery) {
                    _errorMessage.value = if (e is java.net.UnknownHostException) "No internet" else "Error searching"
                }
            }
        }
    }

    fun clearSearchState() {
        _searchQuery.value = ""
        clearData()
    }

    fun startDiscoveryForCity(name: String, lat: Double, lon: Double, trip: Trip? = null) {
        // If it's a new city, or we are re-anchoring to a pinned hotel, proceed
        val isNewCity = _activeDiscoveryCity.value != name
        val isReAnchoring = trip?.pinnedHotel != null && _searchResults.value.isNotEmpty()
        
        if (!isNewCity && !isReAnchoring && _searchResults.value.isNotEmpty()) {
            setMapFocus(lat, lon)
            return
        }

        _activeDiscoveryCity.value = name
        viewModelScope.launch {
            if (isNewCity) clearData()
            
            // Use hotel location if available, otherwise city center
            val anchorLat = trip?.pinnedHotel?.latitude ?: lat
            val anchorLon = trip?.pinnedHotel?.longitude ?: lon
            setMapFocus(anchorLat, anchorLon)

            launch {
                try {
                    val weatherInfo = getWeatherUseCase(name)
                    _currentWeather.value = WeatherResponse(
                        main = com.example.androidassignment4travelplannerapp.data.remote.MainWeather(weatherInfo.currentTemp.toDouble(), 0.0, 0),
                        weather = listOf(com.example.androidassignment4travelplannerapp.data.remote.WeatherDescription(weatherInfo.description, "")),
                        name = weatherInfo.cityName,
                        coord = com.example.androidassignment4travelplannerapp.data.remote.Coord(weatherInfo.latitude, weatherInfo.longitude)
                    )
                } catch (_: Exception) {}
            }

            launch {
                try {
                    val attractionResult = getNearbyAttractionsUseCase(anchorLat, anchorLon)
                    _searchResults.value = attractionResult.first
                    currentNextPageToken = attractionResult.second
                } catch (_: Exception) {}
            }

            // STAY-ANCHORED DISCOVERY: Narrow 5km radius around anchor
            if (trip?.pinnedHotel != null) {
                launch {
                    try {
                        val nearStay = getNearbyAttractionsUseCase(anchorLat, anchorLon, radius = 5000)
                        _nearStayResults.value = nearStay.first.take(15)
                    } catch (_: Exception) {}
                }
            }

            launch {
                try {
                    _nearbyHotels.value = getNearbyHotelsUseCase(anchorLat, anchorLon)
                } catch (_: Exception) {}
            }

            launch {
                try {
                    _nearbyAmenities.value = fetchNearbyAmenitiesUseCase(anchorLat, anchorLon)
                } catch (_: Exception) {}
            }

            launch {
                try {
                    val forecastJson = fetchForecastJsonUseCase(name)
                    _forecast.value = Gson().fromJson(forecastJson, ForecastResponse::class.java)
                } catch (_: Exception) {}
            }
        }
    }

    fun loadMoreNearby(lat: Double, lon: Double) {
        if (currentNextPageToken == null) return
        
        viewModelScope.launch {
            try {
                val result = getNearbyAttractionsUseCase(lat, lon, currentNextPageToken)
                _searchResults.value = (_searchResults.value + result.first).sortedByDescending { it.trustScore?.score ?: 0 }
                currentNextPageToken = result.second
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    fun pinHotel(tripId: Int, hotel: Hotel) {
        val currentCity = _activeDiscoveryCity.value
        viewModelScope.launch {
            pinHotelUseCase(tripId, hotel)
            if (currentCity != null) {
                // Fetch latest state to get updated trip
                val updatedTrip = savedTrips.value.find { it.id == tripId }
                startDiscoveryForCity(currentCity, hotel.latitude, hotel.longitude, updatedTrip)
            }
        }
    }

    fun unpinHotel(tripId: Int) {
        viewModelScope.launch {
            removeHotelUseCase(tripId)
        }
    }

    fun selectSuggestion(place: Place) {
        _searchQuery.value = place.name ?: ""
        _suggestions.value = emptyList() // Clear suggestions after selection
        viewModelScope.launch {
            try {
                val detail = getPlaceDetailsUseCase(place.id!!)
                startDiscoveryForCity(
                    name = place.name ?: "", 
                    lat = detail.geometry.location.lat, 
                    lon = detail.geometry.location.lng
                )
            } catch (e: Exception) {
                _errorMessage.value = "Unable to load location details."
            }
        }
    }

    fun fetchPlaceDetail(placeId: String) {
        viewModelScope.launch {
            try {
                _selectedPlaceDetail.value = getPlaceDetailsUseCase(placeId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error loading details"
            }
        }
    }

    fun saveTrip(title: String, weather: WeatherResponse, forecast: ForecastResponse?, start: Long, end: Long) {
        viewModelScope.launch {
            val photoRef = _searchResults.value.firstOrNull { it.photoReference != null }?.photoReference
            
            val trip = Trip(
                id = 0,
                title = title,
                destination = weather.name,
                latitude = weather.coord.lat,
                longitude = weather.coord.lon,
                startDate = start,
                endDate = end,
                weatherSummary = "${weather.main.temp.toInt()}°C, ${weather.weather.firstOrNull()?.description}",
                forecastJson = forecast?.let { Gson().toJson(it) },
                photoReference = photoRef
            )
            saveTripUseCase(trip)
        }
    }

    fun addPlaceToTrip(tripId: Int, attraction: Attraction, day: Int) {
        viewModelScope.launch {
            addAttractionUseCase(tripId, attraction, day)
            _selectedTripPlaces.value = getAttractionsForTripUseCase(tripId)
        }
    }

    fun deleteTrip(tripId: Int) {
        viewModelScope.launch { deleteTripUseCase(tripId) }
    }

    fun loadTripDetails(trip: Trip) {
        viewModelScope.launch {
            setMapFocus(trip.latitude, trip.longitude)
            _selectedTripPlaces.value = getAttractionsForTripUseCase(trip.id)
        }
    }

    fun updateTripDates(tripId: Int, start: Long, end: Long) {
        viewModelScope.launch {
            updateTripDatesUseCase(tripId, start, end)
        }
    }

    fun setMapFocus(lat: Double, lon: Double) { _mapFocus.value = Pair(lat, lon) }
    fun getPhotoUrl(ref: String?) = getPhotoUrlUseCase(ref)
    fun clearPlaceDetail() { _selectedPlaceDetail.value = null }
    private fun clearData() {
        _errorMessage.value = null
        _suggestions.value = emptyList()
        _searchResults.value = emptyList()
        _nearbyHotels.value = emptyList()
        currentNextPageToken = null
        _currentWeather.value = null
        _forecast.value = null
        // Note: We don't clear _activeDiscoveryCity here so UI knows what city it's showing
    }
}
