package com.google.ar.core.examples.java.helloar.SGP4;

/**
 * Created by TY on 1/1/2018.
 */

import android.util.Log;

import com.google.ar.core.examples.java.helloar.Position;
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
     * Calculates the path of a satellite for its entire period. Calculates the location of the
     * satellite at separate points throughout the period so that a vector may be created
     * outlining the satellite's path.
     * @param sat satellite to track path for
     * @param points the number of latitude,longitude pairs that will be generated for the path
     * @return an ArrayList of LatLng points
     */
    // Similar to trackSat, but returns an arraylist of coordinates
    // Points parameter represents the amount of points will be calculated for the list.
    public static List<Position> getSatellitePath(Satellite sat, int points) {

        List<Position> positions = new ArrayList<>();

        // Sets mean motion if not already set.
        // Mean motion used to find orbital period, which is used in the path creation method.
        double meanMotion = sat.mData.no;
        meanMotion = Double.parseDouble("2 25544  51.6408 118.2208 0002870 334.1045 127.0693 15.54253729 92951".substring(52, 63));


        double hoursPerOrbit  = 24 / meanMotion;                 // Hours per orbit as a decimal
        int hours             = (int) hoursPerOrbit;             // Hours ... Truncated decimal
        double minutesDecimal = (hoursPerOrbit - hours) * 60.0;  // Minutes expressed as decimal
        double orbitalPeriod  = minutesDecimal + (hours * 60);   // Orbital Period in minutes
        System.out.println("orbitalPeriod: " + orbitalPeriod);

        int bound = (int) (orbitalPeriod / 2); // Array bounds. Split period in half because array goes from - to +
        double increment = orbitalPeriod / points;

        // Calculates longitude and latitude at points for an entire orbital period.
        double julTime = getJulianTime();
        for(double i = -bound; i < bound + 1; i += increment ) {

            double newJD = julTime + (julMinute * i);

            double minutesSinceEpoch = (newJD - sat.mData.jdsatepoch) * 24.0 * 60.0;
            double[] pos = new double[3];
            double[] vel = new double[3];

            boolean result = SGP4unit.sgp4(sat.mData, minutesSinceEpoch, pos, vel);
            double[] ecefPos = CoordConvert.ecefPosVector(pos, 0, 0, newJD, 86400.87); // PM of 0,0 is more consistent with online trackers
            double[] longLat = CoordConvert.ecefToLongLat(ecefPos, newJD);
            positions.add(new Position(longLat[1], longLat[2], longLat[3]));
            //System.out.println("julTime: " + julTime + "   newJD: " + newJD);
           // System.out.println("minutesSinceEpoch: " + minutesSinceEpoch);
            //System.out.println("long: " + (longLat[2] * 180.0 / Math.PI));
        }
        return positions;
    }

}