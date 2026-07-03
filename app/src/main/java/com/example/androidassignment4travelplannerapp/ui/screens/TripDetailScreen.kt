package com.example.androidassignment4travelplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.androidassignment4travelplannerapp.data.remote.ForecastResponse
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.ui.viewmodel.TravelViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371 // Radius of the earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

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

    LaunchedEffect(trip) { viewModel.loadTripDetails(trip) }

    var showEditDates by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(trip.title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { 
                    IconButton(onClick = { showEditDates = true }) { Icon(Icons.Default.EditCalendar, contentDescription = "Edit Dates") }
                    IconButton(onClick = { viewModel.deleteTrip(trip.id); onBack() }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) } 
                },
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
                                            Icon(imageVector = Icons.Default.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

                    OsmMapView(
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(20.dp)),
                        center = mapFocus?.let { LatLng(it.first, it.second) } ?: cityLatLng,
                        markers = savedPlaces.map { LatLng(it.latitude, it.longitude) to it.name } + (cityLatLng to trip.destination)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Itinerary Plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
            }

            for (day in 1..daysCount) {
                val placesForDay = groupedPlaces[day] ?: emptyList()
                
                if (placesForDay.isNotEmpty() || day <= daysCount) {
                    item {
                        val dayDate = Date(trip.startDate + (day - 1) * 86400000L)
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Text(
                                text = "Day $day", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(dayDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    if (placesForDay.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    "No activities planned yet. Tap 'Find More' to add attractions.", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // ITINERARY HEALTH CHECK: Conflict warnings
                        if (placesForDay.size >= 2) {
                            val distances = placesForDay.zipWithNext { a, b ->
                                calculateDistance(a.latitude, a.longitude, b.latitude, b.longitude)
                            }
                            if (distances.any { it > 40 }) {
                                item {
                                    Surface(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Tight Schedule: Some spots are 40km+ apart.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }
                        }

                        items(placesForDay, key = { it.id }) { place ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                PremiumItineraryPlaceItem(
                                    place = place,
                                    daysCount = daysCount,
                                    onDayChange = { newDay -> viewModel.addPlaceToTrip(trip.id, place, newDay) },
                                    onClick = { 
                                        viewModel.setMapFocus(place.latitude, place.longitude)
                                        viewModel.fetchPlaceDetail(place.id)
                                    }
                                )
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
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
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

    if (showEditDates) {
        FullPageDateDialog(
            cityName = trip.destination,
            initialTitle = trip.title,
            initialStart = trip.startDate,
            initialEnd = trip.endDate,
            onDismiss = { showEditDates = false },
            onSave = { _, start, end ->
                viewModel.updateTripDates(trip.id, start, end)
                showEditDates = false
            }
        )
    }
}
