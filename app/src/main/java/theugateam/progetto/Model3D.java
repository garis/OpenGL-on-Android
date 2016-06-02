package theugateam.progetto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

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


    public enum OBJECT_STATUS {
        INITIALIZED, LOADING_OBJ, REQUEST_LOAD_OBJ, LOADING_TEXTURE, REQUEST_LOAD_TEXTURE, COMPLETE
    }
    protected String mName= "";
    protected OBJECT_STATUS objectState;
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
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
    protected int drawListBufferCapacity;
    private Vector3 position;
    private Vector3 scale;
    private Vector3 angle;

    protected float[] modelMatrix = new float[16];
    protected float[] tempMatrix = new float[16];
    protected float[] mvpMatrix = new float[16];

    private float[] lastRotation = new float[16];

    public Model3D(Context context) {
        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER, MyGLRenderer.readTextFileFromResource(context,R.raw.vertexshader));
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,  MyGLRenderer.readTextFileFromResource(context,R.raw.fragmentshader));

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables

        position = new Vector3(0, 0, 0);
        scale = new Vector3(1, 1, 1);
        angle = new Vector3(0, 0, 0);

        Matrix.setIdentityM(accumulatedRotation, 0);
        Matrix.setIdentityM(modelMatrix, 0);


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

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                GeometryCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(GeometryCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per int)
                DrawOrder.length * 4);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asIntBuffer();
        drawListBuffer.put(DrawOrder);
        drawListBuffer.position(0);

        drawListBufferCapacity = drawListBuffer.capacity();

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

        Matrix.multiplyMM(tempMatrix, 0, mViewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, tempMatrix, 0);

        System.arraycopy(mvpMatrix, 0, tempMatrix, 0, 16);

        Matrix.multiplyMM(mvpMatrix, 0, tempMatrix, 0, accumulatedRotation, 0);

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

    public void moveScaleRotate(Vector3 position, Vector3 scale, Vector3 rotationVector) {
        Matrix.setIdentityM(modelMatrix, 0);
        move(position);
        scale(scale);
        rotate(rotationVector);
    }

    private void scale(Vector3 newScale) {
        Matrix.setIdentityM(modelMatrix, 0);

        move(position);

        scale.x(newScale.x());
        scale.y(newScale.y());
        scale.z(newScale.z());

        Matrix.scaleM(modelMatrix, 0, (float) scale.x(), (float) scale.y(), (float) scale.z());
        rotate(new Vector3(0, 0, 0));
    }

    public float getGlobalScale()
    {
        return (float) scale.x();
    }

    public void setGlobalScale(float newScale)
    {
        scale(new Vector3(newScale,newScale,newScale));
    }

    private void move(Vector3 newPosition) {
        position.x(newPosition.x());
        position.y(newPosition.y());
        position.z(newPosition.z());

        Matrix.translateM(modelMatrix, 0, (float) position.x(), (float) position.y(), (float) position.z());
    }

    protected float[] accumulatedRotation = new float[16];
    protected float[] rotationMatrix = new float[16];

    //in degree
    public void rotate(Vector3 rotationVector) {
        Matrix.setIdentityM(lastRotation, 0);
        Matrix.rotateM(lastRotation, 0, (float) rotationVector.x(), 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(lastRotation, 0, (float) rotationVector.y(), 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(lastRotation, 0, (float) rotationVector.z(), 0.0f, 0.0f, 1.0f);

        Matrix.multiplyMM(rotationMatrix, 0, lastRotation, 0, accumulatedRotation, 0);

        System.arraycopy(rotationMatrix, 0, accumulatedRotation, 0, 16);

        angle.x(angle.x() + rotationVector.x());
        angle.y(angle.y() + rotationVector.y());
        angle.z(angle.x() + rotationVector.z());
    }

    public float getRotation(int n) {
        switch (n) {
            case 0:
                return (float) angle.x();
            case 1:
                return (float) angle.y();
            case 2:
                return (float) angle.z();
        }
        return -1;
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
