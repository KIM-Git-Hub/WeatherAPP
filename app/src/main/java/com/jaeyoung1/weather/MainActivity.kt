package com.jaeyoung1.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.jaeyoung1.weather.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    private var textResult = ""
    private var latitude: Double? = 0.0
    private var longitude: Double? = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar
        actionBar?.hide()



        fusedLocationProviderClient()

        binding.button.setOnClickListener {
            Log.d("test3", "$longitude, $latitude ")

            getWeather()
            Thread.sleep(1000)
            binding.textView.text = textResult
        }


    }

    private fun unixTimeChange(unixTime: String): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE)
        val nowTime = Date(unixTime.toInt() * 1000L)
        return sdf.format(nowTime)
    }

    private fun getWeather(): Job = GlobalScope.launch {  //날씨 시각 얻기
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
            val getObject = hourly.getJSONObject(i)
            val weatherList = getObject.getJSONArray("weather").getJSONObject(0)
            // unix time 형식의 시간 얻기
            val time = getObject.getString("dt")
            //날씨얻기
            val descriptionText = weatherList.getString("description")
            textResult += "${unixTimeChange(time)} $descriptionText \n\n"
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
            }
        }

        //////

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
                        Log.d("AAAA", "$latitude, $longitude")

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


    /* override fun onRequestPermissionsResult(
         requestCode: Int,
         permissions: Array<out String>,
         grantResults: IntArray
     ) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)

         if(requestCode == REQUEST_CODE_LOCATION){
             if(grantResults.isNotEmpty()){
                 for(grant in grantResults){
                     if(grant != PackageManager.PERMISSION_GRANTED){
                         exitProcess(0)
                     }
                 }
             }
         }

     }*/

}