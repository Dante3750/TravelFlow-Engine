package com.example.androidassignment4travelplannerapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TravelApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
