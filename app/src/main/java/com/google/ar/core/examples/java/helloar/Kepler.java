package com.google.ar.core.examples.java.helloar;

/**
 * Created by TY on 1/6/2018.
 * Kepler equation solver using algorithms described in
 * "A Practical Method for Solving the Kepler Equation", Marc A. Murison, 2006
 * Link: http://alpheratz.net/dynamics/twobody/KeplerIterations_summary.pdf
 */
public class Kepler {

    private static final double TOLERANCE = 1.0e-14;
    private static final int MAX_ITERS    = 100;
    /**
     * Solves Kepler equation for E, the eccentric anomaly.
     * Again, credit to Marc A. Murison in "A Practical Method for Solving the Kepler Equation"
     * @param M the mean anomaly [0, 2pi]
     * @param ecc eccentricity of the orbit
     * @return E the eccentric anomaly
     */
    public static double solve(double M, double ecc) {
        double E  = 0;
        double E0 = initialE(M, ecc);
        double dE = TOLERANCE + 1.0;
        int count = 0;
        while (dE > TOLERANCE && count < MAX_ITERS) {
            E = E0 - eps(M, ecc, E0);
            dE = Math.abs(E - E0);
            E0 = E;
            count++;
        }
        return E;
    }

    /**
     * Determines a good starting value for E, eccentric anomaly. A more accurate
     * initial E will provide faster convergence.
     * @param M mean anomaly (in radians)
     * @param ecc orbital eccentricity
     * @return the starting E value for the kepler solve() method
     */
    private static double initialE(double M, double ecc) {
        double t34 = ecc * ecc;
        double t35 = ecc * t34;
        double t33 = Math.cos(M);
        return M + (-0.5*t35 + ecc + (t34 + 1.5 * t33 * t35) * t33) * Math.sin(M);
    }

    /**
     * Calculate the error in the E approximation. (Third order method)
     * @param M mean anomaly (in radians)
     * @param ecc orbital eccentricity
     * @param x the approximate of E that is in error
     * @return The error, eps
     */
    private static double eps(double M, double ecc, double x) {
        double t1 = Math.cos(x);
        double t2 = -1 + ecc * t1;
        double t3 = Math.sin(x);
        double t4 = ecc * t3;
        double t5 = -x + t4 + M;
        double t6 = t5 / (0.5 * t5 * t4/t2 + t2);
        return t5 / ((0.5 * t3 - (1.0/6.0) * t1 * t6) * ecc * t6 + t2);

    }
}
