package theugateam.progetto;

import android.content.Context;
import android.opengl.GLSurfaceView;
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

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getPointerCount() == 1) {
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
        return true;
    }

}
