package com.example.androidassignment4travelplannerapp.domain.usecase

import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import javax.inject.Inject

class RemoveHotelUseCase @Inject constructor(private val repository: ITravelRepository) {
    suspend operator fun invoke(tripId: Int) = repository.removeHotelFromTrip(tripId)
}
