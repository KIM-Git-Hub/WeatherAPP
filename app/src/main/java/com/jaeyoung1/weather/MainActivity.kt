package com.jaeyoung1.weather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationListener
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.annotations.SerializedName
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.jaeyoung1.weather.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToLong
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), LocationListener {

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!


    companion object {
        private var latitude: Double? = 0.0
        private var longitude: Double? = 0.0
        private const val baseURL = "http://api.openweathermap.org/"
        private const val appId = "01cbde2e5ca2f0fea3d136fe95ce3aa0"
    }

    var stringBuilder = ""
    var stringBuilder2 = ""

    private lateinit var locationManager: LocationManager
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 使用が許可された
            locationStart()

        } else {
            // それでも拒否された時の対応
            val toast = Toast.makeText(
                this,
                "これ以上なにもできません", Toast.LENGTH_SHORT
            )
            toast.show()

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            locationStart()
        }

        val actionBar = supportActionBar
        actionBar?.hide()
        getDate()

    }


    private fun getDate() {
        val c = Calendar.getInstance()
        val currentYear = c.get(Calendar.YEAR)
        val currentMonth = c.get(Calendar.MONTH) + 1
        val currentDay = c.get(Calendar.DAY_OF_MONTH)
        val currentDate: String = "$currentYear" + "年" + " " + "$currentMonth" +
                "月" + " " + "$currentDay" + "日"
        binding.currentTimeText.text = currentDate
    }

    private fun getWeather(): Job = GlobalScope.launch {
        val retrofit =
            Retrofit.Builder().baseUrl(baseURL).addConverterFactory(GsonConverterFactory.create())
                .build()


        val services = retrofit.create(WeatherService::class.java)
        val call = services.getCurrentWeatherData(latitude.toString(), longitude.toString(), appId)

        //enqueue 인터페이스로부터 함수를 호출할 수 있다.
        call.enqueue(object : Callback<CurrentWeatherResponse> {
            override fun onFailure(call: Call<CurrentWeatherResponse>, t: Throwable) {
                Log.d("result:", t.toString())
            }

            override fun onResponse(
                call: Call<CurrentWeatherResponse>,
                response: Response<CurrentWeatherResponse>
            ) {
                if (response.code() == 200) {
                    val weatherResponse = response.body()

                    val lTemp = weatherResponse!!.main!!.temp - 273.15
                    val cTemp = lTemp.roundToLong()
                    val lMinTemp = weatherResponse.main!!.tempMin - 273.15
                    val minTemp = lMinTemp.roundToLong()
                    val lMaxTemp = weatherResponse.main!!.tempMax - 273.15
                    val maxTemp = lMaxTemp.roundToLong()
                    val lFeelsLike = weatherResponse.main!!.feelsLike - 273.15
                    val feelsLike = lFeelsLike.roundToLong()
                    val sunriseSunsetTime = SimpleDateFormat("HH:mm a", Locale.JAPANESE)
                    val lSunrise = weatherResponse.sys!!.sunrise
                    val sunrise = sunriseSunsetTime.format(lSunrise*1000L)
                    val lSunset = weatherResponse.sys!!.sunset
                    val sunset = sunriseSunsetTime.format(lSunset*1000L)
                    val lHumidity = weatherResponse.main!!.humidity
                    val humidity = lHumidity.roundToLong()
                    val lWindSpeed = weatherResponse.wind!!.speed
                    val windSpeed = lWindSpeed.roundToLong()
                    val lWindDeg = weatherResponse.wind!!.deg
                    val windDeg = windDegPosition(lWindDeg)
                    val lIcon = weatherResponse.weather[0].icon
                    val iconUrl = "http://openweathermap.org/img/w/$lIcon.png"
                    Picasso.get().load(iconUrl).into(binding.currentWeatherIcon)

                    val cTempString = "$cTemp°"
                    val feelsLikeString = "$feelsLike° 体感温度"
                    val cMaxMinTempString = "$minTemp°/$maxTemp°"
                    val sunriseSunsetString = "$sunrise/$sunset"
                    val humidityString = "$humidity%"
                    val windSpeedDegString = "$windSpeed m/s, $windDeg"

                    binding.currentTemp.text = cTempString
                    binding.feelsLike.text = feelsLikeString
                    binding.currentMaxMinTemp.text = cMaxMinTempString
                    binding.sunriseSunset.text = sunriseSunsetString
                    binding.humidity.text = humidityString
                    binding.windSpeedDeg.text = windSpeedDegString

                }

            }

        })

    }

    private fun getDailyWeather(): Job = GlobalScope.launch {

        val retrofit =
            Retrofit.Builder().baseUrl(baseURL).addConverterFactory(GsonConverterFactory.create())
                .build()


        val services = retrofit.create(WeatherService::class.java)
        val call = services.getDailyWeatherData(latitude.toString(), longitude.toString(), appId)

        //enqueue 인터페이스로부터 함수를 호출할 수 있다.
        call.enqueue(object : Callback<DailyWeatherResponse> {
            override fun onFailure(call: Call<DailyWeatherResponse>, t: Throwable) {
                Log.d("result:", t.toString())
            }

            override fun onResponse(
                call: Call<DailyWeatherResponse>,
                response: Response<DailyWeatherResponse>
            ) {

                Log.d("GGd", "GG")
                if (response.code() == 200) {
                    Log.d("GGdd", "GG2")
                    val weatherResponse = response.body()

                    for (i in 1..7) {
                        val lMaxTemp = weatherResponse!!.daily[i].temp!!.max - 273.15
                        val maxTemp = lMaxTemp.roundToLong()
                        val lMinTemp = weatherResponse.daily[i].temp!!.min - 273.15
                        val minTemp = lMinTemp.roundToLong()
                        val lPop = (weatherResponse.daily[i].pop) * 100
                        val pop = lPop.roundToLong()
                        val dailyTime = (weatherResponse.daily[i].dt).toString()
                        val unixTime = unixTimeChange(dailyTime)
                        val dailyUnixTime = unixTime.substring(0 until 11)
                        val dIcon = weatherResponse.daily[i].weather[0].icon
                        val dailyIconUrl = "http://openweathermap.org/img/w/$dIcon.png"
                        Picasso.get().load(dailyIconUrl).into(binding.dailyWeatherIcon)

                        stringBuilder +=
                            dailyUnixTime + "\n" + "최고온도 : " + maxTemp + " " +
                                    "최저온도 : " + minTemp + " " +
                                    "강수확률 : " + pop + "%" + "\n" + "\n"
                    }

                    binding.dailyWeatherText.text = stringBuilder

                    //currentPop
                    val lPop = (weatherResponse!!.daily[0].pop) * 100
                    val cPop = lPop.roundToLong().toString() + "%"
                    binding.currentPop.text = cPop

                    for (i in 0..12) {
                        val hourlyTime = (weatherResponse.hourly[i].dt).toString()
                        val unixTime2 = unixTimeChange(hourlyTime)
                        val hourlyUnixTime = unixTime2.substring(11 until 16)
                        val lHourlyTemp = weatherResponse.hourly[i].temp - 273.15
                        val hourlyTemp = lHourlyTemp.roundToLong()
                        val hourlyWeather = weatherResponse.hourly[i].weather[0].main
                        val hIcon = weatherResponse.hourly[i].weather[0].icon
                        val hourlyIconUrl = "http://openweathermap.org/img/w/$hIcon.png"
                        Picasso.get().load(hourlyIconUrl).into(binding.hourlyWeatherIcon)
                        stringBuilder2 += hourlyUnixTime + "\n" + " 온도 : " +
                                hourlyTemp + "\n" + " 날씨 : " + hourlyWeather + "\n"


                    }


                    binding.hourlyWeatherText.text = stringBuilder2

                }

            }

        })

    }

    private fun unixTimeChange(unixTime: String): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE)
        val nowTime = Date(unixTime.toInt() * 1000L)
        return sdf.format(nowTime)
    }


    private fun locationStart() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("debug", "location manager Enabled")
        } else {
            // to prompt setting up GPS
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(settingsIntent)
            Log.d("debug", "not gpsEnable, startActivity")
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000
            )

            Log.d("debug", "checkSelfPermission false")

        }

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                val address = getAddress(latitude!!, longitude!!)
                binding.address.text = address

                getWeather()
                getDailyWeather()
                Log.d("xx11", "11")
            }
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            10000,
            50f,
            this
        )

    }

    override fun onLocationChanged(location: Location) {

        if (latitude == 0.0 && longitude == 0.0) {
            // Latitude
            latitude = location.latitude

            // Longitude
            longitude = location.longitude

            val address = getAddress(latitude!!, longitude!!)
            binding.address.text = address
            getWeather()
            getDailyWeather()

        }


    }

    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }


    private fun getAddress(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this)
        val list = geocoder.getFromLocation(lat, lng, 1)

        return "${list[0].adminArea} ${list[0].locality}"
        //adminArea = 시 locality = 구 thoroughfare = 동
        //getAddressLine = 전체 주소
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(this@MainActivity, "권한 허가", Toast.LENGTH_SHORT).show()


            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String?>) {
                Toast.makeText(this@MainActivity, "권한 거부\n$deniedPermissions", Toast.LENGTH_SHORT)
                    .show()
                exitProcess(0)
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setRationaleMessage("구글 로그인을 하기 위해서는 권한이 필요해요")
            .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있어요.")
            .setPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .check()

    }


}

