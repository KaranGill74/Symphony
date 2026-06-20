package com.example.ui.screens

import android.widget.Toast
import android.net.Uri
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.CategoryEntity
import com.example.data.database.SongEntity
import com.example.data.database.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel

data class SimpleAudioMeta(
    val title: String?,
    val artist: String?,
    val album: String?,
    val duration: String?
)

suspend fun copyMp3ToLocalStorage(
    context: android.content.Context,
    uri: Uri
): Pair<String, SimpleAudioMeta?> = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val fileName = "local_music_${System.currentTimeMillis()}.mp3"
    val outputDir = File(context.filesDir, "uploads")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val destFile = File(outputDir, fileName)
    
    resolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destFile).use { output ->
            input.copyTo(output)
        }
    }
    
    val localUriString = Uri.fromFile(destFile).toString()
    var meta: SimpleAudioMeta? = null
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        
        val durationFormatted = durationMs?.let { ms ->
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format("%d:%02d", minutes, seconds)
        }
        meta = SimpleAudioMeta(title, artist, albumName, durationFormatted)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Pair(localUriString, meta)
}

suspend fun copyImageToLocalStorage(
    context: android.content.Context,
    uri: Uri
): String = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val fileName = "local_thumb_${System.currentTimeMillis()}.jpg"
    val outputDir = File(context.filesDir, "uploads")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val destFile = File(outputDir, fileName)
    
    resolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destFile).use { output ->
            input.copyTo(output)
        }
    }
    Uri.fromFile(destFile).toString()
}

