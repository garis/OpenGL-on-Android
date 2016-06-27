package theugateam.progetto;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

public class OpenGL extends Activity {

    private GLSurfaceView mGLView;
    private View decorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //oggetto usato per mantenere nascoste le barre di stato e di navigazione di android
        decorView = getWindow().getDecorView();

        //istanzia la classe di rendering
        mGLView = new MyGLSurfaceView(this);
        //attiva la classe di rendering
        setContentView(mGLView);

        //mantiene il thread di rendering anche con l'app in background
        mGLView.setPreserveEGLContextOnPause(true);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //ogni volta che l'activity sarà in primo piano vengono reimpostati tutti i flag in modo
        //che l'app sia a schermo intero senza la barra di navigazione e senza la barra di stato
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mette in pausa il thread di rendering
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Riesuma il thread di rendering precedentemente messo in pausa
        mGLView.onResume();
    }
}
