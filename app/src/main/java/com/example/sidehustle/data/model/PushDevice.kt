package com.example.sidehustle.data.model

import com.google.gson.annotations.SerializedName

data class PushDeviceRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("user_id") val userId: String,
    val token: String,
    val enabled: Boolean,
)

data class PushDeviceResponse(val registered: Boolean)
