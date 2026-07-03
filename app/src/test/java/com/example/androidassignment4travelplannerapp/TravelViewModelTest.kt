package com.example.androidassignment4travelplannerapp

import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.domain.repository.ITravelRepository
import com.example.androidassignment4travelplannerapp.domain.usecase.*
import com.example.androidassignment4travelplannerapp.ui.viewmodel.TravelViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TravelViewModelTest {

    private lateinit var viewModel: TravelViewModel
    private val repository: ITravelRepository = mockk(relaxed = true)
    
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getSavedTrips() } returns flowOf(emptyList())
        
        viewModel = TravelViewModel(
            GetTripsUseCase(repository),
            SearchLocationsUseCase(repository),
            GetNearbyAttractionsUseCase(repository),
            GetNearbyHotelsUseCase(repository),
            SaveTripUseCase(repository),
            DeleteTripUseCase(repository),
            SyncTripWeatherUseCase(repository),
            AddAttractionUseCase(repository),
            GetWeatherUseCase(repository),
            FetchForecastJsonUseCase(repository),
            GetPlaceDetailsUseCase(repository),
            GetAttractionsForTripUseCase(repository),
            GetPhotoUrlUseCase(repository),
            PinHotelUseCase(repository),
            RemoveHotelUseCase(repository),
            UpdateTripDatesUseCase(repository),
            FetchNearbyAmenitiesUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel should initialize with empty trips state`() {
        assertEquals(0, viewModel.savedTrips.value.size)
    }

    @Test
    fun `onQueryChanged should update suggestions when query is valid`() = runTest {
        val mockPlaces = listOf(PlaceSuggestion("1", "Paris", "France", 48.8, 2.3))
        coEvery { repository.searchLocations("Paris") } returns mockPlaces

        viewModel.onQueryChanged("Paris")
        advanceUntilIdle()

        assertEquals(mockPlaces, viewModel.suggestions.value)
    }

    @Test
    fun `selectSuggestion should update results`() = runTest {
        val mockPlace = PlaceSuggestion("1", "London", "UK", 51.5, -0.1)
        
        coEvery { repository.fetchNearbyAttractions(51.5, -0.1, any(), any()) } returns Pair(emptyList(), null)

        viewModel.selectSuggestion(mockPlace)

        assertEquals("London", viewModel.activeDiscoveryCity.value)
    }

    @Test
    fun `deleteTrip should call delete usecase`() = runTest {
        viewModel.deleteTrip(123)
        coVerify { repository.deleteExistingTrip(123) }
    }
}
