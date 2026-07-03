package com.example.androidassignment4travelplannerapp.domain.model

data class Trip(
    val id: Int,
    val title: String,
    val destination: String,
    val latitude: Double,
    val longitude: Double,
    val startDate: Long,
    val endDate: Long,
    val weatherSummary: String?,
    val forecastJson: String?,
    val photoReference: String?,
    val pinnedHotel: Hotel? = null
)

data class Attraction(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val photoReference: String?,
    val rating: Double? = 0.0,
    val totalRatings: Int? = 0,
    val dayNumber: Int = 1,
    val trustScore: TrustScore? = null
)

data class TrustScore(
    val score: Int,
    val tier: TrustTier,
    val reason: String,
    val ratingFactor: Double,
    val volumeFactor: Double,
    val recencyFactor: Double,
    val verificationLevel: Int
)

data class NearbyAmenity(
    val id: String,
    val type: AmenityType,
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String?
)

enum class AmenityType {
    TOILET,
    POLICE,
    PHARMACY,
    TEA_STALL,
    SMOKING_SPOT
}

enum class TrustTier {
    HIGHLY_TRUSTED,
    RELIABLE,
    LIMITED_DATA,
    UNVERIFIED
}

data class Hotel(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val photoReference: String?,
    val bookingStatus: BookingStatus = BookingStatus.NONE
)

enum class BookingStatus {
    NONE,
    INTERESTED,
    PLANNING_TO_BOOK,
    BOOKED
}

data class WeatherInfo(
    val cityName: String,
    val currentTemp: Int,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

data class ForecastItem(
    val date: Long,
    val temp: Int,
    val description: String
)

data class ForecastInfo(
    val items: List<ForecastItem>
)
