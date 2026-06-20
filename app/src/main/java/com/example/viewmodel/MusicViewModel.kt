package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager
) : ViewModel() {

    // Global lists
    val songs: StateFlow<List<SongEntity>> = repository.getAllSongsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<SongEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchSongs(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User-specific states (reacts to repository.currentUser)
    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteSongs: StateFlow<List<SongEntity>> = repository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getFavoriteSongsFlow(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadedSongs: StateFlow<List<SongEntity>> = repository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getDownloadedSongsFlow(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlists: StateFlow<List<PlaylistEntity>> = repository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getPlaylistsFlow(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentSongs: StateFlow<List<SongEntity>> = repository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getRecentSongsFlow(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback states mapped from AudioPlayerManager
    val currentSong: StateFlow<SongEntity?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val progress: StateFlow<Long> = playerManager.progress
    val duration: StateFlow<Long> = playerManager.duration
    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleEnabled
    val isRepeatEnabled: StateFlow<Boolean> = playerManager.isRepeatEnabled

    // Download progression indicator states
    private val _downloadStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val downloadStates = _downloadStates.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // PLAYBACK CONTROLS
    fun playSong(song: SongEntity, queue: List<SongEntity> = emptyList()) {
        viewModelScope.launch {
            repository.incrementPlayCount(song.id)
            if (queue.isNotEmpty() && queue.contains(song)) {
                val index = queue.indexOf(song)
                playerManager.playQueue(queue, index)
            } else {
                playerManager.playSingle(song)
            }
        }
    }

    fun playQueue(queue: List<SongEntity>, startIndex: Int) {
        if (queue.isEmpty() || startIndex !in queue.indices) return
        viewModelScope.launch {
            repository.incrementPlayCount(queue[startIndex].id)
            playerManager.playQueue(queue, startIndex)
        }
    }

    fun resume() = playerManager.resume()
    fun pause() = playerManager.pause()
    fun stop() = playerManager.stop()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun toggleRepeat() = playerManager.toggleRepeat()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun setVolume(vol: Float) = playerManager.setVolume(vol)

    // ACTIONS
    fun toggleFavorite(song: SongEntity) {
        val user = repository.currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(user.uid, song.id)
        }
    }

    fun clearAllFavorites() {
        val user = repository.currentUser.value ?: return
        viewModelScope.launch {
            repository.clearAllFavorites(user.uid)
        }
    }

    fun downloadSong(song: SongEntity) {
        val user = repository.currentUser.value ?: return
        _downloadStates.value = _downloadStates.value + (song.id to true)
        viewModelScope.launch {
            repository.downloadSong(user.uid, song)
            _downloadStates.value = _downloadStates.value - song.id
        }
    }

    fun deleteDownload(song: SongEntity) {
        val user = repository.currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteDownloadedSong(user.uid, song)
        }
    }

    // PLAYLIST MANAGEMENT
    fun createPlaylist(name: String) {
        val user = repository.currentUser.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(user.uid, name)
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: String) {
        val user = repository.currentUser.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylistAndAddSong(user.uid, name, songId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun renamePlaylist(playlistId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, name)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsInPlaylist(playlistId: String): Flow<List<SongEntity>> {
        return repository.getSongsInPlaylistFlow(playlistId)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    class Factory(
        private val repository: MusicRepository,
        private val playerManager: AudioPlayerManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MusicViewModel(repository, playerManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
