package com.example.whatstheweather.apiClasses

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {

    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat : Double,
        @Query("lon") lon : Double,
//        @Query("lang") lang : String?,
        @Query("appid") appid : String?
    ) : Call<JSONWeather>
}