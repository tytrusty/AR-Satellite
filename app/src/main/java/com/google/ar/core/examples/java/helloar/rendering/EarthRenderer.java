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
import java.util.ArrayList;

/**
 * Renders an object loaded from an OBJ file in OpenGL.
 */
public class EarthRenderer extends ObjectRenderer {
    private static final String TAG = EarthRenderer.class.getSimpleName();

    public EarthRenderer() {
    }

    private ArrayList<Float> vertices = new ArrayList<>();
    private ArrayList<Float> texCoords = new ArrayList<>();
    private ArrayList<Float> normals = new ArrayList<>();
    private ArrayList<Integer> indices = new ArrayList<>();
    private final int stacks = 40;
    private final int slices = 40;

    public void create_sphere() {
        // Credit mostly to:
        // https://stackoverflow.com/questions/26116923/modern-opengl-draw-a-sphere-and-cylinder
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

                // Push Back Vertex Data
                vertices.add(x);
                vertices.add(y);
                vertices.add(z);
                normals.add(x);
                normals.add(y);
                normals.add(z);
                texCoords.add(1.0f - theta/ ((float)Math.PI * 2.0f));
                texCoords.add(1.0f - phi/(float)Math.PI); // FLIP Orientation with 1 - ...
            }
        }

        // Calc The Index Positions
        for (int i = 0; i < slices * stacks + slices; ++i){

            indices.add (i);
            indices.add (i + slices + 1);
            indices.add (i + slices);

            indices.add (i + slices + 1);
            indices.add (i);
            indices.add (i + 1);
        }
        System.out.println("Numba of vertices loul: " + indices.size());
    }
    /**
     * Creates and initializes OpenGL resources needed for rendering the model.
     *
     * @param context Context for loading the shader and below-named model and texture assets.
     * @param objAssetName  Name of the OBJ file containing the model geometry.
     * @param diffuseTextureAssetName  Name of the PNG file containing the diffuse texture map.
     */
    public void createOnGlThread(Context context, String objAssetName,
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

        // Load vertex buffer
        // What a mess, but it works lol
        create_sphere();
        float[] vertArr = new float[vertices.size()];
        float[] textArr = new float[texCoords.size()];
        float[] normArr = new float[normals.size()];
        short[] indicesArr = new short[indices.size()];

        int i = 0; for (Float f : vertices) { vertArr[i++] = (f != null ? f : Float.NaN);}
        i = 0; for (Float f : texCoords) { textArr[i++] = (f != null ? f : Float.NaN);}
        i = 0; for (Float f : normals) { normArr[i++] = (f != null ? f : Float.NaN);}
        i = 0;
        for(i = 0; i < indices.size(); ++i) { indicesArr[i] = indices.get(i).shortValue();
        }


        FloatBuffer vertBuffer = FloatBuffer.allocate(vertices.size());
        FloatBuffer texBuffer  = FloatBuffer.allocate(texCoords.size());
        FloatBuffer normBuffer = FloatBuffer.allocate(normals.size());
        vertBuffer.position(0); vertBuffer.put(vertArr); vertBuffer.rewind();
        texBuffer.position(0); texBuffer.put(textArr); texBuffer.rewind();
        normBuffer.position(0); normBuffer.put(normArr); normBuffer.rewind();
        ShortBuffer indecesBuf = ShortBuffer.allocate(indices.size());
        indecesBuf.position(0); indecesBuf.put(indicesArr); indecesBuf.rewind();

        mVerticesBaseAddress = 0;
        mTexCoordsBaseAddress = mVerticesBaseAddress + 4 * vertBuffer.limit();
        mNormalsBaseAddress = mTexCoordsBaseAddress + 4 * texBuffer.limit();
        final int totalBytes = mNormalsBaseAddress + 4 * normBuffer.limit();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, mVerticesBaseAddress, 4 * vertArr.length, vertBuffer);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress, 4 * textArr.length, texBuffer);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress, 4 * normArr.length, normBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Load index buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId);
        mIndexCount = indecesBuf.limit();
        GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * mIndexCount, indecesBuf, GLES20.GL_STATIC_DRAW);
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

    /**
     * Selects the blending mode for rendering.
     *
     * @param blendMode The blending mode.  Null indicates no blending (opaque rendering).
     */
    public void setBlendMode(BlendMode blendMode) {
        mBlendMode = blendMode;
    }

    /**
     * Updates the object model matrix and applies scaling.
     *
     * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
     * @param scaleFactor A separate scaling factor to apply before the {@code modelMatrix}.
     * @see android.opengl.Matrix
     */
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    /**
     * Sets the surface characteristics of the rendered model.
     *
     * @param ambient  Intensity of non-directional surface illumination.
     * @param diffuse  Diffuse (matte) surface reflectivity.
     * @param specular  Specular (shiny) surface reflectivity.
     * @param specularPower  Surface shininess.  Larger values result in a smaller, sharper
     *     specular highlight.
     */
    public void setMaterialProperties(
            float ambient, float diffuse, float specular, float specularPower) {
        mAmbient = ambient;
        mDiffuse = diffuse;
        mSpecular = specular;
        mSpecularPower = specularPower;
    }


}
