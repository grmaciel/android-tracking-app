package com.gilson.where

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    lateinit var rest: TrackingRest
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askMofoPermissions()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initRest()
        btnStart.setOnClickListener { startTracking() }
        btnStop.setOnClickListener { stopTracking() }
    }

    private fun askMofoPermissions() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                    123)
        }
    }

    private fun initRest() {
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })

        rest = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClient.build())
                .build().create(TrackingRest::class.java)
    }

    private fun startTracking() {
        rest.startSession()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("track", "received id: $it")
                    this.id = it.get("data").asJsonObject.get("_id").asString
                    this.trackPosition(this.id)
                }, { Log.e("track", "Error: $it") })
    }

    @SuppressLint("MissingPermission")
    private fun trackPosition(id: String) {
        val locationRequest =  LocationRequest()
        locationRequest.interval = 10000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationClient.requestLocationUpdates(locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        locationResult ?: return
                        for (location in locationResult.locations){
                            if (location != null) {
                                Log.d("track", "received location: $location")
                                val position = HashMap<String, String>().apply {
                                    put("latitude", location.latitude.toString())
                                    put("longitude", location.longitude.toString())
                                }
                                rest.trackLocation(id, position)
                                        .subscribeOn(Schedulers.io())
                                        .subscribe(
                                                { Log.d("track", "Sent pos $location and received: $it") },
                                                { Log.e("track", "Error sending pos: $it") })
                            }
                        }
                        }
                    }, null)
    }

    private fun stopTracking() {
        rest.endSession(this.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { Log.d("track", "Tracking ended: $it") },
                        { Log.e("track", "Error ending tracking pos: $it") })
    }
}
