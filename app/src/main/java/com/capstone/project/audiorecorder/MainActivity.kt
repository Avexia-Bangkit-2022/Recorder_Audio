package com.capstone.project.audiorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.SyncStateContract
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
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
import okhttp3.MediaType.Companion.parse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
        const val PERMISSION_REQUEST_CODE = 1
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
    private var output: String? = null
    private var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
    private var files = MultipartBody.Part

    private lateinit var waveFormView: WaveFormView
    private lateinit var recorder: MediaRecorder
    private lateinit var timer: Timer
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var viewModel: ViewModel

    private val REQUEST_GALLERY = 2
    private var catchAudio: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomSheet()

        timer = Timer(this)

        pick_folder_button.setOnClickListener {
            startGallery()
            permission()
            //val intentGallery = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            //startActivityForResult(intentGallery, REQUEST_GALLERY)

        }

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
            File("$dirPath$fileName").delete()
            dismis()
        }

        btn_cardView_fetch.setOnClickListener {
            dismis()
            save()
        }

        bottomSheetBG.setOnClickListener {
            File("$dirPath$fileName").delete()
            dismis()
        }

        btn_Delete.setOnClickListener {
            stopRecording()
            File("$dirPath$fileName").delete()
            Toast.makeText(this, "Record delete", Toast.LENGTH_SHORT).show()
        }
        showLoading(false)
        btn_Delete.isClickable = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
