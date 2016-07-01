package theugateam.progetto;

import android.content.Context;

import theugateam.progetto.Utils.AnimationHandler;
import theugateam.progetto.Utils.Vector3;

public class Model3DVBOAnimated extends Model3DVBO {

    private enum STATE {IDLE, ANIMATION}

    private STATE state;

    private AnimationHandler rotationAnimation;
    private AnimationHandler scaleAnimation;

    public Model3DVBOAnimated(Context context) {
        super(context);
        rotationAnimation = new AnimationHandler();
        scaleAnimation = new AnimationHandler();
        state = STATE.IDLE;
    }

    public void update(float frameTime) {

        if (state == STATE.ANIMATION) {
            Vector3 v=rotationAnimation.update(frameTime);
            super.resetRotation(new Vector3(v.x(),v.y(),v.z()));
            super.scale(scaleAnimation.update(frameTime));
            if (rotationAnimation.isEnded())
                state = STATE.IDLE;
        }
    }

    public void initializeResetAnimation(Vector3 targetRotation, Vector3 targetScale, float speed) {
        if (state == STATE.IDLE) {
            rotationAnimation.initialize(this.getRotation(), targetRotation, speed);
            scaleAnimation.initialize(this.getScale(), targetScale, speed);
            state = STATE.ANIMATION;
        }
    }

    //impedisce la rotazione nel caso sia in corso un'animazione
    @Override
    public void rotate(Vector3 rotationVector) {
        if (state == STATE.IDLE)
            super.rotate(rotationVector);
    }

    //impedisce lo zoom nel caso sia in corso un'animazione
    @Override
    public void setGlobalScale(float newScale) {
        if (state == STATE.IDLE)
            super.setGlobalScale(newScale);
    }

    //ritorna lo sta in cui si trova l'oggetto
    public boolean isIdling()
    {
        if(state == STATE.IDLE)
            return true;
        return false;
    }
}
