package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import com.example.data.database.PlaylistEntity
import com.example.data.database.SongEntity
import com.example.data.database.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.MusicViewModel

sealed interface UserTab {
    object Home : UserTab
    object Search : UserTab
    object Library : UserTab
    object Downloads : UserTab
    object Profile : UserTab
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMusicApp(
    authViewModel: AuthViewModel,
    musicViewModel: MusicViewModel,
    onOpenFullPlayer: () -> Unit,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf<UserTab>(UserTab.Home) }
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentSong by musicViewModel.currentSong.collectAsState()
    
    // Playlist detail view state
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    Scaffold(
        bottomBar = {
            Column {
                // Mini Player hover when a song is loaded
                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    currentSong?.let { song ->
                        MiniPlayerRow(
                            song = song,
                            isPlaying = musicViewModel.isPlaying.collectAsState().value,
                            progress = musicViewModel.progress.collectAsState().value,
                            duration = musicViewModel.duration.collectAsState().value,
                            onTogglePlay = {
                                if (musicViewModel.isPlaying.value) musicViewModel.pause()
                                else musicViewModel.resume()
                            },
                            onNext = { musicViewModel.next() },
                            onOpenFullPlayer = onOpenFullPlayer
                        )
                    }
                }

                // Standard Bottom Navigation Bar
                NavigationBar(
                    containerColor = Color(0xFF1C1B1F),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF1C1B1F))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(32.dp))
                ) {
                    NavigationBarItem(
                        selected = activeTab == UserTab.Home && selectedPlaylist == null,
                        onClick = {
                            activeTab = UserTab.Home
                            selectedPlaylist = null
                        },
                        icon = { Icon(if (activeTab == UserTab.Home) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotifyGreen,
                            selectedTextColor = SpotifyGreen,
                            unselectedIconColor = SpotifyGrayText,
                            unselectedTextColor = SpotifyGrayText,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == UserTab.Search && selectedPlaylist == null,
                        onClick = {
                            activeTab = UserTab.Search
                            selectedPlaylist = null
                        },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        label = { Text("Search", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotifyGreen,
                            selectedTextColor = SpotifyGreen,
                            unselectedIconColor = SpotifyGrayText,
                            unselectedTextColor = SpotifyGrayText,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == UserTab.Library || selectedPlaylist != null,
                        onClick = { activeTab = UserTab.Library },
                        icon = { Icon(if (activeTab == UserTab.Library) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotifyGreen,
                            selectedTextColor = SpotifyGreen,
                            unselectedIconColor = SpotifyGrayText,
                            unselectedTextColor = SpotifyGrayText,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == UserTab.Downloads && selectedPlaylist == null,
                        onClick = {
                            activeTab = UserTab.Downloads
                            selectedPlaylist = null
                        },
                        icon = { Icon(if (activeTab == UserTab.Downloads) Icons.Filled.DownloadDone else Icons.Outlined.Download, contentDescription = "Downloads") },
                        label = { Text("Downloads", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotifyGreen,
                            selectedTextColor = SpotifyGreen,
                            unselectedIconColor = SpotifyGrayText,
                            unselectedTextColor = SpotifyGrayText,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == UserTab.Profile && selectedPlaylist == null,
                        onClick = {
                            activeTab = UserTab.Profile
                            selectedPlaylist = null
                        },
                        icon = { Icon(if (activeTab == UserTab.Profile) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 10.sp, maxLines = 1, softWrap = false) },
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
                .padding(innerPadding)
        ) {
            if (selectedPlaylist != null) {
                PlaylistDetailScreen(
                    playlist = selectedPlaylist!!,
                    musicViewModel = musicViewModel,
                    onBack = { selectedPlaylist = null }
                )
            } else {
                when (activeTab) {
                    UserTab.Home -> HomeScreenContent(
                        user = currentUser,
                        musicViewModel = musicViewModel,
                        onProfileClick = {
                            activeTab = UserTab.Profile
                            selectedPlaylist = null
                        }
                    )
                    UserTab.Search -> SearchScreenContent(musicViewModel)
                    UserTab.Library -> LibraryScreenContent(musicViewModel, onOpenPlaylist = { selectedPlaylist = it })
                    UserTab.Downloads -> DownloadsScreenContent(musicViewModel)
                    UserTab.Profile -> ProfileScreenContent(authViewModel, onLogout)
                }
            }
        }
    }
}

// HOME TAB CONTENT
@Composable
fun HomeScreenContent(
    user: UserEntity?,
    musicViewModel: MusicViewModel,
    onProfileClick: () -> Unit
) {
    val songs by musicViewModel.songs.collectAsState()
    val categories by musicViewModel.categories.collectAsState()
    val recentSongs by musicViewModel.recentSongs.collectAsState()
    var selectedGenreFilter by remember { mutableStateOf<String?>(null) }

    val filteredSongs = remember(songs, selectedGenreFilter) {
        val filter = selectedGenreFilter
        if (filter == null) songs else songs.filter { it.genre.equals(filter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Header (Fixed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "WELCOME BACK,",
                    fontSize = 11.sp,
                    color = SpotifyGrayText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = user?.name ?: "Melody Lover",
                    fontSize = 26.sp,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            // User Avatar Click
            ProfileAvatar(
                name = user?.name ?: "User",
                imageUrl = user?.image,
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onProfileClick() },
                fontSize = 16.sp,
                email = user?.email
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category Filter Chips (Fixed)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedGenreFilter == null,
                    onClick = { selectedGenreFilter = null },
                    label = { Text("All", color = if (selectedGenreFilter == null) SpotifyBlack else SpotifyWhite) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen,
                        containerColor = SpotifyLightGray
                    ),
                    border = null
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedGenreFilter == cat.name,
                    onClick = { selectedGenreFilter = cat.name },
                    label = { Text(cat.name, color = if (selectedGenreFilter == cat.name) SpotifyBlack else SpotifyWhite) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen,
                        containerColor = SpotifyLightGray
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recommended Tracks section (Fixed title & horizontal row)
        Text(
            text = "Recommended for You",
            fontSize = 20.sp,
            color = SpotifyWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(songs.take(10)) { song ->
                SongSliderCard(song = song, onClick = {
                    musicViewModel.playSong(song, songs)
                })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // All Songs section Header (Fixed)
        Text(
            text = "All Songs",
            fontSize = 20.sp,
            color = SpotifyWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Only "All Songs" list is vertically scrollable!
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No songs loaded in this category.", color = SpotifyGrayText)
                    }
                }
            } else {
                items(filteredSongs) { song ->
                    SongRowItem(song = song, musicViewModel = musicViewModel, queue = filteredSongs)
                }
            }
        }
    }
}

// SEARCH TAB CONTENT
@Composable
fun SearchScreenContent(musicViewModel: MusicViewModel) {
    val query by musicViewModel.searchQuery.collectAsState()
    val results by musicViewModel.searchResults.collectAsState()
    val allSongs by musicViewModel.songs.collectAsState()
    val recentSongs by musicViewModel.recentSongs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Search",
            fontSize = 28.sp,
            color = SpotifyWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        //SearchView
        OutlinedTextField(
            value = query,
            onValueChange = { musicViewModel.setSearchQuery(it) },
            placeholder = { Text("Search title, artist, genres...", color = SpotifyGrayText) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SpotifyGrayText) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { musicViewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SpotifyGrayText)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SpotifyDarkGray,
                unfocusedContainerColor = SpotifyDarkGray,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (query.isBlank()) {
            if (recentSongs.isNotEmpty()) {
                Text("Recently Played", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    items(recentSongs.take(10)) { song ->
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { musicViewModel.playSong(song, recentSongs) },
                            colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    if (!song.thumbnail.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = song.thumbnail,
                                            contentDescription = song.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Gray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyWhite)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = song.title,
                                    color = SpotifyWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = SpotifyGrayText,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Display static visual suggestions
            Text("Browse all genres", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allSongs.distinctBy { it.genre }) { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(SpotifyDarkGray, SpotifyGreen.copy(alpha = 0.3f))
                                )
                            )
                            .clickable { musicViewModel.setSearchQuery(song.genre) }
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(song.genre, fontSize = 18.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Display search results
            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tracks match \"$query\"", color = SpotifyGrayText)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { song ->
                        SongRowItem(song = song, musicViewModel = musicViewModel, queue = results)
                    }
                }
            }
        }
    }
}

// LIBRARY TAB CONTENT
@Composable
fun LibraryScreenContent(
    musicViewModel: MusicViewModel,
    onOpenPlaylist: (PlaylistEntity) -> Unit
) {
    val favorites by musicViewModel.favoriteSongs.collectAsState()
    val playlists by musicViewModel.playlists.collectAsState()
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showAddPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlaylistDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Name of your playlist") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            musicViewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showAddPlaylistDialog = false
                            Toast.makeText(context, "Playlist created!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text("Save", color = SpotifyBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaylistDialog = false }) {
                    Text("Cancel", color = SpotifyWhite)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Library",
                fontSize = 28.sp,
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAddPlaylistDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = SpotifyGreen, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Favorites quick slide
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Favorites", fontSize = 18.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
            if (favorites.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    fontSize = 12.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { musicViewModel.clearAllFavorites() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("clear_all_favorites_button")
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (favorites.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Favorite tracks you like will show here.", color = SpotifyGrayText, fontSize = 13.sp)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favorites) { song ->
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { musicViewModel.playSong(song, favorites) },
                        colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(modifier = Modifier.size(120.dp)) {
                                AsyncImage(
                                    model = song.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .clickable { musicViewModel.toggleFavorite(song) }
                                        .testTag("remove_favorite_${song.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Remove from Favorites",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(song.title, fontSize = 14.sp, color = SpotifyWhite, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                            Text(song.artist, fontSize = 12.sp, color = SpotifyGrayText, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playlists category
        Text("Playlists", fontSize = 18.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = SpotifyGrayText, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You don't have any playlists.", color = SpotifyGrayText)
                    TextButton(onClick = { showAddPlaylistDialog = true }) {
                        Text("Create and add tracks!", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(playlists) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyDarkGray)
                            .clickable { onOpenPlaylist(pl) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SpotifyGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = SpotifyBlack)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(pl.playlistName, fontSize = 16.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold)
                                Text("Custom Playlist", fontSize = 12.sp, color = SpotifyGrayText)
                            }
                        }
                        IconButton(onClick = { musicViewModel.deletePlaylist(pl.playlistId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// PLAYLIST DETAIL OVERLAY
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    musicViewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val songsInPlaylist by musicViewModel.getSongsInPlaylist(playlist.playlistId).collectAsState(emptyList())
    val allSongs by musicViewModel.songs.collectAsState()
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var renameState by remember { mutableStateOf(false) }
    var renameVal by remember { mutableStateOf(playlist.playlistName) }

    if (showAddSongsDialog) {
        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            title = { Text("Add Track to Playlist") },
            text = {
                Box(modifier = Modifier.size(height = 300.dp, width = 280.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allSongs) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        musicViewModel.addSongToPlaylist(playlist.playlistId, s.id)
                                        showAddSongsDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(model = s.thumbnail, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(s.title, color = SpotifyWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(s.artist, color = SpotifyGrayText, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddSongsDialog = false }) {
                    Text("Close", color = SpotifyGreen)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SpotifyWhite)
            }
            if (renameState) {
                OutlinedTextField(
                    value = renameVal,
                    onValueChange = { renameVal = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                IconButton(onClick = {
                    musicViewModel.renamePlaylist(playlist.playlistId, renameVal)
                    renameState = false
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = SpotifyGreen)
                }
            } else {
                Text(
                    text = playlist.playlistName,
                    fontSize = 24.sp,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                IconButton(onClick = { renameState = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = SpotifyWhite)
                }
            }
            Button(
                onClick = { showAddSongsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Add", color = SpotifyBlack, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (songsInPlaylist.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No songs in this playlist.", color = SpotifyGrayText)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { showAddSongsDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)) {
                        Text("Add Songs Now", color = SpotifyBlack)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(songsInPlaylist) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyDarkGray)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { musicViewModel.playSong(song, songsInPlaylist) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(model = song.thumbnail, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(song.title, color = SpotifyWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(song.artist, color = SpotifyGrayText, fontSize = 13.sp)
                            }
                        }
                        IconButton(onClick = { musicViewModel.removeSongFromPlaylist(playlist.playlistId, song.id) }) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// DOWNLOADS TAB CONTENT
@Composable
fun DownloadsScreenContent(musicViewModel: MusicViewModel) {
    val downloads by musicViewModel.downloadedSongs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Downloads",
                    fontSize = 28.sp,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Play offline without internet",
                    fontSize = 12.sp,
                    color = SpotifyGreen,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(Icons.Default.DownloadDone, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SpotifyGrayText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No downloaded songs.", color = SpotifyGrayText, fontWeight = FontWeight.Bold)
                    Text("Go to home and hit the download key!", color = SpotifyGrayText, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(downloads) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyDarkGray)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { musicViewModel.playSong(song, downloads) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(song.title, color = SpotifyWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Cached", tint = SpotifyGreen, modifier = Modifier.size(14.dp))
                                }
                                Text(song.artist, color = SpotifyGrayText, fontSize = 13.sp)
                            }
                        }
                        IconButton(onClick = { musicViewModel.deleteDownload(song) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Download", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// USER PROFILE TAB CONTENT
@Composable
fun ProfileScreenContent(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var editNameState by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(currentUser?.name ?: "") }
    var newUsername by remember { mutableStateOf(currentUser?.username ?: "") }
    var newImage by remember { mutableStateOf(currentUser?.image ?: "") }
    val context = LocalContext.current

    if (editNameState) {
        AlertDialog(
            onDismissRequest = { editNameState = false },
            title = { Text("Edit Credentials") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Display Name") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen))
                    OutlinedTextField(value = newUsername, onValueChange = { newUsername = it }, label = { Text("Username") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen))
                    OutlinedTextField(value = newImage, onValueChange = { newImage = it }, label = { Text("Profile Pic Image URL") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotifyGreen))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.updateProfile(newName, newUsername, newImage)
                        editNameState = false
                        Toast.makeText(context, "Credentials updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text("Save", color = SpotifyBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { editNameState = false }) {
                    Text("Cancel", color = SpotifyWhite)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile Dashboard", fontSize = 28.sp, color = SpotifyWhite, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(30.dp))

        // Avatar Core
        ProfileAvatar(
            name = currentUser?.name ?: "Symphonian User",
            imageUrl = currentUser?.image,
            modifier = Modifier.size(130.dp),
            fontSize = 48.sp,
            email = currentUser?.email
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Image Picker launcher
        val coroutineScope = rememberCoroutineScope()
        val profileImagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        val localPath = copyImageToLocalStorage(context, uri)
                        authViewModel.updateProfile(
                            name = currentUser?.name ?: "",
                            username = currentUser?.username ?: "",
                            image = localPath
                        )
                        Toast.makeText(context, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to update profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { profileImagePickerLauncher.launch("image/*") },
                border = BorderStroke(1.dp, SpotifyGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyGreen),
                modifier = Modifier.testTag("upload_profile_pic_btn")
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Change Photo", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Change Photo", fontSize = 12.sp)
            }

            if (!currentUser?.image.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        authViewModel.updateProfile(
                            name = currentUser?.name ?: "",
                            username = currentUser?.username ?: "",
                            image = ""
                        )
                        Toast.makeText(context, "Profile picture removed successfully!", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, Color.Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("remove_profile_pic_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Photo", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove Photo", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentUser?.name ?: "Symphonian User",
            fontSize = 22.sp,
            color = SpotifyWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "@${currentUser?.username ?: "username"}",
            fontSize = 15.sp,
            color = SpotifyGreen,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Email", color = SpotifyGrayText)
                    Text(currentUser?.email ?: "user@symphony.com", color = SpotifyWhite, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = SpotifyLightGray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Date Joined", color = SpotifyGrayText)
                    Text("June 20, 2026", color = SpotifyWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                newName = currentUser?.name ?: ""
                newUsername = currentUser?.username ?: ""
                newImage = currentUser?.image ?: ""
                editNameState = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyLightGray)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = SpotifyGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit My Profile Info", color = SpotifyWhite)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                Toast.makeText(context, "Password modification simulated. Link sent to registered email.", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyLightGray)
        ) {
            Icon(Icons.Default.LockReset, contentDescription = null, tint = SpotifyGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Secret Password", color = SpotifyWhite)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                authViewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = SpotifyWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOG OUT SECURELY", color = SpotifyWhite, fontWeight = FontWeight.Bold)
        }
    }
}

// SHARED COMPONENTS
fun getAtmosphericGradient(seed: String): Brush {
    val hash = seed.hashCode().coerceAtLeast(0)
    val gradients = listOf(
        listOf(Color(0xFF403060), Color(0xFF121212)),
        listOf(Color(0xFF204060), Color(0xFF121212)),
        listOf(Color(0xFF502030), Color(0xFF121212)),
        listOf(Color(0xFF205040), Color(0xFF121212)),
        listOf(Color(0xFF504020), Color(0xFF121212)),
        listOf(Color(0xFF5C2C6D), Color(0xFF121212))
    )
    val pair = gradients[hash % gradients.size]
    return Brush.verticalGradient(pair)
}

@Composable
fun SongSliderCard(song: SongEntity, onClick: () -> Unit) {
    val gradient = remember(song.title) { getAtmosphericGradient(song.title) }
    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(10.dp)
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = song.title,
                fontSize = 13.sp,
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 11.sp,
                color = SpotifyGrayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongRowItem(
    song: SongEntity,
    musicViewModel: MusicViewModel,
    queue: List<SongEntity> = emptyList()
) {
    var isFav by remember { mutableStateOf(false) }
    var isDown by remember { mutableStateOf(false) }
    val currentUser by musicViewModel.playlists.collectAsState() // trigger reload flow trigger helper
    val activeUser = musicViewModel.currentSong.collectAsState().value // arbitrary state for update trigger
    val downloadsFlowVal by musicViewModel.downloadedSongs.collectAsState()
    val favsFlowVal by musicViewModel.favoriteSongs.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(downloadsFlowVal, favsFlowVal, activeUser) {
        isFav = favsFlowVal.any { it.id == song.id }
        isDown = downloadsFlowVal.any { it.id == song.id }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SpotifyDarkGray)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { musicViewModel.playSong(song, queue) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = song.title,
                    fontSize = 16.sp,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.duration}",
                    fontSize = 13.sp,
                    color = SpotifyGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Favorite Button
            IconButton(onClick = { musicViewModel.toggleFavorite(song) }) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    tint = if (isFav) SpotifyGreen else SpotifyGrayText,
                    contentDescription = "Fav"
                )
            }

            // Downloader button with explicit text
            val isDownloadingActive = musicViewModel.downloadStates.collectAsState().value[song.id] ?: false
            if (isDownloadingActive) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                    Text("Saving...", color = SpotifyGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SpotifyGreen, strokeWidth = 1.5.dp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDown) SpotifyGreen.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            if (isDown) {
                                musicViewModel.deleteDownload(song)
                                Toast.makeText(context, "Download deleted from storage.", Toast.LENGTH_SHORT).show()
                            } else {
                                musicViewModel.downloadSong(song)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isDown) "Downloaded" else "Download",
                        color = if (isDown) SpotifyGreen else SpotifyGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isDown) Icons.Filled.CloudDone else Icons.Outlined.CloudDownload,
                        tint = if (isDown) SpotifyGreen else SpotifyGrayText,
                        contentDescription = "Down",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// MINI AUDIO PLAYER ROW
@Composable
fun MiniPlayerRow(
    song: SongEntity,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenFullPlayer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onOpenFullPlayer() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        val miniPlayerGradient = remember {
            Brush.horizontalGradient(
                colors = listOf(SpotifyGreen, Color(0xFF1ED760))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(miniPlayerGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artist} • ${song.duration}",
                            fontSize = 11.sp,
                            color = Color.Black.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp),
                            contentDescription = "Play/Pause"
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp),
                            contentDescription = "Next"
                        )
                    }
                }
            }
            // Real-time tiny progress line
            val progressPercent = if (duration > 0) progress.toFloat() / duration else 0f
            LinearProgressIndicator(
                progress = progressPercent,
                color = Color.Black.copy(alpha = 0.5f),
                trackColor = Color.Black.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}
