package com.example.androidassignment4travelplannerapp.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenTripMapApiService {
    @GET("0.1/en/places/geoname")
    suspend fun getGeoname(
        @Query("name") name: String,
        @Query("apikey") apiKey: String
    ): OtmGeonameResponse

    @GET("0.1/en/places/radius")
    suspend fun getPlacesInRadius(
        @Query("radius") radius: Int,
        @Query("lon") lon: Double,
        @Query("lat") lat: Double,
        @Query("kinds") kinds: String = "interesting_places",
        @Query("format") format: String = "json",
        @Query("apikey") apiKey: String
    ): List<OtmPlaceModel>

    @GET("0.1/en/places/xid/{xid}")
    suspend fun getPlaceDetail(
        @Path("xid") xid: String,
        @Query("apikey") apiKey: String
    ): OtmPlaceDetailModel
}

data class OtmGeonameResponse(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String?
)

data class OtmPlaceModel(
    val xid: String,
    val name: String,
    val dist: Double,
    val rate: Int,
    val kinds: String,
    val point: OtmPoint
)

data class OtmPoint(
    val lat: Double,
    val lon: Double
)

data class OtmPlaceDetailModel(
    val xid: String,
    val name: String,
    val address: OtmAddress?,
    val image: String?,
    val preview: OtmPreview?,
    val wikipedia_extracts: OtmWiki?,
    val point: OtmPoint,
    val kinds: String,
    val otm: String?
)

data class OtmAddress(
    val city: String?,
    val road: String?,
    val house_number: String?,
    val suburb: String?
)

data class OtmPreview(
    val source: String?
)

data class OtmWiki(
    val title: String?,
    val text: String?
)
