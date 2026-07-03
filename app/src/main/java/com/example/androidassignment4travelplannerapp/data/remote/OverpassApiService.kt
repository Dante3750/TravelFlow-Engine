package com.example.androidassignment4travelplannerapp.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApiService {
    @GET("api/interpreter")
    suspend fun getNearbyPOIs(
        @Query("data") data: String
    ): OverpassResponse
}

data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val tags: Map<String, String>?
)
