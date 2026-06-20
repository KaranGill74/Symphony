package com.example.data.repository

import android.content.Context
import com.example.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val database: MusicDatabase
) {
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("symphony_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient()

    init {
        // Initialize default seed data
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialData()
        }
    }

    // Seed default admin, user, and standard tracks if db is empty
    private suspend fun seedInitialData() {
        val users = database.userDao.getAllUsers()
        if (users.isEmpty()) {
            // Seed Admin
            val adminUser = UserEntity(
                uid = "admin_uid_symphony",
                name = "Symphony Admin",
                username = "admin",
                email = "admin@symphony.com",
                image = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                role = "Admin",
                passwordHash = "admin",
                isVerified = true
            )
            database.userDao.insertUser(adminUser)

            // Seed Regular User
            val demoUser = UserEntity(
                uid = "demo_uid_symphony",
                name = "Jane MusicFan",
                username = "janedoe",
                email = "user@symphony.com",
                image = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                role = "User",
                passwordHash = "user123",
                isVerified = true
            )
            database.userDao.insertUser(demoUser)
        }

        // Restore logged in user if exists
        val savedUid = sharedPrefs.getString("logged_in_uid", null)
        if (savedUid != null) {
            val user = database.userDao.getUserById(savedUid)
            if (user != null) {
                _currentUser.value = user
            }
        }

        val categories = database.categoryDao.getAllCategoriesFlow()
        // Wait, since categories is a Flow, let's insert if empty
        val currentCategories = listOf("English", "Punjabi", "Hindi", "Pop", "Rock", "Hip-Hop", "EDM", "Classical")
        currentCategories.forEach { cat ->
            database.categoryDao.insertCategory(CategoryEntity(cat, "$cat music genre"))
        }

        // Preload default albums
        val currentAlbums = listOf("Bensound Acoustic", "Vibe Odyssey", "Sunset Anthems", "Late Night Study", "Virasat", "Bollywood Chill", "Retro Classics")
        currentAlbums.forEach { alb ->
            database.albumDao.insertAlbum(AlbumEntity(alb, "Awesome compilation of $alb", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"))
        }

        val songs = database.songDao.getAllSongs()
        val hasLegacySongs = songs.any { it.id == "song_1" && it.title == "Acoustic Breeze" }
        if (hasLegacySongs || songs.isEmpty()) {
            if (hasLegacySongs) {
                database.songDao.deleteAllSongs()
            }
            val defaultSongs = listOf(
                SongEntity(
                    id = "punjabi_1",
                    title = "Legend",
                    artist = "Sidhu Moose Wala",
                    album = "Virasat",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    duration = "6:12"
                ),
                SongEntity(
                    id = "punjabi_2",
                    title = "Brown Munde",
                    artist = "AP Dhillon",
                    album = "Sunset Anthems",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    duration = "7:05"
                ),
                SongEntity(
                    id = "punjabi_3",
                    title = "High Rated Gabru",
                    artist = "Guru Randhawa",
                    album = "Vibe Odyssey",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    duration = "5:44"
                ),
                SongEntity(
                    id = "punjabi_4",
                    title = "Elevate",
                    artist = "Shubh",
                    album = "Virasat",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    duration = "5:02"
                ),
                SongEntity(
                    id = "punjabi_5",
                    title = "Satisfya",
                    artist = "Imran Khan",
                    album = "Late Night Study",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    duration = "6:03"
                ),
                SongEntity(
                    id = "punjabi_6",
                    title = "Dil Nu",
                    artist = "AP Dhillon",
                    album = "Bensound Acoustic",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                    duration = "5:17"
                ),
                SongEntity(
                    id = "punjabi_7",
                    title = "Mi Amor",
                    artist = "Sharn",
                    album = "Sunset Anthems",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                    duration = "5:02"
                ),
                SongEntity(
                    id = "punjabi_8",
                    title = "White Brown Black",
                    artist = "Karan Aujla",
                    album = "Late Night Study",
                    genre = "Punjabi",
                    thumbnail = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400",
                    mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                    duration = "6:12"
                )
            )
            defaultSongs.forEach { database.songDao.insertSong(it) }
        }
    }

    // AUTH ACTIONS
    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = database.userDao.getUserByEmail(email)
        if (user != null) {
            if (user.role == "Blocked") {
                return@withContext Result.failure(Exception("Your account has been blocked."))
            }
            if (!user.isVerified) {
                return@withContext Result.failure(Exception("Please verify your email address. email_not_verified"))
            }
            val isPasswordCorrect = when {
                user.email == "admin@symphony.com" -> password == "admin"
                user.email == "user@symphony.com" -> password == "user123"
                else -> user.passwordHash == password
            }
            if (!isPasswordCorrect) {
                return@withContext Result.failure(Exception("Invalid email or password"))
            }
            _currentUser.value = user
            sharedPrefs.edit().putString("logged_in_uid", user.uid).apply()
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid email or password"))
        }
    }

    suspend fun signup(
        name: String,
        username: String,
        email: String,
        image: String,
        role: String = "User",
        password: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val existing = database.userDao.getUserByEmail(email)
        if (existing != null) {
            return@withContext Result.failure(Exception("Email already taken"))
        }
        val uid = UUID.randomUUID().toString()
        val otpCode = (100000..999999).random().toString()
        val newUser = UserEntity(
            uid = uid,
            name = name,
            username = username,
            email = email,
            image = image,
            role = role,
            passwordHash = password,
            isVerified = false,
            verificationCode = otpCode
        )
        database.userDao.insertUser(newUser)
        Result.success(newUser)
    }

    suspend fun verifyOtp(email: String, otp: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = database.userDao.getUserByEmail(email)
            ?: return@withContext Result.failure(Exception("No account registered with this email"))
        if (user.isVerified) {
            return@withContext Result.failure(Exception("Email already verified"))
        }
        if (user.verificationCode == otp) {
            val verifiedUser = user.copy(
                isVerified = true,
                verificationCode = null
            )
            database.userDao.updateUser(verifiedUser)
            Result.success(verifiedUser)
        } else {
            Result.failure(Exception("Incorrect verification code"))
        }
    }

    suspend fun getSimulatedOtp(email: String): String? = withContext(Dispatchers.IO) {
        val user = database.userDao.getUserByEmail(email)
        user?.verificationCode
    }

    suspend fun updateProfile(
        name: String,
        username: String,
        image: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("No user logged in"))
        val updated = current.copy(
            name = name,
            username = username,
            image = image
        )
        database.userDao.updateUser(updated)
        _currentUser.value = updated
        Result.success(updated)
    }

    suspend fun loginWithGoogle(email: String, name: String, image: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val existing = database.userDao.getUserByEmail(email)
        if (existing != null) {
            _currentUser.value = existing
            sharedPrefs.edit().putString("logged_in_uid", existing.uid).apply()
            Result.success(existing)
        } else {
            val uid = java.util.UUID.randomUUID().toString()
            val username = email.substringBefore("@").lowercase().replace(".", "")
            val newUser = UserEntity(
                uid = uid,
                name = name,
                username = username,
                email = email,
                image = image,
                role = "User"
            )
            database.userDao.insertUser(newUser)
            _currentUser.value = newUser
            sharedPrefs.edit().putString("logged_in_uid", newUser.uid).apply()
            Result.success(newUser)
        }
    }

    fun logout() {
        _currentUser.value = null
        sharedPrefs.edit().remove("logged_in_uid").apply()
    }

    // USER MANAGEMENT (Admin)
    fun getAllUsersFlow(): Flow<List<UserEntity>> = database.userDao.getAllUsersFlow()
    
    suspend fun deleteUser(uid: String) = withContext(Dispatchers.IO) {
        database.userDao.deleteUser(uid)
    }

    suspend fun updateUserRole(uid: String, role: String) = withContext(Dispatchers.IO) {
        val user = database.userDao.getUserById(uid)
        if (user != null) {
            database.userDao.updateUser(user.copy(role = role))
        }
    }

    // SONGS (Admin + User)
    fun getAllSongsFlow(): Flow<List<SongEntity>> = database.songDao.getAllSongsFlow()
    fun searchSongs(query: String): Flow<List<SongEntity>> = database.songDao.searchSongs(query)

    suspend fun addOrUpdateSong(song: SongEntity) = withContext(Dispatchers.IO) {
        database.songDao.insertSong(song)
    }

    suspend fun deleteSong(id: String) = withContext(Dispatchers.IO) {
        database.songDao.deleteSong(id)
    }

    suspend fun incrementPlayCount(songId: String) = withContext(Dispatchers.IO) {
        database.songDao.incrementPlayCount(songId)
        val song = database.songDao.getSongById(songId)
        _currentUser.value?.uid?.let { userId ->
            database.recentPlayDao.insertRecent(
                RecentPlayEntity(userId = userId, songId = songId)
            )
        }
    }

    // CATEGORIES (Admin + User)
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = database.categoryDao.getAllCategoriesFlow()
    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        database.categoryDao.insertCategory(CategoryEntity(name = name))
    }
    suspend fun deleteCategory(name: String) = withContext(Dispatchers.IO) {
        database.categoryDao.deleteCategory(name)
    }

    // ALBUMS (Admin + User)
    fun getAllAlbumsFlow(): Flow<List<AlbumEntity>> = database.albumDao.getAllAlbumsFlow()
    suspend fun addAlbum(name: String, description: String = "", coverUrl: String = "") = withContext(Dispatchers.IO) {
        database.albumDao.insertAlbum(AlbumEntity(name = name, description = description, coverUrl = coverUrl))
    }
    suspend fun deleteAlbum(name: String) = withContext(Dispatchers.IO) {
        database.albumDao.deleteAlbum(name)
    }

    // FAVORITES
    fun getFavoriteSongsFlow(userId: String): Flow<List<SongEntity>> = database.favoriteDao.getFavoriteSongs(userId)
    
    suspend fun clearAllFavorites(userId: String) = withContext(Dispatchers.IO) {
        database.favoriteDao.clearAllFavorites(userId)
    }

    suspend fun toggleFavorite(userId: String, songId: String) = withContext(Dispatchers.IO) {
        val fav = database.favoriteDao.isFavorite(userId, songId)
        if (fav != null) {
            database.favoriteDao.deleteFavorite(userId, songId)
        } else {
            database.favoriteDao.insertFavorite(FavoriteEntity(userId, songId))
        }
    }

    suspend fun isFavorite(userId: String, songId: String): Boolean = withContext(Dispatchers.IO) {
        database.favoriteDao.isFavorite(userId, songId) != null
    }

    // PLAYLISTS
    fun getPlaylistsFlow(userId: String): Flow<List<PlaylistEntity>> = database.playlistDao.getPlaylistsOfUser(userId)
    fun getSongsInPlaylistFlow(playlistId: String): Flow<List<SongEntity>> = database.playlistDao.getSongsInPlaylist(playlistId)

    suspend fun createPlaylist(userId: String, playlistName: String) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        database.playlistDao.insertPlaylist(PlaylistEntity(id, userId, playlistName))
    }

    suspend fun createPlaylistAndAddSong(userId: String, playlistName: String, songId: String) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        database.playlistDao.insertPlaylist(PlaylistEntity(id, userId, playlistName))
        database.playlistDao.insertPlaylistSong(PlaylistSongEntity(id, songId))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        database.playlistDao.deletePlaylist(playlistId)
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        database.playlistDao.renamePlaylist(playlistId, name)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        database.playlistDao.insertPlaylistSong(PlaylistSongEntity(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        database.playlistDao.deletePlaylistSong(playlistId, songId)
    }

    // RECENTLY PLAYED
    fun getRecentSongsFlow(userId: String): Flow<List<SongEntity>> = database.recentPlayDao.getRecentSongs(userId)

    // DOWNLOADS & REAL OFFLINE STORAGE
    fun getDownloadedSongsFlow(userId: String): Flow<List<SongEntity>> = database.downloadDao.getDownloadedSongs(userId)

    suspend fun isDownloaded(userId: String, songId: String): Boolean = withContext(Dispatchers.IO) {
        database.downloadDao.getDownload(userId, songId) != null
    }

    suspend fun downloadSong(userId: String, song: SongEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Retrieve file download from mp3Url
            val filename = "symphony_song_${song.id}.mp3"
            val file = File(context.filesDir, filename)
            
            val request = Request.Builder().url(song.mp3Url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful && response.body != null) {
                val inputstream = response.body!!.byteStream()
                val outputstream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputstream.read(buffer).also { bytesRead = it } != -1) {
                    outputstream.write(buffer, 0, bytesRead)
                }
                outputstream.flush()
                outputstream.close()
                inputstream.close()

                // Register file configuration in local table
                val download = DownloadEntity(
                    userId = userId,
                    songId = song.id,
                    localPath = file.absolutePath
                )
                database.downloadDao.insertDownload(download)

                // Update the song object to carry isDownloadedLocal = true & the local path
                val updatedSong = song.copy(
                    isDownloadedLocal = true,
                    localFilePath = file.absolutePath
                )
                database.songDao.updateSong(updatedSong)
                database.songDao.incrementDownloadCount(song.id)

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to download song stream from internet."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDownloadedSong(userId: String, song: SongEntity) = withContext(Dispatchers.IO) {
        val download = database.downloadDao.getDownload(userId, song.id)
        if (download != null) {
            val file = File(download.localPath)
            if (file.exists()) {
                file.delete()
            }
            database.downloadDao.deleteDownload(userId, song.id)

            val updatedSong = song.copy(
                isDownloadedLocal = false,
                localFilePath = null
            )
            database.songDao.updateSong(updatedSong)
        }
    }
}
