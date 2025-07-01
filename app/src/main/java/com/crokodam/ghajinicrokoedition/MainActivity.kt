package com.crokodam.ghajinicrokoedition

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.crokodam.ghajinicrokoedition.ui.theme.GhajiniCrokoEditionTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GhajiniCrokoEditionTheme {
                AppNavigator()
            }
        }
    }

    @Composable
    fun AppNavigator() {
        var currentScreen by remember { mutableStateOf("main") }
        val context = LocalContext.current

        var notes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
        var photoUri by remember { mutableStateOf<Uri?>(null) }

        // Load notes when on notes screen
        LaunchedEffect(currentScreen) {
            if (currentScreen == "notes") {
                val db = AppDatabase.getDatabase(context)
                notes = db.noteDao().getAllNotes()
            }
        }

        // BackHandler: navigate back to main if not on main screen
        BackHandler(enabled = currentScreen != "main") {
            currentScreen = "main"
        }

        // Camera launcher
        val photoFile = remember { mutableStateOf<File?>(null) }
        val takePictureLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                photoUri?.let {
                    Toast.makeText(context, "Photo saved!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to take photo", Toast.LENGTH_SHORT).show()
                photoUri = null
            }
        }

        when (currentScreen) {
            "main" -> MainScreen(
                onShowNotes = { currentScreen = "notes" },
                onShowNotificationSettings = { currentScreen = "notifications" },
                onTakePhoto = {
                    // Create file for photo
                    val photoFileCreated = createImageFile(context)
                    photoFile.value = photoFileCreated
                    photoFileCreated?.also { file ->
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        photoUri = uri
                        takePictureLauncher.launch(uri)
                    } ?: run {
                        Toast.makeText(context, "Could not create file for photo", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            "notes" -> NoteListScreen(
                notes = notes,
                onBack = { currentScreen = "main" }
            )

            "notifications" -> NotificationsSettingsScreen(
                onBack = { currentScreen = "main" }
            )
        }

        // Show photo preview below main screen if photoUri is set
        if (currentScreen == "main" && photoUri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(photoUri),
                contentDescription = "Captured photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
            )
        }
    }

    @Composable
    fun NotificationsSettingsScreen(onBack: () -> Unit) {
        // Placeholder screen for notifications settings
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
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text("Notification settings will go here", modifier = Modifier.padding(16.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(
        onShowNotes: () -> Unit,
        onShowNotificationSettings: () -> Unit,
        onTakePhoto: () -> Unit
    ) {
        val context = LocalContext.current
        var noteText by remember { mutableStateOf(TextFieldValue("")) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(title = { Text("Ghajini - Croko Edition") })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Type something to remember") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take Photo")
                }

                Button(
                    onClick = {
                        val noteTextString = noteText.text.trim()
                        if (noteTextString.isNotEmpty()) {
                            val note = NoteEntity(text = noteTextString, timestamp = System.currentTimeMillis())
                            val db = AppDatabase.getDatabase(context)
                            CoroutineScope(Dispatchers.IO).launch {
                                db.noteDao().insertNote(note)
                            }
                            Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
                            noteText = TextFieldValue("")
                        } else {
                            Toast.makeText(context, "Please write something", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Note")
                }

                Button(
                    onClick = onShowNotes,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show Notes")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onShowNotificationSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Notification Settings")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NoteListScreen(notes: List<NoteEntity>, onBack: () -> Unit) {
        val grouped = notes.groupBy { note ->
            val hour = Calendar.getInstance().apply {
                timeInMillis = note.timestamp
            }.get(Calendar.HOUR_OF_DAY)

            when (hour) {
                in 0..5 -> "12am - 6am"
                in 6..11 -> "6am - 12pm"
                in 12..17 -> "12pm - 6pm"
                else -> "6pm - 12am"
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Your Notes") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                grouped.forEach { (timeRange, noteList) ->
                    item {
                        Text(
                            text = timeRange,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(noteList) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = note.text,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Helper: create image file for camera capture
    private fun createImageFile(context: Context): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir: File? = context.getExternalFilesDir(null)
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}
