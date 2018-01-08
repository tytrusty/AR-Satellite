package com.google.ar.core.examples.java.helloar.rendering;

/**
 * Created by TY on 1/7/2018.
 */

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.examples.java.helloar.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by TY on 12/31/2017.
 */
public class EarthShadowRenderer {
    private static final String TAG = EarthShadowRenderer.class.getSimpleName();

    private static float mOrigin[] = { 0.0f, 0.0f };
    private static final double TWO_PI = 2 * Math.PI;
    private static final int COORDS_PER_VERTEX = 3;

    // Object vertex buffer variables.
    private int mVertexBufferId;
    private int mProgram;

    // Shader location: model view projection matrix.
    private int mModelViewProjectionUniform;

    // Shader location: object attributes.
    private int mPositionAttribute;

    // Shader location: color
    private int mColorUniform;

    // Shader location: origin of line (center point)
    private int mOriginUniform;

    // Shader location: height of area between object and shadow
    private int mHeightUniform;

    private static final int DEFAULT_POINTS = 40;
    private final float[] mVertices;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];
    private float[] mColor = {0.0f, 0.0f, 0.0f, 1.0f};

    public EarthShadowRenderer(final int circlePoints) {
        mVertices = create_circle(circlePoints);
    }

    public EarthShadowRenderer() {
        mVertices = create_circle(DEFAULT_POINTS);
    }

    /**
     * Generates a filled circle with radius = 1. Circle is filled in that the initial
     * vertex is at the origin so it can be rendered with TRIANGLE_FAN
     *
     * @param points The number of points for the circle
     * @return An array of the circle's vertices
     */
    private static float[] create_circle(int points) {
        float[] vertices = new float[(points + 1) * COORDS_PER_VERTEX ];
        // Initial point at the origin for rendering as triangle fan
        vertices[0] = 0.0f;
        vertices[1] = 0.0f;
        vertices[2] = 0.0f;

        int vertices_idx = 3;
        for (int i = 0; i < points; ++i) {
            vertices[vertices_idx++] = (float) Math.cos(i * TWO_PI / (points - 1)); // x
            vertices[vertices_idx++] = 0.0f;                                        // y
            vertices[vertices_idx++] = (float) Math.sin(i * TWO_PI / (points - 1)); // z
        }
        return vertices;
    }

    /**
     * Creates and initializes OpenGL resources needed for rendering the model.
     *
     * @param context Context for loading the shader and below-named model and texture assets.
     */
    public void createOnGlThread(Context context) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(mVertices.length * 4);
        buf.order(ByteOrder.nativeOrder());

        FloatBuffer vertices = buf.asFloatBuffer();
        vertices.put(mVertices);
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
                GLES20.GL_VERTEX_SHADER, R.raw.line_vertex);
        final int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.earth_shadow_fragment);

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

        mHeightUniform = GLES20.glGetUniformLocation(mProgram, "u_Height");

        ShaderUtil.checkGLError(TAG, "Program parameters");

        Matrix.setIdentityM(mModelMatrix, 0);

    }

    /**
     * Updates the lines origin and end points
     *
     * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
     * @see android.opengl.Matrix
     */
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0]  = scaleFactor;
        scaleMatrix[5]  = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    /**
     * Draws the line.
     *
     * @param cameraView  A 4x4 view matrix, in column-major order.
     * @param cameraPerspective  A 4x4 projection matrix, in column-major order.
     * @param translateHeight The height of the area between shadow and the center of the earth
     * @see android.opengl.Matrix
     */
    public void draw(float[] cameraView, float[] cameraPerspective, float translateHeight) {
        ShaderUtil.checkGLError(TAG, "Before draw");
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

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

        // Set origin
        GLES20.glUniform2fv(mOriginUniform, 1, mOrigin, 0);

        // Set height
        GLES20.glUniform1f(mHeightUniform, translateHeight);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mPositionAttribute);

        // Draw Line
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, mVertices.length);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        ShaderUtil.checkGLError(TAG, "After draw");
    }

}
