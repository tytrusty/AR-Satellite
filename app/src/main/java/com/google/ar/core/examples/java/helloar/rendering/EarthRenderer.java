package com.google.ar.core.examples.java.helloar.rendering;

/**
 * Created by TY on 12/31/2017.
 */

/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.google.ar.core.examples.java.helloar.R;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Renders an object loaded from an OBJ file in OpenGL.
 */
public class EarthRenderer extends ObjectRenderer {
    public static final float EARTH_RADIUS = 6.371f; // in kilometers
    private static final String TAG = EarthRenderer.class.getSimpleName();

    public EarthRenderer() {}


    private final int stacks = 40;
    private final int slices = 40;

    private final int verticesSize  = (stacks + 1) * (slices + 1) * 3;
    private final int texCoordsSize = (stacks + 1) * (slices + 1) * 3;
    private final int indicesSize   = (slices * stacks + slices) * 6;

    private float[] vertices = new float[verticesSize];
    private float[] normals = new float[verticesSize];
    private float[] texCoords = new float[texCoordsSize];
    private short[] indices = new short[indicesSize];

    private void create_sphere() {
        // Credit mostly to:
        // https://stackoverflow.com/questions/26116923/modern-opengl-draw-a-sphere-and-cylinder
        int vertex_idx = 0;
        int normal_idx = 0;
        int tex_idx = 0;
        int indices_idx = 0;

        for (int i = 0; i <= stacks; ++i){

            // PHI is the angle along the longitudinal lines
            // Specifically it is the angle between the positive y-axis and the line connecting
            // the origin to the x, y, z point
            float V   = i / (float) stacks;
            float phi = (float)(V * Math.PI);

            for (int j = 0; j <= slices; ++j){

                // Theta is angle along the equator -- Angle at each slice
                // It is the angle between the x-axis and the point in the x-z plane
                float U = j / (float) slices;
                float theta = U * (float) (Math.PI * 2);

                // Calc The Vertex Positions
                float y = (float) (Math.cos (phi));
                float z = (float) (Math.sin (theta) * Math.sin  (phi));
                float x = (float) (Math.cos (theta) * Math.sin  (phi));

                // Add vertex point
                vertices[vertex_idx++] = x;
                vertices[vertex_idx++] = y;
                vertices[vertex_idx++] = z;

                // Add normal point
                normals[normal_idx++] = x;
                normals[normal_idx++] = y;
                normals[normal_idx++] = z;

                texCoords[tex_idx++] = (1.0f - theta/ ((float)Math.PI * 2.0f));
                texCoords[tex_idx++] = (1.0f - phi/(float)Math.PI); // FLIP Orientation with 1 - ...
            }
        }

        // Calc The Index Positions
        for (short i = 0; i < slices * stacks + slices; ++i){
            indices[indices_idx++] = (i);
            indices[indices_idx++] = (short) (i + slices + 1);
            indices[indices_idx++] = (short) (i + slices);

            indices[indices_idx++] = (short) (i + slices + 1);
            indices[indices_idx++] = (i);
            indices[indices_idx++] = (short) (i + 1);
        }
    }
    /**
     * Creates and initializes OpenGL resources needed for rendering the model.
     *
     * @param context Context for loading the shader and below-named model and texture assets.
     * @param diffuseTextureAssetName  Name of the PNG file containing the diffuse texture map.
     */
    public void createOnGlThread(Context context,
                                 String diffuseTextureAssetName) throws IOException {
        // Read the texture.
        Bitmap textureBitmap = BitmapFactory.decodeStream(
                context.getAssets().open(diffuseTextureAssetName));

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(mTextures.length, mTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        textureBitmap.recycle();

        ShaderUtil.checkGLError(TAG, "Texture loading");

        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        mVertexBufferId = buffers[0];
        mIndexBufferId = buffers[1];

        // Generate sphere
        create_sphere();

        // Load buffers
        FloatBuffer vertBuffer = FloatBuffer.allocate(verticesSize);
        FloatBuffer texBuffer  = FloatBuffer.allocate(texCoordsSize);
        FloatBuffer normBuffer = FloatBuffer.allocate(verticesSize);
        vertBuffer.position(0); vertBuffer.put(vertices); vertBuffer.rewind();
        texBuffer.position(0); texBuffer.put(texCoords); texBuffer.rewind();
        normBuffer.position(0); normBuffer.put(normals); normBuffer.rewind();
        ShortBuffer indicesBuf = ShortBuffer.allocate(indicesSize);
        indicesBuf.position(0); indicesBuf.put(indices); indicesBuf.rewind();

        mVerticesBaseAddress = 0;
        mTexCoordsBaseAddress = mVerticesBaseAddress + 4 * vertBuffer.limit();
        mNormalsBaseAddress = mTexCoordsBaseAddress + 4 * texBuffer.limit();
        final int totalBytes = mNormalsBaseAddress + 4 * normBuffer.limit();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, mVerticesBaseAddress, 4 * vertices.length, vertBuffer);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress, 4 * texCoords.length, texBuffer);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress, 4 * normals.length, normBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Load index buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId);
        mIndexCount = indicesBuf.limit();
        GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * mIndexCount, indicesBuf, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "OBJ buffer load");

        final int vertexShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_VERTEX_SHADER, R.raw.object_vertex);
        final int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.object_fragment);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
        GLES20.glUseProgram(mProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        mModelViewUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelView");
        mModelViewProjectionUniform =
                GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");

        mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mNormalAttribute = GLES20.glGetAttribLocation(mProgram, "a_Normal");
        mTexCoordAttribute = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");

        mTextureUniform = GLES20.glGetUniformLocation(mProgram, "u_Texture");

        mLightingParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_LightingParameters");
        mMaterialParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_MaterialParameters");

        ShaderUtil.checkGLError(TAG, "Program parameters");

        Matrix.setIdentityM(mModelMatrix, 0);
    }

    private float angle = 0.0f;
    /**
     * Updates the object model matrix and applies scaling.
     *
     * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
     * @param scaleFactor A separate scaling factor to apply before the {@code modelMatrix}.
     * @param translateFactor A constant scalar to apply for translation along the y-axis
     * @param isPositioning Indicates whether earth is in positioning stage.
     * @see android.opengl.Matrix
     */
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor, float translateFactor,
                                  boolean isPositioning) {
        // Matrix structure:
        // [ 0  4  8   12 ]
        // [ 1  5  9   13 ]
        // [ 2  6  10  14 ]
        // [ 3  7  11  15 ]
        // 0, 5, 10 may be used to scale x, y, z respectively.
        // 12, 13, 14 may be used to translate along x, y, z respectively
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0]  = scaleFactor;
        scaleMatrix[5]  = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        //TODO don't rotate modelMatrix -- fuckin up the pointer
        Matrix.rotateM(mModelMatrix, 0, angle, 0.0f, 1.0f, 0.0f);

        // Rotate if in positioning mode
        if (isPositioning) {
            angle++;
        } else {
            angle = 0;
        }
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);

        // Translate along the y-axis only
        mModelMatrix[13] = translateFactor;
    }

    /**
     * Draws the model.
     *
     * @param cameraView  A 4x4 view matrix, in column-major order.
     * @param cameraPerspective  A 4x4 projection matrix, in column-major order.
     * @param lightIntensity  Illumination intensity.  Combined with diffuse and specular material
     *     properties.
     * @param isPositioning Indicates whether earth is in positioning stage. Will draw wireframe if so.
     * @see #setBlendMode(BlendMode)
     * @see #updateModelMatrix(float[], float)
     * @see #setMaterialProperties(float, float, float, float)
     * @see android.opengl.Matrix
     */
    public void draw(float[] cameraView, float[] cameraPerspective, float lightIntensity, boolean isPositioning) {

        ShaderUtil.checkGLError(TAG, "Before draw");

        // Build the ModelView and ModelViewProjection matrices
        // for calculating object position and light.
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);

        GLES20.glUseProgram(mProgram);

        // Set the lighting environment properties.
        Matrix.multiplyMV(mViewLightDirection, 0, mModelViewMatrix, 0, LIGHT_DIRECTION, 0);
        normalizeVec3(mViewLightDirection);
        GLES20.glUniform4f(mLightingParametersUniform,
                mViewLightDirection[0], mViewLightDirection[1], mViewLightDirection[2], lightIntensity);

        // Set the object material properties.
        if (isPositioning) {
            // When positioning, the wireframe appears dark so it's necessary to amplify the
            // material properties to make it appear more vibrant.
            GLES20.glUniform4f(mMaterialParametersUniform, mAmbient * 10, mDiffuse * 10,
                    mSpecular * 10, mSpecularPower);
        } else {
            GLES20.glUniform4f(mMaterialParametersUniform, mAmbient, mDiffuse, mSpecular,
                    mSpecularPower);
        }


        // Attach the object texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glUniform1i(mTextureUniform, 0);

        // Set the vertex attributes.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);

        GLES20.glVertexAttribPointer(
                mPositionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mVerticesBaseAddress);
        GLES20.glVertexAttribPointer(
                mNormalAttribute, 3, GLES20.GL_FLOAT, false, 0, mNormalsBaseAddress);
        GLES20.glVertexAttribPointer(
                mTexCoordAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexCoordsBaseAddress);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(
                mModelViewUniform, 1, false, mModelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mPositionAttribute);
        GLES20.glEnableVertexAttribArray(mNormalAttribute);
        GLES20.glEnableVertexAttribArray(mTexCoordAttribute);

        if (mBlendMode != null) {
            GLES20.glDepthMask(false);
            GLES20.glEnable(GLES20.GL_BLEND);
            switch (mBlendMode) {
                case Shadow:
                    // Multiplicative blending function for Shadow.
                    GLES20.glBlendFunc(GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case Grid:
                    // Grid, additive blending function.
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                    break;
            }
        }

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId);

        // When positioning, draw a wireframe earth. Just looks kinda cool
        if (isPositioning) {
            GLES20.glDrawElements(GLES20.GL_LINE_STRIP, mIndexCount, GLES20.GL_UNSIGNED_SHORT, 0);

        } else {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        if (mBlendMode != null) {
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDepthMask(true);
        }

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        GLES20.glDisableVertexAttribArray(mNormalAttribute);
        GLES20.glDisableVertexAttribArray(mTexCoordAttribute);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        ShaderUtil.checkGLError(TAG, "After draw");
    }

}
