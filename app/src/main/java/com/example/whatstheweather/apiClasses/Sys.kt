package com.example.whatstheweather.apiClasses

data class Sys(
    var type : Int,
    var id  : Int,
    var message : String,
    var country : String,
    var sunrise : Long,
    var sunset : Long

)