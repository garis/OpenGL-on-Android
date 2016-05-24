package theugateam.progetto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import theugateam.progetto.Utils.OBJParser;
import theugateam.progetto.Utils.Vector3;

/**
 * A class for displaying 3D objects from an .OBJ file using OpenGL ES 2.0.
 */
public class Model3D {


    public static enum OBJECT_STATUS {
        INITIALIZED, LOADING_OBJ, REQUEST_LOAD_OBJ, LOADING_TEXTURE, REQUEST_LOAD_TEXTURE, COMPLETE
    }
    protected String mName= "";
    protected OBJECT_STATUS objectState;
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    //Definisce il codice degli shader che verranno usati dal processore grafici
    private final String vertexShaderCode =
            /* This matrix member variable provides a hook to manipulate
             the coordinates of the objects that use this vertex shader*/
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
            /*texture location as input*/
                    "attribute vec2 a_texCoord;" +
            /*...and gives it to the fragment shader */
                    "varying vec2 v_texCoord;" +
                    "void main() {" +
                /*
                The matrix must be included as a modifier of gl_Position.
                Note that the uMVPMatrix factor *must be first* in order
                for the matrix multiplication product to be correct.
                */
                    "gl_Position = uMVPMatrix * vPosition;" +
                    "v_texCoord = a_texCoord;" +
                    "}";
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec2 v_texCoord;" +
                    "uniform sampler2D s_texture;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "gl_FragColor = vColor * texture2D( s_texture, v_texCoord );" +
                    "}";
    private final int vertexStride = COORDS_PER_VERTEX * 0; //n° byte totale al caricamento riempirà la varibile con il valore corretto
    private final int UV_COORDS_PER_VERTEX = 2;
    float color[] = {1.0f, 1.0f, 1.0f, 1.0f};
    Bitmap bitmap;
    //buffers
    protected FloatBuffer vertexBuffer;
    protected FloatBuffer uvBuffer;
    protected IntBuffer drawListBuffer;
    //GPU pointers
    protected int mProgram = 0;
    protected int mPositionHandle = 0;
    protected int mMVPMatrixHandle = 0;
    protected int colorUniformHandle = 0;
    protected int textureCoordinateHandle = 0;
    protected int textureUniformHandle = 0;
    protected int texture[] = {0};
    //geometry with defaults values in it
    //private float geometryCoords[]; = {
           /* -1.0f, 1.0f, 0.0f,   // top left
            -1.0f, -1.0f, 0.0f,   // bottom left
            1.0f, -1.0f, 0.0f,   // bottom right
            1.0f, 1.0f, 0.0f}; // top right*/
    //private float[] uvCoords; = {
     /*       0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };*/
    //private int drawOrder[];// = {0, 1, 2, 0, 2, 3}; // order to draw vertices
    protected int drawListBufferCapacity;
    protected float _x;
    protected float _y;
    protected float _z;
    protected float _scaleX;
    protected float _scaleY;
    protected float _scaleZ;
    protected float _angleX;
    protected float _angleY;
    protected float _angleZ;

    public Model3D() {
        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables

        _x = 0;
        _y = 0;
        _z = 0;
        _scaleX = 1;
        _scaleY = 1;
        _scaleZ = 1;
        _angleX = 0;
        _angleY = 0;
        _angleZ = 0;

        updatePointerVariables();
    }

    protected void updatePointerVariables() {
        MyGLRenderer.checkGlError("start updatePointerVariables");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");    // get handle to vertex shader's vPosition member
        colorUniformHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        textureUniformHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");
        textureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");//get handle to shape's transformation matrix

        MyGLRenderer.checkGlError("end updatePointerVariables");
    }
    // trasferisce la Bitmap della Texture da file a memoria
    public void saveBitmap(Context context, int id) {
        bitmap = BitmapFactory.decodeResource(context.getResources(), id);

        objectState=OBJECT_STATUS.REQUEST_LOAD_TEXTURE;
    }
    // copia il bitmap dalla emoria di Android a quella di OpenGL
    public void loadFromSavedBitmap() {
        if (bitmap != null) {
            // generate one texture pointer
            GLES20.glGenTextures(1, texture, 0);
            // ...and bind it to our array
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            //load texture
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            // Clean up from Android memory
            bitmap.recycle();
        }
        MyGLRenderer.checkGlError("loadFromSavedBitmap");

        objectState=OBJECT_STATUS.COMPLETE;
    }

    //use for lightweight task
    public void loadGLTexture(Context context, int id) {
        // loading texture
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);

        // generate one texture pointer
        GLES20.glGenTextures(1, texture, 0);
        // ...and bind it to our array
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        //load texture

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        // Clean up
        bitmap.recycle();
        MyGLRenderer.checkGlError("loadGLTexture");

