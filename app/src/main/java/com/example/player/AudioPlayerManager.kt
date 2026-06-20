package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.database.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class AudioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    private var playlist: List<SongEntity> = emptyList()
    private var currentIndex: Int = -1

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
                
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startProgressUpdate()
                        } else {
                            stopProgressUpdate()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            _duration.value = duration
                        } else if (state == Player.STATE_ENDED) {
                            handleSongEnded()
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        // Sync current song from tag if playing queue of tracks
                        mediaItem?.localConfiguration?.tag?.let { tag ->
                            if (tag is SongEntity) {
                                _currentSong.value = tag
                                updateCurrentIndex(tag)
                            }
                        }
                    }
                })
            }
        }
    }

    private fun updateCurrentIndex(song: SongEntity) {
        currentIndex = playlist.indexOfFirst { it.id == song.id }
    }

    fun playQueue(songs: List<SongEntity>, startIndex: Int) {
        if (songs.isEmpty()) return
        playlist = songs
        currentIndex = startIndex
        val songToPlay = songs[startIndex]
        _currentSong.value = songToPlay

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            
            // Build and set song items
            songs.forEach { song ->
                // Use mp3Url; standard http urls directly streamed
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(song.mp3Url))
                    .setMediaId(song.id)
                    .setTag(song)
                    .build()
                player.addMediaItem(mediaItem)
            }
            
            player.seekTo(startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    fun playSingle(song: SongEntity) {
        playlist = listOf(song)
        currentIndex = 0
        _currentSong.value = song

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(song.mp3Url))
                .setMediaId(song.id)
                .setTag(song)
                .build()
            player.addMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stop() {
        exoPlayer?.stop()
        _isPlaying.value = false
        stopProgressUpdate()
    }

    fun next() {
        if (playlist.isEmpty()) return
        
        if (_isShuffleEnabled.value) {
            val nextIdx = (0 until playlist.size).random()
            playIndex(nextIdx)
        } else {
            val nextIdx = (currentIndex + 1) % playlist.size
            playIndex(nextIdx)
        }
    }

    fun previous() {
        if (playlist.isEmpty()) return
        val prevIdx = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        playIndex(prevIdx)
    }

    private fun playIndex(index: Int) {
        if (index in playlist.indices) {
            currentIndex = index
            val song = playlist[index]
            _currentSong.value = song
            exoPlayer?.let { player ->
                player.seekTo(index, 0L)
                player.prepare()
                player.play()
            }
        }
    }

    fun toggleShuffle() {
        val nextVal = !_isShuffleEnabled.value
        _isShuffleEnabled.value = nextVal
        exoPlayer?.shuffleModeEnabled = nextVal
    }

    fun toggleRepeat() {
        val nextVal = !_isRepeatEnabled.value
        _isRepeatEnabled.value = nextVal
        exoPlayer?.repeatMode = if (nextVal) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _progress.value = positionMs
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }

    private fun handleSongEnded() {
        if (_isRepeatEnabled.value) {
            playIndex(currentIndex)
        } else {
            next()
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let {
                    _progress.value = it.currentPosition
                }
                delay(1000L)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        progressJob?.cancel()
        scope.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
