package com.capstone.project.audiorecorder

import android.os.Handler
import android.os.Looper

class Timer(listener: OnTimerTickListener) {

    interface OnTimerTickListener{
        fun onTimerTick(duration: String)
    }

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private var duration = 0L
    private var delay = 100L

    init {
        runnable = Runnable {
            duration += delay
            handler.postDelayed(runnable, delay)
            listener.onTimerTick(format())
        }
    }

    fun start(){
        handler.postDelayed(runnable, delay)
    }

    fun pause(){
        handler.removeCallbacks(runnable)
    }

    fun stop(){
        handler.removeCallbacks(runnable)
        duration = 0L
    }

    fun format(): String {
        val mills: Long = duration % 1000
        val second: Long = (duration / 1000) % 60
        val minutes: Long = (duration / (1000 * 60)) % 60
        val hour: Long = (duration / (1000 * 60 * 60))

        //Tambahkan = pada kondisi if untuk memilih dari fornat hour
        var formatted: String = if(hour < 0)
            "%02d:%02d:%02d.%02d".format(hour, minutes, second, mills/10)
        else
            "%02d:%02d.%02d".format(minutes, second, mills/10)

        return formatted
    }
}