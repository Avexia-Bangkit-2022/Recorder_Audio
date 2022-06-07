package com.capstone.project.audiorecorder

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_player.*
import java.text.DecimalFormat
import java.text.NumberFormat

class PlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L
    private var jumpValue = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")
        val file = fileName+filePath
        Log.d("player", file.toString())

        showLoading(true)

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        var tvFileName = findViewById<TextView>(R.id.tvFileName)
        var tvTrackProgress = findViewById<TextView>(R.id.tvTrackProgress)
        var tvTrackDuration = findViewById<TextView>(R.id.tvTrackDuration)
        var materialToolbar = findViewById<MaterialToolbar>(R.id.toolBar_Player)
        var btnPlay = findViewById<ImageButton>(R.id.btnPlay)
        var btnForward = findViewById<ImageButton>(R.id.btnForward)
        var btnBackward = findViewById<ImageButton>(R.id.btnBackward)
        var seekBar = findViewById<SeekBar>(R.id.seekBar)

        setSupportActionBar(materialToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        materialToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        tvFileName.text = "$fileName.mp3"
        tvTrackDuration.text = dateFormat(mediaPlayer.duration)

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            seekBar.progress = mediaPlayer.currentPosition
            tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
            handler.postDelayed(runnable, delay)
        }

        btnPlay.setOnClickListener { playPausePlayer() }

        playPausePlayer()
        seekBar.max = mediaPlayer.duration

        mediaPlayer.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_round_play_circle_24, theme)
            handler.removeCallbacks(runnable)
        }

        btnForward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
            seekBar.progress += jumpValue
        }

        btnBackward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
            seekBar.progress -= jumpValue
            seekBar.progress = seekBar.progress - jumpValue
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2){ mediaPlayer.seekTo(p1) }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })

    }

    private fun playPausePlayer() {
        if (!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_round_pause_circle_24, theme)
            handler.postDelayed(runnable, delay)
        }else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_round_play_circle_24, theme)
            handler.removeCallbacks(runnable)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }

    private fun dateFormat(duration: Int): String {
        var d = duration/1000
        var s = d % 60 //66 % 60 = 6
        var m = (d / 60 % 60) //66 minutes: m=6 & h=1
        var h = ((d - m * 60)/360).toInt()

        val f: NumberFormat = DecimalFormat("00")
        var str = "$m:${f.format(s)}"

        if (h > 0)
            str = "$h:$str"
        return str
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) { progressBar_Player.visibility = View.VISIBLE }
        else{ progressBar_Player.visibility = View.GONE }
    }
}