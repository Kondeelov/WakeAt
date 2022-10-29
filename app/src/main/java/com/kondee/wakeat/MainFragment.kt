package com.kondee.wakeat

import android.Manifest
import android.content.Context
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLngBounds
import android.os.Vibrator
import android.os.Parcelable
import android.os.Bundle
import com.kondee.wakeat.LocationModel
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.kondee.wakeat.R
import android.content.Intent
import com.kondee.wakeat.ForegroundLocationService
import com.kondee.wakeat.service.ServiceConstant
import com.kondee.wakeat.MainActivity
import com.kondee.wakeat.MainActivity.onOptionMenuCreated
import com.kondee.wakeat.MainActivity.onOptionItemSelected
import com.kondee.wakeat.MainFragment
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.CameraUpdateFactory
import android.content.IntentSender.SendIntentException
import android.location.Location
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.*
import com.kondee.wakeat.databinding.FragmentMainBinding
import com.kondee.wakeat.utils.Utils

/**
 * Created by Kondee on 5/3/2017.
 */
class MainFragment : Fragment(), OnMapReadyCallback, LocationListener, ConnectionCallbacks, OnConnectionFailedListener {
    private val ZOOM_DEFAULT_VALUE = 15
    var binding: FragmentMainBinding? = null
    private var googleMap: GoogleMap? = null
    private var googleApiClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null
    var shouldMoveCamera = false
    private var builder: PlacePicker.IntentBuilder? = null
    private var menu: Menu? = null
    private var marker: MarkerOptions? = null
    private var latLngBounds: LatLngBounds? = null
    private var vibrator: Vibrator? = null
    var vibratePattern = longArrayOf(0, 1000, 100, 300, 100, 300)
    private val isServiceStarted = false
    private var isFirstTime = false
    private var parcelable: Parcelable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parcelable = arguments?.getParcelable("parcelable")
        if (parcelable != null) {
            val latLngBounds1 = parcelable as LatLngBounds
            latLngBounds = latLngBounds1
        }
        isFirstTime = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        initInstance(savedInstanceState)
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        if (googleApiClient != null) {
            if (!googleApiClient!!.isConnecting || !googleApiClient!!.isConnected) {
                googleApiClient!!.connect()
            }
        }
        binding!!.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding!!.map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding!!.map.onDestroy()
        if (latLngBounds != null) {
            val intent = Intent(activity, ForegroundLocationService::class.java)
            intent.action = ServiceConstant.STARTFOREGROUND_ACTION
            val locationModel = LocationModel()
            locationModel.setLatLngBounds(latLngBounds)
            intent.putExtra("parcelable", locationModel)
            activity?.startService(intent)
        }
        vibrator!!.cancel()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding!!.map.onLowMemory()
    }

    private fun initInstance(savedInstanceState: Bundle?) {
        binding!!.map.onCreate(savedInstanceState)
        binding!!.map.getMapAsync(this)
        googleApiClient = GoogleApiClient.Builder(requireActivity())
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        builder = PlacePicker.IntentBuilder()
        vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val mainActivity = activity as MainActivity?
        mainActivity!!.setOnOptionMenuCreatedListener(object : onOptionMenuCreated {
            override fun onMenuCreated(menu: Menu?) {
                this@MainFragment.menu = menu
                setLocationIcon()
            }
        })
        mainActivity.setOnOptionItemSelectedListener(object : onOptionItemSelected {
            override fun onMenuSelected() {
                startPlacePicker()
            }
        })
    }

    private fun setLocationIcon() {
        if (menu != null) {
            val location = menu!!.findItem(R.id.location)
            if (marker == null) {
                location.setIcon(R.drawable.location_off)
            } else {
                location.setIcon(R.drawable.location_on)
            }
        }
    }

    private fun startPlacePicker() {
        try {
            startActivityForResult(builder!!.build(activity), PLACE_PICKER_REQUEST)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        if (latLngBounds != null) {
            val center = latLngBounds!!.center
            val markerOptions = MarkerOptions()
            markerOptions.position(center)
            setMarker(center, markerOptions)
        }
        setMyLocationEnable()
        googleMap.setOnMyLocationButtonClickListener {
            shouldMoveCamera = true
            setLocationRequest()
            true
        }
    }

    private fun setMyLocationEnable() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE)
                return
            }
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE)
            return
        }
        googleMap!!.isMyLocationEnabled = true
    }

    private fun setLocationRequest() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE)
                return
            }
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE)
            return
        }
        showLocationEnableRequest()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]
                if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        setMyLocationEnable()
                    } else {
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (data != null) {
                googleMap!!.clear()
                val place = PlacePicker.getPlace(activity, data)
                val markerOptions = MarkerOptions()
                markerOptions.position(place.latLng)
                setMarker(place.latLng, markerOptions)
                latLngBounds = Utils.getLatLngBounds(place.latLng, 250.0)
            }
        }
    }

    private fun setMarker(latLng: LatLng, markerOptions: MarkerOptions) {
        googleMap!!.addMarker(markerOptions)
        marker = markerOptions
        googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_DEFAULT_VALUE.toFloat()))
        setLocationIcon()
    }

    override fun onConnected(bundle: Bundle?) {
        locationRequest = LocationRequest.create()
        locationRequest?.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest?.setInterval(4000)
        locationRequest?.setFastestInterval(1500)

//        setLocationRequest();
    }

    fun updateCameraPosition() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE)
                return
            }
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE)
            return
        }

//        TODO : Get location by another method.
        val location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
            isFirstTime = false
        } else {
            if (isFirstTime) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
                isFirstTime = false
            } else {
                googleMap!!.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        ZOOM_DEFAULT_VALUE.toFloat()
                    )
                )
            }
            shouldMoveCamera = false
        }
    }

    private fun showLocationEnableRequest() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!)
        builder.setAlwaysShow(true)
        val result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback(object : ResultCallback<LocationSettingsResult> {

            val REQUEST_CHECK_SETTINGS = 0x1
            override fun onResult(locationSettingsResult: LocationSettingsResult) {
                val status = locationSettingsResult.status
                when (status.statusCode) {
                    LocationSettingsStatusCodes.SUCCESS -> {
                        updateCameraPosition()
                        Log.i(TAG, "All location settings are satisfied.")
                    }
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ")
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                        } catch (e: SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Log.i(
                        TAG,
                        "Location settings are inadequate, and cannot be fixed here. Dialog not created."
                    )
                }
            }
        })
    }

    override fun onConnectionSuspended(i: Int) {}
    override fun onConnectionFailed(connectionResult: ConnectionResult) {}
    override fun onLocationChanged(location: Location) {
        if (shouldMoveCamera) {
            updateCameraPosition()
        }
        checkTargetArea(location)
    }

    private fun checkTargetArea(location: Location) {
        if (latLngBounds != null) {
            if (latLngBounds!!.contains(LatLng(location.latitude, location.longitude))) {
                Log.d(TAG, "checkTargetArea: Success")
                vibrator!!.vibrate(vibratePattern, -1)
            }
        }
    }

    companion object {
        private const val TAG = "Kondee"
        private const val REQUEST_CODE = 1234
        private const val PLACE_PICKER_REQUEST = 4
        fun newInstance(parcelable: Parcelable?): MainFragment {
            val fragment = MainFragment()
            val args = Bundle()
            args.putParcelable("parcelable", parcelable)
            fragment.arguments = args
            return fragment
        }
    }
}