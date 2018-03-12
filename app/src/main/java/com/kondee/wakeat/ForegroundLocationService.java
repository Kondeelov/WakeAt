package com.kondee.wakeat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.kondee.wakeat.service.ServiceConstant;

import org.parceler.Parcels;

import java.util.Objects;

public class ForegroundLocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "Kondee";
    private GoogleApiClient googleApiClient;
    private NotificationManager notificationManager;
    private Location location = new Location("DummyProvider");
    private LocationRequest locationRequest;
    private LocationModel locationModel;
    private Vibrator vibrator;
    long[] vibratePattern = {0, 1000, 100, 300, 100, 300};
    private Parcelable parcelable;

    @Override
    public void onCreate() {
        super.onCreate();

        location.setLatitude(0);
        location.setLongitude(0);

        googleApiClient = new GoogleApiClient.Builder(ForegroundLocationService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
        createLocationRequest();

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Objects.equals(intent.getAction(), ServiceConstant.STARTFOREGROUND_ACTION)) {
            parcelable = intent.getParcelableExtra("parcelable");
            locationModel = Parcels.unwrap(parcelable);

            Log.d(TAG, "onStartCommand: " + locationModel.getLatLngBounds().getCenter());

            Notification notification = getNotification();

            startForeground(ServiceConstant.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);

        } else if (Objects.equals(intent.getAction(), ServiceConstant.STOPFOREGROUND_ACTION)) {
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private Notification getNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);

        notificationIntent.setAction(ServiceConstant.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        notificationIntent.putExtra("parcelable", parcelable);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new Notification.Builder(this)
                .setContentTitle("Start Location Service!")
                .setTicker("Ticker")
                .setContentText("Wake At... Your location is " + locationModel.getLatLngBounds().getCenter().latitude + " " + locationModel.getLatLngBounds().getCenter().longitude)
                .setSmallIcon(R.drawable.location_on)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        googleApiClient.disconnect();

        vibrator.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        notificationManager.notify(ServiceConstant.NOTIFICATION_ID.FOREGROUND_SERVICE, getNotification());
        checkTargetArea();
    }

    private void checkTargetArea() {
        if (locationModel.getLatLngBounds().contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
            Log.d(TAG, "checkTargetArea: Success");
            vibrator.vibrate(vibratePattern, -1);
        }
    }

    public void requestLocationUpdates() {
        startService(new Intent(getApplicationContext(), ForegroundLocationService.class));
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, ForegroundLocationService.this);
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    public void removeLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,
                    ForegroundLocationService.this);
            stopSelf();
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(1500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
}
