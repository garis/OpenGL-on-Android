package theugateam.progetto;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.ArrayList;

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
        LOADING, APPLYING_TEXTURE, DRAWING
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
    private Model3D[] heads;
    private Model3D selectedHeads;
    private Vector3 startingRotation;
    private Model3D loadingGear;
    private Model3D loadingText;
    private long startTime;
    private long stateTime;

    public void initialize(Context androidContext) {
        context = androidContext;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation

        GLES20.glViewport(0, 0, width, height);

        ZNear = 1;
        ZFar = 100;

        camera.setProjectionMatrix(width, height, ZNear, ZFar);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        changeState(STATUS.LOADING);
        //z buffer
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);

        camera = new Camera();
        this.camera.setCameraPosition(new Vector3(0, 0, 12));
        this.camera.setCameraLookAt(new Vector3(0, 0, 0));

        loadingGear = new Model3D();
        loadingGear.loadFromOBJ(context, "gear");
        loadingGear.loadGLTexture(context, R.drawable.white);
        loadingGear.scale(new Vector3(1.6, 1.6, 1.6));
        loadingGear.move(new Vector3(0, 4, 0));
        loadingGear.rotate(new Vector3(0, 0, 0));
        loadingGear.setColor(255, 76, 59,255);

        loadingText = new Model3D();
        loadingText.loadFromOBJ(context, "loading");
        loadingText.loadGLTexture(context, R.drawable.white);
        loadingText.scale(new Vector3(2.7, 2.7, 2.7));
        loadingText.move(new Vector3(0, -7, 0));
        loadingText.rotate(new Vector3(0, 0, 0));
        loadingText.setColor(0, 114, 187,255);

        LoadingThread loadingThread = new LoadingThread();

        selectedHeads = new Model3D();
        heads = new Model3D[3];

        heads[0] = new Model3D();
        heads[0].scale(new Vector3(2, 2, 2));
        heads[0].move(new Vector3(-10, 0, 0));
        heads[0].rotate(new Vector3(90, 0, 0));
        loadingThread.loadObject(new LoadingInfo(heads[0], "mobius", R.drawable.mobius));

        heads[1] = new Model3D();
        heads[1].scale(new Vector3(0.9, 0.9, 0.9));
        heads[1].move(new Vector3(0, 0, 0));
        heads[1].rotate(new Vector3(90, 0, 0));
        loadingThread.loadObject(new LoadingInfo(heads[1], "pigna", R.drawable.tex));

        heads[2] = new Model3D();
        heads[2].scale(new Vector3(1.5, 1.5, 1.5));
        heads[2].move(new Vector3(10, 0, 0));
        heads[2].rotate(new Vector3(90, 0, 0));
        loadingThread.loadObject(new LoadingInfo(heads[2], "kleinbottle", R.drawable.kleinbottle));
        loadingThread.start();
    }

    public void update() {
        stateTime = System.currentTimeMillis() - startTime;

        switch (STATE) {

            case LOADING:
                loadingGear.rotate(new Vector3(loadingGear.getRotation(0), loadingGear.getRotation(1), stateTime * 0.1f));
                break;

            //the texture must be loaded on the GLThread not from some others thread
            case APPLYING_TEXTURE:
                for (i = 0; i < heads.length; i++)
                    heads[i].loadFromSavedBitmap();
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
        GLES20.glClearColor(0, 0, 1.0f, 1.0f);

        update();

        switch (STATE) {

            case LOADING:
                loadingGear.draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                loadingText.draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                break;

            //the texture must be loaded on the GLThread not from some others thread
            case APPLYING_TEXTURE:
                for (Model3D head : heads) head.loadFromSavedBitmap();
                changeState(STATUS.DRAWING);
                break;

            case DRAWING:
                for (i = 0; i < heads.length; i++) {
                    heads[i].draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                }
                break;
        }
    }

    public void changeState(STATUS status) {
        STATE = status;
        startTime = System.currentTimeMillis();
        switch (status) {

            case LOADING:
                break;

            case APPLYING_TEXTURE:
                break;

            case DRAWING:
                break;
        }
    }

    class LoadingThread extends Thread {
        ArrayList<LoadingInfo> objects;

        public LoadingThread() {
            objects = new ArrayList<>();
        }

        public void loadObject(LoadingInfo head) {
            objects.add(head);
        }

        public void run() {
            for (LoadingInfo load : objects
                    ) {
                load.load();
            }
            changeState(STATUS.APPLYING_TEXTURE);
        }
    }

    class LoadingInfo {
        public Model3D head;
        public String objFile;
        public int texture;

        public LoadingInfo(Model3D headObj, String file, int textureId) {
            head = headObj;
            objFile = file;
            texture = textureId;
        }

        public void load() {
            head.loadFromOBJ(context, objFile);
            head.saveBitmap(context, texture);
        }
    }

    //region user touch action

    /*select between the 3 objects
    it divide the width of the screen in three regions
    than select the desidered object for that part of the screen
    */
    public void touchDown(Vector3 screenCoords) {
        //Log.d("DEBUG",screenCoords.toString());
        if (STATE == STATUS.DRAWING) {
            touchDownCoords = screenCoords;
            if (touchDownCoords.x() / camera.getScreenWidth() < 0.33f)
                selectedHeads = heads[0];
            else if (touchDownCoords.x() / camera.getScreenWidth() < 0.66f)
                selectedHeads = heads[1];
            else selectedHeads = heads[2];
            startingRotation = new Vector3(selectedHeads.getRotation(0), selectedHeads.getRotation(1), selectedHeads.getRotation(2));
        }
    }

    //move the selected object
    public void touchMove(Vector3 screenCoords) {
        if (STATE == STATUS.DRAWING && touchDownCoords!=null) {
            //get the angle between the touchDown and the current position tapped on the screen
            float angle = (float) Math.atan2((screenCoords.y() - touchDownCoords.y()), (screenCoords.x() - touchDownCoords.x()));

            //distance of actual touch and first touch down
            float dist = (float) Math.sqrt((Math.pow((screenCoords.y() - touchDownCoords.y()), 2) + Math.pow((screenCoords.x() - touchDownCoords.x()), 2)));

            //time to normalize the distance based on the screen diagonal
            dist = dist / camera.getDiagonalLength();

            //angle for every single axis
            //magic number to decide how much rotation to give
            Vector3 angleVector = new Vector3(Math.sin(angle) * ANGLE_MAGNITUDE * dist, Math.cos(angle) * ANGLE_MAGNITUDE * dist, 0);

            selectedHeads.rotate(new Vector3(angleVector.x(), 0, -angleVector.y()).add(startingRotation));
        }
    }
    //endregion

    //region OPENGL shader & error stuff

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
     * <p/>
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     * <p/>
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
    //endregion
}