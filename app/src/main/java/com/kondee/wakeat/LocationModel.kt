package com.kondee.wakeat

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationModel(
    var latLngBounds: LatLngBounds? = null
) : Parcelable {

    fun setLatLngBounds(latLngBounds: LatLngBounds?): LocationModel {
        this.latLngBounds = latLngBounds
        return this
    }
}
