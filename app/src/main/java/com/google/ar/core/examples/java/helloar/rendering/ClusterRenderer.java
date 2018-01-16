package com.google.ar.core.examples.java.helloar.rendering;

import android.content.Context;
import android.nfc.Tag;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.examples.java.helloar.R;
import com.google.ar.core.examples.java.helloar.SatelliteCluster;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renders a cluster of dots. In my case, satellites of various types
 * Created by TY on 1/8/2018.
 */
public class ClusterRenderer {

    private static final String TAG = ClusterRenderer.class.getSimpleName();

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int COORDS_PER_POINT = 3;  // X,Y,Z
    private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * COORDS_PER_POINT;
    private static final int INITIAL_BUFFER_POINTS = 1000;

    private int mVbo;
    private int mVboSize;

    private int mProgramName;
    private int mPositionAttribute;
    private int mModelViewProjectionUniform;
    private int mColorUniform;
    private int mPointSizeUniform;

    private int mNumPoints = 0;

    private SatelliteCluster mPrevCluster;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];

    public ClusterRenderer() {
    }

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer.  Must be
     * called on the OpenGL thread, typically in
     * {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
     *
     * @param context Needed to access shader source.
     */
    public void createOnGlThread(Context context) {
        ShaderUtil.checkGLError(TAG, "before create");

        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        mVbo = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

        mVboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "buffer alloc");

        int vertexShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_VERTEX_SHADER, R.raw.point_cloud_vertex);
        int passthroughShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        mProgramName = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgramName, vertexShader);
        GLES20.glAttachShader(mProgramName, passthroughShader);
        GLES20.glLinkProgram(mProgramName);
        GLES20.glUseProgram(mProgramName);

        ShaderUtil.checkGLError(TAG, "program");

        mPositionAttribute = GLES20.glGetAttribLocation(mProgramName, "a_Position");
        mColorUniform = GLES20.glGetUniformLocation(mProgramName, "u_Color");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(
                mProgramName, "u_ModelViewProjection");
        mPointSizeUniform = GLES20.glGetUniformLocation(mProgramName, "u_PointSize");

        ShaderUtil.checkGLError(TAG, "program  params");
    }

    /**
     * Updates the OpenGL buffer contents to the provided point.  Repeated calls with the same
     * cluster will be ignored.
     */
    public void update(SatelliteCluster cluster) {
        if (mPrevCluster == cluster) {
            // Redundant call.
            return;
        }

        ShaderUtil.checkGLError(TAG, "before update");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        mPrevCluster = cluster;

        // If the VBO is not large enough to fit the new point cloud, resize it.
        mNumPoints = mPrevCluster.getPoints().remaining() / COORDS_PER_POINT;
        if (mNumPoints * BYTES_PER_POINT > mVboSize) {
            while (mNumPoints * BYTES_PER_POINT > mVboSize) {
                mVboSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * BYTES_PER_POINT,
                mPrevCluster.getPoints());
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "after update");
        Log.d(TAG, "CLUSTER SIZE: " + cluster.getPoints().limit());
    }

    /**
     * Updates the lines origin and end points
     *
     * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
     * @see android.opengl.Matrix
     */
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor, float translateFactor,
                                  float rotateAngle) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0]  = scaleFactor;
        scaleMatrix[5]  = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, rotateAngle, 0.0f, 1.0f, 0.0f);

        mModelMatrix[13] = translateFactor;
    }


    /**
     * Renders the satellite cluster
     *
     * @param cameraView        the camera view matrix for this frame, typically from {@link
     *                          com.google.ar.core.Camera#getViewMatrix(float[], int)}.
     * @param cameraPerspective the camera projection matrix for this frame, typically from {@link
     *                          com.google.ar.core.Camera#getProjectionMatrix(float[], int, float, float)}.
     */
    public void draw(float[] cameraView, float[] cameraPerspective) {
        // Build the ModelView and ModelViewProjection matrices
        // for calculating object position and light.
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);


        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(mProgramName);
        GLES20.glEnableVertexAttribArray(mPositionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        GLES20.glVertexAttribPointer(
                mPositionAttribute, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(mColorUniform, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f);
        GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);
        GLES20.glUniform1f(mPointSizeUniform, 5.0f);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mNumPoints);
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "Draw");
    }
}