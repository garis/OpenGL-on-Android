package theugateam.progetto;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

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

    //TODO
    /*
    sistemare le cose che settano OBJECT_STATUS in giro
    e discutere se questo sistema di loading va bene
    ora: maggior CPU e RAM (anche 80 megabyte di picco) ma minor TEMPO
    prima: minor CPU e RAM (30 megabyte di picco) ma maggior TEMPO
     */

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
    private Vector3 startingRotation;
    private Model3DVBO loadingGear;
    private Model3DVBO loadingText;
    private float startTime;
    private float stateTime;

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
        //GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        //GLES20.glDepthMask(true);

        camera = new Camera();
        this.camera.setCameraPosition(new Vector3(0, 0, 12));
        this.camera.setCameraLookAt(new Vector3(0, 0, 0));

        loadingGear = new Model3DVBO();
        loadingGear.loadFromOBJ(context, "gear");
        loadingGear.loadGLTexture(context, R.drawable.white);
        loadingGear.scale(new Vector3(1.6, 1.6, 1.6));
        loadingGear.move(new Vector3(0, 4, 0));
        loadingGear.rotate(new Vector3(0, 0, 0));
        loadingGear.setColor(255, 76, 59,255);

        loadingText = new Model3DVBO();
        loadingText.loadFromOBJ(context, "loading");
        loadingText.loadGLTexture(context, R.drawable.white);
        loadingText.scale(new Vector3(2.7, 2.7, 2.7));
        loadingText.move(new Vector3(0, -7, 0));
        loadingText.rotate(new Vector3(0, 0, 0));
        loadingText.setColor(0, 114, 187,255);

       // LoadingThread loadingThread = new LoadingThread();

        selectedHeads = new Model3DVBO();
        heads = new Model3DVBO[3];

        heads[0] = new Model3DVBO();
        heads[0].setName("nastro");
        heads[0].scale(new Vector3(2, 2, 2));
        heads[0].move(new Vector3(-10, 0, 0));
        heads[0].rotate(new Vector3(90, 0, 0));
        //loadingThread.loadObject(new LoadingInfo(heads[0], "mobius", R.drawable.mobius));
        Thread thread0 = new Thread(){
            public void run(){
                heads[0].loadFromOBJThreaded(context, "mobius");
                heads[0].saveBitmap(context, R.drawable.mobius);
            }
        };

        thread0.start();

        heads[1] = new Model3DVBO();
        heads[1].setName("pigna");
        heads[1].scale(new Vector3(0.9, 0.9, 0.9));
        heads[1].move(new Vector3(0, 0, 0));
        heads[1].rotate(new Vector3(90, 0, 0));
        //loadingThread.loadObject(new LoadingInfo(heads[1], "pigna", R.drawable.tex));
        Thread thread1 = new Thread(){
            public void run(){
                heads[1].loadFromOBJThreaded(context, "pigna");
                heads[1].saveBitmap(context, R.drawable.tex);
            }
        };

        thread1.start();

        heads[2] = new Model3DVBO();
        heads[2].setName("bottiglia");
        heads[2].scale(new Vector3(1.5, 1.5, 1.5));
        heads[2].move(new Vector3(10, 0, 0));
        heads[2].rotate(new Vector3(90, 0, 0));
        //loadingThread.loadObject(new LoadingInfo(heads[2], "kleinbottle", R.drawable.kleinbottle));
        Thread thread2 = new Thread(){
            public void run(){
                heads[2].loadFromOBJThreaded(context, "kleinbottle");
                heads[2].saveBitmap(context,  R.drawable.kleinbottle);
            }
        };

        thread2.start();
        //loadingThread.start();
    }

    public void update() {
        stateTime =  (float)System.nanoTime()/10000000f - startTime;

        switch (STATE) {

            case LOADING:
                loadingGear.rotate(new Vector3(loadingGear.getRotation(0), loadingGear.getRotation(1), stateTime));
                boolean flag=true;
                for (i = 0; i < heads.length; i++) {
                    if(heads[i].state()== Model3D.OBJECT_STATUS.REQUEST_LOAD_TEXTURE)
                    {
                        // lo scatto eventuale dell'ingranaggio è dovuto a queste chiamate
                        // che vanno eseguite nel mainThread come le funzioni di draw OpenGL
                        Log.d("DEBUG","LOADING"+i);
                        heads[i].loadObjData();
                        heads[i].loadFromSavedBitmap();
                    }
                    else if(heads[i].state()!= Model3D.OBJECT_STATUS.COMPLETE)
                        flag=false;
                }
                if(flag)
                    changeState(STATUS.DRAWING);
                break;

            //the texture must be loaded on the GLThread not from some others thread
            case APPLYING_TEXTURE:
                /*for (i = 0; i < heads.length; i++) {
                    heads[i].loadObjData();
                    heads[i].loadFromSavedBitmap();
                }
                changeState(STATUS.DRAWING);*/
                break;

            case DRAWING:
                break;
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0, 1.0f, 1.0f, 1.0f);

        update();

        switch (STATE) {

            case LOADING:
                loadingGear.draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                loadingText.draw(camera.getViewMatrix(), camera.getProjectionMatrix());
                break;

            //the texture must be loaded on the GLThread not from some others thread
            case APPLYING_TEXTURE:
                //for (Model3D head : heads) head.loadFromSavedBitmap();
                //changeState(STATUS.DRAWING);
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
        startTime = System.nanoTime()/10000000f;
        switch (status) {

            case LOADING:
                break;

            case APPLYING_TEXTURE:
                break;

            case DRAWING:
                break;
        }
    }
/*
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
*/
    /*
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
            head.loadFromOBJThreaded(context, objFile);
            head.saveBitmap(context, texture);
        }
    }
*/

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
    float scaleZoom;
    float startZoomDist;

    public void zoom(Vector3 punto1, Vector3 punto2)
    {
        //Log.d("debug", "tocco1 " + punto1.toString());
        //Log.d("debug", "tocco2 " + punto2.toString());
        //Log.d("debug", "########################");

        //acquisisco il punto medio
        if (STATE == STATUS.DRAWING) {
            Vector3 mid_point = new Vector3(0, 0, 0);
            mid_point.x((punto2.x() + punto1.x()) / 2.0f);
            mid_point.y((punto2.y() + punto1.y()) / 2.0f);
//            Log.d("debug", heads[1].toString()+ "\t" + heads[2].toString()+ "\t" + heads[3].toString() );
            Log.d("debug", "**************************");
            Log.d("debug", mid_point.x() + "");
            if (mid_point.x() / camera.getScreenWidth() < 0.33f) {
                selectedHeads = heads[0];
            }
            else if (mid_point.x() / camera.getScreenWidth() < 0.66f)
                selectedHeads = heads[1];
            else
                selectedHeads = heads[2];

            float dist = (float) Math.sqrt((Math.pow((punto1.y() - punto2.y()), 2) + Math.pow((punto1.x() - punto2.x()), 2)));
            dist = dist / camera.getDiagonalLength();

            if(scaleZoom < 0)
            {
                scaleZoom = selectedHeads.getGlobalScale();
                //scaleZoom *= dist;
                startZoomDist = dist;
            }
            selectedHeads.setGlobalScale(scaleZoom - (startZoomDist-dist)*9);
        }
    }
    //move the selected object
    public void touchMove(Vector3 screenCoords) {
        scaleZoom = -1;
        startZoomDist = -1;
        if (STATE == STATUS.DRAWING /* && touchDownCoords!=null*/) {
            //this is BEFORE a bit of thinking

            /*//get the angle between the touchDown and the current position tapped on the screen
            float angle = (float) Math.atan2((screenCoords.y() - touchDownCoords.y()), (screenCoords.x() - touchDownCoords.x()));

            //distance of actual touch and first touch down
            float dist = (float) Math.sqrt((Math.pow((screenCoords.y() - touchDownCoords.y()), 2) + Math.pow((screenCoords.x() - touchDownCoords.x()), 2)));

            //time to normalize the distance based on the screen diagonal
            dist = dist / camera.getDiagonalLength();

            //angle for every single axis
            //magic number to decide how much rotation to give
            Vector3 angleVector = new Vector3(Math.sin(angle) * ANGLE_MAGNITUDE * dist, Math.cos(angle) * ANGLE_MAGNITUDE * dist, 0);

            //selectedHeads.rotate(new Vector3(angleVector.x(), 0, -angleVector.y()).add(startingRotation));
            selectedHeads.addRotate(new Vector3(angleVector.x(), 0, -angleVector.y()).add(startingRotation));
            touchDownCoords=screenCoords;*/

            //this is AFTER a bit of thinking
            Vector3 vector = new Vector3(
                    (screenCoords.y()-touchDownCoords.y())/camera.getScreenWidth() * ANGLE_MAGNITUDE,
                    0,
                    -(screenCoords.x()-touchDownCoords.x())/camera.getScreenHeigth() * ANGLE_MAGNITUDE);

            selectedHeads.addRotate(vector);
            touchDownCoords=screenCoords;
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