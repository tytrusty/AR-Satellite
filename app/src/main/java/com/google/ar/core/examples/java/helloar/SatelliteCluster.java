package com.google.ar.core.examples.java.helloar;

import com.google.ar.core.examples.java.helloar.SGP4.SGP4track;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by TY on 1/8/2018.
 */

public class SatelliteCluster {

    public enum DensityEnum {
        ALL,
        HIGH,
        MEDIUM,
        LOW,
        NONE
    }
    private static final int INITIAL_SIZE = 100;
    private boolean dirty = true;

    private DensityEnum mDensity;
    private FloatBuffer pointBuffer;

    public SatelliteCluster() {
        mDensity = DensityEnum.MEDIUM;
    }

    private ArrayList<Satellite> satellites = new ArrayList<>();

    public void addSatellite(final Satellite sat) {
        satellites.add(sat);
        dirty = true;
    }

    public void addSatellite(final Collection<Satellite> sats) {
        satellites.addAll(sats);
        dirty = true;
    }

    public void setDensity(DensityEnum density) {
        mDensity = density;
        dirty = true;
    }

    public FloatBuffer getPoints() {
        if (dirty) {
            Collections.shuffle(satellites);
            int arrayPos = 0;
            int bound = calcNumPoints(mDensity, satellites.size());
            float[] positions = new float[bound * 3];

            for (int i = 0; i < bound; ++i) {
                Point3D pos = satellites.get(i).getPosition();
                positions[arrayPos++] = (float) pos.x;
                positions[arrayPos++] = (float) pos.y;
                positions[arrayPos++] = (float) pos.z;
            }
            pointBuffer = FloatBuffer.allocate(positions.length);
            pointBuffer.position(0); pointBuffer.put(positions); pointBuffer.rewind();
            dirty = false;
        }
        return pointBuffer;
    }

    /*
     * Small helper to return the number of points to sample given the density
     */
    private static int calcNumPoints(DensityEnum density, int size) {
        final int numPoints;
        switch (density) {
            case ALL:
                numPoints = size;
                break;
            case HIGH:
                numPoints = (int) (size * 0.75);
                break;
            case MEDIUM:
                numPoints = (int) (size * 0.50);
                break;
            case LOW:
                numPoints = (int) (size * 0.25);
                break;
            case NONE:
                numPoints = 0;
                break;
            default:
                numPoints = 0;
        }
        return numPoints;
    }

}
