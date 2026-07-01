package com.example.androidassignment4travelplannerapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.androidassignment4travelplannerapp.data.remote.ForecastResponse
import com.example.androidassignment4travelplannerapp.data.remote.WeatherResponse
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.ui.viewmodel.TravelViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: TravelViewModel,
    onBack: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val nearbyHotels by viewModel.nearbyHotels.collectAsState()
    val weather by viewModel.currentWeather.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val mapFocus by viewModel.mapFocus.collectAsState()
    val selectedDetail by viewModel.selectedPlaceDetail.collectAsState()
    val savedTrips by viewModel.savedTrips.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var showSaveDialog by remember { mutableStateOf(false) }
    var placeToSave by remember { mutableStateOf<Attraction?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Attractions, 1 = Hotels

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Explore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                // 1. Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = { Text("Search city or landmark...") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Discover Tabs
                if (suggestions.isEmpty() && weather != null) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions -> 
                            TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = MaterialTheme.colorScheme.primary) 
                        }
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("THINGS TO DO") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("HOTELS") })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        weather?.let {
                            if (suggestions.isEmpty()) {
                                HighEndWeatherMapCard(it, mapFocus, forecast, searchResults, viewModel) { showSaveDialog = true }
                            }
                        }
                    }

                    if (suggestions.isEmpty()) {
                        if (selectedTab == 0 && searchResults.isNotEmpty()) {
                            items(searchResults, key = { it.id }) { place ->
                                HighEndPlaceItem(
                                    place = place, 
                                    viewModel = viewModel,
                                    onAddClick = { placeToSave = it },
                                    onClick = {
                                        viewModel.setMapFocus(place.latitude, place.longitude)
                                        viewModel.fetchPlaceDetail(place.id)
                                    }
                                )
                            }
                            // Chunk Loading Button
                            item {
                                OutlinedButton(
                                    onClick = { weather?.let { viewModel.loadMoreNearby(it.coord.lat, it.coord.lon) } },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("EXPLORE MORE")
                                }
                            }
                        } else if (selectedTab == 1 && nearbyHotels.isNotEmpty()) {
                            items(nearbyHotels, key = { it.id }) { hotel ->
                                HotelCardItem(
                                    hotel = hotel,
                                    viewModel = viewModel,
                                    onPinClick = { 
                                        // Find first city trip to pin to
                                        val trip = savedTrips.find { it.destination.equals(weather?.name, true) }
                                        if (trip != null) {
                                            viewModel.pinHotel(trip.id, hotel)
                                            scope.launch { snackbarHostState.showSnackbar("Pinned to ${trip.title}!") }
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("Create a trip for this city first!") }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Suggestions Overlay
            if (suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = 56.dp + 4.dp).wrapContentHeight().shadow(12.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        suggestions.forEachIndexed { index, suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion.name ?: "", fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(suggestion.address ?: "Nearby location") },
                                modifier = Modifier.clickable { viewModel.selectSuggestion(suggestion); focusManager.clearFocus() }
                            )
                            if (index < suggestions.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSaveDialog && weather != null) {
        FullPageDateDialog(cityName = weather!!.name, onDismiss = { showSaveDialog = false }, onSave = { title, start, end ->
            viewModel.saveTrip(title, weather!!, forecast, start, end)
            showSaveDialog = false
            scope.launch { snackbarHostState.showSnackbar("Trip planned successfully!") }
        })
    }

    if (placeToSave != null) {
        AddToItineraryDialog(
            place = placeToSave!!,
            trips = savedTrips,
            currentCity = weather?.name ?: "",
            viewModel = viewModel,
            onDismiss = { placeToSave = null },
            onCreateTrip = { placeToSave = null; showSaveDialog = true },
            onAdd = { trip, day ->
                viewModel.addPlaceToTrip(trip.id, placeToSave!!, day)
                scope.launch { snackbarHostState.showSnackbar("Added to ${trip.title} - Day $day") }
                placeToSave = null
            }
        )
    }

    selectedDetail?.let {
        PremiumPlaceDetailSheet(it, viewModel.getPhotoUrl(it.photos?.firstOrNull()?.photoReference), onDismiss = viewModel::clearPlaceDetail)
    }
}

@Composable
fun HotelCardItem(hotel: Hotel, viewModel: TravelViewModel, onPinClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.setMapFocus(hotel.latitude, hotel.longitude) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.size(80.dp)) {
                if (hotel.photoReference != null) {
                    AsyncImage(model = viewModel.getPhotoUrl(hotel.photoReference), contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(hotel.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    Text("${hotel.rating ?: "N/A"} (${hotel.userRatingsTotal ?: 0})", style = MaterialTheme.typography.labelSmall)
                }
                Text(hotel.address, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
                
                Button(onClick = onPinClick, modifier = Modifier.padding(top = 4.dp).height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), shape = RoundedCornerShape(8.dp)) {
                    Text("PIN TO TRIP", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AddToItineraryDialog(
    place: Attraction, 
    trips: List<Trip>, 
    currentCity: String,
    viewModel: TravelViewModel,
    onDismiss: () -> Unit, 
    onCreateTrip: () -> Unit,
    onAdd: (Trip, Int) -> Unit
) {
    val relevantTrips = remember(trips, currentCity) {
        trips.filter { it.destination.equals(currentCity, ignoreCase = true) }
    }

    if (relevantTrips.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Trip Needed", fontWeight = FontWeight.Bold) },
            text = { Text("To add this attraction, you need to create a trip for '$currentCity' first.") },
            confirmButton = { Button(onClick = { onCreateTrip() }) { Text("Plan '$currentCity'") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
        return
    }

    var selectedTrip by remember { mutableStateOf(relevantTrips.first()) }
    val savedPlaces by viewModel.selectedTripPlaces.collectAsState()
    
    LaunchedEffect(selectedTrip) {
        viewModel.loadTripDetails(selectedTrip)
    }

    val isAlreadyInTrip = savedPlaces.any { it.id == place.id }
    val diffInMs = selectedTrip.endDate - selectedTrip.startDate
    val daysCount = (TimeUnit.MILLISECONDS.toDays(diffInMs).toInt() + 1).coerceIn(1, 30)
    var selectedDay by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Trip Day", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Adding: ${place.name}")
                Text("Select Itinerary:", style = MaterialTheme.typography.labelLarge)
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                    relevantTrips.forEach { trip ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTrip = trip }.padding(vertical = 4.dp)
                                .background(if (selectedTrip.id == trip.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
                        ) {
                            RadioButton(selected = selectedTrip.id == trip.id, onClick = { selectedTrip = trip })
                            Column {
                                Text(trip.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(trip.destination, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
                Text("Which Day?", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..daysCount).toList()) { day ->
                        FilterChip(selected = selectedDay == day, onClick = { selectedDay = day }, label = { Text("Day $day") })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedTrip, selectedDay) }, enabled = !isAlreadyInTrip) { 
                Text(if (isAlreadyInTrip) "Already in Trip" else "Save to Day $selectedDay") 
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun HighEndWeatherMapCard(
    weather: WeatherResponse, 
    mapFocus: Pair<Double, Double>?,
    forecast: ForecastResponse?,
    searchResults: List<Attraction>,
    viewModel: TravelViewModel,
    onSave: () -> Unit
) {
    val cityLatLng = LatLng(weather.coord.lat, weather.coord.lon)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(cityLatLng, 12f) }

    LaunchedEffect(mapFocus) {
        mapFocus?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.first, it.second), 14f)) }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(weather.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("${weather.main.temp.toInt()}°C • ${weather.weather.first().description}", color = MaterialTheme.colorScheme.primary)
                }
                Button(onClick = onSave) { Text("Save Trip") }
            }

            if (forecast != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(forecast.list.filterIndexed { i, _ -> i % 8 == 0 }.take(4)) { item ->
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.width(60.dp).padding(vertical = 4.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Text(SimpleDateFormat("EEE", Locale.getDefault()).format(Date(item.dt * 1000)), style = MaterialTheme.typography.labelSmall)
                                Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFB300))
                                Text("${item.main.temp.toInt()}°", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
                cameraPositionState = cameraPositionState
            ) {
                Marker(state = MarkerState(position = cityLatLng), title = weather.name)
                searchResults.forEach { place ->
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPageDateDialog(cityName: String, onDismiss: () -> Unit, onSave: (String, Long, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { 
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
    }
    val today = calendar.timeInMillis
    
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = today,
        initialSelectedEndDateMillis = today + 86400000 * 2,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= today
            }
            override fun isSelectableYear(year: Int): Boolean {
                return year >= calendar.get(Calendar.YEAR)
            }
        }
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Plan Journey", fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) } },
                        actions = {
                            TextButton(
                                onClick = { 
                                    onSave(
                                        title.ifBlank { "Trip to $cityName" }, 
                                        dateRangePickerState.selectedStartDateMillis ?: today, 
                                        dateRangePickerState.selectedEndDateMillis ?: today
                                    ) 
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("DONE", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp)
                        .fillMaxSize()
                ) {
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("Name of Journey") }, 
                        placeholder = { Text("e.g., Summer in $cityName") }, 
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), 
                        singleLine = true, 
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    DateRangePicker(
                        state = dateRangePickerState, 
                        title = { Text("Select your travel dates", modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
                        headline = null, 
                        showModeToggle = false, 
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun HighEndPlaceItem(place: Attraction, viewModel: TravelViewModel, onAddClick: (Attraction) -> Unit, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                if (place.photoReference != null) {
                    AsyncImage(model = viewModel.getPhotoUrl(place.photoReference), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    Text("${place.rating ?: "N/A"} (${place.totalRatings ?: 0})", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(place.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = { onAddClick(place) }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add to Trip", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
