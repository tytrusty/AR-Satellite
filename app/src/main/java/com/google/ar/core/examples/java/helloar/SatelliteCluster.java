package com.google.ar.core.examples.java.helloar;

import com.google.ar.core.examples.java.helloar.SGP4.SGP4track;
import com.google.ar.core.exceptions.DeadlineExceededException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by TY on 1/8/2018.
 */

public class SatelliteCluster {
    private static final int INITIAL_SIZE = 100;
    private boolean dirty = true;

    private int size;
    private float[] array;
    private FloatBuffer pointBuffer;

    public SatelliteCluster() {
        this.size  = 0;
        this.array = new float[INITIAL_SIZE];
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

    public FloatBuffer getPoints() {
        if (dirty) {
            float[] positions = new float[satellites.size() * 3];
            int i = 0;
            for (Satellite sat : satellites) {
                SGP4track.updateSatellite(sat);
                Point3D pos = sat.getPosition();
                positions[i++] = (float) pos.x;
                positions[i++] = (float) pos.y;
                positions[i++] = (float) pos.z;
            }
            pointBuffer = FloatBuffer.allocate(positions.length);
            pointBuffer.position(0); pointBuffer.put(positions); pointBuffer.rewind();
            dirty = false;
        }
        return pointBuffer;
    }
}
