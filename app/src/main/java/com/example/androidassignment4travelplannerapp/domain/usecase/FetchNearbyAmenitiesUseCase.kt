package com.example.androidassignment4travelplannerapp.domain.usecase

import com.example.androidassignment4travelplannerapp.domain.model.NearbyAmenity
import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import javax.inject.Inject

class FetchNearbyAmenitiesUseCase @Inject constructor(
    private val repository: ITravelRepository
) {
    suspend operator fun invoke(lat: Double, lon: Double): List<NearbyAmenity> {
        return repository.fetchNearbyAmenities(lat, lon)
    }
}
