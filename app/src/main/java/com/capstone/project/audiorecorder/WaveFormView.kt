package com.capstone.project.audiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveFormView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()
    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()
    private var maxSpikes = 0
    private var radius = 6f
    private var d = 6f
    private var w = 9f
    private var sw = 0f
    private var sh = 400f


    init {
        paint.color = Color.rgb(128, 0, 0)
        sw = resources.displayMetrics.widthPixels.toFloat()
        maxSpikes = (sw / (w + d)).toInt()
    }

    fun addAmplitude(amp: Float){
        var norm = Math.min(amp.toInt()/7, 400).toFloat()
        amplitudes.add(norm)

        spikes.clear()
        var amps = amplitudes.takeLast(maxSpikes)
        for (i in amps.indices){
            var left = sw - i * (w + d)
            var top = sh/2 - amps[i]/2
            var right= left + w
            var bottom = top + amps[i]
            spikes.add(RectF(left, top, right, bottom))
        }

        invalidate()
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        spikes.forEach {
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
        //canvas?.drawRoundRect(RectF(20F, 30F, 20+30F, 30F+60F), 6F, 6F, paint)
        //canvas?.drawRoundRect(RectF(60F, 60F, 60+80F, 60F+360F), 6F, 6F, paint)
        //canvas?.drawRoundRect(RectF(20F, 200F, 300+300F, 60F+60F), 6F, 6F, paint)
    }
}