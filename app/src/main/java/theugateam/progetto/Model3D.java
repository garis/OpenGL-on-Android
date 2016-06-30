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

    // region TRACCIA_CARICAMENTO

    // 2==> pronto per passare i dati ad OpenGL
    // 4=LOADING_COMPLETED==> tutto caricato
    // altri valori ==> operazioni in corso
    protected int resourcesLoaded;

    public static final int LOADING_COMPLETED = 4;

    // endregion

    // region BUFFER_VETTORI_BITMAP_PER_DATI

    protected int drawListBufferCapacity;

    // number of coordinates per vertex in this array
    private final int COORDS_PER_VERTEX = 3;
    private final int vertexStride = COORDS_PER_VERTEX * 0; // n° byte totale al caricamento riempirà la varibile con il valore corretto
    private final int UV_COORDS_PER_VERTEX = 2;
    float color[] = {1.0f, 1.0f, 1.0f, 1.0f};
    Bitmap bitmap;

    // buffers
    protected FloatBuffer vertexBuffer;
    protected FloatBuffer uvBuffer;
    protected IntBuffer drawListBuffer;

    private float[] SquareCoords;
    private float[] UVCoords;
    private int[] DrawOrder;
    // endregion

    // region SHADER
    protected int mProgram = 0;
    protected int mPositionHandle = 0;
    protected int mMVPMatrixHandle = 0;
    protected int colorUniformHandle = 0;
    protected int textureCoordinateHandle = 0;
    protected int textureUniformHandle = 0;
    protected int texture[] = {0};
    // endregion

    // region MANIPOLAZIONE_OGGETTO

    private Vector3 position;
    private Vector3 scale;
    private Vector3 angle;

    protected float[] modelMatrix = new float[16];
    protected float[] tempMatrix = new float[16];
    protected float[] mvpMatrix = new float[16];

    private float[] lastRotation = new float[16];
    protected float[] accumulatedRotation = new float[16];
    protected float[] rotationMatrix = new float[16];

    // endregion

    protected String mName = "";

    public Model3D(Context context) {

        // preleva da file e compila il vertex shader e il fragment shader
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER, MyGLRenderer.readTextFileFromResource(context, R.raw.vertexshader));
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER, MyGLRenderer.readTextFileFromResource(context, R.raw.fragmentshader));

        // creiamo un nuvo programma OpenGL
        mProgram = GLES20.glCreateProgram();
        // gli attacchiamo un vertex shader....
        GLES20.glAttachShader(mProgram, vertexShader);
        // ...e un fragment shader
        GLES20.glAttachShader(mProgram, fragmentShader);
        // e infine rendiamo il programma usabile da OpenGL
        GLES20.glLinkProgram(mProgram);

        resourcesLoaded = 0;

        position = new Vector3(0, 0, 0);
        scale = new Vector3(1, 1, 1);
        angle = new Vector3(0, 0, 0);

        Matrix.setIdentityM(accumulatedRotation, 0);
        Matrix.setIdentityM(modelMatrix, 0);

        updatePointerVariables();
    }

    // setta i puntatori alle variabili d'interesse contenute negli shader
    protected void updatePointerVariables() {
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        colorUniformHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        textureUniformHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");
        textureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    }

    // carica i vettori xcontenenti tutta la geometria di un modello dentro a delle strutture dati usabili da OpenGL
    protected void updateGeometryAndUVs(float[] GeometryCoords, float[] UVCoords, int[] DrawOrder) {

        MyGLRenderer.checkGlError("updateGeometryAndUVs");

        // carica il buffer per i vertici
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# di punti * 4 bytes per float)
                GeometryCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(GeometryCoords);
        vertexBuffer.position(0);

        // carica il buffer per l'ordine con cui i vertici devono essere disegnati
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# di elementi * 4 bytes per int)
                DrawOrder.length * 4);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asIntBuffer();
        drawListBuffer.put(DrawOrder);
        drawListBuffer.position(0);

        drawListBufferCapacity = drawListBuffer.capacity();

        // carica il buffer che conterrà i punti che descrivono come deve essere disegnata la texture
        bb = ByteBuffer.allocateDirect(UVCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(UVCoords);
        uvBuffer.position(0);

        resourcesLoaded++;
    }

    protected void compute_mvpMatrix(float[] mViewMatrix, float[] mProjectionMatrix) {
        // preparativi per generare la matrice di model-view-projection....
        Matrix.multiplyMM(tempMatrix, 0, mViewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, tempMatrix, 0);

        // copia mvpMatrix in tempMatrix
        System.arraycopy(mvpMatrix, 0, tempMatrix, 0, 16);

        // ... moltiplica accumulatedRotation con tempMatrix per dar vita a mvpMatrix
        Matrix.multiplyMM(mvpMatrix, 0, tempMatrix, 0, accumulatedRotation, 0);
    }

    public void draw(float[] mViewMatrix, float[] mProjectionMatrix) {

        compute_mvpMatrix(mViewMatrix, mProjectionMatrix);

        // specifica quale programma OpenGL usare in questa draw
        GLES20.glUseProgram(mProgram);

        // preparazione per il passaggio delle coordinate dei vertici
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // manda a OpenGL i vertici
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        // istruisce il fragment shader riguardo al colore da applicare
        GLES20.glUniform4f(colorUniformHandle, color[0], color[1], color[2], color[3]);

        // preparazione per il passaggio delle coordinate della texture
        GLES20.glVertexAttribPointer(textureCoordinateHandle, UV_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, uvBuffer);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);

        // attiva l'uso delle texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // attiva la texture in texture[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        // comunica al fragment shader di usare la texture precedentemente attivata
        GLES20.glUniform1i(textureUniformHandle, 0);

        // manda al vertex shader la matrice di model-view-projection
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // disegna l'oggetto a triangoli nell'ordine specificato da drawListBuffer
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawListBufferCapacity, GLES20.GL_UNSIGNED_INT, drawListBuffer);

        // disabilita gli array di posizone dei vertici e di texture precedentemente attivati
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
    }

    // region TEXTURE

    // metodo possibilmente chiamato da un thread ausiliario o comunque non dal main thread di OpenGL
    // trasferisce la Bitmap della Texture in memoria prelevandola da un file
    public void saveBitmap(Context context, int id) {
        bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        resourcesLoaded++;
    }

    // copia il bitmap dalla memoria dell'app a quella di OpenGL
    public void loadFromSavedBitmap() {
        if (bitmap != null) {
            // genera un nuovo puntatore ad una texture
            GLES20.glGenTextures(1, texture, 0);
            // lega questo puntatore ad una variabile disponibile all'app
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);

            // setta alcuni parametri che diranno a OpenGL come trattare la texture
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            // caricamento della texture
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            // libera memoria
            bitmap.recycle();
        }
        resourcesLoaded++;
    }

    // per oggetti non troppo complessi si può usare questo mwtodo per caricare delle texture
    // senza pasare per thread diverso da quello di OpenGL
    public void loadGLTexture(Context context, int id) {
        saveBitmap(context, id);
        loadFromSavedBitmap();
    }

    // endregion

    // region VERTICI

    // carica la geometria prendendola da un .obj e operando dal thread principale
    public void loadFromOBJ(Context context, String filename) {
        loadFromOBJThreaded(context, filename);
        loadObjData();
    }

    // metodo possibilmente chiamato da un thread ausiliario o comunque non dal main thread di OpenGL
    // carica la geometria prendendola da un .obj e usando
    public void loadFromOBJThreaded(Context context, String filename) {
        OBJParser objparser = new OBJParser();
        objparser.loadFromOBJ(context, filename);
        SquareCoords = objparser.getVertices();
        UVCoords = objparser.getUVVertices();
        DrawOrder = objparser.getOrder();

        resourcesLoaded++;
    }

    // carica la geometria in strutture dati usabili da OpenGL
    public void loadObjData() {
        this.updateGeometryAndUVs(SquareCoords, UVCoords, DrawOrder);
        SquareCoords = new float[0];
        UVCoords = new float[0];
        DrawOrder = new int[0];
    }

    // endregion

    // region MANIPOLAZIONE_OGGETTO

    // muove scala e ruota l'oggetto
    public void moveScaleRotate(Vector3 position, Vector3 scale, Vector3 rotationVector) {
        Matrix.setIdentityM(modelMatrix, 0);
        move(position);
        scale(scale);
        rotate(rotationVector);
    }

    // azzera la matrice di rotazione e gli applica una rotazione
    public void resetRotation(Vector3 rotationVector) {
        // setta la matrice accumulatedRotation come matrice identità...
        Matrix.setIdentityM(accumulatedRotation, 0);
        // ...gli applica la rotazione...
        Matrix.rotateM(accumulatedRotation, 0, (float) rotationVector.x(), 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(accumulatedRotation, 0, (float) rotationVector.y(), 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(accumulatedRotation, 0, (float) rotationVector.z(), 0.0f, 0.0f, 1.0f);

        // come ultima cosa tiene traccia degli angoli di rotazione sui 3 assi
        angle.x(rotationVector.x());
        angle.y(rotationVector.y());
        angle.z(rotationVector.z());
    }

    // ruota l'oggetto
    // angoli in gradi
    public void rotate(Vector3 rotationVector) {
        // setta la matrice lastRotation come matrice identità...
        Matrix.setIdentityM(lastRotation, 0);
        // ...gli applica la rotazione...
        Matrix.rotateM(lastRotation, 0, (float) rotationVector.x(), 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(lastRotation, 0, (float) rotationVector.y(), 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(lastRotation, 0, (float) rotationVector.z(), 0.0f, 0.0f, 1.0f);

        // ...applica questa nuova rotazione alla matrice di rotazione complessiva...
        Matrix.multiplyMM(rotationMatrix, 0, lastRotation, 0, accumulatedRotation, 0);

        // ...e salva questa nuova matrice di rotazione in accumulatedRotation che verrà usata nel rendering
        System.arraycopy(rotationMatrix, 0, accumulatedRotation, 0, 16);

        // come ultima cosa tiene traccia degli angoli di rotazione sui 3 assi
        angle.x(angle.x() + rotationVector.x());
        angle.y(angle.y() + rotationVector.y());
        angle.z(angle.z() + rotationVector.z());
    }

    // scala l'oggetto
    protected void scale(Vector3 newScale) {
        Matrix.setIdentityM(modelMatrix, 0);

        move(position);

        scale.x(newScale.x());
        scale.y(newScale.y());
        scale.z(newScale.z());

        Matrix.scaleM(modelMatrix, 0, (float) scale.x(), (float) scale.y(), (float) scale.z());
        rotate(new Vector3(0, 0, 0));
    }

    // setta la stessa scala su tutti gli assi
    public void setGlobalScale(float newScale) {
        scale(new Vector3(newScale, newScale, newScale));
    }

    // muove l'oggetto
    private void move(Vector3 newPosition) {
        position.x(newPosition.x());
        position.y(newPosition.y());
        position.z(newPosition.z());

        Matrix.translateM(modelMatrix, 0, (float) position.x(), (float) position.y(), (float) position.z());
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

    public Vector3 getRotation() {
        return new Vector3(angle.x(), angle.y(), angle.z());
    }


    // ritorna la scala globale
    public float getGlobalScale() {
        // per convenzione se la scala deve essere su tutti e tre gli assi
        // allora si considera scale.x() per tutti gli assi
        return (float) scale.x();
    }

    public Vector3 getScale() {
        return new Vector3(scale.x(), scale.y(), scale.z());
    }

    // endregion

    // region VARIE

    // red, green, blue and alpha from 0 to 255
    public void setColor(int r, int g, int b, int a) {
        color[0] = r / 255f;
        color[1] = g / 255f;
        color[2] = b / 255f;
        color[3] = a / 255f;
    }

    public int loadingState() {
        return resourcesLoaded;
    }

    public void setName(String Name) {
        mName = Name;
    }

    @Override
    public String toString() {
        return mName;
    }

    // endregion
}