//            val path: String? = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)
            val uri = Uri.parse(Environment.getExternalStorageDirectory().absolutePath + "/Recordings/")
            val path = data?.data
            if (path != null){
                val file1 = path.path?.let { File(it) }
                val requestFile = file1?.asRequestBody(".wav".toMediaTypeOrNull())
                val myfile =
                    requestFile?.let { MultipartBody.Part.createFormData("audio", file1?.name, it) }
                Log.d("OnAct", myfile.toString())

                val retrofit = myfile?.let { ApiConfig.getApiService().uploadAudio(it) }
                showLoading(true)
                retrofit?.enqueue(object : Callback<PostResponse>{
                    override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                        val responseBody = response.body()
                        if (response.isSuccessful){
                            /*val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                            intent.putExtra("filepath", filePath)
                            intent.putExtra("filename", newFileName)
                            intent.putExtra("error", responseBody?.error)
                            intent.putExtra("Predict", responseBody?.prediction)
                            startActivity(intent)
                            finish()*/

                            Toast.makeText(this@MainActivity, "Upload Succes : " + responseBody?.error,
                                Toast.LENGTH_LONG).show()
                            Log.d("Main", responseBody?.error.toString())
                            showLoading(false)

                        }
                    }

                    override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "Upload Failed : " + t.message, Toast.LENGTH_LONG).show()
                        Log.d("Main_onFailure", t.message.toString())
                        showLoading(false)
                    }
                })

            }
        }
    }

    private fun startGallery(){
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "audio/*"
        startActivityForResult(Intent.createChooser(intent, "select audio"), 111)
        /*val chooser = Intent.createChooser(intent, "select audio")
            launcherIntentGallery.launch(chooser)*/
    }

    private var getFile: File? = null

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == RESULT_OK){
            val selectedAudio: Uri = it.data?.data as Uri
            val myFile = uriToFile(selectedAudio, this@MainActivity)
            getFile = myFile
        }
    }

    private fun save() {
        val newFileName = fileName_Input.text.toString()
        /*if (newFileName != output){
            var newFile = File(Environment.getExternalStorageDirectory().absolutePath + "/soundrecorder/recording" + date + ".mp3")
            File(Environment.getExternalStorageDirectory().absolutePath + "/soundrecorder/recording" + date + ".mp3").renameTo(newFile)
        }
        var filePath = newFileName*/
        if (newFileName != fileName){
            var newFile = File("$dirPath$newFileName")
            File("$dirPath$fileName").renameTo(newFile)
        }

        var filePath = "$dirPath$newFileName"
        var timeStamp = Date().time

        //val file = File(filePath)
        //val reqAudio = file.asRequestBody("mp3/wav".toMediaTypeOrNull())

        //val reqFileUpload = RequestBody.create("mp3/wav", filePath)
        val dir = Environment.getExternalStorageDirectory().absolutePath + "/Recordings/"
        //val dir = Environment.getExternalStorageDirectory().absolutePath + "/Recordings/record_2022.06.161_08.42.20.wav"
        //val dir = Environment.getExternalStorageDirectory().absolutePath + "$filePath"
        val uri = Uri.parse(Environment.getExternalStorageDirectory().absolutePath)
        val name = dir.toRequestBody("text/plain".toMediaType())
        val file = File(dir).asRequestBody("mp3/wav".toMediaTypeOrNull())
        //val file = File(filePath).asRequestBody("mp3/wav".toMediaTypeOrNull())
        //val fileToUpload: MultipartBody.Part = MultipartBody.Part.createFormData(name.toString(), file.toString())
        val fileToUpload: MultipartBody.Part = MultipartBody.Part.createFormData("audio",
            "Egg_Song1.wav",file)
        Log.d("Main", file.toString())
        Log.d("Main", name.toString())
        Log.d("Main", fileToUpload.toString())
        Log.d("Main", dir)

        val retrofit = ApiConfig.getApiService().uploadAudio(fileToUpload)
        showLoading(true)
        retrofit.enqueue(object : Callback<PostResponse>{
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                val responseBody = response.body()
                if (response.isSuccessful){
                    /*val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                    intent.putExtra("filepath", filePath)
                    intent.putExtra("filename", newFileName)
                    intent.putExtra("error", responseBody?.error)
                    intent.putExtra("Predict", responseBody?.prediction)
                    startActivity(intent)
                    finish()*/

                    Toast.makeText(this@MainActivity, "Upload Succes : " + responseBody?.error,
                        Toast.LENGTH_LONG).show()
                    Log.d("Main", responseBody?.error.toString())
                    showLoading(false)

                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Upload Failed : " + t.message, Toast.LENGTH_LONG).show()
                Log.d("Main_onFailure", t.message.toString())
                showLoading(false)
            }
        })

        /*Toast.makeText(this, "Record fetch", Toast.LENGTH_SHORT).show()
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
        showLoading(false)
    }

    @SuppressLint("NewApi")
    private fun resumeRecording() {
        try {
            recorder.resume()
        }catch (e: IOException){
            e.printStackTrace()
        }

        isPause = false
        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_stopped, theme)
        timer.start()
    }

    @SuppressLint("NewApi")
    private fun pauseRecording() {
        try {
            recorder.pause()
        }catch (e: IOException){
            e.printStackTrace()
        }

        isPause = true
        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_recording, theme)
        timer.pause()
    }

    private fun startRecording(){

        var date = simpleDateFormat.format(Date())

        if (!permissionGranted){
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
            return
        }

        /*recorder = MediaRecorder()
        try {
            val recorderDirectory = File(Environment.getExternalStorageDirectory().absolutePath + "/soundrecorder/")
            recorderDirectory.mkdirs()
        }catch (e: IOException){
            e.printStackTrace()
        }

        if(dir.exists()){
            val count = dir.listFiles().size
            output = Environment.getExternalStorageDirectory().absolutePath + "/soundrecorder/recording" + date + ".mp3"
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(output)

        try {
            recorder.prepare()
            recorder.start()
        }catch (e: IllegalStateException){
            e.printStackTrace()
        }catch (e: IOException){
            e.printStackTrace()
        }*/

        recorder = MediaRecorder()
        fileName = "record_$date.wav"
        dirPath = Environment.getExternalStorageDirectory().absolutePath + "/Recordings/"
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile("$dirPath$fileName")
        try {
            recorder.prepare()
            recorder.start()
        }catch (e: IOException){
            e.printStackTrace()
        }

        //recorder = MediaRecorder()
        //fileName = "audio_record_$date"
        //dirPath = "{$externalCacheDir?.absolutePath}"
        //dirPath = externalCacheDir?.absolutePath.toString()
        /*recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.wav")

            try {
                prepare()
            }catch (e: IOException){}

            start()
        }*/

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

        try {
            recorder.stop()
            recorder.release()
        }catch (e: IOException){
            e.printStackTrace()
        }

        isPause = false
        isRecording = false

        btn_Done.visibility = View.GONE

        btn_Delete.isClickable = false
        btn_Delete.visibility = View.GONE

        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_recording, theme)

        tv_Timer.text = "00:00.00"
    }

    private fun bottomSheet(){
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_Sheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) { progressBar_Main.visibility = View.GONE }
        else{ progressBar_Main.visibility = View.VISIBLE }
    }

    @SuppressLint("NewApi")
    private fun permission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

}