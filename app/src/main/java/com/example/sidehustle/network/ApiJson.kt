package com.example.sidehustle.network

import retrofit2.converter.gson.GsonConverterFactory

/** Shared by the REST client and JVM contract tests so they use identical JSON settings. */
internal object ApiJson {
    val converterFactory: GsonConverterFactory = GsonConverterFactory.create()
}
