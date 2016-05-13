package theugateam.progetto;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by riccardo on 13/05/16.
 */
public class Model3DVBO extends Model3D{

    private int[] coordsVBO;
    private int[] uvVBO;
    private int[] indexVBO;

    protected void updatePointerVariables() {

        super.updatePointerVariables();

        coordsVBO = new int[1];
        uvVBO = new int[1];
        indexVBO = new int[1];

        //VBO stuffs
        GLES20.glGenBuffers(1, coordsVBO, 0);
        GLES20.glGenBuffers(1, uvVBO, 0);
        GLES20.glGenBuffers(1, indexVBO, 0);

        MyGLRenderer.checkGlError("updatePointerVariables");

    }

    protected void updateGeometryAndUVs(float[] GeometryCoords, float[] UVCoords, int[] DrawOrder) {

        super.updateGeometryAndUVs(GeometryCoords, UVCoords, DrawOrder);

        //
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, coordsVBO[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity()
                * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        //
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvVBO[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, uvBuffer.capacity()
                * 4, uvBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        //
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVBO[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, drawListBuffer.capacity()
                * 4, drawListBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        MyGLRenderer.checkGlError("end updateGeometryAndUVs");
    }

    //IT'S AN ALPHA DRAW (PRE-BETA)
    public void draw(float[] mViewMatrix, float[] mProjectionMatrix) {

        MyGLRenderer.checkGlError("pre draw");
        float[] model = new float[16];
        float[] temp = new float[16];
        float[] mvpMatrix = new float[16];

        Matrix.setIdentityM(model, 0); // initialize to identity matrix

        Matrix.translateM(model, 0, _x, _y, _z);

        Matrix.scaleM(model, 0, _scaleX, _scaleY, _scaleZ);

        Matrix.rotateM(model, 0, _angleX, 1, 0, 0);
        Matrix.rotateM(model, 0, _angleY, 0, 1, 0);
        Matrix.rotateM(model, 0, _angleZ, 0, 0, 1);

        //MyGLRenderer.checkGlError("_angleZ");
        Matrix.multiplyMM(temp, 0, mViewMatrix, 0, model, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, temp, 0);

        //MyGLRenderer.checkGlError("draw");
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        //MyGLRenderer.checkGlError("draw");

        //coords
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, coordsVBO[0]);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
        //MyGLRenderer.checkGlError("coords");

        //texture
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvVBO[0]);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
        //MyGLRenderer.checkGlError("texture");

        //color
        GLES20.glUniform4f(colorUniformHandle, color[0], color[1], color[2], color[3]);

        //MATRIX AND TEXTURE
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);//Apply the projection and view transformation

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glUniform1i(textureUniformHandle, 0);
       // MyGLRenderer.checkGlError("texture");
        // Draw the arrays
        // glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        // glDrawElements(GL_TRIANGLE_STRIP, sizeof(indices)/sizeof(GLubyte), GL_UNSIGNED_BYTE, (void*)0);
        //https://developer.apple.com/library/mac/documentation/GraphicsImaging/Conceptual/OpenGL-MacProgGuide/opengl_vertexdata/opengl_vertexdata.html*/

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVBO[0]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawListBufferCapacity, GLES20.GL_UNSIGNED_INT, indexVBO[0]);//Draw

        // Log.d("DEBUG","pos "+mPositionHandle+" text "+textureCoordinateHandle);

        //Disable the arrays as needed
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);

        //MyGLRenderer.checkGlError("draw");
    }
}