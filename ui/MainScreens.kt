package com.example.lightcontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: DatabaseReference, onSettingsClick: () -> Unit) {

    // State variables
    var uptime by remember { mutableIntStateOf(0) }
    var ldrValue by remember { mutableIntStateOf(0) }
    var lightState by remember { mutableStateOf(false) }
    var manualState by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(false) }
    var previousUptime by remember { mutableIntStateOf(0) }
    var ipAddress by remember { mutableStateOf("Fetching...") }

    // Firebase References
    val sensorRef = database.child("sensor")
    val wifiRef = database.child("device").child("w")
    val controlRef = database.child("device").child("cs").child("c")
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

        controlRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val control = snapshot.value as? Map<*, *>
                control?.let {
                    lightState = (it["lc"] as? Long)?.toInt() == 1
                    manualState = (it["ms"] as? Long)?.toInt() == 1
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Light Control") },
                actions = {
                    IconButton(onClick = { onSettingsClick() }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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

            // LightState Button
            Button(
                onClick = {
                    lightState = !lightState
                    val lightStateValue = if (lightState) 1 else 0
                    controlRef.child("lc").setValue(lightStateValue).addOnCompleteListener {
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lightState) Color.Green else Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (lightState) "Light ON" else "Light OFF")
            }

            // ManualState Button
            Button(
                onClick = {
                    manualState = !manualState
                    val manualStateValue = if (manualState) 1 else 0
                    controlRef.child("ms").setValue(manualStateValue).addOnCompleteListener {
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (manualState) Color.Blue else Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (manualState) "Manual ON" else "Manual OFF")
            }
        }
    }
}