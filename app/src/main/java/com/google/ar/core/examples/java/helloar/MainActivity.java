/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.math.MathUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.Trackable.TrackingState;
import com.google.ar.core.examples.java.helloar.SGP4.SGP4track;
import com.google.ar.core.examples.java.helloar.SGP4.TLEdata;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.DottedLineRenderer;
import com.google.ar.core.examples.java.helloar.rendering.EarthRenderer;
import com.google.ar.core.examples.java.helloar.rendering.EarthShadowRenderer;
import com.google.ar.core.examples.java.helloar.rendering.OrbitRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.view.MotionEvent.INVALID_POINTER_ID;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;

    private Session mSession;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;
    private Snackbar mMessageSnackbar;
    private DisplayRotationHelper mDisplayRotationHelper;

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final EarthRenderer mEarthObject             = new EarthRenderer();
    private final EarthShadowRenderer mShadowRenderer    = new EarthShadowRenderer();
    private final DottedLineRenderer mLineRenderer       = new DottedLineRenderer();
    private final PlaneRenderer mPlaneRenderer           = new PlaneRenderer();
    private final PointCloudRenderer mPointCloud         = new PointCloudRenderer();

    Satellite mSat;
    OrbitRenderer mOrbitRenderer1;

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    // Tap handling and UI.
    private final ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);

    private Anchor mEarthAnchor = null;

    private final float TRANSLATE_MIN   = -1.0f;
    private final float TRANSLATE_MAX   = 1.0f;
    private final float TRANSLATE_SPEED = 0.002f;
    private final float ROTATE_SPEED    = 0.5f;

    private final float SCALE_MAX = 5.0f;
    private final float SCALE_MIN = 0.1f;

    private float mScaleFactor     = 0.15f;
    private float mTranslateFactor = -0.5f;
    private float mRotateAngle     = 0.0f; // in degrees

    // The ID of the current pointer that is dragging
    private int mActivePointerID = INVALID_POINTER_ID;
    private float mPrevX;
    private float mPrevY;

    private boolean isPositioning = true;
    private Button mConfirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        mConfirmButton = findViewById(R.id.confirm_button);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YoYo.with(Techniques.FadeOutDown)
                        .duration(500)
                        .playOn(mConfirmButton);
                isPositioning = false;
            }
        });


        // Set up tap listener.
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

                mScaleFactor *= scaleGestureDetector.getScaleFactor();

                // Don't let the object get too small or too large.
                mScaleFactor = Math.max(SCALE_MIN, Math.min(mScaleFactor, SCALE_MAX));
                Log.i(TAG, "mScaleFactor: " + mScaleFactor);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {}
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                mGestureDetector.onTouchEvent(event);

                if (mScaleDetector.isInProgress()) {
                    mActivePointerID = INVALID_POINTER_ID;
                    return true;
                }

                /* Source: https://developer.android.com/training/gestures/scale.html */
                final int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        final int pointerIndex = event.getActionIndex();

                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        mPrevX = x;
                        mPrevY = y;
                        mActivePointerID = event.getPointerId(pointerIndex);
                        break;
                    } case MotionEvent.ACTION_MOVE : {
                        if (mActivePointerID == INVALID_POINTER_ID) break;
                        final int pointerIndex = event.findPointerIndex(mActivePointerID);
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        // Calculate change
                        final float dx = x - mPrevX;
                        final float dy = y - mPrevY;

                        mPrevX = x;
                        mPrevY = y;

                        mTranslateFactor += -dy * TRANSLATE_SPEED; // flip Y
                        mTranslateFactor  = MathUtils.clamp(mTranslateFactor, TRANSLATE_MIN, TRANSLATE_MAX);

                        mRotateAngle += dx * ROTATE_SPEED;

                        Log.i(TAG, "TranslateFactor: " + mTranslateFactor);
                        break;
                    } case MotionEvent.ACTION_UP: {
                        mActivePointerID = INVALID_POINTER_ID;
                        break;
                    } case MotionEvent.ACTION_CANCEL: {
                        mActivePointerID = INVALID_POINTER_ID;
                        break;
                    } case MotionEvent.ACTION_POINTER_UP: {
                        if (mActivePointerID == INVALID_POINTER_ID) break;
                        final int pointerIndex = event.getActionIndex();
                        final int pointerID = event.getPointerId(pointerIndex);

                        // If the active pointer is being released
                        if (pointerID == mActivePointerID) {
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            mPrevX = event.getX(newPointerIndex);
                            mPrevY = event.getY(newPointerIndex);
                            mActivePointerID = event.getPointerId(newPointerIndex);
                        }
                        break;
                    }
                }
                return true;
            }
        });

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Exception exception = null;
        String message = null;
        try {
            mSession = new Session(/* context= */ this);
        } catch (UnavailableArcoreNotInstalledException e) {
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            exception = e;
        } catch (Exception e) {
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            showSnackbarMessage(message, true);
            Log.e(TAG, "Exception creating session", exception);
            return;
        }

        // Create default config and check if supported.
        Config config = new Config(mSession);
        if (!mSession.isSupported(config)) {
            showSnackbarMessage("This device does not support AR", true);
        }
        mSession.configure(config);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            if (mSession != null) {
                showLoadingMessage();
                // Note that order matters - see the note in onPause(), the reverse applies here.
                mSession.resume();
            }
            mSurfaceView.onResume();
            mDisplayRotationHelper.onResume();
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mDisplayRotationHelper.onPause();
        mSurfaceView.onPause();
        if (mSession != null) {
            mSession.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/ this);
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
        }

        TLEdata tle = new TLEdata(
                "0 ISS (ZARYA)",
                "1 25544U 98067A   18008.54103705  .00016717  00000-0  10270-3 0  9032",
                "2 25544  51.6417  94.5927 0003240 348.1782  11.9295 15.54288204 13688"
        );
        mSat = new Satellite(this, tle);

        // Prepare the other rendering objects.
        try {
            mEarthObject.createOnGlThread(/*context=*/this,"Albedo.jpg");
            mEarthObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mShadowRenderer.createOnGlThread(this);

            mOrbitRenderer1 = new OrbitRenderer(SGP4track.getSatellitePath(mSat, 80, true));
            mOrbitRenderer1.createOnGlThread(this);

            mLineRenderer.createOnGlThread(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            mPlaneRenderer.createOnGlThread(/*context=*/this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        mPointCloud.createOnGlThread(/*context=*/this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();
                    if (trackable instanceof Plane
                            && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                        // Set anchor where the earth object will appear
                        if (mEarthAnchor != null) {
                            mEarthAnchor.detach();
                        }

                        mEarthAnchor = hit.createAnchor();

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break;
                    }
                }
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mMessageSnackbar != null) {
                for (Plane plane : mSession.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                        break;
                    }
                }
            }

            // Visualize planes and point cloud if not in positioning mode
            if (isPositioning) {
                // Visualize tracked points.
                PointCloud pointCloud = frame.acquirePointCloud();
                mPointCloud.update(pointCloud);
                mPointCloud.draw(viewmtx, projmtx);

                // Application is responsible for releasing the point cloud resources after
                // using it.
                pointCloud.release();

                // Visualize Planes
                mPlaneRenderer.drawPlanes(
                        mSession.getAllTrackables(Plane.class),
                        camera.getDisplayOrientedPose(), projmtx);
            }


            // Visualize anchors created by touch.
//            for (Anchor anchor : mAnchors) {
                if (mEarthAnchor == null || mEarthAnchor.getTrackingState() != TrackingState.TRACKING) {
                    // TODO re-enter positioning
                    // Notify user that tracking is Lost and attempting to find anchor again
                    // Also add button that user can press to restart to positioning phase
                    return;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                mEarthAnchor.getPose().toMatrix(mAnchorMatrix, 0);
                float[] origin = new float[4];
                mEarthAnchor.getPose().getTranslation(origin, 0);

                // Update and draw the model and its shadow.
                mEarthObject.updateModelMatrix(mAnchorMatrix, mScaleFactor, mTranslateFactor, mRotateAngle);
                mEarthObject.draw(viewmtx, projmtx, lightIntensity, isPositioning);

                mShadowRenderer.updateModelMatrix(mAnchorMatrix, mScaleFactor);
                mShadowRenderer.draw(viewmtx, projmtx, mTranslateFactor / 2 + 1);

                mSat.update(mAnchorMatrix, mScaleFactor, mTranslateFactor, mRotateAngle);
                mSat.draw(viewmtx, projmtx, lightIntensity);
                mOrbitRenderer1.updateModelMatrix(mAnchorMatrix, mScaleFactor, mTranslateFactor, mRotateAngle);
                mOrbitRenderer1.draw(viewmtx, projmtx);

                // Only render y-axis if in positioning stage
                if (isPositioning) {
                    mLineRenderer.updateModelMatrix(mAnchorMatrix);
                    mLineRenderer.draw(viewmtx, projmtx);
                }

//            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        mMessageSnackbar = Snackbar.make(
            MainActivity.this.findViewById(android.R.id.content),
            message, Snackbar.LENGTH_INDEFINITE);
        mMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        if (finishOnDismiss) {
            mMessageSnackbar.setAction(
                "Dismiss",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMessageSnackbar.dismiss();
                    }
                });
            mMessageSnackbar.addCallback(
                new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        finish();
                    }
                });
        }
        mMessageSnackbar.show();
    }

    private void showLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSnackbarMessage("Searching for surfaces...", false);
            }
        });
    }

    private void hideLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMessageSnackbar != null) {
                    mMessageSnackbar.dismiss();
                }
                mMessageSnackbar = null;
            }
        });
    }
}
