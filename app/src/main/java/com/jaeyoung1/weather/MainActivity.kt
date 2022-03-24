package com.jaeyoung1.weather

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
    private var placeLat = 35.689499
    private var placeLon = 139.691711

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar
        actionBar?.hide()


        binding.button.setOnClickListener {
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
                "lat=" + placeLat + "&" + "lon=" + placeLon + "&" + "lang=" + "ja" +
                "&" + "APPID=" + apiKey // 장소 언어 key 설정
        val url = URL(apiUrl)
        val br = BufferedReader(InputStreamReader(url.openStream())) // 정보 얻기
        //openStream url 읽기 //Buf StringType or Char 직렬화 //Inp CharType

        val str = br.readText() //문자열화
        val json = JSONObject(str) //json 형식 데이터로 식별
        val hourly = json.getJSONArray("hourly") //hourly 배열 획득

        //열시간 분 얻기
        for(i in 0..9){
            val getObject = hourly.getJSONObject(i)
            val weatherList = getObject.getJSONArray("weather").getJSONObject(0)
            // unix time 형식의 시간 얻기
            val time = getObject.getString("dt")
            //날씨얻기
            val descriptionText = weatherList.getString("description")
            textResult += "${unixTimeChange(time)} $descriptionText \n\n"
        }

    }
}