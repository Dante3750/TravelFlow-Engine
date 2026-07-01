package com.example.androidassignment4travelplannerapp.domain.usecase

import com.example.androidassignment4travelplannerapp.domain.model.Hotel
import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import javax.inject.Inject

class PinHotelUseCase @Inject constructor(private val repository: ITravelRepository) {
    suspend operator fun invoke(tripId: Int, hotel: Hotel) = repository.pinHotelToTrip(tripId, hotel)
}
