package theugateam.progetto;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import theugateam.progetto.Utils.Vector3;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;

    // rilevatore di gesture relative alla scala
    private ScaleGestureDetector mScaleDetector;
    // rilevatore di gesture relative ad un doppio tap
    // usato per impedire la rotazione di un oggetto subito prima che avvenga uno zoom eseguito con un dto solo
    private GestureDetector mDoubleTapDetector;

    // scala rilevata da mScaleDetector
    private float mDeltaScaleFactor;

    private boolean scaleDetectorWasInProgress = false;
    public int mActivePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean resetted = false;

    public MyGLSurfaceView(Context context) {
        super(context);

        // Crea un nuovo contesto OpenGL ES 2.0
        setEGLContextClientVersion(2);

        mDeltaScaleFactor = 1;

        // Setta il render per disegnare nella GLSurfaceView
        mRenderer = new MyGLRenderer();
        mRenderer.initialize(context);
        setRenderer(mRenderer);

        // Render continuo per poter usare l'ingranaggio di caricamento
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        // disabilità lo zoom con un singolo dito
        mScaleDetector.setQuickScaleEnabled(false);
        mDoubleTapDetector = new GestureDetector(context, new GestureDoubleTap());
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if (mRenderer.getState() == MyGLRenderer.STATUS.DRAWING) {

            // chiamo ScaleGestureDetector per ispezionare l'evento appena accaduto
            mScaleDetector.onTouchEvent(e);
            // chiamo il rilevatore di doppi tap per ispezionare l'evento appena accaduto
            mDoubleTapDetector.onTouchEvent(e);

            // se è avventuo un doppio tap e non ho ancora resettato l'oggetto allora lo resetto
            if (doubleTapOccoured && !resetted) {
                int pointerIndex = e.findPointerIndex((e.getPointerId(0)));
                mRenderer.selectHeadSingleTouch(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                mRenderer.resetHead();
                resetted = true;
            }
            // se c'è un solo tocco e non sto zoomando, interpretarlo come rotazione
            else if (!mScaleDetector.isInProgress() && e.getPointerCount() == 1) {
                // se il tocco dovesse passare da modalità "ruota" a modalità "zoom"
                // il metodo che scala troverà un valore inziale su cui basarla
                scaleDetectorWasInProgress = false;

                int pointerIndex;
                switch (e.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        // se l'utente ha cambiato dito o se il puntatore relativo al tocco è invalido
                        if ((e.getPointerId(0) != mActivePointerId) || (mActivePointerId == MotionEvent.INVALID_POINTER_ID)) {
                            // troviamo un nuovo punto di riferimento iniziale su cui basare la rotazione
                            mActivePointerId = e.getPointerId(0);
                            pointerIndex = e.findPointerIndex(mActivePointerId);
                            mRenderer.selectHeadSingleTouch(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                            //se però l'oggetto è in animazione annulla il puntatore appena trovato
                            //in modo da rimandare l'operazione di rotazione nel prossimo touch event che accadrà
                            if (mRenderer.isSelectedHeadSingleTouchAnimating()) {
                                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                            }
                        } else {
                            // ruotiamo
                            pointerIndex = e.findPointerIndex(mActivePointerId);
                            mRenderer.rotateHead(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // annulliamo la variabile che indica se è avvenuto un doppio tap...
                        doubleTapOccoured = false;
                        // ...e permettiamo un nuovo possibile reset di un oggetto
                        resetted = false;
                        mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                        break;
                }
            } else
            // logica per lo zoom
            {
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                // se è il primo tocco di una sequenza di tocchi per scalare un oggetto
                // oppure l'oggeto si sta ancora animando
                if (mRenderer.selectedForScaleIsAnimating() || !scaleDetectorWasInProgress) {
                    // decidiamo quale oggetto è interessato dalla scala
                    mRenderer.selectedForScale(new Vector3(mScaleDetector.getFocusX(), mScaleDetector.getFocusY(), 0));
                    // e facciamo in modo che la prossima volta che si entra
                    // nel touch event per la scala si scali
                    scaleDetectorWasInProgress = true;
                } else if (mScaleDetector.isInProgress()) {
                    mRenderer.zoom(mRenderer.getSelectedScale() + mDeltaScaleFactor);
                }
            }
        }
        return true;
    }

    private boolean doubleTapOccoured = false;

    public class GestureDoubleTap extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            doubleTapOccoured = true;
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final int scaleFactor = 4;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // resetta l'id del pointer attivo ogni volta che si scala, così quando si ritorna alla
            // rotazione si riparte prendendo un punto di riferimento (su schermo) per effettuarla
            mActivePointerId = MotionEvent.INVALID_POINTER_ID;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mDeltaScaleFactor = (detector.getScaleFactor() - 1) * scaleFactor;
            return true;
        }
    }
}