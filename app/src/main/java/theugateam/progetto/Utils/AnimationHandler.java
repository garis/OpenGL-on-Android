package theugateam.progetto.Utils;

// classe per gestire la transizione tra due stati di un'animazione
public class AnimationHandler {

    private Vector3 startingPoint;
    private Vector3 endingPoint;
    private Vector3 currentPoint;
    private float position;
    private float time;

    //consigliati valori da 0 a 1
    private float speed;

    private boolean ended;

    public AnimationHandler()
    {
        startingPoint=new Vector3(0,0,0);
        endingPoint=new Vector3(0,0,0);
        currentPoint=new Vector3(0,0,0);
        position=0;
        time=0;
        speed=0;
        ended=false;
    }

    public void initialize(Vector3 starting,Vector3 ending, float animationSpeed)
    {
        startingPoint=new Vector3(starting.x(),starting.y(),starting.z());
        endingPoint=new Vector3(ending.x(),ending.y(),ending.z());
        currentPoint=new Vector3(0,0,0);
        position=0;
        time=0;
        speed=animationSpeed;
        ended=false;
    }

    public Vector3 update(float frameTime)
    {
        if(!ended) {
            time = time + frameTime*speed;
            if (time >= 1) {
                currentPoint.x(endingPoint.x());
                currentPoint.y(endingPoint.y());
                currentPoint.z(endingPoint.z());
                ended = true;
            } else {
                //y=x^2*(-2*x+3)
                position = time * time * (-2 * time + 3);
                currentPoint.x(endingPoint.x() * position + startingPoint.x() * (1 - position));
                currentPoint.y(endingPoint.y() * position + startingPoint.y() * (1 - position));
                currentPoint.z(endingPoint.z() * position + startingPoint.z() * (1 - position));
            }
        }
        return currentPoint;
    }

    public boolean isEnded()
    {
        return ended;
    }
}
