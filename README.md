# WePlan: Travel Itinerary & Weather Analytics

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com/android)
[![Architecture](https://img.shields.io/badge/Architecture-Clean--Arch-blue.svg)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![Tech](https://img.shields.io/badge/UI-Jetpack--Compose-orange.svg)](https://developer.android.com/jetpack/compose)

A high-performance Travel Planning application built to demonstrate **Clean Architecture**, **Dependency Injection**, and **Offline-First** synchronization. WePlan allows users to build complex, multi-day itineraries with real-time weather integration and intelligent destination search.

---

## 🏗 Modular Architecture

The application is structured following **Uncle Bob's Clean Architecture** principles, ensuring that business logic is completely decoupled from the Android framework and UI.

- **Domain Layer**: 100% Pure Kotlin. Contains Entities, Use Cases, and Repository Interfaces. No dependencies on Android libraries.
- **Data Layer**: Handles data persistence (Room) and network communication (Retrofit). Implements repository interfaces defined in the Domain layer.
- **Presentation Layer**: Built with **Jetpack Compose** following the MVVM pattern. Uses `StateFlow` for reactive UI updates.

---

## 🚀 Technical Features

### 📡 Offline-First Synchronization
Utilizes a **Single Source of Truth (SSOT)** strategy. All data fetched from the Google Places and OpenWeather APIs is cached in a local **Room** database, ensuring a seamless user experience even in zero-connectivity environments.

### ⚡ Background Processing (WorkManager)
Implemented periodic background sync using **WorkManager** to update weather forecasts for upcoming trips, ensuring users always have the latest data without manually refreshing the app.

### 💰 API Cost Optimization
Leveraged **Google Autocomplete Session Tokens**. This ensures that multiple keystrokes in a single search session are billed as a single request, significantly reducing API overhead and operational costs.

### 💉 Dependency Injection
Uses **Hilt** for scoped dependency management, facilitating easier testing and better resource lifecycle handling.

---

## 🛠 Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Database**: Room
- **Networking**: Retrofit & OkHttp
- **DI**: Hilt
- **Async**: Coroutines & Flow
- **Background**: WorkManager

---

## 🏃 Getting Started
1. Clone the repository.
2. Add your API keys to `local.properties`:
   ```properties
   GOOGLE_MAPS_KEY=your_key_here
   WEATHER_API_KEY=your_key_here
   ```
3. Sync Gradle and run the `:app` module.

---

## 📝 License
MIT License
