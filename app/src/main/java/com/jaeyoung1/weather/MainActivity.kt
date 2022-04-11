package com.jaeyoung1.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.jaeyoung1.weather.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    private var textResult = ""
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var locationManager: LocationManager? = null
    private var currentLocation: String = ""

    private val REQUEST_CODE_LOCATION: Int = 100
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar
        actionBar?.hide()



        binding.button.setOnClickListener {

            getCurrentLoc()
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

    private fun getCurrentLoc() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var userLocation: Location = getLatLng()
        if (userLocation != null) {
            latitude = userLocation.latitude //현재 내 위치 값
            longitude = userLocation.longitude

            //지오 코더 위도 경도 변환
            //applicationContext Application (어플리케이션)의 LifeCycle 에 종속적인 객체
            //	-> 어플리케이션 실행 - 종료까지의 객체 정보를 참조할 수 있음
            var mGeocoder = Geocoder(applicationContext, Locale.JAPANESE)
            var mResultList: List<Address>? = null
            try {
                mResultList =
                    mGeocoder.getFromLocation(latitude!!, longitude!!, 1) // 위도 경도 얻어올 값의 개수
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (mResultList != null) {
                Log.d(
                    "CheckCurrentLocation",
                    mResultList[0].getAddressLine(0)
                ) // getAddressLine 주소 문자열 얻기
                currentLocation = mResultList[0].getAddressLine(0)
                currentLocation = currentLocation.substring(11) // substring 문자열에서 임의의 범위를 정해 잘라줍니다

            }
        }
    }

    private fun getLatLng(): Location {
        var currentLatLng: Location? = null

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val locationProvider = LocationManager.GPS_PROVIDER
            //getLastKnownLocation 가장 마지막으로 확인된 위치
            currentLatLng = locationManager?.getLastKnownLocation(locationProvider)
        }else{
            //ActivityCompat.shouldShowRequestPermissionRationale
            // 사용자가 권한 요청을 명시적으로 거부한 경우 true 를 반환한다. → 사용자가 권한 요청을 처음 보거나, 다시 묻지 않음 선택한 경우, 권한을 허용한 경우 false를 반환한다
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])){
                Toast.makeText(this, "앱을 실행 할려면 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_LOCATION)
            }else{
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_LOCATION)
            }
            currentLatLng = getLatLng()
        }
        return currentLatLng!!
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_CODE_LOCATION && grantResults.size == REQUIRED_PERMISSIONS.size){
            var checkResult = true
            for(result in grantResults){
                if(result != PackageManager.PERMISSION_GRANTED ){
                    checkResult = false
                    break
                }
            }
            //if(check_result)부분이 권한을 허용했을 경우 실행하는 부분이고, else 는 그 반대이다.
            if(checkResult){
            }else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])){
                    Toast.makeText(this, "권한 설정이 거부되었습니다.\n앱을 사용하시려면 다시 실행해주세요.", Toast.LENGTH_SHORT).show()
                    finish()
                }else{
                    Toast.makeText(this, "권한 설정이 거부되었습니다.\n설정에서 권한을 허용해야 합니다..", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

}