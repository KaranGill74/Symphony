package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.CategoryEntity
import com.example.data.database.SongEntity
import com.example.data.database.UserEntity
import com.example.data.database.AlbumEntity
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: MusicRepository) : ViewModel() {

    // Lists
    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSongs: StateFlow<List<SongEntity>> = repository.getAllSongsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumEntity>> = repository.getAllAlbumsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query for UI management
    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery.asStateFlow()

    val filteredUsers: StateFlow<List<UserEntity>> = combine(allUsers, _userSearchQuery) { users, query ->
        if (query.isBlank()) users
        else users.filter {
            it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DASHBOARD AND ANALYTICS METRICS
    val dashboardMetrics = combine(allUsers, allSongs) { users, songs ->
        val totalUsersCount = users.size
        val totalSongsCount = songs.size
        val totalDownloadsCount = songs.sumOf { it.downloads }
        val totalPlaysCount = songs.sumOf { it.playCount }

        AdminDashboardMetrics(
            totalUsers = totalUsersCount,
            totalSongs = totalSongsCount,
            totalDownloads = totalDownloadsCount,
            totalPlays = totalPlaysCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardMetrics())

    val popularSongs: StateFlow<List<SongEntity>> = allSongs.map { songs ->
        songs.sortedByDescending { it.playCount }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUserSearchQuery(query: String) {
        _userSearchQuery.value = query
    }

    // ADMINISTRATIVE ACTIONS
    fun uploadSong(
        title: String,
        artist: String,
        album: String,
        genre: String,
        duration: String,
        thumbnail: String,
        mp3Url: String
    ) {
        val songId = "song_${System.currentTimeMillis()}"
        val newSong = SongEntity(
            id = songId,
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            duration = if (duration.isBlank()) "4:00" else duration,
            thumbnail = if (thumbnail.isBlank()) "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400" else thumbnail,
            mp3Url = if (mp3Url.isBlank()) "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" else mp3Url
        )
        viewModelScope.launch {
            repository.addOrUpdateSong(newSong)
        }
    }

    fun editSong(song: SongEntity) {
        viewModelScope.launch {
            repository.addOrUpdateSong(song)
        }
    }

    fun deleteSong(id: String) {
        viewModelScope.launch {
            repository.deleteSong(id)
        }
    }

    // USER ADMINISTRATION (Delete, block, unblock)
    fun deleteUser(uid: String) {
        viewModelScope.launch {
            repository.deleteUser(uid)
        }
    }

    fun toggleUserBlock(uid: String, currentRole: String) {
        val targetRole = if (currentRole == "Blocked") "User" else "Blocked"
        viewModelScope.launch {
            repository.updateUserRole(uid, targetRole)
        }
    }

    fun changeUserRole(uid: String, role: String) {
        viewModelScope.launch {
            repository.updateUserRole(uid, role)
        }
    }

    // CATEGORIES
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name)
        }
    }

    // ALBUMS
    fun addAlbum(name: String, description: String = "", coverUrl: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addAlbum(name, description, coverUrl)
        }
    }

    fun deleteAlbum(name: String) {
        viewModelScope.launch {
            repository.deleteAlbum(name)
        }
    }

    class Factory(private val repository: MusicRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class AdminDashboardMetrics(
    val totalUsers: Int = 0,
    val totalSongs: Int = 0,
    val totalDownloads: Int = 0,
    val totalPlays: Int = 0
)
