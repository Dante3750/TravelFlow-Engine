package com.example.androidassignment4travelplannerapp.domain.usecase

import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import javax.inject.Inject

class UpdateTripDatesUseCase @Inject constructor(
    private val repository: ITravelRepository
) {
    suspend operator fun invoke(tripId: Int, start: Long, end: Long) {
        repository.updateTripDates(tripId, start, end)
    }
}
