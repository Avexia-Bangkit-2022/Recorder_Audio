package com.capstone.project.audiorecorder.api

import com.capstone.project.audiorecorder.response.PostResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


interface ApiService {

    @Multipart
    @POST("predict")
    fun uploadAudio(
        @Part file: MultipartBody.Part
    ): Call<PostResponse>
}