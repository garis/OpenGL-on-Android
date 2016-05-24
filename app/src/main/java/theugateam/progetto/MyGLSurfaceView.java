package theugateam.progetto;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import theugateam.progetto.Utils.Vector3;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;

    public MyGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer();
        mRenderer.initialize(context);
        setRenderer(mRenderer);

        // Render continuosly per poter usare l'ingranaggio di caricamento
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //Log.d("debug", "" + e.getPointerCount());
        if (e.getPointerCount() == 1)
        {
            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    mRenderer.touchMove(new Vector3(e.getX(), e.getY(), 0));
                    break;
                case MotionEvent.ACTION_DOWN:
                    mRenderer.touchDown(new Vector3(e.getX(), e.getY(), 0));
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
        }
        else if(e.getPointerCount() == 2)
        {
            mRenderer.zoom( new Vector3(e.getX(e.getPointerId(0)), e.getY(e.getPointerId(0)), 0),
                            new Vector3(e.getX(e.getPointerId(1)), e.getY(e.getPointerId(1)), 0));
        }

        return true;
    }

}
