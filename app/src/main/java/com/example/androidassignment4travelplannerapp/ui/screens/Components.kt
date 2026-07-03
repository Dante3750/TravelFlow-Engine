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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.androidassignment4travelplannerapp.data.remote.ForecastResponse
import com.example.androidassignment4travelplannerapp.data.remote.GooglePlaceDetailModel
import com.example.androidassignment4travelplannerapp.data.remote.WeatherResponse
import com.example.androidassignment4travelplannerapp.domain.model.*
import com.example.androidassignment4travelplannerapp.ui.viewmodel.TravelViewModel
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    center: LatLng,
    zoom: Double = 15.0,
    markers: List<Pair<LatLng, String>> = emptyList()
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(center.lat, center.lng))
                
                markers.forEach { (pos, title) ->
                    val marker = Marker(this)
                    marker.position = GeoPoint(pos.lat, pos.lng)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = title
                    overlays.add(marker)
                }
            }
        },
        modifier = modifier,
        update = { view ->
            view.controller.animateTo(GeoPoint(center.lat, center.lng))
            view.controller.setZoom(zoom)
        }
    )
}

@Composable
fun PhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Image, 
                contentDescription = null, 
                modifier = Modifier.size(40.dp), 
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                "No photo available", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPlaceDetailSheet(
    detail: GooglePlaceDetailModel,
    photoUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val placeLatLng = LatLng(detail.geometry.location.lat, detail.geometry.location.lng)

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            },
                            error = { PhotoPlaceholder(Modifier.fillMaxSize()) }
                        )
                    } else {
                        PhotoPlaceholder(Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Text(
                    text = detail.name, 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.ExtraBold
                )

                detail.rating?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$it", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }

                detail.address?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Text(
                    text = detail.summary?.overview ?: "No detailed description available for this location.", 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    "Location Map", 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    OsmMapView(
                        modifier = Modifier.fillMaxSize(),
                        center = placeLatLng,
                        markers = listOf(placeLatLng to detail.name)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Button(
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${detail.name}"))
                        context.startActivity(intent)
                    }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPLORE MORE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPageDateDialog(
    cityName: String, 
    initialTitle: String = "",
    initialStart: Long? = null,
    initialEnd: Long? = null,
    onDismiss: () -> Unit, 
    onSave: (String, Long, Long) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { 
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
    }
    val today = calendar.timeInMillis
    
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart ?: today,
        initialSelectedEndDateMillis = initialEnd ?: (today + 86400000 * 2),
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
                        title = { Text(if (initialStart == null) "Plan Journey" else "Edit Journey", fontWeight = FontWeight.Bold) },
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
                    
                    DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        navigationContentColor = MaterialTheme.colorScheme.onSurface,
                        yearContentColor = MaterialTheme.colorScheme.onSurface,
                        currentYearContentColor = MaterialTheme.colorScheme.primary,
                        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                        dayContentColor = MaterialTheme.colorScheme.onSurface,
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = Color.Transparent,
                        dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSurface,
                        dividerColor = MaterialTheme.colorScheme.outlineVariant,
                    ).let { customColors ->
                        DateRangePicker(
                            state = dateRangePickerState, 
                            title = { Text("Select your travel dates", modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
                            headline = null, 
                            showModeToggle = false, 
                            modifier = Modifier.weight(1f),
                            colors = customColors
                        )
                    }
                }
            }
        }
    }
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
    val focusLatLng = mapFocus?.let { LatLng(it.first, it.second) } ?: cityLatLng

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
                    Text("${weather.main.temp.toInt()}°C • ${weather.weather.first().description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}", color = MaterialTheme.colorScheme.primary)
                }
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

            OsmMapView(
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
                center = focusLatLng,
                markers = searchResults.map { LatLng(it.latitude, it.longitude) to it.name } + (cityLatLng to weather.name)
            )
        }
    }
}

@Composable
fun BoutiqueAttractionCard(
    attraction: Attraction, 
    viewModel: TravelViewModel, 
    onAdd: (Attraction) -> Unit, 
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val photoUrl = viewModel.getPhotoUrl(attraction.photoReference)
                    if (photoUrl != null) {
                        AsyncImage(model = photoUrl, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        val icon = when(attraction.category) {
                            "Landmark" -> Icons.Default.LocationCity
                            "Religious Site" -> Icons.Default.TempleHindu
                            "Nature" -> Icons.Default.Landscape
                            "Museum" -> Icons.Default.Museum
                            "Entertainment" -> Icons.Default.LocalActivity
                            "Shopping" -> Icons.Default.ShoppingBag
                            else -> Icons.Default.Place
                        }
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        }
                    }
                }
                
                IconButton(
                    onClick = { onAdd(attraction) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(36.dp).background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = attraction.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.ExtraBold, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${attraction.rating ?: "N/A"} (${attraction.totalRatings ?: 0})", 
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
                
                attraction.trustScore?.let { trust ->
                    val (label, color, icon) = when (trust.tier) {
                        TrustTier.HIGHLY_TRUSTED -> Triple("Highly Trusted", Color(0xFF2E7D32), Icons.Default.Verified)
                        TrustTier.RELIABLE -> Triple("Reliable", Color(0xFFE65100), Icons.Default.GppGood)
                        TrustTier.LIMITED_DATA -> Triple("Limited Data", Color(0xFF616161), Icons.Default.Info)
                        TrustTier.UNVERIFIED -> Triple("Unverified", Color(0xFFC62828), Icons.Default.Warning)
                    }

                    Surface(
                        color = color.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = color
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "${trust.score}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = color
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = trust.reason,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                                color = color.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmenityItem(amenity: NearbyAmenity) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val (icon, color) = when(amenity.type) {
                AmenityType.TOILET -> Icons.Default.Wc to Color(0xFF795548)
                AmenityType.POLICE -> Icons.Default.LocalPolice to Color(0xFF1976D2)
                AmenityType.PHARMACY -> Icons.Default.LocalPharmacy to Color(0xFFD32F2F)
                AmenityType.TEA_STALL -> Icons.Default.EmojiFoodBeverage to Color(0xFFFFA000)
                AmenityType.SMOKING_SPOT -> Icons.Default.SmokingRooms to Color(0xFF616161)
            }
            
            Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(amenity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                amenity.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
            }
        }
    }
}

@Composable
fun HotelCardItem(hotel: Hotel, viewModel: TravelViewModel, onPinClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.setMapFocus(hotel.latitude, hotel.longitude) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                if (hotel.photoReference != null) {
                    AsyncImage(
                        model = viewModel.getPhotoUrl(hotel.photoReference),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${hotel.rating ?: "N/A"}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PREMIUM STAY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hotel.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = hotel.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${hotel.userRatingsTotal ?: 0} verified reviews",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onPinClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("STAY HERE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
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
fun PremiumItineraryPlaceItem(
    place: Attraction, 
    daysCount: Int,
    onDayChange: (Int) -> Unit,
    onClick: () -> Unit
) {
    var showDayPicker by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(place.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (daysCount > 1) {
                TextButton(onClick = { showDayPicker = true }) {
                    Text("Day ${place.dayNumber}", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
        }
    }

    if (showDayPicker) {
        AlertDialog(
            onDismissRequest = { showDayPicker = false },
            title = { Text("Move to Day") },
            text = {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..daysCount).toList()) { day ->
                        FilterChip(
                            selected = place.dayNumber == day,
                            onClick = { onDayChange(day); showDayPicker = false },
                            label = { Text("Day $day") }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDayPicker = false }) { Text("Cancel") } }
        )
    }
}
