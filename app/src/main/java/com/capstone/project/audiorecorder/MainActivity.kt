package com.capstone.project.audiorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.capstone.project.audiorecorder.api.ApiConfig
import com.capstone.project.audiorecorder.response.PostResponse
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.progress_button.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    companion object{
        private val PERMISSION = arrayOf(Manifest.permission.RECORD_AUDIO,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE)
        const val REQUEST_CODE = 200
        const val FILENAME_FORMAT = "dd-MMM-yyyy_hh:mm:ss"
    }

    @SuppressLint("SimpleDateFormat")
    private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT).format(Date())

    private var permission = arrayOf(Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE)
    private var permissionGranted = false
    private var isRecording = false
    private var isPause = false
    private var duration = ""
    private var files = MultipartBody.Part
    private var getFile: File? = null

    private lateinit var recorder: MediaRecorder
    private lateinit var timer: Timer
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    var selectedPaths = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
        }

        bottomSheet()
        timer = Timer(this)

        pick_folder_button.setOnClickListener {
            startGallery()
        }

        button_Main_Fetch_file.setOnClickListener {
            uploadAudio()
        }

        btn_Record.setOnClickListener {
            when{
                isPause -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
        }

        btn_Done.setOnClickListener {
            stopRecording()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBG.visibility = View.VISIBLE
            fileName_Input.setText(timeStamp)
        }

        imageButton_Cancel.setOnClickListener {
            createCustomTempFile(applicationContext).delete()
            createFile(application).delete()
            Toast.makeText(this, "Record delete", Toast.LENGTH_LONG).show()
            dismiss()
        }

        bottomSheetBG.setOnClickListener {
            createFile(application).delete()
            Toast.makeText(this, "Record delete", Toast.LENGTH_LONG).show()
            dismiss()
        }

        btn_cardView_fetch.setOnClickListener {
            save()
            dismiss()
        }

        btn_Delete.setOnClickListener {
            stopRecording()
            createCustomTempFile(applicationContext).delete()
            createFile(application).delete()
            Toast.makeText(this, "Record delete", Toast.LENGTH_SHORT).show()
        }
        btn_Delete.isClickable = false
    }

    private fun createCustomTempFile(context: Context): File {
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File.createTempFile("Record_$timeStamp", ".mp3", storageDir)
    }

    private fun createFile(application: Application): File {
        val mediaDir = application.externalMediaDirs.firstOrNull()?.let {
            File(it, application.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        val outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else application.filesDir

        return File(outputDirectory, "Record_$timeStamp.mp3")
    }

    private fun uriToFile(selectedAudio: Uri, context: Context): File {
        val contentResolver: ContentResolver = context.contentResolver
        val myFile = createCustomTempFile(context)

        val inputStream = contentResolver.openInputStream(selectedAudio) as InputStream
        val outputStream: OutputStream = FileOutputStream(myFile)
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
        outputStream.close()
        inputStream.close()

        return myFile
    }

    private fun startGallery(){
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "*/*"
        val chooser = Intent.createChooser(intent, "select audio")
        launcherIntentGallery.launch(chooser)
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == RESULT_OK){
            val selectedAudio: Uri = result.data?.data as Uri
            val myFile = uriToFile(selectedAudio, this@MainActivity)
            myFile.let { selectedPaths.add(it.absolutePath) }
            getFile = myFile
            textView_Name_Audio.visibility = View.VISIBLE
            textView_Name_Audio.text = getFile.toString()
        }
    }

    private fun uploadAudio(){
        progressBar_Main.visibility = View.VISIBLE
        if (getFile != null){
            val file = getFile as File
            val reqAudioFile = file.asRequestBody("audio/mp3".toMediaTypeOrNull())
            val audioMultiPart: MultipartBody.Part = MultipartBody.Part.createFormData("audio", file.name, reqAudioFile)
            val retrofit = ApiConfig.getApiService().uploadAudio(audioMultiPart)

            retrofit.enqueue(object : Callback<PostResponse>{
                override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                    val responseBody = response.body()
                    if (response.isSuccessful){
                        Toast.makeText(this@MainActivity, "Upload Succes : " + response.body()?.message, Toast.LENGTH_LONG).show()
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                        intent.putExtra("filepath", getFile)
                        intent.putExtra("filename", file.name)
                        intent.putExtra("message", responseBody?.message)
                        intent.putExtra("accuracy", responseBody?.accuracy)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, "Upload Succes : " + responseBody?.message.toString(),
                            Toast.LENGTH_LONG).show()
                        Log.d("Main", responseBody?.error.toString())
                        progressBar_Main.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Upload Failed : " + t.message, Toast.LENGTH_LONG).show()
                    Log.d("Main_onFailure", t.message.toString())
                    progressBar_Main.visibility = View.GONE
                }

            })
        }
        progressBar_Main.visibility = View.GONE
    }

    @SuppressLint("NewApi")
    private fun save(){
        recorder.setOutputFile(createCustomTempFile(applicationContext))
        recorder.setOutputFile(createFile(application))
        Toast.makeText(this, "Record save", Toast.LENGTH_LONG).show()
    }

    private fun dismiss() {
        bottomSheetBG.visibility = View.GONE
        hideKeyboard(fileName_Input)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
        showLoading(false)
    }

    private fun resumeRecording() {
        try {
            recorder.resume()
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e:IllegalStateException){
            e.printStackTrace()
        }

        isPause = false
        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_stopped, theme)
        timer.start()
    }

    private fun pauseRecording() {
        try {
            recorder.pause()
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e:IllegalStateException){
            e.printStackTrace()
        }
        isPause = true
        btn_Record.background = ResourcesCompat.getDrawable(resources, R.drawable.record_btn_recording, theme)
        timer.pause()
    }

    private fun startRecording(){
        if (!permissionGranted){
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
            return
        }

        recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        //recorder.setOutputFile(dirPath)
        try {
            recorder.prepare()
            recorder.start()
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e:IllegalStateException){
            e.printStackTrace()
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
        try {
            recorder.stop()
            recorder.release()
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e:IllegalStateException){
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

    override fun onTimerTick(duration: String) {
        tv_Timer.text = duration
        this.duration = duration.dropLast(3)
        WaveFormView.addAmplitude(recorder.maxAmplitude.toFloat())
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar_Main.visibility = if (isLoading) View.VISIBLE else View.GONE
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