package com.google.ar.core.examples.java.helloar;

/**
 * Created by TY on 1/5/2018.
 */
public class Position {
    public double latitude;  // in radians
    public double longitude; // in radians
    public double altitude;  // in km
    public Position(double latitude, double longitude, double altitude) {
        this.latitude  = latitude;
        this.longitude = longitude;
        this.altitude  = altitude;
    }
}