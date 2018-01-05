package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.examples.java.helloar.SGP4.SGP4track;
import com.google.ar.core.examples.java.helloar.SGP4.TLEdata;
import com.google.ar.core.examples.java.helloar.rendering.SatelliteRenderer;

import com.google.ar.core.examples.java.helloar.SGP4.SGP4SatData;

import java.io.IOException;

/**
 * Created by TY on 1/1/2018. Damn this is how I spend my new year?
 */

public class Satellite {
    private static final String TAG = Satellite.class.getSimpleName();

    private int mID;           // Norad ID
    private double mLatitude;  // latitude in radians
    private double mLongitude; // longitude in radians
    private double mAltitude;  // altitude in kilometers
    private double mSpeed;     // speed in m/s
    public SGP4SatData mData;  // SGP4 data
    private SatelliteRenderer mRenderer; // Render satellite object

    Satellite(Context context, TLEdata tle) {
        mData = SGP4track.initSatellite(tle);
        initRenderer(context);
    }

    private void initRenderer(Context context) {
        mRenderer = new SatelliteRenderer();
        try {
            mRenderer.createOnGlThread(/*context=*/context,"iss.obj", 0xCC0000FF);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        mRenderer.setMaterialProperties(1.0f, 3.5f, 1.0f, 6.0f);
    }

    public void update(float[] modelMatrix, float scaleFactor, float translateFactor) {
        SGP4track.updateSatellite(this); // Get new coordinates
        mRenderer.updateModelMatrix(modelMatrix, scaleFactor, translateFactor, mAltitude, mLatitude, mLongitude);
    }

    public void draw(float[] cameraView, float[] cameraPerspective, float lightIntensity) {
        mRenderer.draw(cameraView, cameraPerspective, lightIntensity);
    }

    // Getters and setters
    public void setLongitude(double longitude) { mLongitude = longitude; }
    public void setLatitude(double latitude) { mLatitude = latitude; }
    public void setAltitude(double altitude) { mAltitude = altitude; }
    public void setSpeed(double speed) { mSpeed = speed; }
    public double getLongitude() { return mLongitude; }
    public double getLatitude() { return mLatitude; }
    public double getAltitude() { return mAltitude; }
    public double getSpeed() { return mSpeed; }

}