fun windDegPosition(windDeg : Float): String {
    if (windDeg>337.5) return "北"
    if (windDeg>292.5) return "北西"
    if(windDeg>247.5) return "西"
    if(windDeg>202.5) return "南西"
    if(windDeg>157.5) return "南"
    if(windDeg>122.5) return "東南"
    if(windDeg>67.5) return "東"
    if(windDeg>22.5) return "東北"
    return "北"
}

interface WeatherService {

    @GET("data/2.5/weather")
    fun getCurrentWeatherData(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("appId") appId: String
    ): Call<CurrentWeatherResponse>

    @GET("data/2.5/onecall")
    fun getDailyWeatherData(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("appId") appId: String
    ): Call<DailyWeatherResponse>

}

//dailyWeather
//////////////////////////////////////////////////////////////

class DailyWeatherResponse {
    @SerializedName("daily")
    var daily = ArrayList<Daily>()

    @SerializedName("hourly")
    var hourly = ArrayList<Hourly>()

}

class Daily {
    @SerializedName("weather")
    var weather = ArrayList<DailyWeather>()

    @SerializedName("temp")
    var temp: Temp? = null

    @SerializedName("pop")
    var pop: Float = 0.toFloat()

    @SerializedName("dt")
    var dt: Int = 0
}

class DailyWeather {
    @SerializedName("icon")
    var icon: String? = null
}


