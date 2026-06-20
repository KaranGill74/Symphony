package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val username: String,
    val email: String,
    val image: String,
    val role: String, // "User" or "Admin"
    val createdAt: Long = System.currentTimeMillis(),
    val passwordHash: String? = null,
    val isVerified: Boolean = true,
    val verificationCode: String? = null
)

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val thumbnail: String, // URL or local path
    val mp3Url: String, // streaming URL or downloaded local file path
    val duration: String, // e.g. "3:45"
    val downloads: Int = 0,
    val playCount: Int = 0,
    val uploadedAt: Long = System.currentTimeMillis(),
    val isDownloadedLocal: Boolean = false,
    val localFilePath: String? = null
)

@Entity(tableName = "favorites", primaryKeys = ["userId", "songId"])
data class FavoriteEntity(
    val userId: String,
    val songId: String
)

@Entity(tableName = "downloads", primaryKeys = ["userId", "songId"])
data class DownloadEntity(
    val userId: String,
    val songId: String,
    val localPath: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val playlistId: String,
    val userId: String,
    val playlistName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: String
)

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val songId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val description: String = ""
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val name: String,
    val description: String = "",
    val coverUrl: String = ""
)

