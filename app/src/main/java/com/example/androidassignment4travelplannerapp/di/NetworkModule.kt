package com.example.androidassignment4travelplannerapp.di

import com.example.androidassignment4travelplannerapp.data.remote.OverpassApiService
import com.example.androidassignment4travelplannerapp.data.remote.TravelApiService
import com.example.androidassignment4travelplannerapp.data.remote.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("WeatherRetrofit")
    fun provideWeatherRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("TravelRetrofit")
    fun provideTravelRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("OverpassRetrofit")
    fun provideOverpassRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(@Named("WeatherRetrofit") retrofit: Retrofit): WeatherApiService {
        return retrofit.create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTravelApiService(@Named("TravelRetrofit") retrofit: Retrofit): TravelApiService {
        return retrofit.create(TravelApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOverpassApiService(@Named("OverpassRetrofit") retrofit: Retrofit): OverpassApiService {
        return retrofit.create(OverpassApiService::class.java)
    }
}
