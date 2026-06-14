package repmp3;

public class LineaLetra {
    private long milisegundos;
    private String texto;

    public LineaLetra(long milisegundos, String texto) {
        this.milisegundos = milisegundos;
        this.texto = texto;
    }

    public long getMilisegundos() { return milisegundos; }
    public String getTexto() { return texto; }
}