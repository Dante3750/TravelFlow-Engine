package com.example.androidassignment4travelplannerapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NominatimApiService {
    @GET("search")
    suspend fun searchLocations(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Header("User-Agent") userAgent: String = "FreePlanTravelApp"
    ): List<NominatimResponse>
}

data class NominatimResponse(
    val place_id: Long,
    val display_name: String,
    val lat: Double,
    val lon: Double,
    val address: NominatimAddress?
)

data class NominatimAddress(
    val city: String?,
    val town: String?,
    val village: String?,
    val state: String?,
    val country: String?
)
