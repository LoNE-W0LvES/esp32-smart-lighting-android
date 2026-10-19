package com.example.lightcontrol.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WifiNetwork(
    val SSID: String,
    val RSSI: Int,
    val SignalStrength: Int,
    val PasswordProtected: Boolean
)

@Composable
fun WiFiSettingsScreen(context: Context) {
    val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var wifiNetworks by remember { mutableStateOf(listOf<WifiNetwork>()) }
    var isLoading by remember { mutableStateOf(false) }
    var deviceIp by remember { mutableStateOf(sharedPrefs.getString("deviceIp", "") ?: "") }
    var errorPopupVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Wi-Fi Settings", style = MaterialTheme.typography.titleLarge)

        // Button to set device IP
        Button(
            onClick = { /* logic to set Device IP */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Set Device IP")
        }

        // Scan Wi-Fi Networks button
        Button(
            onClick = {
                if (deviceIp.isEmpty()) {
                    errorPopupVisible = true
                } else {
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        val networks = fetchWifiNetworks("http://$deviceIp/scan-wifi")
                        wifiNetworks = networks
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Scanning..." else "Scan Wi-Fi Networks")
        }

        // Wi-Fi networks list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(wifiNetworks.size) { index ->
                val network = wifiNetworks[index]
                WiFiNetworkItem(network = network, onClick = { ssid = network.SSID })
            }
        }

        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("SSID") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = showPassword,
                onCheckedChange = { showPassword = it }
            )
            Text(text = "Show Password")
        }

        Button(
            onClick = {
                if (deviceIp.isEmpty()) {
                    errorPopupVisible = true
                } else {
                    val url = "http://$deviceIp/wifisave?s=${URLEncoder.encode(ssid, "UTF-8")}&p=${URLEncoder.encode(password, "UTF-8")}"
                    scope.launch(Dispatchers.IO) {
                        val result = sendSaveRequest(url)
                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Save Settings")
        }

        if (errorPopupVisible) {
            AlertDialog(
                onDismissRequest = { errorPopupVisible = false },
                title = { Text(text = "Error") },
                text = { Text(text = "Please set the device IP address first.") },
                confirmButton = {
                    Button(onClick = { errorPopupVisible = false }) {
                        Text(text = "OK")
                    }
                }
            )
        }
    }
}

@Composable
fun WiFiNetworkItem(network: WifiNetwork, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = network.SSID, style = MaterialTheme.typography.bodyLarge)
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "Signal: ${network.SignalStrength} dBm", style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (network.PasswordProtected) "Encrypted" else "Open",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

suspend fun fetchWifiNetworks(url: String): List<WifiNetwork> {
    val client = OkHttpClient()
    return try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
        if (response.isSuccessful) {
            val body = response.body?.string()
            body?.let { parseJsonToNetworkList(it) } ?: listOf()
        } else {
            listOf()
        }
    } catch (e: Exception) {
        Log.e("WiFi", "Error fetching Wi-Fi networks", e)
        listOf()
    }
}

fun parseJsonToNetworkList(json: String): List<WifiNetwork> {
    return try {
        val gson = Gson()
        val listType = object : TypeToken<List<WifiNetwork>>() {}.type
        gson.fromJson(json, listType) ?: listOf()
    } catch (e: Exception) {
        Log.e("WiFi", "Error parsing JSON", e)
        listOf()
    }
}

suspend fun sendSaveRequest(url: String): String {
    val client = OkHttpClient()
    return try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
        if (response.isSuccessful) {
            response.body?.string() ?: "Settings saved successfully!"
        } else {
            "Error saving settings: ${response.message}"
        }
    } catch (e: Exception) {
        "Failed to save settings: ${e.localizedMessage}"
    }
}
