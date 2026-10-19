package com.example.lightcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.lightcontrol.ui.theme.LightControlTheme
import com.google.firebase.database.FirebaseDatabase
import com.example.lightcontrol.ui.MainScreen
import com.example.lightcontrol.ui.SettingsScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightControlTheme {
                LightControlUI()
            }
        }
    }
}

@Composable
fun LightControlUI() {
    val database = FirebaseDatabase.getInstance().reference
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main_screen") {
        composable("main_screen") {
            MainScreen(
                database = database,
                onSettingsClick = {
                    navController.navigate("settings_screen")
                }
            )
        }
        composable("settings_screen") {
            SettingsScreen(
                database = database,
                onClose = { navController.popBackStack() }
            )
        }
        composable("wifi_settings_screen") {
            WiFiSettingsScreen(context = applicationContext)
        }
    }
}
