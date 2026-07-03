# FreePlan Travel Itinerary Planner

A travel planning app for Android. Users can search destinations, browse nearby places, check weather, and build day-wise itineraries that work offline.

## Architecture

The project uses Clean Architecture with three layers.

### Domain Layer
Business logic and data models in plain Kotlin. Contains use cases for trip management and discovery.

### Data Layer
Handles Room for local storage and Retrofit for network calls. Fetches data from OpenTripMap, OpenStreetMap (Nominatim/Overpass), and OpenWeatherMap.

### Presentation Layer (MVVM)
Built with Jetpack Compose. ViewModels hold UI state and call Use Cases to run business logic.

## Tech Stack

* Kotlin, Jetpack Compose, Material 3
* Hilt for dependency injection
* Retrofit and OkHttp for networking
* Room for local database and offline cache
* Coroutines and Flow for async work
* OSMDroid (OpenStreetMap) for maps
* OpenTripMap API for discovery
* OpenWeatherMap API

## Key Features

* Destination search using OSM Nominatim
* Itinerary builder that organizes places by day
* Weather info per destination
* Offline access for saved trips
* Nearby attractions discovery via OpenTripMap
* Essential services (Police, Toilets) via OSM Overpass

## Setup

1. Clone the repository
2. Open or create local.properties in the project root
3. Add your API keys

WEATHER_API_KEY=your_key
OPEN_TRIP_MAP_KEY=your_key

local.properties is excluded from git.

4. Sync Gradle and run the app

## Implementation Notes

* Trip context filtering: the Add to Trip dialog only shows trips that match the city currently being explored.
* Discovery is anchored to the city center or pinned hotel for maximum relevance.
* Room is the single source of truth for the UI.

## Assumptions
* Free tier API keys and rate limits are sufficient for evaluation use.
* Device has internet access at least once, at trip creation time, to fetch initial data.
