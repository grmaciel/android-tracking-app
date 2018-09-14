package com.gilson.where

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TrackingRest {
    @PUT("/tracking/{id}")
    fun trackLocation(@Path("id") id: String, @Body coordinates: HashMap<String, String>): Observable<JsonElement>

    @POST("/tracking")
    fun startSession(): Observable<JsonObject>

    @POST("/tracking/{id}")
    fun endSession(@Path("id") id: String): Observable<JsonElement>
}