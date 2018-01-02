package com.google.ar.core.examples.java.helloar.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.examples.java.helloar.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * I mean, the name says it all.
 *
 * Created by TY on 1/1/2018.
 */

public class DottedLineRenderer {

    private static final String TAG = SatelliteRenderer.class.getSimpleName();

    static float origin[] = { 0.0f, 0.0f };

    static float lineVertices[] = {
            0.0f, -1.0f, 0.0f,
            0.0f, 1.0f, 0.0f
    };

    private static final int COORDS_PER_VERTEX = 3;
    private final int vertexCount = lineVertices.length / COORDS_PER_VERTEX;
    private final int stride      = COORDS_PER_VERTEX * 4; // sizeof(float) == 4 per vertex

    // Object vertex buffer variables.
    private int mVertexBufferId;
    private int mVerticesBaseAddress;
    private int mProgram;

    // Shader location: model view projection matrix.
    private int mModelViewProjectionUniform;

    // Shader location: object attributes.
    private int mPositionAttribute;

    // Shader location: color
    private int mColorUniform;

    // Shader location: origin of line (center point)
    private int mOriginUniform;

    private SatelliteRenderer.BlendMode mBlendMode = null;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];
    private float[] mColor = {1.0f, 1.0f, 1.0f, 1.0f};


    public DottedLineRenderer() {}

    /**
     * Creates and initializes OpenGL resources needed for rendering the model.
     *
     * @param context Context for loading the shader and below-named model and texture assets.
     */
    public void createOnGlThread(Context context) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(lineVertices.length * 4);
        buf.order(ByteOrder.nativeOrder());

        FloatBuffer vertices = buf.asFloatBuffer();
        vertices.put(lineVertices);
        vertices.position(0);

        int[] buffer = new int[1];
        GLES20.glGenBuffers(1, buffer, 0);
        mVertexBufferId = buffer[0];

        // Load vertex buffer
        mVerticesBaseAddress = 0;
        final int totalBytes = 4 * vertices.limit();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, mVerticesBaseAddress, 4 * vertices.limit(), vertices);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "OBJ buffer load");

        final int vertexShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_VERTEX_SHADER, R.raw.line_vertex);
        final int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.line_fragment);

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

        mOriginUniform = GLES20.glGetUniformLocation(mProgram, "u_Origin");

        ShaderUtil.checkGLError(TAG, "Program parameters");

        Matrix.setIdentityM(mModelMatrix, 0);
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
                mPositionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mVerticesBaseAddress);


        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);

        // Set color
        GLES20.glUniform4fv(mColorUniform, 1, mColor, 0);

        // Set origin
        GLES20.glUniform2fv(mOriginUniform, 1, origin, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mPositionAttribute);

        // Draw Line
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        ShaderUtil.checkGLError(TAG, "After draw");
    }
}
