package com.kondee.wakeat;

import com.google.android.gms.maps.model.LatLngBounds;

import org.parceler.Parcel;

@Parcel
public class LocationModel {

    private LatLngBounds latLngBounds;

    public LatLngBounds getLatLngBounds() {
        return latLngBounds;
    }

    public LocationModel setLatLngBounds(LatLngBounds latLngBounds) {
        this.latLngBounds = latLngBounds;
        return this;
    }
}
