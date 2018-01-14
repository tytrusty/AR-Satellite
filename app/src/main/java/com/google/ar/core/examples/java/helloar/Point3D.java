package com.google.ar.core.examples.java.helloar;

/**
 * Created by TY on 1/5/2018.
 */
public class Point3D {
    public double x;
    public double y;
    public double z;

    public Point3D () {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    public Point3D (double x, double y, double z) {
        this.x  = x;
        this.y = y;
        this.z  = z;
    }
}