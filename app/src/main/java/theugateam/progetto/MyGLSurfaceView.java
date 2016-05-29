package theugateam.progetto;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
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

    //see:http://android-developers.blogspot.it/2010/06/making-sense-of-multitouch.html?m=1
    private ScaleGestureDetector mScaleDetector;
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

        // Render continuosly per poter usare l'ingranaggio di caricamento
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    private float startingScale = 1;

    private boolean scaleDetectorWasInProgress = false;

    private boolean pointerUp = false;

    private int mActivePointerId=MotionEvent.INVALID_POINTER_ID;

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if(mRenderer.getState()== MyGLRenderer.STATUS.DRAWING) {
            // Let the ScaleGestureDetector inspect all events.

            mScaleDetector.onTouchEvent(e);

            if (!mScaleDetector.isInProgress() && e.getPointerCount() == 1) {
                //logic for rotation

                //this is necessary because when a new multitouch occour the scaling knows that
                //he needs the current scale of the selected object
                scaleDetectorWasInProgress = false;
                mScaleFactor = 1;

                int pointerIndex;
                switch (e.getAction()) {
                    //basically:
                    //if primary touch id != previous touch id
                    //              we need to find a new point of reference for the rotation and a new id
                    //else
                    //              we can rotate
                    case MotionEvent.ACTION_MOVE:
                        if (e.getPointerId(0) != mActivePointerId) {
                            mActivePointerId = e.getPointerId(0);
                            pointerIndex = e.findPointerIndex(mActivePointerId);
                            mRenderer.selectHeadRotation(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                        } else {
                            pointerIndex = e.findPointerIndex(mActivePointerId);
                            mRenderer.rotateHead(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                        }
                        break;
                    case MotionEvent.ACTION_DOWN:
                        mActivePointerId = e.getPointerId(0);
                        pointerIndex = e.findPointerIndex(mActivePointerId);
                        mRenderer.selectHeadRotation(new Vector3(e.getX(pointerIndex), e.getY(pointerIndex), 0));
                        break;
                }
            } else
            //logic for scale
            {
                if (!scaleDetectorWasInProgress) {
                    startingScale = mRenderer.selectedForScale(new Vector3(e.getX(), e.getY(), 0),
                            new Vector3(e.getX(e.getPointerId(1)), e.getY(e.getPointerId(1)), 0));
                }

                mRenderer.zoom(startingScale + (mScaleFactor - 1) * 2f);
                scaleDetectorWasInProgress = true;
            }
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            return true;
        }
    }
}
