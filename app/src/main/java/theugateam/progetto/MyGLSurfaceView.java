package theugateam.progetto;

import android.content.Context;
import android.opengl.GLSurfaceView;
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

    //vedi: http://android-developers.blogspot.it/2010/06/making-sense-of-multitouch.html?m=1
    // e https://developer.android.com/training/gestures/scale.html
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mDoubleTapDetector;

    private float mScaleFactor;

    public MyGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
        mScaleFactor = 1;
        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer();
        mRenderer.initialize(context);
        setRenderer(mRenderer);

        // Render continuo per poter usare l'ingranaggio di caricamento
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        mDoubleTapDetector = new GestureDetector(context, new GestureDoubleTap());
    }

    private float startingScale = 1;

    private boolean scaleDetectorWasInProgress = false;

    public int mActivePointerId = MotionEvent.INVALID_POINTER_ID;

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if(mRenderer.getState()== MyGLRenderer.STATUS.DRAWING) {

            // chiamo ScaleGestureDetector per ispezionare l'evento appena accaduto
            mScaleDetector.onTouchEvent(e);
            mDoubleTapDetector.onTouchEvent(e);

            //se c'è un solo tocco e non stai zoomando, interpretarlo come rotazione
            if (!mScaleDetector.isInProgress() && e.getPointerCount() == 1) {

                //se il tocco dovesse passare da modalità "ruota" a modalità "scala"
                //il metodo che scala troverà un valore inziale su cui basarla
                scaleDetectorWasInProgress = false;
                mScaleFactor = 1;

                int pointerIndex;
                switch (e.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        //se l'utente ha cambiato dito o se il puntatore relativo al tocco è invalido
                        if ((e.getPointerId(0) != mActivePointerId)) {// || (mActivePointerId == MotionEvent.INVALID_POINTER_ID)) {
                            //troviamo un nuovo punto di riferimento iniziale su cui basare la rotazione
                            mActivePointerId = e.getPointerId(0);
                            pointerIndex = e.findPointerIndex(mActivePointerId);
                            mRenderer.selectHeadRotation(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                        } else if (!doubleTapOccoured) {
                            //ruotiamo solo se non è stato rilevato un double tap (che implica uno zoom)
                            pointerIndex = e.findPointerIndex(mActivePointerId);
                            mRenderer.rotateHead(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        doubleTapOccoured = false;
                        mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                        break;
                }
            } else
            //logica per lo zoom
            {
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                //se è il primo tocco di una sequenza di tocca per scalare un oggetto
                if (!scaleDetectorWasInProgress) {
                    //se si sta scalando l'oggetto usando 2 dita
                    if (e.getPointerCount() >= 2) {
                        startingScale = mRenderer.selectedForScale(new Vector3(e.getX(), e.getY(), 0),
                                new Vector3(e.getX(1), e.getY(1), 0));
                    }
                    //oppure se si sta usando una "scala a trascinamento"
                    //cioè un tap seguito da uno slide subito dopo
                    else {
                        startingScale = mRenderer.selectedForScale(new Vector3(e.getX(), e.getY(), 0),
                                new Vector3(e.getX(0), e.getY(0), 0));
                    }
                }

                mRenderer.zoom(startingScale + (mScaleFactor - 1) * 1.5f);

                //ora facciamo in modo che la prossima volta che si entra qui, non ci si salvi una
                //nuova scala di partenza
                scaleDetectorWasInProgress = true;
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

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            //resetta l'id del pointer attivo ogni volta che si scala, così quando si ritorna alla
            //rotazione si riparte prendendo un punto di riferimento (su schermo) per effettuarla

            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            return true;
        }
    }
}
