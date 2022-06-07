package com.capstone.project.audiorecorder.api

import com.capstone.project.audiorecorder.response.GetResponse
import com.capstone.project.audiorecorder.response.PostResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @Multipart
    @POST("")
    fun postAudio(
        @Field("name") name: String,
        @Part file: MultipartBody.Part
    ):Call<PostResponse>

    @GET("")
    fun getAudio(
        @Path("name") name: String
    ):Call<GetResponse>
}