        objectState=OBJECT_STATUS.COMPLETE;
    }

    protected void updateGeometryAndUVs(float[] GeometryCoords, float[] UVCoords, int[] DrawOrder) {

        MyGLRenderer.checkGlError("updateGeometryAndUVs");
        //geometryCoords = GeometryCoords;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                GeometryCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(GeometryCoords);
        vertexBuffer.position(0);

        //drawOrder = DrawOrder;
        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per int)
                DrawOrder.length * 4);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asIntBuffer();
        drawListBuffer.put(DrawOrder);
        drawListBuffer.position(0);

        drawListBufferCapacity = drawListBuffer.capacity();

        //uvCoords = UVCoords;

        // The texture buffer
        bb = ByteBuffer.allocateDirect(UVCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(UVCoords);
        uvBuffer.position(0);

        MyGLRenderer.checkGlError("end updateGeometryAndUVs");

        objectState=OBJECT_STATUS.LOADING_TEXTURE;
    }

    public void draw(float[] mViewMatrix, float[] mProjectionMatrix) {

        float[] model = new float[16];
        float[] temp = new float[16];
        float[] mvpMatrix = new float[16];

        Matrix.setIdentityM(model, 0); // initialize to identity matrix

        Matrix.translateM(model, 0, _x, _y, _z);

        Matrix.scaleM(model, 0, _scaleX, _scaleY, _scaleZ);

        Matrix.rotateM(model, 0, _angleX, 1, 0, 0);
        Matrix.rotateM(model, 0, _angleY, 0, 1, 0);
        Matrix.rotateM(model, 0, _angleZ, 0, 0, 1);

        Matrix.multiplyMM(temp, 0, mViewMatrix, 0, model, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, temp, 0);

        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        GLES20.glEnableVertexAttribArray(mPositionHandle);    //Enable a handle to the triangle vertices

        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);    // Prepare the triangle coordinate data

        GLES20.glUniform4f(colorUniformHandle, color[0], color[1], color[2], color[3]);

        GLES20.glVertexAttribPointer(textureCoordinateHandle, UV_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, uvBuffer);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glUniform1i(textureUniformHandle, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);//Apply the projection and view transformation

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawListBufferCapacity, GLES20.GL_UNSIGNED_INT, drawListBuffer);//Draw

        GLES20.glDisableVertexAttribArray(mPositionHandle);// Disable vertex array
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);// Disable vertex array
    }

    float[] SquareCoords;
    float[] UVCoords;
    int[] DrawOrder;

    public void loadFromOBJ(Context context, String filename) {
        OBJParser objparser = new OBJParser();
        objparser.loadFromOBJ(context, filename);
        SquareCoords = objparser.getVertices();
        UVCoords = objparser.getUVVertices();
        DrawOrder = objparser.getOrder();
        this.updateGeometryAndUVs(SquareCoords, UVCoords, DrawOrder);
        SquareCoords=new float[0];
        UVCoords=new  float[0];
        DrawOrder=new int[0];

        objectState=OBJECT_STATUS.COMPLETE;
    }
    public void loadFromOBJThreaded(Context context, String filename) {
        OBJParser objparser = new OBJParser();
        objparser.loadFromOBJ(context, filename);
        SquareCoords = objparser.getVertices();
        UVCoords = objparser.getUVVertices();
        DrawOrder = objparser.getOrder();

        objectState=OBJECT_STATUS.REQUEST_LOAD_OBJ;
    }

    public void loadObjData(){
        this.updateGeometryAndUVs(SquareCoords, UVCoords, DrawOrder);
        SquareCoords=new float[0];
        UVCoords=new  float[0];
        DrawOrder=new int[0];

        objectState=OBJECT_STATUS.LOADING_TEXTURE;
    }

    public void scale(Vector3 scale) {
        _scaleX = (float) scale.x();
        _scaleY = (float) scale.y();
        _scaleZ = (float) scale.z();
    }

    public float getGlobalScale()
    {
        return _scaleX;
    }

    public void setGlobalScale(float newScale)
    {
        scale(new Vector3(newScale,newScale,newScale));
    }

    public void move(Vector3 position) {
        _x = (float) position.x();
        _y = (float) position.y();
        _z = (float) position.z();
    }

    //in degree
    public void rotate(Vector3 rotationVector) {
        _angleX = (float) rotationVector.x();
        _angleY = (float) rotationVector.y();
        _angleZ = (float) rotationVector.z();
    }

    public float getRotation(int n) {
        switch (n) {
            case 0:
                return _angleX;
            case 1:
                return _angleY;
            case 2:
                return _angleZ;
        }
        return _x;
    }

    // red, green, blue and alpha from 0 to 255
    public void setColor(int r, int g, int b, int a) {
        color[0] = r / 255f;
        color[1] = g / 255f;
        color[2] = b / 255f;
        color[3] = a / 255f;
    }

    public OBJECT_STATUS state(){
        return objectState;
    }

    public void setName(String Name)
    {
        mName = Name;
    }

    @Override
    public String toString()
    {
        return mName;
    }

}
