package com.google.ar.core.examples.java.helloar.download;


import android.content.Context;
import android.util.Log;

import com.google.ar.core.examples.java.helloar.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Functions for TLE file management
 * Created by TY on 1/8/2018.
 */
public class DownloadTLE {
    private static final String TAG = DownloadTLE.class.getSimpleName();
    private static final long MAX_FILE_AGE = 43200000; // 12 hrs in milliseconds

    /**
     * Downloads TLE file from server.
     *
     * @param context Android context for getting file directory
     * @param fileName The tle filename
     * @return boolean indicating download success (True) or failure (False)
     */
    public static boolean download(final Context context, final String fileName) {
        try {
            final String baseURL = context.getResources().getString(R.string.url_base); // Gets server url for connection
            final File file = new File(context.getFilesDir(), fileName);
            FileWriter writer = new FileWriter(file);
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);

            // Space track url
            URL url = new URL(baseURL + fileName);

            // Opening connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            //  Reading and accessing TLE file
            String output;
            BufferedReader br = new BufferedReader(new InputStreamReader((url.openStream())));
            Log.d(TAG, "Connection established. Receiving output");
            while ((output = br.readLine()) != null) {
                writer.write(output + "\n");
                writer.flush();
            }
            writer.close();
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Failed to connect to server");
            return false;
        }
        return true;
    }

    /**
     * Checks if a TLE file is outdated. This occurs when the current system's time exceeds
     * the modification time plus MAX_FILE_AGE
     *
     * @param context Android application context used for getting internal file directory
     * @param fileName TLE filename
     * @return boolean indicating whether the file is outdated. True if outdated, false otherwise.
     */
    public static boolean checkTLEFile(final Context context, final String fileName) {
        final File file = new File(context.getFilesDir(), fileName);
        long fileAge;
        long systemTime;
        if(file.exists()){
            if(file.length() == 0 ) {
                Log.d(TAG,"*** File Empty! Download Required ***");
                System.out.println();
                return true;
            }
            systemTime = System.currentTimeMillis();
            fileAge = file.lastModified() + MAX_FILE_AGE;

            // If current time is > modification time plus MAX_FILE_AGE, then file is too old
            return fileAge < systemTime;

        } else {
            Log.d(TAG,"*** File Does Not Exist. Download Required ***");
            return true;
        }
    }

}