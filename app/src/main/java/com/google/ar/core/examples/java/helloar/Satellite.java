package com.google.ar.core.examples.java.helloar;

import com.google.ar.core.examples.java.helloar.rendering.SatelliteRenderer;

import com.google.ar.core.examples.java.helloar.SGP4.SGP4SatData;

/**
 * Created by TY on 1/1/2018. Damn this is how I spend my new year?
 */

public class Satellite {
    private int mID;          // Norad ID
    public double mLongitude; // longitude in radians
    private double mLatitude; // latitude in radians
    private double mAltitude; // altitude in meters
    private double mSpeed;    // speed in m/s
    public SGP4SatData data;  // com.google.ar.core.examples.java.helloar.SGP4 data
    private SatelliteRenderer mRenderer; // Render satellite object

    public Satellite(SGP4SatData data) {
        this.data = data;
    }

    public void setLongitude(double longitude) { mLongitude = longitude; }
    public void setLatitude(double latitude) { mLatitude = latitude; }
    public void setAltitude(double altitude) { mAltitude = altitude; }
    public void setSpeed(double speed) { mSpeed = speed; }


}
