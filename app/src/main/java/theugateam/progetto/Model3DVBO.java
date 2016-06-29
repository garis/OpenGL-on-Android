package theugateam.progetto;

import android.content.Context;
import android.opengl.GLES20;

/**
 * questa sovrascrittura della classe Model3D implementa i vertex buffer
 * object (VBO) invece dei buffer residenti in ram
 * permette di precaricare inizialmente i VertexBuffer in ram in modo da non
 * doverlo rifare ogni volta che Draw() viene chiamata
 * Avendo un modello che non varia nel tempo questa soluzione
 * risulta più efficiente
 *
 */
public class Model3DVBO extends Model3D {

    private int[] coordsVBO;
    private int[] uvVBO;
    private int[] indexVBO;

    public Model3DVBO(Context context){
        super(context);
    }
    @Override
    protected void updatePointerVariables() {

        // aggiorna i puntatori alle variabili degli shader
        super.updatePointerVariables();

        // puntatori a buffer nella memoria di OpenGL
        coordsVBO = new int[1];
        uvVBO = new int[1];
        indexVBO = new int[1];

        // genera i buffer, ed esegue un controllo errori
        // VBO stuffs
        GLES20.glGenBuffers(1, coordsVBO, 0);
        GLES20.glGenBuffers(1, uvVBO, 0);
        GLES20.glGenBuffers(1, indexVBO, 0);

        // variabile per avere lo stato dell'oggetto (utile in multithreading)
        resourcesLoaded = 0;
    }

    // carica la geometria tridimensionale (non disegna nulla ancora)
    @Override
    protected void updateGeometryAndUVs(float[] GeometryCoords, float[] UVCoords, int[] DrawOrder) {

        super.updateGeometryAndUVs(GeometryCoords, UVCoords, DrawOrder);

        // crea, riempi e assegna un buffer OpenGL per gestire i vertici della geometria
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, coordsVBO[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity()
                * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // crea, riempi e assegna un buffer OpenGL per gestire l'ordine con cui devono essere usati i vertici
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvVBO[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, uvBuffer.capacity()
                * 4, uvBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // crea, riempi e assegna un buffer OpenGL per gestire i vertici della texture
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVBO[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, drawListBuffer.capacity()
                * 4, drawListBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    // draw molto simile a quella del Model3D solo che usa i Vertex Buffer Object (VBO) invece dei
    // buffer di java
    @Override
    public void draw(float[] mViewMatrix, float[] mProjectionMatrix) {

        compute_mvpMatrix(mViewMatrix, mProjectionMatrix);

        // specifica quale programma OpenGL usare in questa draw
        GLES20.glUseProgram(mProgram);

        // informa attiva e usa il buffer che contiene i vertici
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, coordsVBO[0]);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        // informa attiva e usa il buffer che contiene le coordinate della texture
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvVBO[0]);
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, 0);

        // istruisce il fragment shader riguardo al colore da applicare
        GLES20.glUniform4f(colorUniformHandle, color[0], color[1], color[2], color[3]);

        // MATRIX AND TEXTURE
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // attiva l'uso delle texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // attiva la texture in texture[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        // comunica al fragment shader di usare la texture precedentemente attivata
        GLES20.glUniform1i(textureUniformHandle, 0);

        // fissa il buffer da usare per sapere l'ordine con cui disegnare i vertici
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexVBO[0]);
        // disegna l'oggetto a triangoli nell'ordine specificato dal buffer appena fissato
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawListBufferCapacity, GLES20.GL_UNSIGNED_INT, 0);

        // disabilita gli indici che specificano i vertici di geometria e texture usati dagli shader
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
    }

}
