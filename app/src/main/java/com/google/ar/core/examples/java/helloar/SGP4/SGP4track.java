package com.google.ar.core.examples.java.helloar.SGP4;

/**
 * Created by TY on 1/1/2018.
 */

import android.graphics.Point;
import android.util.Log;

import com.google.ar.core.examples.java.helloar.Point3D;
import com.google.ar.core.examples.java.helloar.Satellite;
import com.google.ar.core.examples.java.helloar.rendering.SatelliteRenderer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class SGP4track {

    private static final String TAG = SatelliteRenderer.class.getSimpleName();

    private static final double julMinute = 1.0 / ( 24.0 * 60);

    public static double getJulianTime() {
        // prop to a given date  -- This is out of loop because unnecessary to calculate each time
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int year, month, day, hour, minute, second;
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH) + 1;
        day = c.get(Calendar.DAY_OF_MONTH);
        hour = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);
        second = c.get(Calendar.SECOND);
        return SGP4utils.jday(year, month, day, hour, minute, second); // JD to prop to
    }

    // Private Class Variables //
    private static final char opsMode = SGP4utils.OPSMODE_IMPROVED;
    private static final SGP4unit.Gravconsttype gravConstType = SGP4unit.Gravconsttype.wgs72;


    /**
     * Take in TLE information and extract a satellite
     *
     * @param tleData The raw TLE string data
     * @return a satellite data object OR null if failure to initialize
     */
    public static SGP4SatData initSatellite(TLEdata tleData) {
        // Read TLE information and initialize com.google.ar.core.examples.java.helloar.SGP4 model
        SGP4SatData data = new SGP4SatData();
        boolean result = SGP4utils.readTLEandIniSGP4(
                tleData.line0,          // Name of satellite
                tleData.line1,          // Orbital data line 1
                tleData.line2,          // Orbital data line 2
                opsMode, gravConstType, // Options
                data                    // Output
        );

        if (!result) {
            Log.e(TAG, "Error reading/initializing TLE. Code: " + data.error);
            return null;
        } else {
            return data;
        }
    }


    /**
     * Executes SGP4 algorithm to determine orbital values and coordinates for the
     * provided satellite.
     * @param sat satellite for which data will be set
     */
    public static void updateSatellite(Satellite sat) {

        // prop to current date
        double propJD = getJulianTime();
        double minutesSinceEpoch = (propJD - sat.mData.jdsatepoch) * 24.0 * 60.0;

        double[] pos = new double[3];
        double[] vel = new double[3];

        boolean result = SGP4unit.sgp4(sat.mData, minutesSinceEpoch, pos, vel);
        if (!result) {
            Log.e(TAG,"sgp4 - Error in Sat Prop");
            return;
        }

        // PM of 0,0 is more consistent with online trackers
        double[] ecefPos = CoordConvert.ecefPosVector(pos, 0, 0, propJD, 86400.87);
        double[] longLat = CoordConvert.ecefToLongLat(ecefPos, propJD);
        sat.setLatitude(longLat[1]);
        sat.setLongitude(longLat[2]);
        sat.setAltitude(longLat[3]);
        sat.setSpeed(Math.sqrt(vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]) * 1000);
    }

    /**
     * Samples a number of points from a given satellite's orbit. Different from getSatellitePath
     * in that the location is not determined at different points along the period. Instead,
     * this method samples some number of points given the satellite's current orbital information.
     * This provides a clear elliptical orbit for rendering.
     *
     * Thanks to https://space.stackexchange.com/a/8915/22335 for explanations of the math
     *
     * @param sat satellite for which orbital path will be extrapolated
     * @param points the number of latitude,longitude pairs that will be generated for the path
     * @return an ArrayList of LatLng points
     */
    public static List<Point3D> getSatelliteOrbit(Satellite sat, int points) {
        List<Point3D> positions = new ArrayList<>();
        final double epsilon   = 1e-6;
        final double TWO_PI    = 2.0 * Math.PI;
        final double increment = TWO_PI / points;

        final double a = sat.mData.a;     // Semi-major axis
        final double e = sat.mData.ecco;  // eccentricity [0, 1]
        final double i = sat.mData.inclo; // inclination
        final double w = sat.mData.argpo; // argument of perigee
        final double W = sat.mData.nodeo; // longitude of ascending node

        for(double M = 0; M < TWO_PI; M += increment ) {
            // Solving Kepler equation (M = E - esin(E)) for eccentric anomaly, E
            // using Newton-Raphson method
            double E = M;
            double deltaE = 1.0;
            while (Math.abs(deltaE) > epsilon) {
                deltaE = (E - e*Math.sin(E) - M) / (1.0 - e*Math.cos(E));
                E -= deltaE;
            }
            // Position in 2-space on the orbit's plane
            double P = a * (Math.cos(E) - e);
            double Q = Math.sin(E) * Math.sqrt(1 - e*e);

            // Rotate by argument of perigee
            double x = Math.cos(w)*P - Math.sin(w)*Q;
            double y = Math.sin(w)*P + Math.cos(w)*Q;

            // Rotate by inclination
            double z = Math.sin(i) * x;
            x = Math.cos(i) * x;

            // Rotate by longitude of ascending node
            double temp = x;
            x = Math.cos(W)*temp - Math.sin(W)*y;
            y = Math.sin(W)*temp + Math.cos(W)*y;
            positions.add(new Point3D(x, y, z));
        }
        return positions;
    }

}