sealed interface AdminTab {
    object Overview : AdminTab
    object Songs : AdminTab
    object Users : AdminTab
    object Categories : AdminTab
    object Albums : AdminTab
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf<AdminTab>(AdminTab.Overview) }
    val metrics by adminViewModel.dashboardMetrics.collectAsState()
    val albumsList by adminViewModel.albums.collectAsState()

    // Add Music quick trigger
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickUploadType by remember { mutableStateOf("Song") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Pop") }
    var duration by remember { mutableStateOf("3:30") }
    var mp3Url by remember { mutableStateOf("") }
    var thumbnail by remember { mutableStateOf("") }
    var expandedAlbums by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopyingQuick by remember { mutableStateOf(false) }
    val quickMusicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCopyingQuick = true
            scope.launch {
                try {
                    val (localPath, meta) = copyMp3ToLocalStorage(context, uri)
                    mp3Url = localPath
                    if (meta != null) {
                        if (title.isBlank() && !meta.title.isNullOrBlank()) title = meta.title
                        if (artist.isBlank() && !meta.artist.isNullOrBlank()) artist = meta.artist
                        if (!meta.duration.isNullOrBlank()) duration = meta.duration
                        if (!meta.album.isNullOrBlank()) {
                            val matchedAlbum = albumsList.find { it.name.lowercase() == meta.album.lowercase() }
                            if (matchedAlbum != null) {
                                album = matchedAlbum.name
                            } else {
                                album = "Single"
                            }
                        }
                    }
                    Toast.makeText(context, "Local MP3 registered successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load MP3: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isCopyingQuick = false
                }
            }
        }
    }

    var isCopyingImageQuick by remember { mutableStateOf(false) }
    val quickImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCopyingImageQuick = true
            scope.launch {
                try {
                    val localPath = copyImageToLocalStorage(context, uri)
                    thumbnail = localPath
                    Toast.makeText(context, "Local thumbnail registered!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load thumbnail: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isCopyingImageQuick = false
                }
            }
        }
    }

    if (showQuickAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuickAddDialog = false
            },
            title = { Text("Quick Upload Center", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Two Options Selector Row: Song and Album
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { quickUploadType = "Song" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (quickUploadType == "Song") SpotifyGreen else SpotifyDarkGray,
                                contentColor = if (quickUploadType == "Song") SpotifyBlack else SpotifyWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("select_song_upload_btn")
                        ) {
                            Text("Song", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { 
                                quickUploadType = "Album"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (quickUploadType == "Album") SpotifyGreen else SpotifyDarkGray,
                                contentColor = if (quickUploadType == "Album") SpotifyBlack else SpotifyWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("select_album_upload_btn")
                        ) {
                            Text("Album (10 Songs)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (quickUploadType == "Song") {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Song Title") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen), modifier = Modifier.fillMaxWidth())
                    } else {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Album Title") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen), modifier = Modifier.fillMaxWidth())
                    }

                    OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen), modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre (e.g. Pop, English)") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen), modifier = Modifier.fillMaxWidth())
                    
                    // Quick Select Genre Chips
                    Text("Popular Genres:", color = SpotifyGrayText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickGenres = listOf("Punjabi", "English", "Pop", "Phonk", "Rock", "Lofi", "Classical", "Hip Hop", "Jazz", "Bollywood")
                        quickGenres.forEach { g ->
                            val isSelected = genre.equals(g, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SpotifyGreen else SpotifyDarkGray)
                                    .border(
                                        1.dp,
                                        if (isSelected) SpotifyGreen else SpotifyGrayText.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { genre = g }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = g,
                                    color = if (isSelected) SpotifyBlack else SpotifyWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    if (quickUploadType == "Song") {
                        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (e.g. 3:45)") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen), modifier = Modifier.fillMaxWidth())
                    }
                    
                    Text(if (quickUploadType == "Song") "Song Audio Source" else "Album Audio Source (Applied to Tracks)", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (isCopyingQuick) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SpotifyGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing local MP3...", color = SpotifyGreen, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { quickMusicPickerLauncher.launch("audio/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (quickUploadType == "Song") "SELECT LOCAL MP3 FILE" else "SELECT ALBUM LOCAL MP3 FILE", color = SpotifyWhite, fontSize = 12.sp)
                        }
                    }

                    if (mp3Url.startsWith("file://")) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyGreen.copy(alpha = 0.1f))
                                .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local MP3 Loaded", color = SpotifyWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(mp3Url.substringAfterLast("/"), color = SpotifyGrayText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { mp3Url = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = mp3Url, 
                        onValueChange = { mp3Url = it }, 
                        label = { Text(if (quickUploadType == "Song") "Or Web MP3 Stream URL" else "Or Album Web MP3 Stream URL") }, 
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Song/Album Thumbnail Photo", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (isCopyingImageQuick) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SpotifyGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing local photo...", color = SpotifyGreen, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { quickImagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHOOSE LOCAL IMAGE FILE", color = SpotifyWhite, fontSize = 12.sp)
                        }
                    }

                    if (thumbnail.startsWith("file://")) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyGreen.copy(alpha = 0.1f))
                                .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local Image Loaded", color = SpotifyWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(thumbnail.substringAfterLast("/"), color = SpotifyGrayText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { thumbnail = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = thumbnail, 
                        onValueChange = { thumbnail = it }, 
                        label = { Text("Or Thumbnail Image URL") }, 
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (artist.isBlank()) {
                            Toast.makeText(context, "Artist is required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (quickUploadType == "Song") {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Title is required.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            adminViewModel.uploadSong(title, artist, "Single", genre, duration, thumbnail, mp3Url)
                            Toast.makeText(context, "Song uploaded successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Album mode: Create new Album and Bulk generate 10 tracks to it
                            val targetAlbum = if (title.isNotBlank()) title.trim() else "New Album"
                            
                            val defaultMp3s = listOf(
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"
                            )
                            val defaultThumbs = listOf(
                                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
                                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
                                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400"
                            )

                            val finalThumb = if (thumbnail.isNotBlank()) thumbnail else defaultThumbs.first()

                            // Dynamically insert the Album Entity!
                            adminViewModel.addAlbum(targetAlbum, "Uploaded via Quick Center", finalThumb)

                            for (i in 1..10) {
                                val trackTitle = "$targetAlbum - Track $i"
                                val trackDuration = "${3 + (i % 2)}:${30 + (i * 3) % 25}"
                                val sampleThumb = finalThumb
                                val sampleMp3 = if (mp3Url.isNotBlank()) mp3Url else defaultMp3s[(i - 1) % defaultMp3s.size]
                                
                                adminViewModel.uploadSong(
                                    title = trackTitle,
                                    artist = artist,
                                    album = targetAlbum,
                                    genre = genre,
                                    duration = trackDuration,
                                    thumbnail = sampleThumb,
                                    mp3Url = sampleMp3
                                )
                            }
                            Toast.makeText(context, "Successfully uploaded 10 songs and created album: $targetAlbum!", Toast.LENGTH_SHORT).show()
                        }
                        showQuickAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text("Upload", color = SpotifyBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQuickAddDialog = false
                }) {
                    Text("Cancel", color = SpotifyWhite)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log out", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyDarkGray)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    title = ""
                    artist = ""
                    album = "Single"
                    genre = "Pop"
                    duration = "3:30"
                    mp3Url = ""
                    thumbnail = ""
                    quickUploadType = "Song"
                    showQuickAddDialog = true
                },
                containerColor = SpotifyGreen,
                contentColor = SpotifyBlack,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Track") },
                text = { Text("Add Track", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1C1B1F),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1C1B1F))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(32.dp))
            ) {
                NavigationBarItem(
                    selected = activeTab == AdminTab.Overview,
                    onClick = { activeTab = AdminTab.Overview },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                    label = { Text("Overview", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        unselectedIconColor = SpotifyGrayText,
                        unselectedTextColor = SpotifyGrayText,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == AdminTab.Songs,
                    onClick = { activeTab = AdminTab.Songs },
                    icon = { Icon(Icons.Default.AudioFile, contentDescription = "Songs") },
                    label = { Text("Songs", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        unselectedIconColor = SpotifyGrayText,
                        unselectedTextColor = SpotifyGrayText,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == AdminTab.Albums,
                    onClick = { activeTab = AdminTab.Albums },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Albums") },
                    label = { Text("Albums", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        unselectedIconColor = SpotifyGrayText,
                        unselectedTextColor = SpotifyGrayText,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == AdminTab.Users,
                    onClick = { activeTab = AdminTab.Users },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Users") },
                    label = { Text("Users", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        unselectedIconColor = SpotifyGrayText,
                        unselectedTextColor = SpotifyGrayText,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == AdminTab.Categories,
                    onClick = { activeTab = AdminTab.Categories },
                    icon = { Icon(Icons.Default.Category, contentDescription = "Genres") },
                    label = { Text("Genres", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        unselectedIconColor = SpotifyGrayText,
                        unselectedTextColor = SpotifyGrayText,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AdminTab.Overview -> AdminOverviewScreen(adminViewModel)
                AdminTab.Songs -> AdminSongsScreen(adminViewModel)
                AdminTab.Albums -> AdminAlbumsScreen(adminViewModel)
                AdminTab.Users -> AdminUsersScreen(adminViewModel)
                AdminTab.Categories -> AdminCategoriesScreen(adminViewModel)
            }
        }
    }
}

// TAB 1: METRICS OVERVIEW
@Composable
fun AdminOverviewScreen(adminViewModel: AdminViewModel) {
    val metrics by adminViewModel.dashboardMetrics.collectAsState()
    val popular by adminViewModel.popularSongs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("System Statistics", fontSize = 20.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)

        // Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(
                title = "Total Users",
                value = metrics.totalUsers.toString(),
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Total Songs",
                value = metrics.totalSongs.toString(),
                icon = Icons.Default.MusicNote,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(
                title = "Downloads",
                value = metrics.totalDownloads.toString(),
                icon = Icons.Default.DownloadDone,
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Total Plays",
                value = metrics.totalPlays.toString(),
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Analytics Popular Tracks Chart Listing
        Text("Analytics: Popular Songs", fontSize = 20.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (popular.isEmpty()) {
                    Text("No aggregate play telemetry available yet.", color = SpotifyGrayText)
                } else {
                    popular.forEachIndexed { idx, song ->
                        val maxCount = popular.firstOrNull()?.playCount ?: 1
                        val percentage = (song.playCount.toFloat() / maxCount.toFloat()).coerceIn(0.1f, 1f)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}. ${song.title}", color = SpotifyWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("${song.playCount} plays", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Progress bar as visual chart bar
                            LinearProgressIndicator(
                                progress = percentage,
                                color = SpotifyGreen,
                                trackColor = SpotifyLightGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: SONGS LIST & MANAGEMENT
@Composable
fun AdminSongsScreen(adminViewModel: AdminViewModel) {
    val songs by adminViewModel.allSongs.collectAsState()
    val categories by adminViewModel.categories.collectAsState()
    val albumsList by adminViewModel.albums.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    
    // Add/Edit inputs
    var editingSong by remember { mutableStateOf<SongEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var mp3Url by remember { mutableStateOf("") }
    var thumbnail by remember { mutableStateOf("") }
    var expandedAlbums by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scopeSongs = rememberCoroutineScope()
    var isCopyingSongs by remember { mutableStateOf(false) }
    val songsMusicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCopyingSongs = true
            scopeSongs.launch {
                try {
                    val (localPath, meta) = copyMp3ToLocalStorage(context, uri)
                    mp3Url = localPath
                    if (meta != null) {
                        if (title.isBlank() && !meta.title.isNullOrBlank()) title = meta.title
                        if (artist.isBlank() && !meta.artist.isNullOrBlank()) artist = meta.artist
                        if (!meta.duration.isNullOrBlank()) duration = meta.duration
                        if (!meta.album.isNullOrBlank()) {
                            val matchedAlbum = albumsList.find { it.name.lowercase() == meta.album.lowercase() }
                            if (matchedAlbum != null) {
                                album = matchedAlbum.name
                            } else {
                                album = "Single"
                            }
                        }
                    }
                    Toast.makeText(context, "Local MP3 registered successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load MP3: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isCopyingSongs = false
                }
            }
        }
    }

    var isCopyingImageSongs by remember { mutableStateOf(false) }
    val songsImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCopyingImageSongs = true
            scopeSongs.launch {
                try {
                    val localPath = copyImageToLocalStorage(context, uri)
                    thumbnail = localPath
                    Toast.makeText(context, "Local thumbnail registered!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load thumbnail: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isCopyingImageSongs = false
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                editingSong = null
            },
            title = { Text(if (editingSong == null) "Upload New Song" else "Edit Song Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Song Title") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen))
                    OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen))
                    
                    // Dropdown for Album selection (removed raw free text field)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (album.isEmpty()) "Single" else album,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Album") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedAlbums = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Album")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedAlbums,
                            onDismissRequest = { expandedAlbums = false },
                            modifier = Modifier.background(SpotifyDarkGray)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Single (No Album)", color = SpotifyWhite) },
                                onClick = {
                                    album = "Single"
                                    expandedAlbums = false
                                }
                            )
                            albumsList.forEach { alb ->
                                DropdownMenuItem(
                                    text = { Text(alb.name, color = SpotifyWhite) },
                                    onClick = {
                                        album = alb.name
                                        expandedAlbums = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Genre simple picker or text
                    OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre (e.g. Pop, English)") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen), modifier = Modifier.fillMaxWidth())
                    
                    // Quick Select Genre Chips
                    Text("Popular Genres:", color = SpotifyGrayText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickGenres = listOf("Punjabi", "English", "Pop", "Phonk", "Rock", "Lofi", "Classical", "Hip Hop", "Jazz", "Bollywood")
                        quickGenres.forEach { g ->
                            val isSelected = genre.equals(g, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SpotifyGreen else SpotifyDarkGray)
                                    .border(
                                        1.dp,
                                        if (isSelected) SpotifyGreen else SpotifyGrayText.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { genre = g }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = g,
                                    color = if (isSelected) SpotifyBlack else SpotifyWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (e.g. 3:45)") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen))
                    
                    Text("Song Audio Source", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (isCopyingSongs) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SpotifyGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing local MP3...", color = SpotifyGreen, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { songsMusicPickerLauncher.launch("audio/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SELECT LOCAL MP3 FILE", color = SpotifyWhite, fontSize = 12.sp)
                        }
                    }

                    if (mp3Url.startsWith("file://")) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyGreen.copy(alpha = 0.1f))
                                .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local MP3 Loaded", color = SpotifyWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(mp3Url.substringAfterLast("/"), color = SpotifyGrayText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { mp3Url = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = mp3Url, 
                        onValueChange = { mp3Url = it }, 
                        label = { Text("Or Web MP3 Stream URL") }, 
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Song Thumbnail Photo", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (isCopyingImageSongs) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SpotifyGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing local photo...", color = SpotifyGreen, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { songsImagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHOOSE LOCAL IMAGE FILE", color = SpotifyWhite, fontSize = 12.sp)
                        }
                    }

                    if (thumbnail.startsWith("file://")) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyGreen.copy(alpha = 0.1f))
                                .border(1.dp, SpotifyGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local Image Loaded", color = SpotifyWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(thumbnail.substringAfterLast("/"), color = SpotifyGrayText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { thumbnail = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = thumbnail, 
                        onValueChange = { thumbnail = it }, 
                        label = { Text("Or Thumbnail Image URL") }, 
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || artist.isBlank()) {
                            Toast.makeText(context, "Title and Artist are required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (editingSong == null) {
                            adminViewModel.uploadSong(title, artist, album, genre, duration, thumbnail, mp3Url)
                            Toast.makeText(context, "Song added successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            val updated = editingSong!!.copy(
                                title = title,
                                artist = artist,
                                album = album,
                                genre = genre,
                                duration = duration,
                                mp3Url = mp3Url,
                                thumbnail = thumbnail
                            )
                            adminViewModel.editSong(updated)
                            Toast.makeText(context, "Song updated!", Toast.LENGTH_SHORT).show()
                        }
                        showDialog = false
                        editingSong = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text("Submit", color = SpotifyBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    editingSong = null
                }) {
                    Text("Cancel", color = SpotifyWhite)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Songs", fontSize = 20.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        title = ""
                        artist = ""
                        album = ""
                        genre = "Pop"
                        duration = "4:12"
                        mp3Url = ""
                        thumbnail = ""
                        editingSong = null
                        showDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text("Add Song", color = SpotifyBlack)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs in database.", color = SpotifyGrayText)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(songs) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyDarkGray)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = song.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(song.title, fontSize = 14.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, fontSize = 12.sp, color = SpotifyGrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Row {
                                IconButton(onClick = {
                                    editingSong = song
                                    title = song.title
                                    artist = song.artist
                                    album = song.album
                                    genre = song.genre
                                    duration = song.duration
                                    mp3Url = song.mp3Url
                                    thumbnail = song.thumbnail
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SpotifyGreen)
                                }
                                IconButton(onClick = { adminViewModel.deleteSong(song.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 3: REGISTERED USERS MANAGEMENT
@Composable
fun AdminUsersScreen(adminViewModel: AdminViewModel) {
    val users by adminViewModel.filteredUsers.collectAsState()
    val query by adminViewModel.userSearchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("User Directory", fontSize = 20.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = query,
            onValueChange = { adminViewModel.setUserSearchQuery(it) },
            placeholder = { Text("Filter users by name or email...", color = SpotifyGrayText) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotifyGrayText) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching user credentials.", color = SpotifyGrayText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users) { usr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyDarkGray)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            ProfileAvatar(
                                name = usr.name,
                                imageUrl = usr.image,
                                modifier = Modifier.size(44.dp),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(usr.name, fontSize = 14.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Interactive Role Switcher Dropdown
                                    var expandedRoleMenu by remember { mutableStateOf(false) }
                                    Box {
                                        Card(
                                            onClick = { expandedRoleMenu = true },
                                            colors = CardDefaults.cardColors(
                                                containerColor = when (usr.role) {
                                                    "Admin" -> SpotifyGreen
                                                    "Blocked" -> Color.Red
                                                    else -> Color(0xFF282828)
                                                }
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = usr.role,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (usr.role == "Admin") SpotifyBlack else SpotifyWhite
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Switch Role",
                                                    tint = if (usr.role == "Admin") SpotifyBlack else SpotifyWhite,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = expandedRoleMenu,
                                            onDismissRequest = { expandedRoleMenu = false },
                                            modifier = Modifier.background(SpotifyDarkGray)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("User", color = SpotifyWhite) },
                                                onClick = {
                                                    adminViewModel.changeUserRole(usr.uid, "User")
                                                    expandedRoleMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Admin", color = SpotifyWhite) },
                                                onClick = {
                                                    adminViewModel.changeUserRole(usr.uid, "Admin")
                                                    expandedRoleMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Blocked", color = SpotifyWhite) },
                                                onClick = {
                                                    adminViewModel.changeUserRole(usr.uid, "Blocked")
                                                    expandedRoleMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Text("${usr.email} • @${usr.username}", fontSize = 12.sp, color = SpotifyGrayText)
                            }
                        }
                        // Admin overrides blocks and deletes
                        if (usr.username != "admin" && usr.name != "Symphony Admin") {
                            Row {
                                IconButton(onClick = { adminViewModel.deleteUser(usr.uid) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 4: CATEGORY / GENRES MANAGEMENT
@Composable
fun AdminCategoriesScreen(adminViewModel: AdminViewModel) {
    val categories by adminViewModel.categories.collectAsState()
    var newCategoryText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Manage Music Genres", fontSize = 20.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Input Category Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCategoryText,
                onValueChange = { newCategoryText = it },
                label = { Text("New Genre Style (e.g. classical)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newCategoryText.isNotBlank()) {
                        adminViewModel.addCategory(newCategoryText.trim())
                        newCategoryText = ""
                        Toast.makeText(context, "Genre registered!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                modifier = Modifier.height(56.dp)
            ) {
                Text("ADD", color = SpotifyBlack, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No genres configured in system.", color = SpotifyGrayText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyDarkGray)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicVideo, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(cat.name, fontSize = 16.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { adminViewModel.deleteCategory(cat.name) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// DASHBOARD REUSABLE METRICS CARD CARD
@Composable
fun AdminAlbumsScreen(adminViewModel: AdminViewModel) {
    val albums by adminViewModel.albums.collectAsState()
    var newAlbumTitle by remember { mutableStateOf("") }
    var newAlbumDesc by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Manage Music Albums", fontSize = 20.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Register New Album Style", color = SpotifyWhite, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = newAlbumTitle,
                    onValueChange = { newAlbumTitle = it },
                    label = { Text("Album Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newAlbumDesc,
                    onValueChange = { newAlbumDesc = it },
                    label = { Text("Short Description") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newAlbumTitle.isNotBlank()) {
                            adminViewModel.addAlbum(newAlbumTitle.trim(), newAlbumDesc.trim())
                            newAlbumTitle = ""
                            newAlbumDesc = ""
                            Toast.makeText(context, "Album registered successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Album title is required.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CREATE NEW ALBUM", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Registered System Albums", color = SpotifyWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        if (albums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No custom albums created yet.", color = SpotifyGrayText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(albums) { alb ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyDarkGray)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(alb.name, fontSize = 16.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
                                if (alb.description.isNotBlank()) {
                                    Text(alb.description, fontSize = 12.sp, color = SpotifyGrayText)
                                }
                            }
                        }
                        IconButton(onClick = { adminViewModel.deleteAlbum(alb.name) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// DASHBOARD REUSABLE METRICS CARD CARD
@Composable
fun DashboardCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = SpotifyGrayText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = SpotifyWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}
