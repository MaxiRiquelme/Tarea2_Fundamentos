package tarea2;

import tarea2.analysis.DepthFirstAdapter;
import tarea2.node.ALyricLine;
import tarea2.node.AMetaLine;
import java.util.ArrayList;

public class LrcVisitor extends DepthFirstAdapter {

    public ArrayList<LineaLetra> listaLetras = new ArrayList<>();

    // Metadatos de la canción con valores por defecto
    public String titulo = "Desconocido";
    public String artista = "Desconocido";
    public String album = "Desconocido";

    @Override
    public void outAMetaLine(AMetaLine node) {
        // Obtener la etiqueta de metadatos en formato String
        String metaTag = node.getMetaTag().getText();

        // Extraer la información dependiendo del tipo: Título (ti), Artista (ar) o Álbum (al)
        if (metaTag.startsWith("[ti:")) {
            // Extraer el título omitiendo "[ti:" (primeros 4 caracteres) y el "]" final
            titulo = metaTag.substring(4, metaTag.length() - 1).trim();
        }
        else if (metaTag.startsWith("[ar:")) {
            // Extraer el artista omitiendo "[ar:" y el "]" final
            artista = metaTag.substring(4, metaTag.length() - 1).trim();
        }
        else if (metaTag.startsWith("[al:")) {
            // Extraer el álbum omitiendo "[al:" y el "]" final
            album = metaTag.substring(4, metaTag.length() - 1).trim();
        }
    }

    @Override
    public void outALyricLine(ALyricLine node) {
        // Obtener la marca de tiempo en formato String (ej: "[01:05.41]")
        String timeTag = node.getTimeTag().getText();

        // Extraer los números usando substring
        // [01:05.41] -> min="01", sec="05", cent="41"
        int min = Integer.parseInt(timeTag.substring(1, 3));
        int sec = Integer.parseInt(timeTag.substring(4, 6));
        int cent = Integer.parseInt(timeTag.substring(7, 9));

        // Convertir todo a Milisegundos
        // 1 min = 60,000 ms | 1 seg = 1000 ms | 1 centésima = 10 ms
        long milisegundos = (min * 60 * 1000) + (sec * 1000) + (cent * 10);

        // Obtener el texto de la letra (si es que hay)
        String texto = "";
        if (node.getText() != null) {
            texto = node.getText().getText().trim(); // .trim() quita los espacios sobrantes
        }

        // Guardar en nuestra lista
        listaLetras.add(new LineaLetra(milisegundos, texto));
    }
}