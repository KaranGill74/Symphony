package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.database.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerManager

class SymphonyApplication : Application() {

    lateinit var database: MusicDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var playerManager: AudioPlayerManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            MusicDatabase::class.java,
            MusicDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = MusicRepository(this, database)
        playerManager = AudioPlayerManager(this)
    }
}
