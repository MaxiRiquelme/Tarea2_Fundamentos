package tarea2;

import tarea2.analysis.DepthFirstAdapter;
import tarea2.node.ALyricLine;
import java.util.ArrayList;

public class LrcVisitor extends DepthFirstAdapter {

    // Aquí guardaremos toda la canción ya procesada
    public ArrayList<LineaLetra> listaLetras = new ArrayList<>();

    @Override
    public void outALyricLine(ALyricLine node) {
        // 1. Obtener la marca de tiempo en formato String (ej: "[01:05.41]")
        String timeTag = node.getTimeTag().getText();

        // 2. Extraer los números usando substring
        // [01:05.41] -> min="01", sec="05", cent="41"
        int min = Integer.parseInt(timeTag.substring(1, 3));
        int sec = Integer.parseInt(timeTag.substring(4, 6));
        int cent = Integer.parseInt(timeTag.substring(7, 9));

        // 3. Convertir todo a Milisegundos
        // 1 min = 60,000 ms | 1 seg = 1000 ms | 1 centésima = 10 ms
        long milisegundos = (min * 60 * 1000) + (sec * 1000) + (cent * 10);

        // 4. Obtener el texto de la letra (si es que hay)
        String texto = "";
        if (node.getText() != null) {
            texto = node.getText().getText().trim(); // .trim() quita los espacios sobrantes
        }

        // 5. Guardar en nuestra lista
        listaLetras.add(new LineaLetra(milisegundos, texto));
    }
}