package com.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isPlaying = false
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playPauseButton.setOnClickListener {
            val action = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
            val serviceIntent =
                Intent(this, MyService::class.java).apply {
                    this.action = action
                }
            startService(serviceIntent)
            isPlaying = !isPlaying
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Thiết lập SeekBar âm lượng
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.max = maxVolume
        binding.volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Thay đổi âm lượng khi SeekBar thay đổi
        binding.volumeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )

        // Đăng ký BroadcastReceiver
        registerReceiver(progressReceiver, IntentFilter("UPDATE_PROGRESS"))

        // Thiết lập SeekBar cho bài hát
        binding.songSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) {
                        // Chuyển đến vị trí mới của bài hát khi người dùng thay đổi SeekBar
                        val serviceIntent = Intent(this@MainActivity, MyService::class.java)
                        serviceIntent.putExtra("seekTo", progress)
                        startService(serviceIntent)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )
    }

    // BroadcastReceiver để nhận cập nhật từ MusicService
    private val progressReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                val currentPosition = intent?.getIntExtra("current_position", 0) ?: 0
                val duration = intent?.getIntExtra("duration", 0) ?: 0

                binding.songSeekBar.max = duration
                binding.songSeekBar.progress = currentPosition
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(progressReceiver)
    }
}
