package theugateam.progetto;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import theugateam.progetto.Utils.Camera;
import theugateam.progetto.Utils.Vector3;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 * <li>{@link GLSurfaceView.Renderer#onSurfaceCreated}</li>
 * <li>{@link GLSurfaceView.Renderer#onDrawFrame}</li>
 * <li>{@link GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    public enum STATUS {
        LOADING, DRAWING
    }

    private static final String TAG = "MyGLRenderer";
    private final int ANGLE_MAGNITUDE = 360;
    private Context context;
    private int i;
    private STATUS STATE;
    private float ZNear;
    private float ZFar;
    private Vector3 touchDownCoords;
    private Camera camera;
    private Model3DVBO[] heads;
    private Model3DVBO selectedHeads;
    private Model3D loadingGear;
    private Model3D loadingText;
    private float startTime;
    private float stateTime;
    private float frameTime;

    public void initialize(Context androidContext) {
        context = androidContext;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // aggiusta il viewport in base alla dimensione dello schermo

        GLES20.glViewport(0, 0, width, height);

        ZNear = 1;
        ZFar = 50;

        camera.setProjectionMatrix(width, height, ZNear, ZFar);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        changeState(STATUS.LOADING);

        // z buffer
        // di default è GL_LESS
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);
        GLES20.glDepthMask(true);

        /*culling - graficamente rende peggio
        GLES20.glFrontFace(GLES20.GL_CCW );
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);*/

        // inizializza la camera
        camera = new Camera();
        this.camera.setCameraPosition(new Vector3(0, 0, 12));
        this.camera.setCameraLookAt(new Vector3(0, 0, 0));

        // inizializza gli oggetti presenti nella schermata di caricamento
        loadingGear = new Model3D(context);
        loadingGear.loadFromOBJ(context, "newgear");
        loadingGear.loadGLTexture(context, R.drawable.white);
        loadingGear.moveScaleRotate(new Vector3(0, 4, 0),
                new Vector3(1.6, 1.6, 1.6),
                new Vector3(0, 0, 0));
        loadingGear.setColor(255, 220, 0, 255);

        loadingText = new Model3D(context);
        loadingText.loadFromOBJ(context, "loading");
        loadingText.loadGLTexture(context, R.drawable.white);
        loadingText.moveScaleRotate(new Vector3(0, -7, 0),
                new Vector3(2.7, 2.7, 2.7),
                new Vector3(0, 0, 0));
        loadingText.setColor(0, 114, 187, 255);

        selectedHeads = new Model3DVBO(context);
        heads = new Model3DVBO[3];

        // inizializza gli oggetti che andranno a disegnare le teste e carica la geometria e
        // le texture usando thread asincroni
        heads[0] = new Model3DVBO(context);
        heads[0].setName("nastro");
        heads[0].moveScaleRotate(new Vector3(-10, 0, 0),
                new Vector3(2, 2, 2),
                new Vector3(90, 0, 0));
        Thread thread0 = new Thread() {
            public void run() {
                heads[0].loadFromOBJThreaded(context, "mobius");
                heads[0].saveBitmap(context, R.drawable.mobius);
            }
        };

        thread0.start();

        heads[1] = new Model3DVBO(context);
        heads[1].setName("pigna");
        heads[1].moveScaleRotate(new Vector3(0, 0, 0),
                new Vector3(0.9, 0.9, 0.9),
                new Vector3(90, 0, 0));
        Thread thread1 = new Thread() {
            public void run() {
                heads[1].loadFromOBJThreaded(context, "mobius");
                heads[1].saveBitmap(context, R.drawable.mobius);
            }
        };

        thread1.start();

        heads[2] = new Model3DVBO(context);
        heads[2].setName("bottiglia");
        heads[2].moveScaleRotate(new Vector3(10, 0, 0),
                new Vector3(1.5, 1.5, 1.5),
                new Vector3(90, 0, 0));
        Thread thread2 = new Thread() {
            public void run() {
                heads[2].loadFromOBJThreaded(context, "mobius");
                heads[2].saveBitmap(context, R.drawable.mobius);
            }
        };

        thread2.start();
    }

    public void update() {
        frameTime=stateTime;
        stateTime = (float) System.nanoTime() / 10000000f - startTime;
        frameTime=stateTime-frameTime;

        switch (STATE) {

            case LOADING:
                loadingGear.rotate(new Vector3(loadingGear.getRotation(0), loadingGear.getRotation(1), frameTime*0.7f));
                boolean flag = true;
                for (i = 0; i < heads.length; i++) {
                    if (heads[i].state() == 2) {
                        // una volta che il thread di appoggio ha caricato texture e geometria
                        // il thread principale deve caricare il tutto in OpenGL
                        heads[i].loadObjData();
                        heads[i].loadFromSavedBitmap();
                    } else if (heads[i].state() != Model3D.LOADING_COMPLETED)
                        // finchè non è tutto caricato rimane nella schermata di loading
                        flag = false;
                }
                if (flag)
                    changeState(STATUS.DRAWING);
                break;

            case DRAWING:
                break;
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0, 0.0f, 0.0f, 1.0f);

        update();

        switch (STATE) {

            case LOADING:
                // disegna gli elementi appartenenti alla schermata di looading
                loadingGear.draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                loadingText.draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                break;

            case DRAWING:
                // disegna gli elementi appartenenti alla schermata principale
                for (i = 0; i < heads.length; i++) {
                    heads[i].draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                }
                break;
        }
    }

    private void changeState(STATUS status) {
        STATE = status;
        startTime = System.nanoTime() / 10000000f;
        // potrebbe servire in sviluppi futuri per eseguire azioni al cambio di stato
        switch (status) {

            case LOADING:
                break;

            case DRAWING:
                break;
        }
    }

    public STATUS getState() {
        return STATE;
    }

    // region AZIONI_TOUCH

    // decide quale testa è da ruotare
    public void selectHeadSingleTouch(Vector3 screenCoords) {
        if (STATE == STATUS.DRAWING) {
            touchDownCoords = screenCoords;
            if (touchDownCoords.x() / camera.getScreenWidth() < 0.33f)
                selectedHeads = heads[0];
            else if (touchDownCoords.x() / camera.getScreenWidth() < 0.66f)
                selectedHeads = heads[1];
            else selectedHeads = heads[2];
        }
    }

    // seleziona l'oggetto interessato dallo zoom e ne ritorna la scala
    public float selectedForScale(Vector3 point1, Vector3 point2) {
        // se ci troviamo nello stato DRAWING allora decide quale oggetto è da modificare e ritorna la sua scala
        if (STATE == STATUS.DRAWING) {
            Vector3 mid_point = new Vector3(
                    (point2.x() + point1.x()) / 2.0f,
                    (point2.y() + point1.y()) / 2.0f,
                    0);

            if (mid_point.x() / camera.getScreenWidth() < 0.33f) {
                selectedHeads = heads[0];
            } else if (mid_point.x() / camera.getScreenWidth() < 0.66f)
                selectedHeads = heads[1];
            else
                selectedHeads = heads[2];
        }
        return selectedHeads.getGlobalScale();

    }

    // imposta la nuova scala all'oggetto d'interesse
    public void zoom(float scale) {
        if (scale > 0)
            selectedHeads.setGlobalScale(scale);
    }

    // imposta la nuova rotazione all'oggetto d'interesse
    public void rotateHead(Vector3 screenCoords) {
        if (STATE == STATUS.DRAWING && touchDownCoords != null) {
            Vector3 vector = new Vector3((screenCoords.y() - touchDownCoords.y()) / camera.getScreenWidth() * ANGLE_MAGNITUDE
                    ,(screenCoords.x() - touchDownCoords.x()) / camera.getScreenHeigth() * ANGLE_MAGNITUDE
                    ,0);

            selectedHeads.rotate(vector);
            touchDownCoords = screenCoords;
        }
    }

    public void resetHead()
    {
        selectedHeads.resetRotation(new Vector3(90,0,0));
        selectedHeads.setGlobalScale(1.0f);
    }

    // endregion

    // region OPENGL

    /**
     * Utility method for compiling a OpenGL shader.
     * <p/>
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type       - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     * <p>
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     * <p>
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    // legge e ritorna su stringa il codice dello shader prelevato da un file resourceId relativo ad un context
    public static String readTextFileFromResource(Context context,
                                                  int resourceId) {
        StringBuilder body = new StringBuilder();
        try {
            InputStream inputStream =
                    context.getResources().openRawResource(resourceId);
            InputStreamReader inputStreamReader =new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String nextLine;
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not open resource: " + resourceId, e);
        } catch (Resources.NotFoundException nfe) {
            throw new RuntimeException("Resource not found: " + resourceId, nfe);
        }
        return body.toString();
    }
    // endregion
}
