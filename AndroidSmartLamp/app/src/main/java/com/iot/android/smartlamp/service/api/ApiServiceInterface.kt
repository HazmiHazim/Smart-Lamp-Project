package com.iot.android.smartlamp.service.api

import com.iot.android.smartlamp.model.Lamp
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiServiceInterface {
    @GET("api/devices")
    suspend fun getAllLamps(): Response<List<Lamp>>

    @GET("api/devices/{public_id}")
    suspend fun getLamp(@Path("public_id") publicId : String): Response<Lamp>

    @POST("api/devices")
    suspend fun addLamp(@Body lamp : Lamp): Response<Lamp>

    @DELETE("api/devices/{id}")
    suspend fun deleteLamp(@Path("id") id: String): Response<Unit>
}