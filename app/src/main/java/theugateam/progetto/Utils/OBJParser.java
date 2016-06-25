package theugateam.progetto.Utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OBJParser {

    private float[] V = {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f};

    private float[] VT = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f};

    private int F[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices

    private List<String> file;

    public OBJParser() {
    }

    //carica ogni riga del file dentro la struttura "file"
    //ritorna vero se ha caricato qualcosa
    public boolean loadFromOBJ(Context context, String name) {
        BufferedReader br = null;
        String sCurrentLine;
        file = new ArrayList<>();
        boolean flag = false;

        try {
            InputStream is = context.getAssets().open(name + ".obj");
            InputStreamReader inputStreamReader = new InputStreamReader(is);

            br = new BufferedReader(inputStreamReader);
            while ((sCurrentLine = br.readLine()) != null) {
                //legge ogni riga saltando quelle che contengono commenti (contrassegnate da #)
                if ((sCurrentLine.length() >= 1) && (sCurrentLine.charAt(0) != '#')) {
                    file.add(sCurrentLine);
                }
            }
            flag = true;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return flag;
    }

    //costruisce i dati della geometria dell'oggetto analizzado il file precedentemente caricato
    private void createVertices() {
        List<Point> VList = new ArrayList<>();
        List<Point> VTList = new ArrayList<>();
        List<Integer> f0List = new ArrayList<>();
        List<Integer> f1List = new ArrayList<>();

        //per ogni riga
        for (String aFile : file) {
            //separa le componenti attraverso il carattere speciale "spazio"
            String[] str = aFile.split("\\s+");

            //str[0] identifica il tipo di dato che la riga contiene
            //se str[0].length() == 1 sicuramente non potrà contenere "vt"
            if (str[0].length() == 1) {
                //se è un vertice (contrassegnato da "v")
                if (str[0].charAt(0) == 'v') {
                    VList.add(new Point(Float.parseFloat(str[1]), Float.parseFloat(str[2]), Float.parseFloat(str[3])));
                }
                //se è una riga che dice di quali punti (sia vertici sia texture) è composto ogni triangolo
                //(contrassegnato da "f")
                else if (str[0].charAt(0) == 'f') {
                    for (int i = 1; i < str.length; i++) {
                        String[] item = str[i].split("/");
                        //vertici
                        f0List.add(Integer.parseInt(item[0]) - 1);
                        //indici e vertici della texture
                        f1List.add(Integer.parseInt(item[1]) - 1);
                    }
                }
            }
            //se è una riga che contiene coordinate relative alla texture (contrassegnato da "vt")
            else if ((str[0].length() == 2) && (str[0].charAt(0) == 'v') && (str[0].charAt(1) == 't')) {
                VTList.add(new Point(Float.parseFloat(str[1]), Float.parseFloat(str[2]), 0));
            }
        }

        //ora ordina vertici e vertici texture in modo da rispettare gli indici
        V = new float[f0List.size() * 3];
        VT = new float[f0List.size() * 2];
        F = new int[f0List.size()];

        Iterator<Integer> iterator0 = f0List.iterator();
        Iterator<Integer> iterator1 = f1List.iterator();

        int countV = 0;
        int countVT = 0;
        int count0 = 0;
        while (iterator0.hasNext()) {
            int n0 = iterator0.next();
            int n1 = iterator1.next();

            V[countV++] = VList.get(n0).getX();
            V[countV++] = VList.get(n0).getY();
            V[countV++] = VList.get(n0).getZ();

            VT[countVT++] = VTList.get(n1).getX();
            VT[countVT++] = 1 - VTList.get(n1).getY();
            //OBJ considera (0, 0) in alto a sinistra della texture
            //OpenGL considera (0, 0) in basso a sinistra


            F[count0] = count0;
            count0++;
        }
    }


    public float[] getVertices() {
        createVertices();
        return V;
    }

    public float[] getUVVertices() {
        return VT;
    }

    public int[] getOrder() {
        return F;
    }


    //classe di supporto che rappresenta un punto
    class Point {

        float x, y, z;

        public Point() {}

        public Point(float valueX, float valueY, float valueZ) {
            x = valueX;
            y = valueY;
            z = valueZ;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }
    }
}
