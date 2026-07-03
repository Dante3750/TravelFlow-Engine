package com.example.androidassignment4travelplannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidassignment4travelplannerapp.ui.screens.HomeScreen
import com.example.androidassignment4travelplannerapp.ui.screens.SearchScreen
import com.example.androidassignment4travelplannerapp.ui.screens.TripDetailScreen
import com.example.androidassignment4travelplannerapp.ui.theme.AndroidAssignment4TravelPlannerAppTheme
import com.example.androidassignment4travelplannerapp.ui.viewmodel.TravelViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMdroid Configuration
        Configuration.getInstance().userAgentValue = packageName
        
        enableEdgeToEdge()
        setContent {
            AndroidAssignment4TravelPlannerAppTheme {
                TravelAppContent()
            }
        }
    }
}

@Composable
fun TravelAppContent() {
    val navController = rememberNavController()
    val viewModel: TravelViewModel = hiltViewModel()
    val savedTrips by viewModel.savedTrips.collectAsState()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToDetail = { tripId -> navController.navigate("detail/$tripId") }
            )
        }
        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("detail/{tripId}") { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")?.toIntOrNull()
            val trip = savedTrips.find { it.id == tripId }
            if (trip != null) {
                TripDetailScreen(
                    trip = trip,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSearch = { navController.navigate("search") }
                )
            }
        }
    }
}
