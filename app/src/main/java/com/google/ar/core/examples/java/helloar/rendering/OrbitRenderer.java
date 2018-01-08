package com.google.ar.core.examples.java.helloar.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.examples.java.helloar.Point3D;
import com.google.ar.core.examples.java.helloar.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * Created by TY on 1/4/2018.
 */
public class OrbitRenderer {

    private static final String TAG = OrbitRenderer.class.getSimpleName();

    private static final int COORDS_PER_VERTEX = 3;
    private static final float[] DEFAULT_COLOR = {1.0f, 0.0f, 0.0f, 1.0f};
    // Object vertex buffer variables.
    private int mVertexBufferId;
    private int mProgram;

    // Shader location: model view projection matrix.
    private int mModelViewProjectionUniform;

    // Shader location: object attributes.
    private int mPositionAttribute;

    // Shader location: color
    private int mColorUniform;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];
    private float[] mColor = {1.0f, 0.0f, 0.0f, 1.0f};
    private float mLineVertices[];

    public OrbitRenderer(List<Point3D> points) {
        initVertices(points);
        mColor = DEFAULT_COLOR;
    }

    public OrbitRenderer(List<Point3D> points, float[] color) {
        initVertices(points);
        mColor = color;
    }

    private void initVertices(List<Point3D> positions) {
        mLineVertices = new float[positions.size() * 3];
        int i = 0;
        for (Point3D pos : positions) {
            //float x = (float) (Math.cos(pos.latitude) * Math.sin(pos.longitude));
            //float y = (float) (Math.sin(pos.latitude));
            //float z = (float) (Math.cos(pos.latitude) * Math.cos(pos.longitude)) ;
            mLineVertices[i++] = (float) pos.x;
            mLineVertices[i++] = (float) pos.y;
            mLineVertices[i++] = (float) pos.z;
        }
    }

    /**
     * Creates and initializes OpenGL resources needed for rendering the model.
     *
     * @param context Context for loading the shader and below-named model and texture assets.
     */
    public void createOnGlThread(Context context) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(mLineVertices.length * 4);
        buf.order(ByteOrder.nativeOrder());

        FloatBuffer vertices = buf.asFloatBuffer();
        vertices.put(mLineVertices);
        vertices.position(0);

        int[] buffer = new int[1];
        GLES20.glGenBuffers(1, buffer, 0);
        mVertexBufferId = buffer[0];

        // Load vertex buffer
        final int totalBytes = 4 * vertices.limit();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, vertices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "OBJ buffer load");

        final int vertexShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_VERTEX_SHADER, R.raw.passthrough_vertex);
        final int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_color_fragment);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
        GLES20.glUseProgram(mProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        mModelViewProjectionUniform =
                GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");

        mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position");

        mColorUniform = GLES20.glGetUniformLocation(mProgram, "u_Color");

        ShaderUtil.checkGLError(TAG, "Program parameters");

        Matrix.setIdentityM(mModelMatrix, 0);
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
     * Draws the line.
     *
     * @param cameraView  A 4x4 view matrix, in column-major order.
     * @param cameraPerspective  A 4x4 projection matrix, in column-major order.
     * @see android.opengl.Matrix
     */
    public void draw(float[] cameraView, float[] cameraPerspective) {
        ShaderUtil.checkGLError(TAG, "Before draw");

        // Build the ModelView and ModelViewProjection matrices
        // for calculating object position and light.
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);

        GLES20.glUseProgram(mProgram);

        // Set the vertex attributes.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);

        GLES20.glVertexAttribPointer(
                mPositionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, 0);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);

        // Set color
        GLES20.glUniform4fv(mColorUniform, 1, mColor, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mPositionAttribute);

        // Draw Line
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mLineVertices.length / COORDS_PER_VERTEX);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        ShaderUtil.checkGLError(TAG, "After draw");
    }
}
