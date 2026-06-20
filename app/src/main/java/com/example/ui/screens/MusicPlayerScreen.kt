package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.PlaylistEntity
import com.example.data.database.SongEntity
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel
import java.util.Locale

@Composable
fun MusicPlayerScreen(
    musicViewModel: MusicViewModel,
    onClose: () -> Unit
) {
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val progress by musicViewModel.progress.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    val isShuffle by musicViewModel.isShuffleEnabled.collectAsState()
    val isRepeat by musicViewModel.isRepeatEnabled.collectAsState()
    
    val favoriteSongs by musicViewModel.favoriteSongs.collectAsState()
    val downloadedSongs by musicViewModel.downloadedSongs.collectAsState()
    val downloadStates by musicViewModel.downloadStates.collectAsState()
    val context = LocalContext.current
    
    var currentVolume by remember { mutableStateOf(1f) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var localSliderValue by remember { mutableStateOf(0f) }

    // Sync local slider during playback unless user is actively dragging it
    LaunchedEffect(progress) {
        if (!isDraggingSlider) {
            localSliderValue = progress.toFloat()
        }
    }

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("No track active.", color = SpotifyWhite)
        }
        return
    }

    val song = currentSong!!

    val playerGradient = remember(song.title) {
        val hash = song.title.hashCode().coerceAtLeast(0)
        val gradients = listOf(
            listOf(Color(0xFF3E2D60), Color(0xFF1B142A), Color(0xFF121212)),
            listOf(Color(0xFF1B3D6A), Color(0xFF10213B), Color(0xFF121212)),
            listOf(Color(0xFF5E2034), Color(0xFF2C101A), Color(0xFF121212)),
            listOf(Color(0xFF1B5E4A), Color(0xFF0F2C23), Color(0xFF121212)),
            listOf(Color(0xFF503E1C), Color(0xFF251D0E), Color(0xFF121212)),
            listOf(Color(0xFF4C1B5E), Color(0xFF240F2C), Color(0xFF121212))
        )
        val triple = gradients[hash % gradients.size]
        Brush.verticalGradient(triple)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(playerGradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Drop down button & Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = song.album.uppercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    color = SpotifyGrayText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* Decorative options */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = SpotifyWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large album artwork card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = SpotifyLightGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                AsyncImage(
                    model = song.thumbnail,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track Details & Fav Key Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 24.sp,
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontSize = 16.sp,
                        color = SpotifyGrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                val isFavorite = favoriteSongs.any { it.id == song.id }
                val isDownloaded = downloadedSongs.any { it.id == song.id }
                val isDownloading = downloadStates[song.id] ?: false
                val playlists by musicViewModel.playlists.collectAsState()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Add to Playlist Button (+)
                    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showAddToPlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            tint = SpotifyWhite,
                            contentDescription = "Add to playlist"
                        )
                    }

                    if (showAddToPlaylistDialog) {
                        var newPlaylistName by remember { mutableStateOf("") }
                        var isCreatingNew by remember { mutableStateOf(false) }
                        
                        AlertDialog(
                            onDismissRequest = { showAddToPlaylistDialog = false },
                            title = {
                                Text(
                                    text = if (isCreatingNew) "Create & Add to Playlist" else "Add to Playlist",
                                    color = SpotifyWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            containerColor = Color(0xFF1E1E1E),
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (!isCreatingNew) {
                                        val playlistList = playlists
                                        if (playlistList.isEmpty()) {
                                            Text(
                                                text = "You don't have any playlists yet.",
                                                color = SpotifyGrayText,
                                                fontSize = 14.sp
                                            )
                                        } else {
                                            Text(
                                                text = "Select a playlist:",
                                                color = SpotifyGrayText,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            LazyColumn(
                                                modifier = Modifier.heightIn(max = 200.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(playlistList) { playlist ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                musicViewModel.addSongToPlaylist(playlist.playlistId, song.id)
                                                                Toast.makeText(context, "Added to ${playlist.playlistName}!", Toast.LENGTH_SHORT).show()
                                                                showAddToPlaylistDialog = false
                                                            }
                                                            .background(Color(0xFF282828), RoundedCornerShape(8.dp))
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlaylistPlay,
                                                            tint = SpotifyGreen,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = playlist.playlistName,
                                                            color = SpotifyWhite,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Button(
                                            onClick = { isCreatingNew = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("CREATE NEW PLAYLIST", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = newPlaylistName,
                                            onValueChange = { newPlaylistName = it },
                                            placeholder = { Text("Playlist Name", color = SpotifyGrayText) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = SpotifyWhite,
                                                unfocusedTextColor = SpotifyWhite,
                                                focusedBorderColor = SpotifyGreen,
                                                unfocusedBorderColor = SpotifyGrayText
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { isCreatingNew = false },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyWhite),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("BACK")
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (newPlaylistName.isNotBlank()) {
                                                        musicViewModel.createPlaylistAndAddSong(newPlaylistName, song.id)
                                                        Toast.makeText(context, "Playlist created & song added!", Toast.LENGTH_SHORT).show()
                                                        showAddToPlaylistDialog = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("CREATE & ADD", color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                                    Text("CANCEL", color = SpotifyGreen)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Favorite button
                    IconButton(onClick = { musicViewModel.toggleFavorite(song) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            tint = if (isFavorite) SpotifyGreen else SpotifyWhite,
                            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Download button
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SpotifyGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (isDownloaded) {
                                    musicViewModel.deleteDownload(song)
                                    Toast.makeText(context, "Download removed.", Toast.LENGTH_SHORT).show()
                                } else {
                                    musicViewModel.downloadSong(song)
                                    Toast.makeText(context, "Downloading track...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Filled.CloudDone else Icons.Outlined.CloudDownload,
                                tint = if (isDownloaded) SpotifyGreen else SpotifyWhite,
                                contentDescription = if (isDownloaded) "Delete Download" else "Download Track"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Progress SeekBar
            Column(modifier = Modifier.fillMaxWidth()) {
                val safeDuration = if (duration > 0) duration else 240000L // 4 min default
                EnhancedInteractiveSlider(
                    value = localSliderValue,
                    onValueChange = {
                        isDraggingSlider = true
                        localSliderValue = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        musicViewModel.seekTo(localSliderValue.toLong())
                    },
                    valueRange = 0f..safeDuration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(localSliderValue.toLong()),
                        fontSize = 12.sp,
                        color = SpotifyGrayText
                    )
                    Text(
                        text = formatTime(safeDuration),
                        fontSize = 12.sp,
                        color = SpotifyGrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shuffle, Previous, Play/Pause Disk, Next, Repeat controllers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { musicViewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        tint = if (isShuffle) SpotifyGreen else SpotifyWhite,
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { musicViewModel.previous() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        tint = SpotifyWhite,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Button(
                    onClick = {
                        if (isPlaying) musicViewModel.pause() else musicViewModel.resume()
                    },
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyWhite),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        tint = SpotifyBlack,
                        contentDescription = "PlayPause",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { musicViewModel.next() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        tint = SpotifyWhite,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { musicViewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        tint = if (isRepeat) SpotifyGreen else SpotifyWhite,
                        contentDescription = "Repeat",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Volume Controller Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (currentVolume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    tint = SpotifyGrayText,
                    contentDescription = "Volume Icon"
                )
                EnhancedInteractiveSlider(
                    value = currentVolume,
                    onValueChange = {
                        currentVolume = it
                        musicViewModel.setVolume(it)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

// Convert long milliseconds into formatted string label e.g: 03:45
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
fun EnhancedInteractiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    // Responsive animations for slider interaction
    val animatedThumbRadius by animateFloatAsState(
        targetValue = if (isDragging) 8f else 5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "thumbRadius"
    )
    val animatedTrackHeight by animateFloatAsState(
        targetValue = if (isDragging) 6f else 4f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "trackHeight"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Touch target accessibility compliance
            .pointerInput(valueRange) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val width = size.width.toFloat()
                        val newValue = if (width > 0f) {
                            ((offset.x / width) * (valueRange.endInclusive - valueRange.start)) + valueRange.start
                        } else {
                            valueRange.start
                        }
                        onValueChange(newValue.coerceIn(valueRange))
                        try {
                            awaitRelease()
                        } finally {
                            isDragging = false
                            onValueChangeFinished?.invoke()
                        }
                    }
                )
            }
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat()
                        val positionX = change.position.x
                        val newValue = if (width > 0f) {
                            ((positionX / width) * (valueRange.endInclusive - valueRange.start)) + valueRange.start
                        } else {
                            valueRange.start
                        }
                        onValueChange(newValue.coerceIn(valueRange))
                    }
                )
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val rangeSpan = valueRange.endInclusive - valueRange.start
        val fraction = if (rangeSpan > 0f) ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f) else 0f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val centerY = size.height / 2f
            val trackHeightPx = animatedTrackHeight.dp.toPx()
            val thumbRadiusPx = animatedThumbRadius.dp.toPx()
            val sliderWidth = size.width
            val progressX = sliderWidth * fraction

            // 1. Semi-translucent inactive slider track (sleek, high-contrast)
            drawRoundRect(
                color = SpotifyLightGray.copy(alpha = 0.25f),
                topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                size = Size(sliderWidth, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            // 2. Main active progress track (emerald green)
            drawRoundRect(
                color = SpotifyGreen,
                topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                size = Size(progressX, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            // 3. Drop shadow underneath the circular gesture selector
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = thumbRadiusPx + 2.dp.toPx(),
                center = Offset(progressX, centerY)
            )

            // 4. Circular gesture selector (solid premium white)
            drawCircle(
                color = SpotifyWhite,
                radius = thumbRadiusPx,
                center = Offset(progressX, centerY)
            )
        }
    }
}
