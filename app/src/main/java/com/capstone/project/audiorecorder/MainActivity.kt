package com.capstone.project.audiorecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.capstone.project.audiorecorder.api.ApiConfig
import com.capstone.project.audiorecorder.response.PostResponse
import com.capstone.project.audiorecorder.viewmodel.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.progress_button.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    companion object{
        const val REQUEST_CODE = 200
    }

    private var permission = arrayOf(Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE)
    private var permissionGranted = false
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPause = false
    private var duration = ""

    private lateinit var waveFormView: WaveFormView
    private lateinit var recorder: MediaRecorder
    private lateinit var timer: Timer
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var viewModel: ViewModel

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

        viewModel = ViewModelProvider(this).get(ViewModel::class.java)

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
            File("$dirPath$fileName.wav").delete()
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
            File("$dirPath$fileName.wav").delete()
            dismis()
        }

        btn_Delete.setOnClickListener {
            stopRecording()
            File("$dirPath$fileName.wav").delete()
            Toast.makeText(this, "Record delete", Toast.LENGTH_SHORT).show()
        }

        btn_Delete.isClickable = false
    }

    private fun save() {
        val newFileName = fileName_Input.text.toString()
        if (newFileName != fileName){
            var newFile = File("$dirPath$newFileName.wav")
            File("$dirPath$fileName.wav").renameTo(newFile)
        }

        var filePath = "$dirPath$newFileName.wav"
        var timeStamp = Date().time

        //val file = File(filePath)
        //val reqAudio = file.asRequestBody("mp3/wav".toMediaTypeOrNull())

        //val reqFileUpload = RequestBody.create("mp3/wav", filePath)
        val name = newFileName.toRequestBody("text/plain".toMediaType())
        val file = File(filePath).asRequestBody("wav".toMediaTypeOrNull())
        val fileToUpload: MultipartBody.Part = MultipartBody.Part.createFormData("name", name.toString(), file)
        /*Log.d("Main", filePath)
        Log.d("Main", newFileName)
        Log.d("Main", fileName)*/

        val retrofit = ApiConfig.getApiService().uploadAudio(fileToUpload)
        retrofit.enqueue(object : Callback<PostResponse>{
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                if (response.isSuccessful){
                    val responseBody = response.body()
                    if (responseBody != null){
                        Toast.makeText(this@MainActivity, "Upload Succes", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                        intent.putExtra("filepath", filePath)
                        intent.putExtra("filename", newFileName)
                        intent.putExtra("Predict", responseBody.prediction)
                        startActivity(intent)
                        Log.d("Main", file.toString())
                        Log.d("Main", name.toString())
                        Log.d("Main", fileToUpload.toString())
                        Log.d("Main", responseBody.toString())
                    }else{
                        Toast.makeText(this@MainActivity, "Upload Failed : " + response.message(),
                                    Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Upload Failed : " + t.message, Toast.LENGTH_SHORT).show()
            }
        })

        /*Toast.makeText(this, "Record save", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("filepath", filePath)
        intent.putExtra("filename", newFileName)
        startActivity(intent)*/

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
            setOutputFile("$dirPath$fileName.wav")

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