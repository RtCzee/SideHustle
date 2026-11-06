package com.example.sidehustle.network

import com.google.gson.Gson
import retrofit2.converter.gson.GsonConverterFactory

/** Shared by the REST client and JVM contract tests so they use identical JSON settings. */
internal object ApiJson {
    val gson = Gson()
    val converterFactory: GsonConverterFactory = GsonConverterFactory.create(gson)
}
