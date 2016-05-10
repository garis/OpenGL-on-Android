package theugateam.progetto;

import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class OpenGL extends AppCompatActivity {

    public static boolean DEBUG = false;

    private GLSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideUIStuff();

        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);

        mGLView.setPreserveEGLContextOnPause(true);
    }

    private void hideUIStuff() {
        View decorView = getWindow().getDecorView();

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideUIStuff();

        // The following call resumes a paused rendering thread.
        mGLView.onResume();
    }
}
