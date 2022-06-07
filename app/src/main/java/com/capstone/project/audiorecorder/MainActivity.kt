package com.capstone.project.audiorecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.progress_button.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    companion object{
        const val REQUEST_CODE = 200
    }

    private var permission = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var permissionGranted = false
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPause = false
    private var duration = ""

    private lateinit var recorder: MediaRecorder
    private lateinit var timer: Timer
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permission[0]) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted)
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_Sheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        timer = Timer(this)

        btn_Record.setOnClickListener {
            when{
                isPause -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
            vibrate()
        }

        btn_Done.setOnClickListener {
            stopRecording()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBG.visibility = View.VISIBLE
            fileName_Input.setText(fileName)
        }

        imageButton_Cancel.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismis()
        }

        /*btn_Ok.setOnClickListener {
            dismis()
            save()
        }*/

        btn_cardView_fetch.setOnClickListener {
            dismis()
            save()
        }

        bottomSheetBG.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismis()
        }

        btn_Delete.setOnClickListener {
            stopRecording()
            File("$dirPath$fileName.mp3").delete()
            Toast.makeText(this, "Record delete", Toast.LENGTH_SHORT).show()
        }

        btn_Delete.isClickable = false
    }

    private fun save() {
        val newFileName = fileName_Input.text.toString()
        if (newFileName != fileName){
            var newFile = File("$dirPath$newFileName.mp3")
            File("$dirPath$fileName.mp3").renameTo(newFile)
        }

        var filePath = "$dirPath$newFileName.mp3"
        var timeStamp = Date().time

        Toast.makeText(this, "Record save", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("filepath", filePath)
        intent.putExtra("filename", newFileName)
        startActivity(intent)

    }

    private fun dismis() {
        bottomSheetBG.visibility = View.GONE
        hideKeyboard(fileName_Input)
        
        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun resumeRecording() {
        recorder.resume()
        isPause = false
        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_stopped, theme)
        timer.start()
    }

    private fun pauseRecording() {
        recorder.pause()
        isPause = true
        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_recording, theme)
        timer.pause()
    }

    private fun startRecording(){

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())

        if (!permissionGranted){
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
            return
        }

        recorder = MediaRecorder()
        fileName = "audio_record_$date"
        //dirPath = "{$externalCacheDir?.absolutePath}"
        dirPath = externalCacheDir?.absolutePath.toString()


        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
            }catch (e: IOException){}

            start()
        }

        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_stopped, theme)
        isRecording = true
        isPause = false

        timer.start()

        btn_Delete.isClickable = true
        btn_Delete.setImageResource(R.drawable.ic_round_close_24)
        btn_Delete.visibility = View.VISIBLE

        btn_Done.visibility = View.VISIBLE
    }

    private fun stopRecording(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPause = false
        isRecording = false

        btn_Done.visibility = View.GONE

        btn_Delete.isClickable = false
        btn_Delete.visibility = View.GONE

        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_recording, theme)

        tv_Timer.text = "00:00.00"
    }

    private fun vibrate(){
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    override fun onTimerTick(duration: String) {
        tv_Timer.text = duration
        this.duration = duration.dropLast(3)
        WaveFormView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}