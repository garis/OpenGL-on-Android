package theugateam.progetto.Utils;

// classe utile per rappresentare vettori o punti
public class Vector3 {

    private double xyz[] = new double[3];

    public Vector3() {
        xyz[0] = 0;
        xyz[1] = 0;
        xyz[2] = 0;
    }

    public Vector3(double x, double y, double z) {
        xyz[0] = x;
        xyz[1] = y;
        xyz[2] = z;
    }

    public Vector3 add(Vector3 rhs) {
        return new Vector3(
                xyz[0] + rhs.xyz[0],
                xyz[1] + rhs.xyz[1],
                xyz[2] + rhs.xyz[2]);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Vector3) {
            Vector3 rhs = (Vector3) obj;

            return xyz[0] == rhs.xyz[0] &&
                    xyz[1] == rhs.xyz[1] &&
                    xyz[2] == rhs.xyz[2];
        } else {
            return false;
        }

    }


    public void x(double newValue)
    {
        xyz[0] = newValue;
    }
    public void y(double newValue)
    {
        xyz[1] = newValue;
    }
    public void z(double newValue)
    {
        xyz[2] = newValue;
    }

    public double x() { return xyz[0]; }

    public double y() { return xyz[1]; }

    public double z() {
        return xyz[2];
    }

    public String toString() {
        return "( " + xyz[0] + " " + xyz[1] + " " + xyz[2] + " )";
    }
}