class Temp {
    @SerializedName("max")
    var max: Float = 0.toFloat()

    @SerializedName("min")
    var min: Float = 0.toFloat()
}

//hourlyWeather
//////////////////////////////////////////////////////////////


class Hourly {
    @SerializedName("dt")
    var dt: Int = 0

    @SerializedName("temp")
    var temp: Float = 0.toFloat()

    @SerializedName("weather")
    var weather = ArrayList<HourlyWeather>()
}

class HourlyWeather {
    @SerializedName("description")
    var description: String? = null

    @SerializedName("icon")
    var icon: String? = null

    @SerializedName("main")
    var main: String? = null
}


//currentWeather
//////////////////////////////////////////////////////////////

class CurrentWeatherResponse {
    @SerializedName("weather")
    var weather = ArrayList<Weather>()

    @SerializedName("main")
    var main: Main? = null

    @SerializedName("wind")
    var wind: Wind? = null

    @SerializedName("sys")
    var sys: Sys? = null
}

class Weather {
    @SerializedName("id")
    var id: Int = 0

    @SerializedName("main")
    var main: String? = null

    @SerializedName("description")
    var description: String? = null

    @SerializedName("icon")
    var icon: String? = null
}


class Main {
    @SerializedName("temp")
    var temp: Float = 0.toFloat()

    @SerializedName("humidity")
    var humidity: Float = 0.toFloat()

    @SerializedName("temp_min")
    var tempMin: Float = 0.toFloat()

    @SerializedName("temp_max")
    var tempMax: Float = 0.toFloat()

    @SerializedName("feels_like")
    var feelsLike: Float = 0.toFloat()
}

class Wind {
    @SerializedName("speed")
    var speed: Float = 0.toFloat()

    @SerializedName("deg")
    var deg: Float = 0.toFloat()
}

class Sys {
    @SerializedName("sunrise")
    var sunrise: Long = 0

    @SerializedName("sunset")
    var sunset: Long = 0
}



