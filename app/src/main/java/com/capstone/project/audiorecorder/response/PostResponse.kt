package com.capstone.project.audiorecorder.response

import com.google.gson.annotations.SerializedName

data class PostResponse(
    val error: Boolean,
    val message: String,
    val accuracy: String
)