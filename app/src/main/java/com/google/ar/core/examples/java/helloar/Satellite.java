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
    public SGP4SatData mData;  // SGP4 data
    private int mID;           // Norad ID
    private double mLatitude;  // latitude in radians
    private double mLongitude; // longitude in radians
    private double mAltitude;  // altitude in kilometers
    private double mSpeed;     // speed in m/s
    private SatelliteRenderer mRenderer;       // Render satellite object
    private Point3D mPosition = new Point3D(); // x,y,z position for rendering


    public Satellite(TLEdata tle) {
        mData = SGP4track.initSatellite(tle);
    }

    public void initRenderer(Context context) {
        mRenderer = new SatelliteRenderer();
        try {
            mRenderer.createOnGlThread(/*context=*/context,"iss.obj", 0xCC0000FF);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        mRenderer.setMaterialProperties(1.0f, 3.5f, 1.0f, 6.0f);
    }

    public void update(float[] modelMatrix, float scaleFactor, float translateFactor, float rotateAngle) {
        SGP4track.updateSatellite(this); // Get new coordinates
        mRenderer.updateModelMatrix(modelMatrix, scaleFactor, translateFactor, rotateAngle,
                mPosition, mAltitude);
    }

    public void draw(float[] cameraView, float[] cameraPerspective, float lightIntensity) {
        mRenderer.draw(cameraView, cameraPerspective, lightIntensity);
    }

    // Getters and setters
    public void setLongitude(double longitude) { mLongitude = longitude; }
    public void setLatitude(double latitude) { mLatitude = latitude; }
    public void setAltitude(double altitude) { mAltitude = altitude; }
    public void setSpeed(double speed) { mSpeed = speed; }
    public void setPosition(Point3D position) { mPosition = position; }
    public void setPosition(double x, double y, double z) {
        mPosition.x = x;
        mPosition.y = y;
        mPosition.z = z;
    }

    public double getLongitude() { return mLongitude; }
    public double getLatitude() { return mLatitude; }
    public double getAltitude() { return mAltitude; }
    public double getSpeed() { return mSpeed; }
    public Point3D getPosition() { return mPosition; }
}
