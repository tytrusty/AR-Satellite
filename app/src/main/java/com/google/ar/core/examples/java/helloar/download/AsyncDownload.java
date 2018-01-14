package com.google.ar.core.examples.java.helloar.download;

import android.content.Context;
import android.os.AsyncTask;
import android.security.NetworkSecurityPolicy;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.ar.core.examples.java.helloar.R;
import com.google.ar.core.examples.java.helloar.SGP4.SGP4track;
import com.google.ar.core.examples.java.helloar.SGP4.TLEdata;
import com.google.ar.core.examples.java.helloar.Satellite;
import com.google.ar.core.examples.java.helloar.SatelliteCluster;

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
 * Created by TY on 1/8/2018.
 *
 * Retrieves a TLE file from internal storage or over the network if TLE is file is old. After each
 * TLE is processed, the data is sent to onProgressUpdate() so that a new satellite may be added
 * to the satelliteList output array
 *
 * Edited version of AsyncUpdate from SatTracker application
 */

//TODO ensure that AsyncDownload is canceled so that context isn't leaked

public class AsyncDownload extends AsyncTask<String, Satellite, Boolean> {
    private static final String TAG = AsyncDownload.class.getSimpleName();

    private final Context context;
    private final RelativeLayout background;
    private final ProgressBar progressBar;
    private final SatelliteCluster cluster;
    private String mFileName = "tle.txt";

    private boolean isFinishedDownloading = false; // Used to indicate that download is done so user can continue to main activity

    public AsyncDownload(Context context, SatelliteCluster cluster) {
        this.context     = context;
        this.cluster     = cluster;
        this.background  = null;
        this.progressBar = null;
    }

    public AsyncDownload(Context context, SatelliteCluster cluster, RelativeLayout background,
                         ProgressBar progressBar) {
        this.context     = context;
        this.cluster     = cluster;
        this.background  = background;
        this.progressBar = progressBar;
    }

    @Override
    protected void onPreExecute() {
        isFinishedDownloading = false;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected Boolean doInBackground(String... tleFile) {
        mFileName = tleFile[0];
        final boolean isFileOld = DownloadTLE.checkTLEFile(context, mFileName);
        if (isFileOld) {
            Log.d(TAG, "Downloading TLE");
            try {
                String baseURL = context.getResources().getString(R.string.url_base);

                File file = new File(context.getFilesDir(), mFileName); // Create file for persistence
                FileWriter writer = new FileWriter(file);

                CookieManager manager = new CookieManager();
                manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                CookieHandler.setDefault(manager);

                // Space track url
                URL url = new URL(baseURL + mFileName);

                // Opening connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");

                //  Reading and accessing TLE file
                BufferedReader br = new BufferedReader(new InputStreamReader((url.openStream())));
                String name, line1, line2; // TLE data
                while(  ((name  = br.readLine()) != null) &&
                        ((line1 = br.readLine()) != null) &&
                        ((line2 = br.readLine()) != null)) {
                    TLEdata tle = new TLEdata(name, line1, line2);
                    writer.write(name + "\n");
                    writer.write(line1 + "\n");
                    writer.write(line2 + "\n");
                    publishProgress(new Satellite(tle)); // Unsuccessful connection
                    writer.flush();
                }
                writer.close();
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG,"Failed to connect to server");
                publishProgress(null); // Unsuccessful connection
            }
            return true;    // File downloaded
        } else {
            Log.d(TAG, "Not Downloading TLE");
            return false;   // File not downloaded as it already exists
        }
    }

    @Override
    protected void onProgressUpdate(Satellite... res) {
//        Satellite sat = res[0];

        // If downloaded succeeded, add the satellite to the adapter so that it may be rendered
        if(res != null && res.length > 0) {
            cluster.addSatellite(res[0]);
        }

        // If downloaded failed, display notification
        else {
            // Progress Update displaying notification that user failed to connect to server
//            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
//            alphaAnimation.setDuration(1000);
//            alphaAnimation.setFillAfter(true);
//
//            if (background != null) { // making sure background is displayed
//                // Layouts
//                final View noConnView = inflater.inflate(R.layout.no_connection_found, background, true);
//                final RelativeLayout container = (RelativeLayout) noConnView.findViewById(R.id.no_connection_layout);
//
//                // Animations
//                final Animation animIn = AnimationUtils.loadAnimation(context, R.anim.translate_alpha_in);
//                final Animation animOut = AnimationUtils.loadAnimation(context, R.anim.translate_alpha_out);
//
//                container.startAnimation(animIn); // initial anim in
//
//                // Button set up
//                Button button = (Button) noConnView.findViewById(R.id.connection_button);
//                button.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        container.startAnimation(animOut);
//                        background.removeView(container);
//                    }
//                });
//            }
        }
    }

    @Override
    protected void onPostExecute(Boolean downloaded) {
        isFinishedDownloading = true;
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // If the file was not downloaded, then satellites need to be read from the pre-existing file.
        if(!downloaded) {
            cluster.addSatellite(SGP4track.readTLE(context, mFileName));
        }

        Log.d(TAG, "AsyncDownload finished executing");

    }

    public boolean isFinishedDownloading() {
        return isFinishedDownloading;
    }
}