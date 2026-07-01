package com.example.androidassignment4travelplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.androidassignment4travelplannerapp.data.remote.ForecastResponse
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.ui.viewmodel.TravelViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip,
    viewModel: TravelViewModel,
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
) {
    val savedPlaces by viewModel.selectedTripPlaces.collectAsState()
    val mapFocus by viewModel.mapFocus.collectAsState()
    val selectedDetail by viewModel.selectedPlaceDetail.collectAsState()

    val cityLatLng = LatLng(trip.latitude, trip.longitude)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(cityLatLng, 12f) }

    LaunchedEffect(trip) { viewModel.loadTripDetails(trip) }
    
    LaunchedEffect(mapFocus) { 
        mapFocus?.let {
            val target = LatLng(it.first, it.second)
            if ((cameraPositionState.position.target.latitude != target.latitude) || 
                (cameraPositionState.position.target.longitude != target.longitude)) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(trip.title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { viewModel.deleteTrip(trip.id); onBack() }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val diffInMs = trip.endDate - trip.startDate
        val daysCount = (TimeUnit.MILLISECONDS.toDays(diffInMs).toInt() + 1).coerceIn(1, 60)
        val groupedPlaces = savedPlaces.groupBy { it.dayNumber }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "${sdf.format(Date(trip.startDate))} - ${sdf.format(Date(trip.endDate))}", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(trip.destination, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)

                    trip.weatherSummary?.let { info ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(info, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }

                    // 1. Pinned Hotel Card (The Anchor)
                    trip.pinnedHotel?.let { hotel ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Your Stay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.size(60.dp)) {
                                    if (hotel.photoReference != null) {
                                        AsyncImage(model = viewModel.getPhotoUrl(hotel.photoReference), contentDescription = null, contentScale = ContentScale.Crop)
                                    } else {
                                        Box(Modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hotel.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(hotel.address, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
                                }
                                IconButton(onClick = { viewModel.unpinHotel(trip.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Unpin", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    val savedForecast = remember(trip.forecastJson) {
                        try {
                            com.google.gson.Gson().fromJson(trip.forecastJson, ForecastResponse::class.java)
                        } catch (_: Exception) { null }
                    }

                    savedForecast?.let { forecastData ->
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val forecastList = forecastData.list.asSequence().filterIndexed { index, _ -> index % 8 == 0 }.take(4).toList()
                            items(items = forecastList) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.width(60.dp).padding(vertical = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                        Text(SimpleDateFormat("EEE", Locale.getDefault()).format(Date(item.dt * 1000)), style = MaterialTheme.typography.labelSmall)
                                        Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFB300))
                                        Text("${item.main.temp.toInt()}°", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GoogleMap(
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(20.dp)), 
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            myLocationButtonEnabled = false
                        )
                    ) {
                        // Trip Center or Hotel Anchor
                        val mainMarkerPos = trip.pinnedHotel?.let { LatLng(it.latitude, it.longitude) } ?: cityLatLng
                        Marker(
                            state = MarkerState(position = mainMarkerPos), 
                            title = trip.pinnedHotel?.name ?: trip.destination,
                            icon = if (trip.pinnedHotel != null) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) else null
                        )
                        
                        savedPlaces.forEach { place ->
                            Marker(
                                state = MarkerState(position = LatLng(place.latitude, place.longitude)), 
                                title = place.name, 
                                onClick = { 
                                    viewModel.setMapFocus(place.latitude, place.longitude)
                                    viewModel.fetchPlaceDetail(place.id)
                                    true 
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Itinerary Plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
            }

            for (day in 1..daysCount) {
                val placesForDay = groupedPlaces[day] ?: emptyList()
                
                if (placesForDay.isNotEmpty() || day == 1) {
                    item {
                        Text(
                            "Day $day", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    
                    if (placesForDay.isEmpty()) {
                        item {
                            Text(
                                "No activities planned for this day.", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    } else {
                        items(placesForDay, key = { it.id }) { place ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                PremiumItineraryPlaceItem(place) { 
                                    viewModel.setMapFocus(place.latitude, place.longitude)
                                    viewModel.fetchPlaceDetail(place.id)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.startDiscoveryForCity(trip.destination, trip.latitude, trip.longitude, trip)
                            onNavigateToSearch()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FIND MORE TO ADD", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    selectedDetail?.let {
        PremiumPlaceDetailSheet(
            it, 
            viewModel.getPhotoUrl(it.photos?.firstOrNull()?.photoReference), 
            onDismiss = viewModel::clearPlaceDetail
        )
    }
}

@Composable
fun PremiumItineraryPlaceItem(place: Attraction, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp)) {}
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(place.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
