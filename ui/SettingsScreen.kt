package com.example.lightcontrol.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlin.math.floor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(database: DatabaseReference, onClose: () -> Unit) {

    // State variables
    var uptime by remember { mutableIntStateOf(0) }
    var ldrTimeout by remember { mutableFloatStateOf(0f) }
    var ldrNight by remember { mutableFloatStateOf(0f) }
    var ldrDay by remember { mutableFloatStateOf(0f) }
    var longitude by remember { mutableStateOf(TextFieldValue("0.0")) }
    var latitude by remember { mutableStateOf(TextFieldValue("0.0")) }
    var ldrValue by remember { mutableIntStateOf(0) }
    var isOnline by remember { mutableStateOf(false) }
    var previousUptime by remember { mutableIntStateOf(0) }
    var ipAddress by remember { mutableStateOf("Fetching...") }

    // Popup visibility state
    var popupVisible by remember { mutableStateOf(false) }

    // Firebase References
    val sensorRef = database.child("sensor")
    val wifiRef = database.child("device").child("w")
    val settingRef = database.child("device").child("cs").child("s")

    // Fetch data from Firebase
    LaunchedEffect(Unit) {
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensor = snapshot.value as? Map<*, *>
                sensor?.let {
                    uptime = (it["ut"] as? Long)?.toInt() ?: 0
                    ldrValue = (it["lv"] as? Long)?.toInt() ?: 0
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        settingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val device = snapshot.value as? Map<*, *>
                device?.let {
                    ldrTimeout = (it["lt"] as? Number)?.toFloat() ?: 1f
                    ldrNight = (it["ln"] as? Number)?.toFloat()?.coerceIn(0f, 4095f) ?: 200f
                    ldrDay = (it["ud"] as? Number)?.toFloat()?.coerceIn(0f, 4095f) ?: 800f
                    latitude = TextFieldValue((it["lat"] as? Number)?.toDouble()?.toString() ?: "23.1697")
                    longitude = TextFieldValue((it["lon"] as? Number)?.toDouble()?.toString() ?: "89.2167")
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        wifiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ipAddress = snapshot.child("ip").value as? String ?: "Unavailable"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    LaunchedEffect(uptime) {
        // If uptime hasn't changed for a few seconds, mark the device as offline
        if (uptime == previousUptime) {
            isOnline = false
        } else {
            isOnline = true
            previousUptime = uptime
        }
    }

    // Calculate time components from uptime
    val seconds = uptime % 60
    val minutes = (uptime / 60) % 60
    val hours = (uptime / 3600) % 24
    val days = floor((uptime / 86400).toDouble()).toInt()

    // Scaffold with top app bar for back button
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues -> // Add padding to adjust for the top bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Use padding from Scaffold to avoid overlap
                    .verticalScroll(rememberScrollState()), // Make the column scrollable
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(if (isOnline) Color.Green else Color.Red, CircleShape)
                    )
                    Text("Device Status: ${if (isOnline) "Online" else "Offline"}")
                    Text("IP: $ipAddress", Modifier.padding(start = 16.dp))
                }

                // Uptime Display
                Text("Uptime: ${days}d ${hours}h ${minutes}m ${seconds}s")

                // LDR Value Display
                Text("LDR Value: $ldrValue")

                // LDR Timeout Slider
                Text("LDR Timeout: ${ldrTimeout.toInt()}s")
                Slider(
                    value = ldrTimeout,
                    onValueChange = { ldrTimeout = it },
                    valueRange = 1f..120f
                )

                // LDR Night Slider
                Text("LDR Night: ${ldrNight.toInt()}")
                Slider(
                    value = ldrNight,
                    onValueChange = { ldrNight = it.coerceIn(0f, 4095f) },
                    valueRange = 0f..4095f
                )

                // LDR Day Slider
                Text("LDR Day: ${ldrDay.toInt()}")
                Slider(
                    value = ldrDay,
                    onValueChange = { ldrDay = it.coerceIn(0f, 4095f) },
                    valueRange = 0f..4095f
                )

                // Longitude Input
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Latitude Input
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Save Button
                Button(
                    onClick = {
                        val updatedValues = mapOf(
                            "lat" to latitude.text.toDoubleOrNull(),
                            "lon" to longitude.text.toDoubleOrNull(),
                            "ln" to ldrNight.toInt(),
                            "lt" to ldrTimeout.toInt(),
                            "ud" to ldrDay.toInt(),
                            "sd" to 1 // Set "sd" to 1
                        )

                        settingRef.updateChildren(updatedValues).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                popupVisible = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }

            // Show the Popup (AlertDialog) if the settings were saved
            if (popupVisible) {
                AlertDialog(
                    onDismissRequest = { popupVisible = false },
                    title = { Text(text = "Saved") },
                    text = { Text(text = "Saved to device") },
                    confirmButton = {
                        Button(onClick = { popupVisible = false }) {
                            Text(text = "OK")
                        }
                    }
                )
            }
        }
    )
}
