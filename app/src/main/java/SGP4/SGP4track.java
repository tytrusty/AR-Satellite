package SGP4;

/**
 * Created by TY on 1/1/2018.
 */

import android.util.Log;

import com.google.ar.core.examples.java.helloar.Satellite;
import com.google.ar.core.examples.java.helloar.rendering.SatelliteRenderer;

import java.util.Calendar;
import java.util.TimeZone;

public class SGP4track {

    private static final String TAG = SatelliteRenderer.class.getSimpleName();

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
     * @return a satellite with the necessary SGP4 data OR null if failure to initialize
     */
    public static Satellite initSatellite(TLEdata tleData) {
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
            return new Satellite(data);
        }
    }


    /**
     * Executes SGP4 propogation algorithm to determine orbital values and coordinates for the
     * provided satellite.
     * @param sat satellite for which data will be set
     */
    public static void trackSat(Satellite sat) {

        // prop to current date
        double propJD = getJulianTime();
        double minutesSinceEpoch = (propJD - sat.data.jdsatepoch) * 24.0 * 60.0;

        double[] pos = new double[3];
        double[] vel = new double[3];

        boolean result = SGP4unit.sgp4(sat.data, minutesSinceEpoch, pos, vel);
        if (!result) {
            Log.e(TAG,"sgp4 - Error in Sat Prop");
            return;
        }

        // PM of 0,0 is more consistent with online trackers
        double[] ecefPos = CoordConvert.ecefPosVector(pos, 0, 0, propJD, 86400.87);
        double[] longLat = CoordConvert.ecefToLongLat(ecefPos, propJD);
        double latitude = longLat[1];
        double longitude = longLat[2];
        sat.setLatitude(latitude);
        sat.setLongitude(longitude);
        sat.setAltitude(longLat[3]);
        sat.setSpeed(Math.sqrt(vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]) * 1000);
    }


    //TODO -- do it nerd
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
//    public ArrayList<LatLng> getSatellitePath(Satellite sat, int points) {
//
//        ArrayList<LatLng> latLngArrayList = new ArrayList<>();
//        LatLng latLng;
//
//        // tle data
//        String name  = sat.getName();
//        String line1 = sat.getLine1();
//        String line2 = sat.getLine2();
//
//        // Getting sat data object and reading TLE
//        SGP4SatData data = sat.getData();
//        if (data == null) {
//            data = new SGP4SatData();
//
//            // read in data and ini SGP4 data
//            boolean result = SGP4utils.readTLEandIniSGP4(name, line1, line2, opsmode, gravconsttype, data);
//            if(!result) Log.i("SGP4track:", "readTLEandIniSGP4 - Error Reading / Ini Data, error code: " + data.error);
//
//            sat.setData(data);
//        }
//
//        // Sets mean motion if not already set.
//        // Mean motion used to find orbital period, which is used in the path creation method.
//        double meanMotion = Double.parseDouble(line2.substring(52, 63));
//        sat.setMeanMotion(meanMotion);
//
//        double hoursPerOrbit  = 24 / meanMotion;                 // Hours per orbit as a decimal
//        int hours             = (int) hoursPerOrbit;             // Hours ... Truncated decimal
//        double minutesDecimal = (hoursPerOrbit - hours) * 60.0;  // Minutes expressed as decimal
//        double orbitalPeriod  = minutesDecimal + (hours * 60);   // Orbital Period in minutes
//
//        int bound = (int) (orbitalPeriod / 2); // Array bounds. Split period in half because array goes from - to +
//        double increment = orbitalPeriod / points;
//
//        // Calculates longitude and latitude at points for an entire orbital period.
//        double newJD;
//        for(double i = -bound; i < bound + 1; i += increment ) {
//
//            newJD = propJD + (julMinute * i);
//
//            double minutesSinceEpoch = (newJD - data.jdsatepoch) * 24.0 * 60.0;
//            double[] pos = new double[3];
//            double[] vel = new double[3];
//
//            boolean result = SGP4unit.sgp4(data, minutesSinceEpoch, pos, vel);
//            if (!result) {
//                System.out.println("Error in Sat Prop");
//                //return;
//            }
//            double[] ecefPos = CoordConvert.ecefPosVector(pos, 0, 0, newJD, 86400.87); // PM of 0,0 is more consistent with online trackers
//            double[] longLat = CoordConvert.ecefToLongLat(ecefPos, newJD);
//            double latitude = longLat[1] * 180 / Math.PI;
//            double longitude = longLat[2] * 180 / Math.PI;
//            latLng = new LatLng(latitude,longitude);
//            latLngArrayList.add(latLng);
//        }
//        return latLngArrayList;
//    }

}