package com.capstone.project.audiorecorder.api

import android.content.ClipDescription
import com.capstone.project.audiorecorder.response.GetResponse
import com.capstone.project.audiorecorder.response.PostResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface ApiService {

    /*@Multipart
    @POST("")
    fun postAudio(
        @Field("name") name: String,
        @Part file: MultipartBody.Part
    ):Call<PostResponse>*/

    /*@GET("")
    fun getAudio(
        @Path("name") name: String
    ):Call<GetResponse>*/

    @Headers("Content-Type: multipart/form-data", "Content-Type: application/json")
    @Multipart
    @POST("predict")
    fun uploadAudio(
        @Part file: MultipartBody.Part
    ): Call<PostResponse>
}