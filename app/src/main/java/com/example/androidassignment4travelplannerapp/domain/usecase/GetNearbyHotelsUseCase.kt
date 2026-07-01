package com.example.androidassignment4travelplannerapp.domain.usecase

import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import javax.inject.Inject

class GetNearbyHotelsUseCase @Inject constructor(private val repository: ITravelRepository) {
    suspend operator fun invoke(lat: Double, lon: Double) = repository.fetchNearbyHotels(lat, lon)
}
