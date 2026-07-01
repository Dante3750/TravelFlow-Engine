package com.example.androidassignment4travelplannerapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val destination: String,
    val lat: Double,
    val lon: Double,
    val startDate: Long,
    val endDate: Long,
    val weatherInfo: String?,
    val forecastJson: String? = null,
    val photoReference: String? = null,
    val pinnedHotelJson: String? = null // Serialized Hotel object for persistence
)

@Entity(
    tableName = "saved_places",
    indices = [Index(value = ["tripId", "xid"], unique = true)]
)
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val name: String,
    val kinds: String,
    val lat: Double,
    val lon: Double,
    val xid: String,
    val dayNumber: Int = 1
)
