package com.crokodam.ghajinicrokoedition

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Create the preferences-based DataStore as an extension property
private val Context.dataStore by preferencesDataStore(name = "settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var intervalText by remember { mutableStateOf(TextFieldValue("")) }

    // Load the saved interval once the Composable launches
    LaunchedEffect(Unit) {
        val savedInterval = getSavedInterval(context)
        if (savedInterval > 0L) {
            intervalText = TextFieldValue(savedInterval.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Set reminder interval in minutes:")

            OutlinedTextField(
                value = intervalText,
                onValueChange = { newValue ->
                    if (newValue.text.all { it.isDigit() }) {
                        intervalText = newValue
                    }
                },
                label = { Text("Interval (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    val minutes = intervalText.text.toLongOrNull()
                    if (minutes != null && minutes > 0L) {
                        coroutineScope.launch {
                            saveInterval(context, minutes)
                            NotificationHelper.scheduleAlarm(context, minutes)
                            Toast.makeText(
                                context,
                                "Notifications scheduled every $minutes minutes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please enter a valid positive number",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Schedule")
            }
        }
    }
}

// Preference key for interval
private val INTERVAL_KEY = longPreferencesKey("notification_interval")

// Save interval to DataStore
suspend fun saveInterval(context: Context, interval: Long) {
    context.dataStore.edit { preferences ->
        preferences[INTERVAL_KEY] = interval
    }
}

// Read interval from DataStore
suspend fun getSavedInterval(context: Context): Long {
    val preferences = context.dataStore.data.first()
    return preferences[INTERVAL_KEY] ?: 0L
}
