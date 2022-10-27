package com.kondee.wakeat

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.Vibrator
import android.util.Log
import androidx.core.content.IntentCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.kondee.wakeat.service.ServiceConstant

class ForegroundLocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private var googleApiClient: GoogleApiClient? = null
    private var notificationManager: NotificationManager? = null
    private var location = Location("DummyProvider")
    private var locationRequest: LocationRequest? = null
    private var locationModel: LocationModel? = null
    private var vibrator: Vibrator? = null
    var vibratePattern = longArrayOf(0, 1000, 100, 300, 100, 300)
    private var parcelable: Parcelable? = null
    override fun onCreate() {
        super.onCreate()
        location.latitude = 0.0
        location.longitude = 0.0
        googleApiClient = GoogleApiClient.Builder(this@ForegroundLocationService)
            .addConnectionCallbacks(this@ForegroundLocationService)
            .addOnConnectionFailedListener(this@ForegroundLocationService)
            .addApi(LocationServices.API)
            .build().apply {
                connect()
            }
        createLocationRequest()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.getAction() == ServiceConstant.STARTFOREGROUND_ACTION) {
            parcelable = intent.getParcelableExtra("parcelable")
            locationModel = parcelable as LocationModel
            Log.d(TAG, "onStartCommand: " + locationModel!!.latLngBounds!!.center)
            val notification = notification
            startForeground(ServiceConstant.NOTIFICATION_ID.FOREGROUND_SERVICE, notification)
        } else if (intent.getAction() == ServiceConstant.STOPFOREGROUND_ACTION) {
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    private val notification: Notification
        private get() {
            val notificationIntent = Intent(this, MainActivity::class.java)
            notificationIntent.setAction(ServiceConstant.MAIN_ACTION)
            notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            notificationIntent.putExtra("parcelable", parcelable)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            return Notification.Builder(this)
                .setContentTitle("Start Location Service!")
                .setTicker("Ticker")
                .setContentText("Wake At... Your location is " + locationModel!!.latLngBounds!!.center.latitude + " " + locationModel!!.latLngBounds!!.center.longitude)
                .setSmallIcon(R.drawable.location_on)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build()
        }

    override fun onDestroy() {
        super.onDestroy()
        googleApiClient?.disconnect()
        vibrator?.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onConnected(bundle: Bundle?) {
        try {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
        requestLocationUpdates()
    }

    override fun onConnectionSuspended(i: Int) {}
    override fun onConnectionFailed(connectionResult: ConnectionResult) {}
    override fun onLocationChanged(location: Location) {
        this.location = location
        notificationManager?.notify(ServiceConstant.NOTIFICATION_ID.FOREGROUND_SERVICE, notification)
        checkTargetArea()
    }

    private fun checkTargetArea() {
        if (locationModel!!.latLngBounds!!.contains(LatLng(location.latitude, location.longitude))) {
            Log.d(TAG, "checkTargetArea: Success")
            vibrator?.vibrate(vibratePattern, -1)
        }
    }

    fun requestLocationUpdates() {
        startService(Intent(applicationContext, ForegroundLocationService::class.java))
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this@ForegroundLocationService
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }
    }

    fun removeLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient,
                this@ForegroundLocationService
            )
            stopSelf()
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest!!.interval = 4000
        locationRequest!!.fastestInterval = 1500
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    companion object {
        private const val TAG = "Kondee"
    }
}