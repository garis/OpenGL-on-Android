package theugateam.progetto;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class OpenGL extends AppCompatActivity {

    private GLSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //istanzia la classe di rendering
        mGLView = new MyGLSurfaceView(this);
        //attiva la classe di rendering
        setContentView(mGLView);

        //mantiene il thread di rendering anche con l'app in background
        mGLView.setPreserveEGLContextOnPause(true);
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
