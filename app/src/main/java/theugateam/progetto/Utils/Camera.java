package theugateam.progetto.Utils;

import android.opengl.Matrix;

public class Camera {

    private float _cameraX;
    private float _cameraY;
    private float _cameraZ;
    private float _cameraLAX;
    private float _cameraLAY;
    private float _cameraLAZ;

    private float ScreenWidth;
    private float ScreenHeight;

    private float[] ProjectionMatrix;
    private float[] InvertedProjectionMatrix;

    //Model View Projection Matrix
    private float[] MVPMatrix;

    private float[] ViewMatrix;

    private float diagonalLength;

    public Camera() {
        _cameraX = 0;
        _cameraY = 0;
        _cameraZ = 0;
        _cameraLAX = 0;
        _cameraLAY = 0;
        _cameraLAZ = 0;

        ProjectionMatrix = new float[16];
        InvertedProjectionMatrix = new float[16];
        MVPMatrix = new float[16];
        ViewMatrix = new float[16];

        ScreenWidth = 0;
        ScreenHeight = 0;
        diagonalLength = 0;
    }

    public float getScreenWidth() {
        return ScreenWidth;
    }

    public void setCameraLookAt(Vector3 lookAt) {
        _cameraLAX = (float) lookAt.x();
        _cameraLAY = (float) lookAt.y();
        _cameraLAZ = (float) lookAt.z();
        setViewMatrix();
    }

    public void setCameraPosition(Vector3 position) {
        _cameraX = (float) position.x();
        _cameraY = (float) position.y();
        _cameraZ = (float) position.z();
        setViewMatrix();
    }

    public void setProjectionMatrix(float screenWidth, float screenHeight, float ZNear, float ZFar) {
        ScreenWidth = screenWidth;
        ScreenHeight = screenHeight;
        float ratio = ScreenWidth / ScreenHeight;
        Matrix.frustumM(ProjectionMatrix, 0, -ratio, ratio, -1, 1, ZNear, ZFar);
        diagonalLength = (float) Math.sqrt((Math.pow(ScreenWidth, 2) + Math.pow(ScreenHeight, 2)));
    }

    private void setViewMatrix() {
        Matrix.setLookAtM(ViewMatrix, 0, _cameraX, _cameraY, _cameraZ, _cameraLAX, _cameraLAY, _cameraLAZ, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(MVPMatrix, 0, ProjectionMatrix, 0, ViewMatrix, 0);

        Matrix.invertM(InvertedProjectionMatrix, 0, MVPMatrix, 0);
    }

    public float[] getViewMatrix() {
        return ViewMatrix;
    }

    public float[] getProjectionMatrix() {
        return ProjectionMatrix;
    }

    public float getDiagonalLength() {
        return diagonalLength;
    }
}
