package com.capstone.project.audiorecorder.response

import com.google.gson.annotations.SerializedName

data class PostResponse(
    @field:SerializedName("error")
    val error: String,

    @field:SerializedName("prediction")
    val prediction: String
)