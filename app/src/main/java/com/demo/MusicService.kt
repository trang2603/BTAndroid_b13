package com.demo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class MusicService : Service() {
    var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()
    private var currentSongIndex = 0
    private val songList = mutableListOf<Int>()
    private val songTitles = mutableListOf<String>()

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadSongsFromRaw()
    }

    private fun loadSongsFromRaw() {
        val fields = R.raw::class.java.fields
        for (field in fields) {
            val resourceId = field.getInt(field)
            songList.add(resourceId)
            songTitles.add(field.name)
        }
    }

    private fun createNotificationChannel() {
        val channel =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(
                    "music_service_channel",
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW,
                )
            } else {
                TODO("VERSION.SDK_INT < O")
            }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val action = intent?.action
        when (action) {
            "PLAY" -> playMusic()
            "PAUSE" -> pauseMusic()
            "STOP" -> stopMusic()
            "NEXT" -> nextMusic()
            "PREVIOUS" -> previousMusic()
        }
        startForeground(1, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification)

        // Set the initial states of the buttons, song title, etc.
        notificationLayout.setTextViewText(R.id.tv_song_title, songTitles[currentSongIndex])
//        notificationLayout.setTextViewText(R.id.tv_artist, artistNames[currentSongIndex])
        notificationLayout.setImageViewResource(
            R.id.btn_play_pause,
            if (mediaPlayer?.isPlaying == true) R.drawable.pause else R.drawable.play_button,
        )

        // Handle button actions
        val playPauseIntent =
            Intent(this, MusicService::class.java).apply {
                action = if (mediaPlayer?.isPlaying == true) "PAUSE" else "PLAY"
            }
        val playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationLayout.setOnClickPendingIntent(R.id.btn_play_pause, playPausePendingIntent)

        val nextIntent = Intent(this, MusicService::class.java).apply { action = "NEXT" }
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationLayout.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)

        val previousIntent = Intent(this, MusicService::class.java).apply { action = "PREVIOUS" }
        val previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationLayout.setOnClickPendingIntent(R.id.btn_previous, previousPendingIntent)

        return NotificationCompat
            .Builder(this, "music_service_channel")
            .setSmallIcon(R.drawable.musical_note)
            .setContent(notificationLayout)
            .setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
    }

    fun playMusic() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, songList[currentSongIndex]) // Thay thế bằng bài hát đầu tiên
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } else {
            mediaPlayer?.start()
        }
        updateNotification()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getSystemService(NotificationManager::class.java)
            } else {
                TODO("VERSION.SDK_INT < M")
            }
        manager.notify(1, notification)
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
        updateNotification()
    }

    fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true)
    }

    fun nextMusic() {
        currentSongIndex = (currentSongIndex + 1) % songList.size
        stopMusic()
        playMusic()
    }

    fun previousMusic() {
        currentSongIndex =
            if (currentSongIndex - 1 < 0) {
                songList.size - 1
            } else {
                currentSongIndex - 1
            }
        stopMusic()
        playMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
