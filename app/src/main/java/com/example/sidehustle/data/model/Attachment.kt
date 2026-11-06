package com.example.sidehustle.data.model

import com.google.gson.annotations.SerializedName

data class Attachment(
    @SerializedName("attachment_id") val id: String,
    val name: String,
    val url: String,
    @SerializedName("storage_path") val storagePath: String,
)

data class AttachmentRequest(
    val name: String,
    val url: String,
    @SerializedName("storage_path") val storagePath: String,
)
