package theugateam.progetto.Utils;

import android.opengl.Matrix;

// classe di supoorto per gestire la camera (posizione e orientamento) di OpenGL e le sue matrici
public class Camera {

    private float _cameraX;
    private float _cameraY;
    private float _cameraZ;
    private float _cameraLAX;
    private float _cameraLAY;
    private float _cameraLAZ;

    private float ScreenWidth;
    private float ScreenHeigth;

    private float[] ProjectionMatrix;
    private float[] ViewMatrix;
    private float[] VPMatrix;


    public Camera() {
        _cameraX = 0;
        _cameraY = 0;
        _cameraZ = 0;
        _cameraLAX = 0;
        _cameraLAY = 0;
        _cameraLAZ = 0;

        ProjectionMatrix = new float[16];
        ViewMatrix = new float[16];

        ScreenWidth = 0;
        ScreenHeigth = 0;
    }

    public float getScreenWidth() {
        return ScreenWidth;
    }

    public float getScreenHeigth() {
        return ScreenHeigth;
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

    // setta la matrice che servirà a proiettare i punti nello spazio tridimensionale
    // nel piano bidimensionale dello schermo
    public void setProjectionMatrix(float screenWidth, float screenHeight, float ZNear, float ZFar) {
        ScreenWidth = screenWidth;
        ScreenHeigth = screenHeight;
        float ratio = ScreenWidth / ScreenHeigth;
        // Matrix.frustumM(ProjectionMatrix, 0, -ratio, ratio, -1, 1, ZNear, ZFar);
        Matrix.orthoM(ProjectionMatrix,0,-10*ratio, 10*ratio, -10, 10, ZNear, ZFar);
        VPMatrix = new float[16];
        Matrix.multiplyMM(VPMatrix, 0, ProjectionMatrix, 0, ViewMatrix, 0);
    }

    // setta la matrice che rappresenta la camera che andrà ad inquadrare la scena
    private void setViewMatrix() {
        Matrix.setLookAtM(ViewMatrix, 0, _cameraX, _cameraY, _cameraZ, _cameraLAX, _cameraLAY, _cameraLAZ, 0.0f, 1.0f, 0.0f);

        VPMatrix = new float[16];
        Matrix.multiplyMM(VPMatrix, 0, ProjectionMatrix, 0, ViewMatrix, 0);
    }

    public float[] getVPMatrix() {
        return VPMatrix;
    }
}
