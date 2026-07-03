package com.example.androidassignment4travelplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.androidassignment4travelplannerapp.data.remote.WeatherResponse
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.ui.viewmodel.TravelViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: TravelViewModel,
    onBack: () -> Unit
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.clearSearchState() }
    }

    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val nearbyHotels by viewModel.nearbyHotels.collectAsState()
    val weather by viewModel.currentWeather.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val mapFocus by viewModel.mapFocus.collectAsState()
    val selectedDetail by viewModel.selectedPlaceDetail.collectAsState()
    val activeCity by viewModel.activeDiscoveryCity.collectAsState()
    val savedTrips by viewModel.savedTrips.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var showSaveDialog by remember { mutableStateOf(false) }
    var placeToSave by remember { mutableStateOf<Attraction?>(null) }
    var selectedTab by remember { mutableStateOf(0) } 
    var selectedCategory by remember { mutableStateOf("All") }

    val activeCityTrip = remember(savedTrips, activeCity, weather) {
        val city = weather?.name ?: activeCity
        if (city == null) null else {
            savedTrips.find { trip ->
                val dest = trip.destination.lowercase().trim()
                val target = city.lowercase().trim()
                dest == target || target.contains(dest) || dest.contains(target)
            }
        }
    }

    val categories = listOf(
        "Top Picks" to "Tourist Spot",
        "Landmarks" to "Landmark", 
        "Nature & Parks" to "Nature", 
        "Culture" to "Museum", 
        "Religious" to "Religious Site", 
        "Shopping" to "Shopping",
        "Entertainment" to "Entertainment"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Explore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (activeCityTrip != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "TRIP ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (weather != null || activeCity != null) {
                        TextButton(onClick = { showSaveDialog = true }) {
                            Text("SAVE TRIP", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = { 
                        if (activeCity != null && query.isEmpty()) {
                            Text("Recommendations for $activeCity")
                        } else {
                            Text("Search city or landmark...") 
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (suggestions.isEmpty() && (weather != null || searchResults.isNotEmpty() || activeCity != null)) {
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
                    
                    if (selectedTab == 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == "All",
                                    onClick = { selectedCategory = "All" },
                                    label = { Text("All") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            items(categories) { (label, id) ->
                                FilterChip(
                                    selected = selectedCategory == id,
                                    onClick = { selectedCategory = id },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
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
                        if (selectedTab == 0) {
                            val displayCategories = if (selectedCategory == "All") categories else categories.filter { it.second == selectedCategory }
                            
                            displayCategories.forEach { (label, id) ->
                                val categoryResults = searchResults.filter { it.category == id }
                                if (categoryResults.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                            Text("See all", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            contentPadding = PaddingValues(bottom = 8.dp)
                                        ) {
                                            items(categoryResults) { attraction ->
                                                BoutiqueAttractionCard(
                                                    attraction = attraction,
                                                    viewModel = viewModel,
                                                    onAdd = { placeToSave = it },
                                                    onClick = { 
                                                        viewModel.setMapFocus(attraction.latitude, attraction.longitude)
                                                        viewModel.fetchPlaceDetail(attraction.id) 
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (selectedCategory == "All") {
                                val unmatched = searchResults.filter { res -> categories.none { it.second == res.category } }
                                if (unmatched.isNotEmpty()) {
                                    item {
                                        Text("Other Gems", modifier = Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            items(unmatched) { attraction ->
                                                BoutiqueAttractionCard(attraction, viewModel, onAdd = { placeToSave = it }, onClick = { viewModel.fetchPlaceDetail(attraction.id) })
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (selectedTab == 1 && nearbyHotels.isNotEmpty()) {
                            items(nearbyHotels, key = { it.id }) { hotel ->
                                HotelCardItem(
                                    hotel = hotel,
                                    viewModel = viewModel,
                                    onPinClick = { 
                                        val cityKey = weather?.name ?: activeCity
                                        val trip = savedTrips.find { it.destination.equals(cityKey, true) }
                                        if (trip != null) {
                                            viewModel.pinHotel(trip.id, hotel)
                                            scope.launch { snackbarHostState.showSnackbar("Pinned to ${trip.title}!") }
                                        } else {
                                            showSaveDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

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

    if (showSaveDialog && (weather != null || activeCity != null)) {
        FullPageDateDialog(cityName = weather?.name ?: activeCity ?: "", onDismiss = { showSaveDialog = false }, onSave = { title, start, end ->
            val targetWeather = weather ?: WeatherResponse(
                main = com.example.androidassignment4travelplannerapp.data.remote.MainWeather(0.0, 0.0, 0),
                weather = emptyList(),
                name = activeCity ?: "",
                coord = com.example.androidassignment4travelplannerapp.data.remote.Coord(mapFocus?.first ?: 0.0, mapFocus?.second ?: 0.0)
            )
            viewModel.saveTrip(title, targetWeather, forecast, start, end)
            showSaveDialog = false
            scope.launch { snackbarHostState.showSnackbar("Trip planned successfully!") }
        })
    }

    if (placeToSave != null) {
        AddToItineraryDialog(
            place = placeToSave!!,
            trips = savedTrips,
            currentCity = weather?.name ?: activeCity ?: "",
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
