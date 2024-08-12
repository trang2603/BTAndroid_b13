package com.demo

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class MyService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private val CHANNEL_ID = "MusicServiceChannel"

    private val handler =
        Handler(Looper.getMainLooper()) {
            updateProgressBar()
            true
        }

    private fun updateProgressBar() {
        val intent = Intent("UPDATE_PROGRESS")
        intent.putExtra("current_position", mediaPlayer.currentPosition)
        intent.putExtra("duration", mediaPlayer.duration)
        sendBroadcast(intent)
        handler.sendEmptyMessageDelayed(0, 1000)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaPlayer = MediaPlayer.create(this, R.raw.motdoi)
        mediaPlayer.isLooping = true // Để phát lặp lại
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == "ACTION_PLAY") {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                handler.sendEmptyMessage(0)
            }
            startForeground(1, createNotification())
        } else if (intent?.action == "ACTION_PAUSE") {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                handler.removeMessages(0)
            }
            startForeground(1, createNotification())
        } else if (intent?.action == "ACTION_STOP") {
            stopSelf()
        }
        val seekTo = intent?.getIntExtra("seekTo", -1)
        if (seekTo != null && seekTo >= 0) {
            mediaPlayer.seekTo(seekTo)
        }
        return START_STICKY
    }

    private fun createNotification(): Notification? {
        val playPauseIntent =
            Intent(this, MyService::class.java).apply {
                action = if (mediaPlayer.isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
            }
        val playPausePendingIntent =
            PendingIntent.getService(
                this,
                0,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent =
            Intent(this, MyService::class.java).apply {
                action = "ACTION_STOP"
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText("Now playing")
            .setSmallIcon(R.drawable.musical_note)
            .addAction(
                if (mediaPlayer.isPlaying) R.drawable.pause else R.drawable.play_buttton,
                if (mediaPlayer.isPlaying) "Pause" else "Play",
                playPausePendingIntent,
            ).addAction(R.drawable.stop_button, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeMessages(0) // Hủy các thông báo handler trước khi dừng Service
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
        stopForeground(true)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent =
            Intent(applicationContext, MyService::class.java).also {
                it.setPackage(packageName)
            }
        val restartServicePendingIntent =
            PendingIntent.getService(
                this,
                1,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
