package repmp3;

import repmp3.analysis.DepthFirstAdapter;
import repmp3.node.ALyricLine;
import repmp3.node.AMetaLine;

import java.util.ArrayList;
import java.util.List;

public class LrcVisitor extends DepthFirstAdapter {

    private List<LineaLetra> repositorio = new ArrayList<>();
    private MetadataLetra metadata = new MetadataLetra();

    public List<LineaLetra> getRepositorio() { return repositorio; }
    public MetadataLetra getMetadata() { return metadata; }

    @Override
    public void caseAMetaLine(AMetaLine node) {
        if (node.getMetaTag() != null) {
            String meta = node.getMetaTag().getText();
            meta = meta.substring(1, meta.length() - 1);
            String[] partes = meta.split(":", 2);

            if (partes.length == 2) {
                String clave = partes[0].trim().toLowerCase();
                String valor = partes[1].trim();

                if (clave.equals("ti")) {
                    metadata.setTitulo(valor);
                } else if (clave.equals("ar")) {
                    metadata.setArtista(valor);
                } else if (clave.equals("al")) {
                    metadata.setAlbum(valor);
                }
            }
        }
    }

    @Override
    public void caseALyricLine(ALyricLine node) {
        if (node.getTimeTag() != null) {
            String t = node.getTimeTag().getText();
            int min = Integer.parseInt(t.substring(1, 3));
            int seg = Integer.parseInt(t.substring(4, 6));
            int cent = Integer.parseInt(t.substring(7, 9));

            long totalMilisegundos = (min * 60000L) + (seg * 1000L) + (cent * 10L);

            String textoLetra = "";
            if (node.getText() != null) {
                textoLetra = node.getText().getText().trim();
            }

            //Validar si la letra está vacía para mostrar Instrumental ---
            if (textoLetra.isEmpty()) {
                textoLetra = "♪ ... (Instrumental) ... ♪";
            }

            repositorio.add(new LineaLetra(totalMilisegundos, textoLetra));
        }
    }
}