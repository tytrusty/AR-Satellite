package SGP4;

/**
 * Created by Ty on 4/12/2016.
 * Translation of david vallado's teme2ecef matlab function
 */
public class CoordConvert {

    public static double[] ecefPosVector(double[] rteme, double xp, double yp, double jdut1, double lod)
    {
        double[] ecefPosVector;
        double[][] st = new double[3][3];
        double[][] pm = new double[3][3];

        double thetasa;
        double[] omegaearth = new double[3];
        double[] rpef = new double[3];

        double gmst;
        double cosxp = Math.cos(xp);
        double sinxp = Math.sin(xp);
        double cosyp = Math.cos(yp);
        double sinyp = Math.sin(yp);
        // ---- perform transformations
        thetasa = 7.29211514670698e-05 * (1.0 - lod / 86400.0);
        omegaearth[0] = 0.0;
        omegaearth[1] = 0.0;
        omegaearth[2] = thetasa;

        // ---- find matrices
        gmst = SGP4unit.gstime(jdut1);
        st[0][0] = Math.cos(gmst);
        st[0][1] = -Math.sin(gmst);
        st[0][2] = 0.0;
        st[1][0] = Math.sin(gmst);
        st[1][1] = Math.cos(gmst);
        st[1][2] = 0.0;
        st[2][0] = 0.0;
        st[2][1] = 0.0;
        st[2][2] = 1.0;

        //polar motion transformation matrix
        pm[0][0]=  cosxp;
        pm[0][1] =  0.0;
        pm[0][2] =  0.0;
        pm[1][0] =  0.0;
        pm[1][1] =  cosyp;
        pm[1][2] =  0.0;
        pm[2][0] =  0.0;
        pm[2][1]  = 0.0;
        pm[2][2] =  cosxp * cosyp;

        transpose(pm);
        transpose(st);
        rpef = matVecMult(st, rteme);
        ecefPosVector = matVecMult(pm, rpef);

        return ecefPosVector;
    }

    public static void transpose(double[][] mat) {
        for (int i = 0; i < 3; i++) {
            for (int j = i+1; j < 3; j++) {
                double temp = mat[i][j];
                mat[i][j] = mat[j][i];
                mat[j][i] = temp;
            }
        }
    }

    public static double[] matVecMult(double[][] mat, double[] vec) {
        int matLen = mat.length;
        int vecLen = mat[0].length;
        double[] newVec = new double[matLen];
        for (int i = 0; i < matLen; i++)
            for (int j = 0; j < vecLen; j++)
                newVec[i] += mat[i][j] * vec[j];
        return newVec;
    }

    //Translation of david vallado's ijk2ll matlab function
    public static double[] ecefToLongLat(double[] rECEF , double jdut1){
        //Constants
        double twoPI  = 2 * Math.PI;
        double small  = 0.00000001; //tolerance value
        double re     = 6378.135;   //Diameter of launcher_earth in km
        double eesqrd = 0.006694385000; //Earth's eccentricity squared
        //latgc = geocentric lat; latgd = geodetic latitude; lon = longitude; hellp = height above ellipsoid
        double[] retCoords = new double[4];
        double latgc, latgd, lon, hellp, temp, rtasc, gst;
        double rMag = SGP4utils.mag(rECEF); //Magnitude of pos vector

        //----------------------Longitude-------------------------------//
        temp = Math.sqrt( rECEF[0]*rECEF[0] + rECEF[1]*rECEF[1]);
        if ( Math.abs(temp) < small )
            rtasc = Math.signum(rECEF[2]) * Math.PI * 0.5;
        else
            rtasc = Math.atan2(rECEF[1], rECEF[0]);

        gst = SGP4unit.gstime(jdut1);
        lon = rtasc;// - gst; was causing errors
        if(Math.abs(lon) >= Math.PI ) {
            if( lon < 0.0 )
                lon = twoPI + lon;
            else
                lon = lon - twoPI;
        }
        latgd = Math.asin( rECEF[2] / rMag);

        //--------------------Geodetic Latitude-------------------------//
        int i = 1;
        double oldDelta = latgd + 10.0;
        double sinTemp;
        double s = 0.0,c = 0.0;
        while(( Math.abs(oldDelta - latgd ) >= small) && (i < 10) ) {
            oldDelta = latgd;
            sinTemp = Math.sin( latgd );
            c = re / ( Math.sqrt( 1.0 - eesqrd*sinTemp*sinTemp) );
            latgd = Math.atan( (rECEF[2] + c*eesqrd*sinTemp)/temp );
            i += 1;
        }

        //Height calculation
        if( (Math.PI*0.5 - Math.abs(latgd)) > (Math.PI/180.0) )
            hellp = (temp/Math.cos(latgd))- c;
        else {
            s = c * (1.0 - eesqrd);
            hellp = (rECEF[2] / Math.sin(latgd)) - s;
        }
        latgc = Math.atan( (1.0 - eesqrd) * Math.tan(latgd));
        retCoords[0] = latgc;
        retCoords[1] = latgd;
        retCoords[2] = lon;
        retCoords[3] = hellp;
        return retCoords;
    }
}