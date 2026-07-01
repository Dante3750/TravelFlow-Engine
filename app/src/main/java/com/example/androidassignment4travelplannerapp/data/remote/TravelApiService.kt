package com.example.androidassignment4travelplannerapp.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface TravelApiService {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String?,
        @Query("radius") radius: Int = 5000,
        @Query("type") type: String = "tourist_attraction",
        @Query("pagetoken") pageToken: String? = null, // Supports "Load More" chunks
        @Query("key") apiKey: String
    ): GooglePlacesResponse

    @GET("maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,rating,formatted_address,photos,editorial_summary,geometry,user_ratings_total",
        @Query("key") apiKey: String
    ): GooglePlaceDetailsResponse
}

data class GooglePlacesResponse(
    val results: List<GooglePlaceModel>,
    @SerializedName("next_page_token") val nextPageToken: String?, // Critical for pagination
    val status: String
)

data class GooglePlaceModel(
    @SerializedName("place_id") val placeId: String,
    val name: String,
    val types: List<String>?,
    val geometry: GoogleGeometry,
    val photos: List<GooglePhoto>?,
    val rating: Double?,
    @SerializedName("user_ratings_total") val userRatingsTotal: Int?,
    @SerializedName("vicinity") val address: String?
)

data class GoogleGeometry(
    val location: GoogleLatLng
)

data class GoogleLatLng(
    val lat: Double,
    val lng: Double
)

data class GooglePhoto(
    @SerializedName("photo_reference") val photoReference: String
)

data class GooglePlaceDetailsResponse(
    val result: GooglePlaceDetailModel,
    val status: String
)

data class GooglePlaceDetailModel(
    val name: String,
    val rating: Double?,
    @SerializedName("formatted_address") val address: String?,
    @SerializedName("editorial_summary") val summary: GoogleSummary?,
    val photos: List<GooglePhoto>?,
    val geometry: GoogleGeometry,
    @SerializedName("user_ratings_total") val userRatingsTotal: Int?
)

data class GoogleSummary(
    val overview: String?
)
