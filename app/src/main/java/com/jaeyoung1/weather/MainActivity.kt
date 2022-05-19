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
import android.view.View
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
                "権限を拒否しました。", Toast.LENGTH_SHORT
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
                    val sunrise = sunriseSunsetTime.format(lSunrise * 1000L)
                    val lSunset = weatherResponse.sys!!.sunset
                    val sunset = sunriseSunsetTime.format(lSunset * 1000L)
                    val lHumidity = weatherResponse.main!!.humidity
                    val humidity = lHumidity.roundToLong()
                    val lWindSpeed = weatherResponse.wind!!.speed
                    val windSpeed = lWindSpeed.roundToLong()
                    val lWindDeg = weatherResponse.wind!!.deg
                    val windDeg = windDegPosition(lWindDeg)
                    val lIcon = weatherResponse.weather[0].icon
                    Log.d("icon", lIcon.toString())
                    when (lIcon) {
                        "01d" -> binding.clearSky.visibility = View.VISIBLE
                        "02d" -> binding.fewClouds.visibility = View.VISIBLE
                        "03d" -> binding.clouds.visibility = View.VISIBLE
                        "04d" -> binding.clouds.visibility = View.VISIBLE
                        "09d" -> binding.rain.visibility = View.VISIBLE
                        "10d" -> binding.rain.visibility = View.VISIBLE
                        "11d" -> binding.thunder.visibility = View.VISIBLE
                        "13d" -> binding.snow.visibility = View.VISIBLE
                        "50d" -> binding.mist.visibility = View.VISIBLE
                        "01n" -> binding.clearSky.visibility = View.VISIBLE
                        "02n" -> binding.fewClouds.visibility = View.VISIBLE
                        "03n" -> binding.clouds.visibility = View.VISIBLE
                        "04n" -> binding.clouds.visibility = View.VISIBLE
                        "09n" -> binding.rain.visibility = View.VISIBLE
                        "10n" -> binding.rain.visibility = View.VISIBLE
                        "11n" -> binding.thunder.visibility = View.VISIBLE
                        "13n" -> binding.snow.visibility = View.VISIBLE
                        "50n" -> binding.mist.visibility = View.VISIBLE
                    }


                    val cTempString = "$cTemp°"
                    val feelsLikeString = "体感温度$feelsLike°"
                    val cMaxMinTempString = "$minTemp°/$maxTemp°"
                    val sunriseString = sunrise.toString()
                    val sunsetString = sunset.toString()
                    val humidityString = "$humidity%"
                    val windSpeedDegString = "$windSpeed m/s($windDeg)"

                    binding.currentTemp.text = cTempString
                    binding.feelsLike.text = feelsLikeString
                    binding.currentMaxMinTemp.text = cMaxMinTempString
                    binding.sunrise.text = sunriseString
                    binding.sunset.text = sunsetString
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

                if (response.code() == 200) {
                    val weatherResponse = response.body()


                    val dIcon1 = weatherResponse!!.daily[1].weather[0].icon
                    val dailyIconUrl1 = "http://openweathermap.org/img/w/$dIcon1.png"
                    Picasso.get().load(dailyIconUrl1).into(binding.dailyWeatherIcon1)

                    val dIcon2 = weatherResponse.daily[2].weather[0].icon
                    val dailyIconUrl2 = "http://openweathermap.org/img/w/$dIcon2.png"
                    Picasso.get().load(dailyIconUrl2).into(binding.dailyWeatherIcon2)

                    val dIcon3 = weatherResponse.daily[3].weather[0].icon
                    val dailyIconUrl3 = "http://openweathermap.org/img/w/$dIcon3.png"
                    Picasso.get().load(dailyIconUrl3).into(binding.dailyWeatherIcon3)

                    val dIcon4 = weatherResponse.daily[4].weather[0].icon
                    val dailyIconUrl4 = "http://openweathermap.org/img/w/$dIcon4.png"
                    Picasso.get().load(dailyIconUrl4).into(binding.dailyWeatherIcon4)

                    val dIcon5 = weatherResponse.daily[5].weather[0].icon
                    val dailyIconUrl5 = "http://openweathermap.org/img/w/$dIcon5.png"
                    Picasso.get().load(dailyIconUrl5).into(binding.dailyWeatherIcon5)

                    val dIcon6 = weatherResponse.daily[6].weather[0].icon
                    val dailyIconUrl6 = "http://openweathermap.org/img/w/$dIcon6.png"
                    Picasso.get().load(dailyIconUrl6).into(binding.dailyWeatherIcon6)

                    val dIcon7 = weatherResponse.daily[7].weather[0].icon
                    val dailyIconUrl7 = "http://openweathermap.org/img/w/$dIcon7.png"
                    Picasso.get().load(dailyIconUrl7).into(binding.dailyWeatherIcon7)


                    //-----------------------------------------------

                    val lMinTemp1 = weatherResponse.daily[1].temp!!.min - 273.15
                    val minTemp1 = lMinTemp1.roundToLong().toString() + "°"
                    binding.dailyMinTemp1.text = minTemp1
                    val lMaxTemp1 = weatherResponse.daily[1].temp!!.max - 273.15
                    val maxTemp1 = lMaxTemp1.roundToLong().toString() + "°"
                    binding.dailyMaxTemp1.text = maxTemp1

                    val lMinTemp2 = weatherResponse.daily[2].temp!!.min - 273.15
                    val minTemp2 = lMinTemp2.roundToLong().toString() + "°"
                    binding.dailyMinTemp2.text = minTemp2
                    val lMaxTemp2 = weatherResponse.daily[2].temp!!.max - 273.15
                    val maxTemp2 = lMaxTemp2.roundToLong().toString() + "°"
                    binding.dailyMaxTemp2.text = maxTemp2

                    val lMinTemp3 = weatherResponse.daily[3].temp!!.min - 273.15
                    val minTemp3 = lMinTemp3.roundToLong().toString() + "°"
                    binding.dailyMinTemp3.text = minTemp3
                    val lMaxTemp3 = weatherResponse.daily[3].temp!!.max - 273.15
                    val maxTemp3 = lMaxTemp3.roundToLong().toString() + "°"
                    binding.dailyMaxTemp3.text = maxTemp3

                    val lMinTemp4 = weatherResponse.daily[4].temp!!.min - 273.15
                    val minTemp4 = lMinTemp4.roundToLong().toString() + "°"
                    binding.dailyMinTemp4.text = minTemp4
                    val lMaxTemp4 = weatherResponse.daily[4].temp!!.max - 273.15
                    val maxTemp4 = lMaxTemp4.roundToLong().toString() + "°"
                    binding.dailyMaxTemp4.text = maxTemp4

                    val lMinTemp5 = weatherResponse.daily[5].temp!!.min - 273.15
                    val minTemp5 = lMinTemp5.roundToLong().toString() + "°"
                    binding.dailyMinTemp5.text = minTemp5
                    val lMaxTemp5 = weatherResponse.daily[5].temp!!.max - 273.15
                    val maxTemp5 = lMaxTemp5.roundToLong().toString() + "°"
                    binding.dailyMaxTemp5.text = maxTemp5

                    val lMinTemp6 = weatherResponse.daily[6].temp!!.min - 273.15
                    val minTemp6 = lMinTemp6.roundToLong().toString() + "°"
                    binding.dailyMinTemp6.text = minTemp6
                    val lMaxTemp6 = weatherResponse.daily[6].temp!!.max - 273.15
                    val maxTemp6 = lMaxTemp6.roundToLong().toString() + "°"
                    binding.dailyMaxTemp6.text = maxTemp6

                    val lMinTemp7 = weatherResponse.daily[7].temp!!.min - 273.15
                    val minTemp7 = lMinTemp7.roundToLong().toString() + "°"
                    binding.dailyMinTemp7.text = minTemp7
                    val lMaxTemp7 = weatherResponse.daily[7].temp!!.max - 273.15
                    val maxTemp7 = lMaxTemp7.roundToLong().toString() + "°"
                    binding.dailyMaxTemp7.text = maxTemp7


                    //------------------------------------------------

                    val lDailyPop1 = (weatherResponse.daily[1].pop) * 100
                    val dailyPop1 = lDailyPop1.roundToLong().toString() + "%"
                    binding.dailyPop1.text = dailyPop1

                    val lDailyPop2 = (weatherResponse.daily[2].pop) * 100
                    val dailyPop2 = lDailyPop2.roundToLong().toString() + "%"
                    binding.dailyPop2.text = dailyPop2

                    val lDailyPop3 = (weatherResponse.daily[3].pop) * 100
                    val dailyPop3 = lDailyPop3.roundToLong().toString() + "%"
                    binding.dailyPop3.text = dailyPop3

                    val lDailyPop4 = (weatherResponse.daily[4].pop) * 100
                    val dailyPop4 = lDailyPop4.roundToLong().toString() + "%"
                    binding.dailyPop4.text = dailyPop4

                    val lDailyPop5 = (weatherResponse.daily[5].pop) * 100
                    val dailyPop5 = lDailyPop5.roundToLong().toString() + "%"
                    binding.dailyPop5.text = dailyPop5

                    val lDailyPop6 = (weatherResponse.daily[6].pop) * 100
                    val dailyPop6 = lDailyPop6.roundToLong().toString() + "%"
                    binding.dailyPop6.text = dailyPop6

                    val lDailyPop7 = (weatherResponse.daily[7].pop) * 100
                    val dailyPop7 = lDailyPop7.roundToLong().toString() + "%"
                    binding.dailyPop7.text = dailyPop7


                    //------------------------------------------------

                    val calendar = Calendar.getInstance()
                    val day = calendar.get(Calendar.DAY_OF_WEEK)

                    var dayPlus1 = day + 1
                    if (dayPlus1 >= 8) {
                        dayPlus1 = 1
                    }

                    when (dayPlus1) {
                        Calendar.SUNDAY -> binding.dayOfWeek1.text = "日曜日"
                        Calendar.MONDAY -> binding.dayOfWeek1.text = "月曜日"
                        Calendar.TUESDAY -> binding.dayOfWeek1.text = "火曜日"
                        Calendar.WEDNESDAY -> binding.dayOfWeek1.text = "水曜日"
                        Calendar.THURSDAY -> binding.dayOfWeek1.text = "木曜日"
                        Calendar.FRIDAY -> binding.dayOfWeek1.text = "金曜日"
                        Calendar.SATURDAY -> binding.dayOfWeek1.text = "土曜日"
                    }

                    var dayPlus2 = day + 2
                    if (dayPlus2 >= 8) {
                        dayPlus2 = day - 5
                    }

                    when (dayPlus2) {
                        Calendar.SUNDAY -> binding.dayOfWeek2.text = "日曜日"
                        Calendar.MONDAY -> binding.dayOfWeek2.text = "月曜日"
                        Calendar.TUESDAY -> binding.dayOfWeek2.text = "火曜日"
                        Calendar.WEDNESDAY -> binding.dayOfWeek2.text = "水曜日"
                        Calendar.THURSDAY -> binding.dayOfWeek2.text = "木曜日"
                        Calendar.FRIDAY -> binding.dayOfWeek2.text = "金曜日"
                        Calendar.SATURDAY -> binding.dayOfWeek2.text = "土曜日"
                    }

                    var dayPlus3 = day + 3
                    if (dayPlus3 >= 8) {
                        dayPlus3 = day - 4
                    }

                    when (dayPlus3) {
                        Calendar.SUNDAY -> binding.dayOfWeek3.text = "日曜日"
                        Calendar.MONDAY -> binding.dayOfWeek3.text = "月曜日"
                        Calendar.TUESDAY -> binding.dayOfWeek3.text = "火曜日"
                        Calendar.WEDNESDAY -> binding.dayOfWeek3.text = "水曜日"
                        Calendar.THURSDAY -> binding.dayOfWeek3.text = "木曜日"
                        Calendar.FRIDAY -> binding.dayOfWeek3.text = "金曜日"
                        Calendar.SATURDAY -> binding.dayOfWeek3.text = "土曜日"
                    }

                    var dayPlus4 = day + 4
                    if (dayPlus4 >= 8) {
                        dayPlus4 = day - 3
                    }

                    when (dayPlus4) {
                        Calendar.SUNDAY -> binding.dayOfWeek4.text = "日曜日"
                        Calendar.MONDAY -> binding.dayOfWeek4.text = "月曜日"
                        Calendar.TUESDAY -> binding.dayOfWeek4.text = "火曜日"
                        Calendar.WEDNESDAY -> binding.dayOfWeek4.text = "水曜日"
                        Calendar.THURSDAY -> binding.dayOfWeek4.text = "木曜日"
                        Calendar.FRIDAY -> binding.dayOfWeek4.text = "金曜日"
                        Calendar.SATURDAY -> binding.dayOfWeek4.text = "土曜日"
                    }

                    var dayPlus5 = day + 5
                    if (dayPlus5 >= 8) {
                        dayPlus5 = day - 2
                    }

                    when (dayPlus5) {
                        Calendar.SUNDAY -> binding.dayOfWeek5.text = "日曜日"
                        Calendar.MONDAY -> binding.dayOfWeek5.text = "月曜日"
                        Calendar.TUESDAY -> binding.dayOfWeek5.text = "火曜日"
                        Calendar.WEDNESDAY -> binding.dayOfWeek5.text = "水曜日"
                        Calendar.THURSDAY -> binding.dayOfWeek5.text = "木曜日"
                        Calendar.FRIDAY -> binding.dayOfWeek5.text = "金曜日"
                        Calendar.SATURDAY -> binding.dayOfWeek5.text = "土曜日"
                    }
                    var dayPlus6 = day + 6
                    if (dayPlus6 >= 8) {
                        dayPlus6 = day - 1
                    }

                    when (dayPlus6) {
                        Calendar.SUNDAY -> binding.dayOfWeek6.text = "日曜日"
                        Calendar.MONDAY -> binding.dayOfWeek6.text = "月曜日"
                        Calendar.TUESDAY -> binding.dayOfWeek6.text = "火曜日"
                        Calendar.WEDNESDAY -> binding.dayOfWeek6.text = "水曜日"
                        Calendar.THURSDAY -> binding.dayOfWeek6.text = "木曜日"
                        Calendar.FRIDAY -> binding.dayOfWeek6.text = "金曜日"
                        Calendar.SATURDAY -> binding.dayOfWeek6.text = "土曜日"
                    }

                    when (day) {
                        Calendar.SUNDAY -> binding.dayOfWeek7.text = "日曜日"
                        Calendar.MONDAY -> binding.dayOfWeek7.text = "月曜日"
                        Calendar.TUESDAY -> binding.dayOfWeek7.text = "火曜日"
                        Calendar.WEDNESDAY -> binding.dayOfWeek7.text = "水曜日"
                        Calendar.THURSDAY -> binding.dayOfWeek7.text = "木曜日"
                        Calendar.FRIDAY -> binding.dayOfWeek7.text = "金曜日"
                        Calendar.SATURDAY -> binding.dayOfWeek7.text = "土曜日"
                    }


                    //------------------------------------------------

                    //currentPop
                    val lcPop = (weatherResponse.daily[0].pop)
                    val cPop = "降水確率" + lcPop.roundToLong().toString() + "%"
                    binding.currentPop.text = cPop

                    //------------------------------------------------

                    val hourlyTime1 = (weatherResponse.hourly[1].dt).toString()
                    val lUnixTime1 = unixTimeChange(hourlyTime1)
                    val hourlyUnixTime1 = lUnixTime1.substring(11 until 16)
                    binding.hourlyTime1.text = hourlyUnixTime1

                    val hourlyTime2 = (weatherResponse.hourly[2].dt).toString()
                    val lUnixTime2 = unixTimeChange(hourlyTime2)
                    val hourlyUnixTime2 = lUnixTime2.substring(11 until 16)
                    binding.hourlyTime2.text = hourlyUnixTime2

                    val hourlyTime3 = (weatherResponse.hourly[3].dt).toString()
                    val lUnixTime3 = unixTimeChange(hourlyTime3)
                    val hourlyUnixTime3 = lUnixTime3.substring(11 until 16)
                    binding.hourlyTime3.text = hourlyUnixTime3

                    val hourlyTime4 = (weatherResponse.hourly[4].dt).toString()
                    val lUnixTime4 = unixTimeChange(hourlyTime4)
                    val hourlyUnixTime4 = lUnixTime4.substring(11 until 16)
                    binding.hourlyTime4.text = hourlyUnixTime4

                    val hourlyTime5 = (weatherResponse.hourly[5].dt).toString()
                    val lUnixTime5 = unixTimeChange(hourlyTime5)
                    val hourlyUnixTime5 = lUnixTime5.substring(11 until 16)
                    binding.hourlyTime5.text = hourlyUnixTime5

                    val hourlyTime6 = (weatherResponse.hourly[6].dt).toString()
                    val lUnixTime6 = unixTimeChange(hourlyTime6)
                    val hourlyUnixTime6 = lUnixTime6.substring(11 until 16)
                    binding.hourlyTime6.text = hourlyUnixTime6

                    val hourlyTime7 = (weatherResponse.hourly[7].dt).toString()
                    val lUnixTime7 = unixTimeChange(hourlyTime7)
                    val hourlyUnixTime7 = lUnixTime7.substring(11 until 16)
                    binding.hourlyTime7.text = hourlyUnixTime7

                    val hourlyTime8 = (weatherResponse.hourly[8].dt).toString()
                    val lUnixTime8 = unixTimeChange(hourlyTime8)
                    val hourlyUnixTime8 = lUnixTime8.substring(11 until 16)
                    binding.hourlyTime8.text = hourlyUnixTime8

                    val hourlyTime9 = (weatherResponse.hourly[9].dt).toString()
                    val lUnixTime9 = unixTimeChange(hourlyTime9)
                    val hourlyUnixTime9 = lUnixTime9.substring(11 until 16)
                    binding.hourlyTime9.text = hourlyUnixTime9

                    val hourlyTime10 = (weatherResponse.hourly[10].dt).toString()
                    val lUnixTime10 = unixTimeChange(hourlyTime10)
                    val hourlyUnixTime10 = lUnixTime10.substring(11 until 16)
                    binding.hourlyTime10.text = hourlyUnixTime10

                    val hourlyTime11 = (weatherResponse.hourly[11].dt).toString()
                    val lUnixTime11 = unixTimeChange(hourlyTime11)
                    val hourlyUnixTime11 = lUnixTime11.substring(11 until 16)
                    binding.hourlyTime11.text = hourlyUnixTime11

                    val hourlyTime12 = (weatherResponse.hourly[12].dt).toString()
                    val lUnixTime12 = unixTimeChange(hourlyTime12)
                    val hourlyUnixTime12 = lUnixTime12.substring(11 until 16)
                    binding.hourlyTime12.text = hourlyUnixTime12

                    val hourlyTime13 = (weatherResponse.hourly[13].dt).toString()
                    val lUnixTime13 = unixTimeChange(hourlyTime13)
                    val hourlyUnixTime13 = lUnixTime13.substring(11 until 16)
                    binding.hourlyTime13.text = hourlyUnixTime13

                    //------------------------------------------------

                    val lHourlyTemp1 = weatherResponse.hourly[1].temp - 273.15
                    val hourlyTemp1 = lHourlyTemp1.roundToLong().toString() + "°"
                    binding.hourlyTemp1.text = hourlyTemp1

                    val lHourlyTemp2 = weatherResponse.hourly[2].temp - 273.15
                    val hourlyTemp2 = lHourlyTemp2.roundToLong().toString() + "°"
                    binding.hourlyTemp2.text = hourlyTemp2

                    val lHourlyTemp3 = weatherResponse.hourly[3].temp - 273.15
                    val hourlyTemp3 = lHourlyTemp3.roundToLong().toString() + "°"
                    binding.hourlyTemp3.text = hourlyTemp3

                    val lHourlyTemp4 = weatherResponse.hourly[4].temp - 273.15
                    val hourlyTemp4 = lHourlyTemp4.roundToLong().toString() + "°"
                    binding.hourlyTemp4.text = hourlyTemp4

                    val lHourlyTemp5 = weatherResponse.hourly[5].temp - 273.15
                    val hourlyTemp5 = lHourlyTemp5.roundToLong().toString() + "°"
                    binding.hourlyTemp5.text = hourlyTemp5

                    val lHourlyTemp6 = weatherResponse.hourly[6].temp - 273.15
                    val hourlyTemp6 = lHourlyTemp6.roundToLong().toString() + "°"
                    binding.hourlyTemp6.text = hourlyTemp6

                    val lHourlyTemp7 = weatherResponse.hourly[7].temp - 273.15
                    val hourlyTemp7 = lHourlyTemp7.roundToLong().toString() + "°"
                    binding.hourlyTemp7.text = hourlyTemp7

                    val lHourlyTemp8 = weatherResponse.hourly[8].temp - 273.15
                    val hourlyTemp8 = lHourlyTemp8.roundToLong().toString() + "°"
                    binding.hourlyTemp8.text = hourlyTemp8

                    val lHourlyTemp9 = weatherResponse.hourly[9].temp - 273.15
                    val hourlyTemp9 = lHourlyTemp9.roundToLong().toString() + "°"
                    binding.hourlyTemp9.text = hourlyTemp9

                    val lHourlyTemp10 = weatherResponse.hourly[10].temp - 273.15
                    val hourlyTemp10 = lHourlyTemp10.roundToLong().toString() + "°"
                    binding.hourlyTemp10.text = hourlyTemp10

                    val lHourlyTemp11 = weatherResponse.hourly[11].temp - 273.15
                    val hourlyTemp11 = lHourlyTemp11.roundToLong().toString() + "°"
                    binding.hourlyTemp11.text = hourlyTemp11

                    val lHourlyTemp12 = weatherResponse.hourly[12].temp - 273.15
                    val hourlyTemp12 = lHourlyTemp12.roundToLong().toString() + "°"
                    binding.hourlyTemp12.text = hourlyTemp12

                    val lHourlyTemp13 = weatherResponse.hourly[13].temp - 273.15
                    val hourlyTemp13 = lHourlyTemp13.roundToLong().toString() + "°"
                    binding.hourlyTemp13.text = hourlyTemp13

                    //------------------------------------------------

                    val lHourlyPop1 = weatherResponse.hourly[1].pop
                    val hourlyPop1 = lHourlyPop1.roundToLong().toString() + "%"
                    binding.hourlyPop1.text = hourlyPop1

                    val lHourlyPop2 = weatherResponse.hourly[2].pop
                    val hourlyPop2 = lHourlyPop2.roundToLong().toString() + "%"
                    binding.hourlyPop2.text = hourlyPop2

                    val lHourlyPop3 = weatherResponse.hourly[3].pop
                    val hourlyPop3 = lHourlyPop3.roundToLong().toString() + "%"
                    binding.hourlyPop3.text = hourlyPop3

                    val lHourlyPop4 = weatherResponse.hourly[4].pop
                    val hourlyPop4 = lHourlyPop4.roundToLong().toString() + "%"
                    binding.hourlyPop4.text = hourlyPop4

                    val lHourlyPop5 = weatherResponse.hourly[5].pop
                    val hourlyPop5 = lHourlyPop5.roundToLong().toString() + "%"
                    binding.hourlyPop5.text = hourlyPop5

                    val lHourlyPop6 = weatherResponse.hourly[6].pop
                    val hourlyPop6 = lHourlyPop6.roundToLong().toString() + "%"
                    binding.hourlyPop6.text = hourlyPop6

                    val lHourlyPop7 = weatherResponse.hourly[7].pop
                    val hourlyPop7 = lHourlyPop7.roundToLong().toString() + "%"
                    binding.hourlyPop7.text = hourlyPop7

                    val lHourlyPop8 = weatherResponse.hourly[8].pop
                    val hourlyPop8 = lHourlyPop8.roundToLong().toString() + "%"
                    binding.hourlyPop8.text = hourlyPop8

                    val lHourlyPop9 = weatherResponse.hourly[9].pop
                    val hourlyPop9 = lHourlyPop9.roundToLong().toString() + "%"
                    binding.hourlyPop9.text = hourlyPop9

                    val lHourlyPop10 = weatherResponse.hourly[10].pop
                    val hourlyPop10 = lHourlyPop10.roundToLong().toString() + "%"
                    binding.hourlyPop10.text = hourlyPop10

                    val lHourlyPop11 = weatherResponse.hourly[11].pop
                    val hourlyPop11 = lHourlyPop11.roundToLong().toString() + "%"
                    binding.hourlyPop11.text = hourlyPop11

                    val lHourlyPop12 = weatherResponse.hourly[12].pop
                    val hourlyPop12 = lHourlyPop12.roundToLong().toString() + "%"
                    binding.hourlyPop12.text = hourlyPop12

                    val lHourlyPop13 = weatherResponse.hourly[13].pop
                    val hourlyPop13 = lHourlyPop13.roundToLong().toString() + "%"
                    binding.hourlyPop13.text = hourlyPop13

                    //------------------------------------------------


                    val hIcon1 = weatherResponse.hourly[1].weather[0].icon
                    val hourlyIconUrl1 = "http://openweathermap.org/img/w/$hIcon1.png"
                    Picasso.get().load(hourlyIconUrl1).into(binding.hourlyWeatherIcon1)

                    val hIcon2 = weatherResponse.hourly[2].weather[0].icon
                    val hourlyIconUrl2 = "http://openweathermap.org/img/w/$hIcon2.png"
                    Picasso.get().load(hourlyIconUrl2).into(binding.hourlyWeatherIcon2)

                    val hIcon3 = weatherResponse.hourly[3].weather[0].icon
                    val hourlyIconUrl3 = "http://openweathermap.org/img/w/$hIcon3.png"
                    Picasso.get().load(hourlyIconUrl3).into(binding.hourlyWeatherIcon3)

                    val hIcon4 = weatherResponse.hourly[4].weather[0].icon
                    val hourlyIconUrl4 = "http://openweathermap.org/img/w/$hIcon4.png"
                    Picasso.get().load(hourlyIconUrl4).into(binding.hourlyWeatherIcon4)

                    val hIcon5 = weatherResponse.hourly[5].weather[0].icon
                    val hourlyIconUrl5 = "http://openweathermap.org/img/w/$hIcon5.png"
                    Picasso.get().load(hourlyIconUrl5).into(binding.hourlyWeatherIcon5)

                    val hIcon6 = weatherResponse.hourly[6].weather[0].icon
                    val hourlyIconUrl6 = "http://openweathermap.org/img/w/$hIcon6.png"
                    Picasso.get().load(hourlyIconUrl6).into(binding.hourlyWeatherIcon6)

                    val hIcon7 = weatherResponse.hourly[7].weather[0].icon
                    val hourlyIconUrl7 = "http://openweathermap.org/img/w/$hIcon7.png"
                    Picasso.get().load(hourlyIconUrl7).into(binding.hourlyWeatherIcon7)

                    val hIcon8 = weatherResponse.hourly[8].weather[0].icon
                    val hourlyIconUrl8 = "http://openweathermap.org/img/w/$hIcon8.png"
                    Picasso.get().load(hourlyIconUrl8).into(binding.hourlyWeatherIcon8)

                    val hIcon9 = weatherResponse.hourly[9].weather[0].icon
                    val hourlyIconUrl9 = "http://openweathermap.org/img/w/$hIcon9.png"
                    Picasso.get().load(hourlyIconUrl9).into(binding.hourlyWeatherIcon9)

                    val hIcon10 = weatherResponse.hourly[10].weather[0].icon
                    val hourlyIconUrl10 = "http://openweathermap.org/img/w/$hIcon10.png"
                    Picasso.get().load(hourlyIconUrl10).into(binding.hourlyWeatherIcon10)

                    val hIcon11 = weatherResponse.hourly[11].weather[0].icon
                    val hourlyIconUrl11 = "http://openweathermap.org/img/w/$hIcon11.png"
                    Picasso.get().load(hourlyIconUrl11).into(binding.hourlyWeatherIcon11)

                    val hIcon12 = weatherResponse.hourly[12].weather[0].icon
                    val hourlyIconUrl12 = "http://openweathermap.org/img/w/$hIcon12.png"
                    Picasso.get().load(hourlyIconUrl12).into(binding.hourlyWeatherIcon12)

                    val hIcon13 = weatherResponse.hourly[13].weather[0].icon
                    val hourlyIconUrl13 = "http://openweathermap.org/img/w/$hIcon13.png"
                    Picasso.get().load(hourlyIconUrl13).into(binding.hourlyWeatherIcon13)


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
                Toast.makeText(this@MainActivity, "権限許可", Toast.LENGTH_SHORT).show()


            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String?>) {
                Toast.makeText(this@MainActivity, "権限拒否", Toast.LENGTH_SHORT)
                    .show()
                exitProcess(0)
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setRationaleMessage("アプリケーションを使用するためには権限が必要です。")
            .setDeniedMessage("[設定]->[権限] 権限を許可してください。")
            .setPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .check()

    }
    override fun onDestroy() {
        super.onDestroy()
        mBinding = null
    }


}

fun windDegPosition(windDeg: Float): String {
    if (windDeg > 337.5) return "北"
    if (windDeg > 292.5) return "北西"
    if (windDeg > 247.5) return "西"
    if (windDeg > 202.5) return "南西"
    if (windDeg > 157.5) return "南"
    if (windDeg > 122.5) return "東南"
    if (windDeg > 67.5) return "東"
    if (windDeg > 22.5) return "東北"
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

    @SerializedName("pop")
    var pop: Float = 0.toFloat()
}

class HourlyWeather {

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



