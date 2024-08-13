package com.demo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.demo.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler
    private var isPlaying = false
    private var isBound = false
    private var isSeeking = false
    private var musicService: MusicService? = null

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                className: ComponentName?,
                service: IBinder?,
            ) {
                val binder = service as MusicService.LocalBinder
                musicService = binder.getService()
                isBound = true
                updateSeekBar()
            }

            override fun onServiceDisconnected(arg0: ComponentName?) {
                isBound = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        binding.volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.volumeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            },
        )

        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) {
                Intent(this, MusicService::class.java).apply {
                    action = "PAUSE"
                    startService(this)
                }
                binding.btnPlayPause.setImageResource(R.drawable.play_button)
            } else {
                Intent(this, MusicService::class.java).apply {
                    action = "PLAY"
                    startService(this)
                }
                binding.btnPlayPause.setImageResource(R.drawable.pause)
            }
            isPlaying = !isPlaying
        }

        binding.btnNext.setOnClickListener {
            Intent(this, MusicService::class.java).apply {
                action = "NEXT"
                startService(this)
            }
        }

        binding.btnPrevious.setOnClickListener {
            Intent(this, MusicService::class.java).apply {
                action = "PREVIOUS"
                startService(this)
            }
        }

        // Add a SeekBar listener for seeking
        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) {
                        // Update the current time TextView as the user drags the SeekBar
                        binding.tvCurrentTime.text = formatTime(progress * 1000)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeeking = true // Indicate that the user is seeking
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isSeeking = false
                    // When the user stops dragging, seek to the new position
                    seekBar?.let {
                        musicService?.mediaPlayer?.seekTo(it.progress * 1000)
                    }
                }
            },
        )

        handler = Handler()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun updateSeekBar() {
        val runnable =
            object : Runnable {
                override fun run() {
                    musicService?.mediaPlayer?.let { mediaPlayer ->
                        binding.seekBar.max = mediaPlayer.duration / 1000
                        binding.seekBar.progress = mediaPlayer.currentPosition / 1000

                        val currentTime = formatTime(mediaPlayer.currentPosition)
                        val totalTime = formatTime(mediaPlayer.duration)

                        binding.tvCurrentTime.text = currentTime
                        binding.tvTotalTime.text = totalTime
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        handler.post(runnable)
    }

    private fun formatTime(milliseconds: Int): String =
        String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60,
        )

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
