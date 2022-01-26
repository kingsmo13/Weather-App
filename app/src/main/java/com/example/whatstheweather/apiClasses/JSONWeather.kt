package com.example.whatstheweather.apiClasses

import java.io.Serializable

data class JSONWeather(
    var coord : Coord,
    var weather : List<Weather>,
    var base : String,
    var main : Main,
    var visibility : Int,
    var wind : Wind,
    var clouds : Clouds,
    var dt : Long,
    var sys : Sys,
    var id : Int,
    var name : String,
    var cod : Int
):Serializable