package com.capstone.project.audiorecorder.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capstone.project.audiorecorder.api.ApiConfig
import com.capstone.project.audiorecorder.response.GetResponse
import com.capstone.project.audiorecorder.response.PostResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Multipart

class ViewModel : ViewModel()  {

    /*val getAudio = MutableLiveData<GetResponse>()
    val postAudio = MutableLiveData<PostResponse>()

    fun setAudioPlayer(name: String){
        val client = ApiConfig.getApiService().getAudio(name)
        client.enqueue(object : Callback<GetResponse>{
            override fun onResponse(call: Call<GetResponse>, response: Response<GetResponse>) {
                if (response.isSuccessful){
                    getAudio.postValue(response.body())
                }
            }

            override fun onFailure(call: Call<GetResponse>, t: Throwable) {
                Log.d("onFailure:", t.message.toString())
            }

        })
    }

    fun getAudioPlayer(): LiveData<GetResponse>{ return getAudio }

    fun setUploadAudio(name: String, file: MultipartBody.Part){
        val client = ApiConfig.getApiService().uploadAudio(name, file)
        client.enqueue(object : Callback<PostResponse>{
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                if (response.isSuccessful){
                    val responseBody = response.body()
                    postAudio.postValue(responseBody)
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                Log.d("onFailure:", t.message.toString())
            }

        })
    }

    fun getUploadAudio(): LiveData<PostResponse>{ return postAudio }*/

}