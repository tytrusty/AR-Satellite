package com.google.ar.core.examples.java.helloar.SGP4;

/**
 * Created by TY on 1/1/2018.
 */

import android.content.Context;
import android.util.Log;

import com.google.ar.core.examples.java.helloar.Kepler;
import com.google.ar.core.examples.java.helloar.Point3D;
import com.google.ar.core.examples.java.helloar.Satellite;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class SGP4track {

    private static final String TAG = SGP4track.class.getSimpleName();

    private static final double TWO_PI    = 2.0 * Math.PI;

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
     * Reads data from tle file and initializes an ArrayList containing newly created
     * satellite objects.
     * @param context Application context necessary for getting the internal file directory
     * @param fileName TLE file name
     * @return An arraylist of satellite objects read from the TLE file
     */
    public static List<Satellite> readTLE(final Context context, final String fileName) {

        List<Satellite> satellites = new ArrayList<>();
        try {
            FileReader reader = new FileReader(context.getFilesDir() + "/" + fileName);
            BufferedReader bufferReader = new BufferedReader(reader);

            // Each TLE element has three lines
            String name, line1, line2;
            while(((name  = bufferReader.readLine()) != null) &&
                    ((line1 = bufferReader.readLine()) != null) &&
                    ((line2 = bufferReader.readLine()) != null)) {
                satellites.add(new Satellite(new TLEdata(name,line1, line2)));
            }
            bufferReader.close();

        } catch (Exception e) {
            Log.e(TAG, "Error reading TLE file: " + fileName + e.getMessage());
        }
        return satellites;
    }

    /**
     * Take in TLE information and extract a satellite
     *
     * @param tleData The raw TLE string data
     * @return a satellite data object OR null if failure to initialize
     */
    public static SGP4SatData initSatellite(TLEdata tleData) {
        // Read TLE information and initialize SGP4 model
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
        double latitude  = longLat[1];
        double longitude = longLat[2];
        sat.setLatitude(latitude);
        sat.setLongitude(longitude);
        sat.setAltitude(longLat[3]);
        sat.setSpeed(Math.sqrt(vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]) * 1000);

        double x = Math.cos(latitude) * Math.sin(longitude);
        double y = Math.sin(latitude);
        double z = Math.cos(latitude) * Math.cos(longitude);
        sat.setPosition(x, y, z);
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
        final double increment = TWO_PI / points;

        // prop to current date
//        double propJD = getJulianTime();
//        double minutesSinceEpoch = (propJD - sat.mData.jdsatepoch) * 24.0 * 60.0;
//        double[] pos = new double[3];
//        double[] vel = new double[3];
//        SGP4unit.sgp4(sat.mData, minutesSinceEpoch, pos, vel);
//        double[] kepler = SGP4utils.rv2coe(pos, vel);
//        final double a = kepler[1];
//        final double e = kepler[2];
//        final double i = kepler[3];
//        final double w = kepler[5];
//        final double W = kepler[4];

        final double a = sat.mData.a;     // Semi-major axis
        final double e = sat.mData.ecco;  // eccentricity [0, 1]
        final double i = sat.mData.inclo; // inclination
        final double w = sat.mData.argpo; // argument of perigee (little omega)
        final double W = sat.mData.nodeo; // longitude of ascending node (great omega)

        for(double M = 0; M <= TWO_PI; M += increment ) {
            // Solving Kepler equation (M = E - esin(E)) for eccentric anomaly, E
            final double E = Kepler.solve(M, e);

//            // true anomaly
            double v = Kepler.calcTrueAnomaly(e, E);

            final double radius = a*(1 - e*Math.cos(E));
            final double x =  1.5 * (Math.sin(W)*Math.cos(w + v) + Math.cos(W)*Math.sin(w + v)*Math.cos(i));
            final double y =  1.5 * (Math.sin(i)*Math.sin(w + v));
            final double z =  1.5 * (Math.cos(W)*Math.cos(w + v) - Math.sin(W)*Math.sin(w + v)*Math.cos(i));

            positions.add(new Point3D(x, y, z));
        }
        return positions;
    }

    /**
     * Calculates the path of a satellite for its entire period. Calculates the location of the
     * satellite at separate points throughout the period so that a vector may be created
     * outlining the satellite's path.
     * @param sat satellite to track path for
     * @param points the number of latitude,longitude pairs that will be generated for the path
     * @param correctLongitude boolean indicating whether longitude should be corrected such that
     *                         the resulting path is a (mostly) perfect ellipse
     * @return an ArrayList of LatLng points
     */
    // Similar to trackSat, but returns an arraylist of coordinates
    // Points parameter represents the amount of points will be calculated for the list.
    public static List<Point3D> getSatellitePath(Satellite sat, int points, boolean correctLongitude) {
        List<Point3D> positions = new ArrayList<>();
        final double MINUTES_PER_DAY = 1440.0;

        // Mean motion used to find orbital period. Converting mean motion to revolutions per day.
        double meanMotion = sat.mData.no * (MINUTES_PER_DAY / TWO_PI);

        double hoursPerOrbit  = 24 / meanMotion;                 // Hours per orbit as a decimal
        int hours             = (int) hoursPerOrbit;             // Hours ... Truncated decimal
        double minutesDecimal = (hoursPerOrbit - hours) * 60.0;  // Minutes expressed as decimal
        double orbitalPeriod  = minutesDecimal + (hours * 60);   // Orbital Period in minutes

        // Calculates longitude and latitude at points for an entire orbital period.
        double julTime = getJulianTime();
        for(int i = 0; i <= points; ++i) {
            double newJD = julTime + (julMinute * i * orbitalPeriod/points);

            double minutesSinceEpoch = (newJD - sat.mData.jdsatepoch) * 24.0 * 60.0;
            double[] pos = new double[3];
            double[] vel = new double[3];

            SGP4unit.sgp4(sat.mData, minutesSinceEpoch, pos, vel);
            double[] ecefPos = CoordConvert.ecefPosVector(pos, 0, 0, newJD, 86400.87);
            double[] longLat = CoordConvert.ecefToLongLat(ecefPos, newJD);

            double lat = longLat[1];
            double lon = longLat[2];
            if (correctLongitude) {
                lon += (newJD - julTime) * TWO_PI;
            }

            double x = 1.5 * (Math.cos(lat) * Math.sin(lon));
            double y = 1.5 * (Math.sin(lat));
            double z = 1.5 * (Math.cos(lat) * Math.cos(lon));
            positions.add(new Point3D(x, y, z));
        }
        return positions;
    }
}