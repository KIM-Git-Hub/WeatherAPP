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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    private var textResult = ""

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
            getOneDayWeather()


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
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.d("result:", t.toString())
            }

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                Log.d("GG", "GG")
                if (response.code() == 200) {
                    Log.d("GG", "GG")
                    val weatherResponse = response.body()
                    Log.d("result2", weatherResponse.toString())
                    val cTemp = weatherResponse!!.main!!.temp - 273.15
                    val minTemp = weatherResponse.main!!.tempMin - 273.15
                    val maxTemp = weatherResponse.main!!.tempMax - 273.15
                    val stringBuilder =
                        "현재 기온 : " + cTemp + "\n" +
                                "최저기온 : " + minTemp + "\n" +
                                "최고기온 : " + maxTemp + "\n" +
                                "풍속 : " + weatherResponse.wind!!.speed + "\n" +
                                "일출시간 : " + weatherResponse.sys!!.sunrise + "\n" +
                                "일몰시간 : " + weatherResponse.sys!!.sunset + "\n" +
                                "아이콘 : " + weatherResponse.weather[0].icon

                    binding.textView.text = stringBuilder
                }

            }

        })

    }

    private fun unixTimeChange(unixTime: String): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE)
        val nowTime = Date(unixTime.toInt() * 1000L)
        return sdf.format(nowTime)
    }

    private fun getOneDayWeather() {  //////////errorrrrr
        textResult = "" //결과 초기화
        val apiKey = "01cbde2e5ca2f0fea3d136fe95ce3aa0"
        val apiUrl = "https://api.openweathermap.org/data/2.5/onecall?" +
                "lat=" + latitude + "&" + "lon=" + longitude + "&" + "lang=" + "ja" +
                "&" + "APPID=" + apiKey // 장소 언어 key 설정
        val url = URL(apiUrl)
        val br = BufferedReader(InputStreamReader(url.openStream())) // 정보 얻기
        //openStream url 읽기 //Buf StringType or Char 직렬화 //Inp CharType
        val str = br.readText() //문자열화
        val json = JSONObject(str) //json 형식 데이터로 식별
        val hourly = json.getJSONArray("hourly") //hourly 배열 획득
        //열시간 분 얻기
        for (i in 0..9) {
            val getObject = hourly.getJSONObject(0)
            val weatherList = getObject.getJSONArray("weather").getJSONObject(0)
            // unix time 형식의 시간 얻기
            val time = getObject.getString("dt")
            //날씨얻기
            val descriptionText = weatherList.getString("description")

            val test = "${unixTimeChange(time)}, $descriptionText "
            binding.oneDayWeather.text = test
        }
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
    ): Call<WeatherResponse>

}

class WeatherResponse {
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



