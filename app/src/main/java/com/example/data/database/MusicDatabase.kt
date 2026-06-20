package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsers(): List<UserEntity>
}

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY uploadedAt DESC")
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY uploadedAt DESC")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)

    @Query("UPDATE songs SET downloads = downloads + 1 WHERE id = :songId")
    suspend fun incrementDownloadCount(songId: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId AND songId = :songId")
    suspend fun isFavorite(userId: String, songId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND songId = :songId")
    suspend fun deleteFavorite(userId: String, songId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearAllFavorites(userId: String)

    @Query("SELECT s.* FROM songs s INNER JOIN favorites f ON s.id = f.songId WHERE f.userId = :userId")
    fun getFavoriteSongs(userId: String): Flow<List<SongEntity>>
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE userId = :userId AND songId = :songId")
    suspend fun deleteDownload(userId: String, songId: String)

    @Query("SELECT s.* FROM songs s INNER JOIN downloads d ON s.id = d.songId WHERE d.userId = :userId")
    fun getDownloadedSongs(userId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM downloads WHERE userId = :userId AND songId = :songId")
    suspend fun getDownload(userId: String, songId: String): DownloadEntity?
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("UPDATE playlists SET playlistName = :name WHERE playlistId = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String)

    @Query("SELECT * FROM playlists WHERE userId = :userId")
    fun getPlaylistsOfUser(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deletePlaylistSong(playlistId: String, songId: String)

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.songId WHERE ps.playlistId = :playlistId")
    fun getSongsInPlaylist(playlistId: String): Flow<List<SongEntity>>
}

@Dao
interface RecentPlayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentPlayEntity)

    @Query("SELECT s.* FROM songs s INNER JOIN recent_plays r ON s.id = r.songId WHERE r.userId = :userId ORDER BY r.playedAt DESC LIMIT 20")
    fun getRecentSongs(userId: String): Flow<List<SongEntity>>
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategory(name: String)
}

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbumsFlow(): Flow<List<AlbumEntity>>

    @Query("DELETE FROM albums WHERE name = :name")
    suspend fun deleteAlbum(name: String)
}

@Database(
    entities = [
        UserEntity::class,
        SongEntity::class,
        FavoriteEntity::class,
        DownloadEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        RecentPlayEntity::class,
        CategoryEntity::class,
        AlbumEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val songDao: SongDao
    abstract val favoriteDao: FavoriteDao
    abstract val downloadDao: DownloadDao
    abstract val playlistDao: PlaylistDao
    abstract val recentPlayDao: RecentPlayDao
    abstract val categoryDao: CategoryDao
    abstract val albumDao: AlbumDao

    companion object {
        const val DATABASE_NAME = "symphony_music.db"
    }
}
