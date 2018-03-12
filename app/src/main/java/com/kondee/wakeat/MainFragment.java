package com.kondee.wakeat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kondee.wakeat.databinding.FragmentMainBinding;
import com.kondee.wakeat.service.ServiceConstant;
import com.kondee.wakeat.utils.Utils;

import org.parceler.Parcels;

/**
 * Created by Kondee on 5/3/2017.
 */

public class MainFragment extends Fragment implements OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Kondee";
    private static final int REQUEST_CODE = 1234;
    private static final int PLACE_PICKER_REQUEST = 4;
    private final int ZOOM_DEFAULT_VALUE = 15;
    FragmentMainBinding binding;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    boolean shouldMoveCamera = false;
    private PlacePicker.IntentBuilder builder;
    private Menu menu;
    private MarkerOptions marker;
    private LatLngBounds latLngBounds = null;
    private Vibrator vibrator;

    long[] vibratePattern = {0, 1000, 100, 300, 100, 300};
    private boolean isServiceStarted;
    private boolean isFirstTime;
    private Parcelable parcelable;

    public static MainFragment newInstance(Parcelable parcelable) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();

        args.putParcelable("parcelable", parcelable);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parcelable = getArguments().getParcelable("parcelable");

        if (parcelable != null) {
            LocationModel model = Parcels.unwrap(parcelable);
            latLngBounds = model.getLatLngBounds();
        }

        isFirstTime = true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);

        initInstance(savedInstanceState);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (googleApiClient != null) {
            if (!googleApiClient.isConnecting() || !googleApiClient.isConnected()) {
                googleApiClient.connect();
            }
        }

        binding.map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        binding.map.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        binding.map.onDestroy();

        if (latLngBounds != null) {
            Intent intent = new Intent(getActivity(), ForegroundLocationService.class);
            intent.setAction(ServiceConstant.STARTFOREGROUND_ACTION);
            LocationModel locationModel = new LocationModel();
            locationModel.setLatLngBounds(latLngBounds);

            intent.putExtra("parcelable", Parcels.wrap(locationModel));
            getActivity().startService(intent);
        }

        vibrator.cancel();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        binding.map.onLowMemory();
    }

    private void initInstance(Bundle savedInstanceState) {

        binding.map.onCreate(savedInstanceState);

        binding.map.getMapAsync(this);

        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        builder = new PlacePicker.IntentBuilder();

        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        MainActivity mainActivity = (MainActivity) getActivity();

        mainActivity.setOnOptionMenuCreatedListener(new MainActivity.onOptionMenuCreated() {

            @Override
            public void onMenuCreated(Menu menu) {
                MainFragment.this.menu = menu;

                setLocationIcon();
            }
        });

        mainActivity.setOnOptionItemSelectedListener(new MainActivity.onOptionItemSelected() {
            @Override
            public void onMenuSelected() {

                startPlacePicker();
            }
        });

    }

    private void setLocationIcon() {
        if (menu != null) {
            MenuItem location = menu.findItem(R.id.location);
            if (marker == null) {
                location.setIcon(R.drawable.location_off);
            } else {
                location.setIcon(R.drawable.location_on);
            }
        }
    }

    private void startPlacePicker() {
        try {
            startActivityForResult(builder.build(getActivity()), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (latLngBounds != null) {
            LatLng center = latLngBounds.getCenter();
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(center);

            setMarker(center, markerOptions);
        }

        setMyLocationEnable();

        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                shouldMoveCamera = true;

                setLocationRequest();

                return true;
            }
        });
    }

    private void setMyLocationEnable() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
                return;
            }

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);

            return;
        }

        googleMap.setMyLocationEnabled(true);
    }

    private void setLocationRequest() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
                return;
            }

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);

            return;
        }

        showLocationEnableRequest();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        setMyLocationEnable();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                    }
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (data != null) {

                googleMap.clear();

                Place place = PlacePicker.getPlace(getActivity(), data);

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(place.getLatLng());
                setMarker(place.getLatLng(), markerOptions);

                latLngBounds = Utils.getLatLngBounds(place.getLatLng(), 250);

            }
        }
    }

    private void setMarker(LatLng latLng, MarkerOptions markerOptions) {
        googleMap.addMarker(markerOptions);

        this.marker = markerOptions;

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_DEFAULT_VALUE));

        setLocationIcon();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(1500);

//        setLocationRequest();

    }

    public void updateCameraPosition() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
                return;
            }

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);

            return;
        }

//        TODO : Get location by another method.

        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

            isFirstTime = false;
        } else {
            if (isFirstTime) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

                isFirstTime = false;
            } else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), ZOOM_DEFAULT_VALUE));
            }
            shouldMoveCamera = false;
        }
    }

    private void showLocationEnableRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            public static final int REQUEST_CHECK_SETTINGS = 0x1;

            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        updateCameraPosition();
                        Log.i(TAG, "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (shouldMoveCamera) {
            updateCameraPosition();
        }
        checkTargetArea(location);
    }

    private void checkTargetArea(Location location) {
        if (latLngBounds != null) {
            if (latLngBounds.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
                Log.d(TAG, "checkTargetArea: Success");
                vibrator.vibrate(vibratePattern, -1);
            }
        }
    }
}
