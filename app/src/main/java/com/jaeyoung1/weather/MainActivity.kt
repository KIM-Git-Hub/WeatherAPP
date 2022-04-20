package com.jaeyoung1.weather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
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


class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!


    companion object {
        private var latitude: Double? = 0.0
        private var longitude: Double? = 0.0
        private const val baseURL = "http://api.openweathermap.org/"
        private const val appId = "01cbde2e5ca2f0fea3d136fe95ce3aa0"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar
        actionBar?.hide()

        getDate()
        fusedLocationProviderClient()

        binding.testButton.setOnClickListener {
            Log.d("AAA", "AAA")
            reloadActivity()
        }



        binding.button.setOnClickListener {


            Log.d("test3", "$longitude, $latitude   ")
            val address = getAddress(latitude!!, longitude!!)
            binding.address.text = address
            getWeather()
            getDailyWeather()
            Thread.sleep(1000)


        }


    }

    private fun getDate() {
        val c = Calendar.getInstance()
        val currentYear = c.get(Calendar.YEAR)
        val currentMonth = c.get(Calendar.MONTH) + 1
        val currentDay = c.get(Calendar.DAY_OF_MONTH)
        val currentDate: String = "$currentYear" + "年" + " " + "$currentMonth" +
                "月" + " " + "$currentDay" + "日"
        binding.timeText.text = currentDate
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
                Log.d("GG", "GG")
                if (response.code() == 200) {
                    Log.d("GG", "GG")
                    val weatherResponse = response.body()
                    Log.d("result2", weatherResponse.toString())

                    val lTemp = weatherResponse!!.main!!.temp - 273.15
                    val cTemp = lTemp.roundToLong()
                    val lMinTemp = weatherResponse.main!!.tempMin - 273.15
                    val minTemp = lMinTemp.roundToLong()
                    val lMaxTemp = weatherResponse.main!!.tempMax - 273.15
                    val maxTemp = lMaxTemp.roundToLong()

                    val lIcon = weatherResponse.weather[0].icon
                    val iconUrl = "http://openweathermap.org/img/w/$lIcon.png"
                    Picasso.get().load(iconUrl).into(binding.currentWeatherIcon)

                    val stringBuilder =
                        "현재 기온 : " + cTemp + "도" + "\n" +
                                "최저기온 : " + minTemp + "도" + "\n" +
                                "최고기온 : " + maxTemp + "도" + "\n" +
                                "풍속 : " + weatherResponse.wind!!.speed + "\n" +
                                "일출시간 : " + weatherResponse.sys!!.sunrise + "\n" +
                                "일몰시간 : " + weatherResponse.sys!!.sunset + "\n"


                    binding.textView.text = stringBuilder
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

                Log.d("GG2", "GG")
                if (response.code() == 200) {
                    Log.d("GG2", "GG2")
                    val weatherResponse = response.body()
                    Log.d("result2", weatherResponse.toString())

                    val lMaxTemp = weatherResponse!!.daily[2].temp!!.max - 273.15
                    val maxTemp = lMaxTemp.roundToLong()
                    val lMinTemp = weatherResponse.daily[2].temp!!.min - 273.15
                    val minTemp = lMinTemp.roundToLong()
                    val lPop = (weatherResponse.daily[2].pop) * 100
                    val pop = lPop.roundToLong()
                    val dailyTime = (weatherResponse.daily[2].dt).toString()
                    val unixTime = unixTimeChange(dailyTime)
                    val dailyUnixTime = unixTime.substring(0 until 11)
                    val dIcon = weatherResponse.daily[2].weather[0].icon
                    val dailyIconUrl = "http://openweathermap.org/img/w/$dIcon.png"
                    Picasso.get().load(dailyIconUrl).into(binding.dailyWeatherIcon)

                    val stringBuilder =
                        dailyUnixTime +  "최고온도 : " + maxTemp + "\n" +
                                "최저온도 : " + minTemp + "\n" +
                                "강수확률 : " + pop + "%"


                    binding.dailyWeatherText.text = stringBuilder
                    Log.d("tete", stringBuilder)


                    val hourlyTime = (weatherResponse.hourly[2].dt).toString()
                    val unixTime2 = unixTimeChange(hourlyTime)
                    val hourlyUnixTime = unixTime2.substring(11 until 16)
                    val lHourlyTemp = weatherResponse.hourly[2].temp - 273.15
                    val hourlyTemp = lHourlyTemp.roundToLong()
                    val hourlyWeather = weatherResponse.hourly[2].weather[0].description
                    val hIcon = weatherResponse.hourly[2].weather[0].icon
                    val hourlyIconUrl = "http://openweathermap.org/img/w/$hIcon.png"
                    Picasso.get().load(hourlyIconUrl).into(binding.hourlyWeatherIcon)

                    val stringBuilder2 = hourlyUnixTime + "\n" + " 온도 : " +
                            hourlyTemp + "\n" + " 날씨 : " + hourlyWeather
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



    private fun fusedLocationProviderClient() {
        // ContextCompat 은 Resource 에서 값을 가져오거나 퍼미션을 확인할 때 사용할 때 SDK 버전을 고려하지 않아도 되도록
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )


        }

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                Log.d("test0", "$longitude, $latitude ")

            }
        }

        val locationRequest = LocationRequest.create()
        //필요한 정확도를 설정하는 값
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 20000
        val locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        Log.d("test444", "$latitude, $longitude")

                    }
                }
            }
        }


        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallBack,
            Looper.getMainLooper()
        )

    }

    private fun getAddress(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this)
        val list = geocoder.getFromLocation(lat, lng, 1)

        return "${list[0].adminArea} ${list[0].locality}"
        //adminArea = 시 locality = 구 thoroughfare = 동
        //getAddressLine = 전체 주소
    }

    private fun reloadActivity() {
        finish()
        overridePendingTransition(0, 0) //인텐트 효과 없애기
        val intent = intent
        startActivity(intent)
        overridePendingTransition(0, 0)
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

    @SerializedName("pressure")
    var pressure: Float = 0.toFloat()

    @SerializedName("temp_min")
    var tempMin: Float = 0.toFloat()

    @SerializedName("temp_max")
    var tempMax: Float = 0.toFloat()
}

class Wind {
    @SerializedName("speed")
    var speed: Float = 0.toFloat()

    @SerializedName("deg")
    var deg: Float = 0.toFloat()
}

class Sys {
    @SerializedName("country")
    var country: String? = null

    @SerializedName("sunrise")
    var sunrise: Long = 0

    @SerializedName("sunset")
    var sunset: Long = 0
}



