package com.example.whatstheweather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.JsonReader
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.whatstheweather.apiClasses.JSONWeather
import com.example.whatstheweather.apiClasses.WeatherService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    var dialog : Dialog? = null
    private lateinit var sharedPreferences : SharedPreferences
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)
        setupUI()
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if(!isLocationEnabled())
        {
            Toast.makeText(this,"Location permissions Required",Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else{
            Dexter.withContext(this).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener{

                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if(p0!!.areAllPermissionsGranted())
                    {
                        requestLocationData()
                    }
                    if(p0.isAnyPermissionPermanentlyDenied)
                    {
                        Toast.makeText(this@MainActivity,"You have permanently denied Location Permission ",Toast.LENGTH_LONG).show()

                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                        showRationalDialogForPermission()
                }
            }).onSameThread().check()
        }


    }
    fun isLocationEnabled():Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun showRationalDialogForPermission(){
        AlertDialog.Builder(this).setTitle(
            "Location Permission Not Granted!!"
        ).setPositiveButton(
            "Go To Settings"
        ){ _,_ ->
            try{
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package",packageName,null)

                intent.data = uri
                startActivity(intent)
            }catch(e : Exception)
            {

            }

        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    private fun requestLocationData()
    {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest,mLocationCallBack, Looper.myLooper()
        )
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private val mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            val mLastLocation = p0.lastLocation
            val latitude = mLastLocation.latitude
            val longitude = mLastLocation.longitude
            Log.i("customWeather",""+latitude)
            Log.i("customWeather",""+longitude)
            getLocationWeatherDetails(latitude,longitude)
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getLocationWeatherDetails(latitude : Double , longitude : Double)
    {
        if(Constants.isNetworkAvailable(this))
        {
            try {
                var retrofit = Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service: WeatherService =
                    retrofit.create<WeatherService>(WeatherService::class.java)

                val listCall: Call<JSONWeather> =
                    service.getWeather(latitude, longitude ,Constants.APP_ID)


                showDialog()

                listCall.enqueue(object : Callback<JSONWeather>{
                    override fun onResponse(
                        call: Call<JSONWeather>?,
                        response: Response<JSONWeather>?
                    ) {
                        dialog!!.hide()
                        val weatherList = response!!.body()

                        val weatherResponseJSONString = Gson().toJson(weatherList)
                        val editor = sharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJSONString)
                        editor.apply()
                        setupUI()
                        Log.i("customLL", "" + weatherList)
                    }

                    override fun onFailure(call: Call<JSONWeather>?, t: Throwable?) {
                        Log.i("customLL","Error")
                        dialog!!.hide()
                    }

                })


            }catch(e:Exception)
            {
                e.printStackTrace()
                Log.i("customLL",""+e)
            }
        }
        else{
            Toast.makeText(this,"Internet Connection not available",Toast.LENGTH_LONG).show()
        }
    }

    fun showDialog()
    {
        dialog = Dialog(this)

        dialog!!.setContentView(R.layout.custom_dialog)

        dialog!!.show()

    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)
    fun setupUI()
    {
        val weatherResponseJSONString = sharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")

        if(!weatherResponseJSONString.isNullOrEmpty())
        {
            val mWeatherObject = Gson().fromJson(weatherResponseJSONString,JSONWeather::class.java)
            weatherMain.text = mWeatherObject.weather[0].main
            temperatureMain.text = (Math.round((mWeatherObject.main.temp-272.15)*10)/10).toString()+"\u2103"
            windMain.text = mWeatherObject.wind.speed.toString()+" KpH"
            humidityMain.text = mWeatherObject.main.humidity.toString()+"%"

            sunriseMain.text = mConvert(mWeatherObject.sys.sunrise)
            sunsetMain.text = mConvert(mWeatherObject.sys.sunset)

            cityMain.text = mWeatherObject.name.toString()+" , " + mWeatherObject.sys.country.toString()

        }
        else
        {
            Toast.makeText(this, "Failed" , Toast.LENGTH_LONG).show()
        }



    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun mConvert(mTime : Long):String{
        val itemDate = Date(mTime * 1000)

        val text: String = SimpleDateFormat("hh:mm a",Locale.getDefault()).format(itemDate)

        return text
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId)
        {
            R.id.menuID ->{
                Log.i("mTag","success")
                requestLocationData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}