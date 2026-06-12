package tarea2;

import javazoom.jlgui.basicplayer.BasicPlayer;
import java.io.File;
import java.io.FileReader;
import java.io.PushbackReader;
import java.util.Timer;
import java.util.TimerTask;

// Importaciones esenciales de tu compilador SableCC
import tarea2.lexer.Lexer;
import tarea2.parser.Parser;
import tarea2.node.Start;

public class Reproductor {

    private BasicPlayer player;

    // Constructor del reproductor
    Reproductor(){
        player = new BasicPlayer();
    }

    // Métodos de control del audio (BasicPlayer)
    public void Play() throws Exception {
        player.play();
    }

    public void AbrirFichero(String ruta) throws Exception {
        player.open(new File(ruta));
    }

    public void Pausa() throws Exception {
        player.pause();
    }

    public void Continuar() throws Exception {
        player.resume();
    }

    public void Stop() throws Exception {
        player.stop();
    }

    // --- MÉTODO PRINCIPAL (PUNTO DE ENTRADA) ---
    public static void main(String args[]){
        try {
            System.out.println("=================================");
            System.out.println("      INICIANDO REPRODUCTOR");
            System.out.println("=================================");

            // 1. PASO COMPILADOR: Leer y parsear el archivo LRC de la letra
            // RECUERDA: Modifica esta ruta por la de tu computadora
            String rutaLrc = "The Weeknd - Blinding Lights.lrc";

            FileReader fileReader = new FileReader(rutaLrc);
            PushbackReader pushbackReader = new PushbackReader(fileReader, 1024);

            Lexer lexer = new Lexer(pushbackReader);
            Parser parser = new Parser(lexer);

            // Construimos el Árbol del archivo LRC
            Start tree = parser.parse();

            // Extraemos los datos usando el Visitador de la Etapa 2
            LrcVisitor miVisitador = new LrcVisitor();
            tree.apply(miVisitador);

            // 2. MOSTRAR METADATOS: Imprimir el título y el artista extraídos del archivo LRC
            System.out.println("Canción: " + miVisitador.titulo);
            System.out.println("Artista: " + miVisitador.artista);
            System.out.println("[Parser] -> Letras cargadas en memoria: " + miVisitador.listaLetras.size() + " líneas.");

            // 3. PASO AUDIO: Inicializar el reproductor MP3
            Reproductor mi_reproductor = new Reproductor();

            // RECUERDA: Modifica esta ruta por la de tu archivo de audio MP3
            String rutaMp3 = "The Weeknd - Blinding Lights.mp3";
            mi_reproductor.AbrirFichero(rutaMp3);

            // 4. PASO SINCRO: Dar Play e inmediatamente programar las alarmas (Timers)
            System.out.println("[Audio]  -> Reproduciendo música...");
            System.out.println("=================================================\n");

            mi_reproductor.Play();

            // Programamos una alarma independiente para cada línea de la canción
            for (LineaLetra linea : miVisitador.listaLetras) {
                // Pasamos el tiempo absoluto en milisegundos y el texto correspondiente
                mi_reproductor.new Reminder(linea.tiempoMs, linea.texto);
            }

        } catch (Exception ex) {
            System.out.println("\n[ERROR] Ocurrió un fallo en la ejecución:");
            ex.printStackTrace();
        }
    }

    public class Reminder {
        Timer timer;
        String texto;

        // Modificamos el constructor para recibir el retraso exacto en milisegundos
        public Reminder(long milisegundos, String texto) {
            this.texto = texto;
            this.timer = new Timer();

            // Programamos la tarea 'RemindTask' para que "despierte" tras 'milisegundos'
            timer.schedule(new RemindTask(), milisegundos);
        }

        // Esta es la tarea asíncrona que se ejecuta al cumplirse el tiempo
        class RemindTask extends TimerTask {
            @Override
            public void run() {
                // Desplegamos el subtítulo con el formato deseado
                if (texto.isEmpty()) {
                    // Si es una línea de silencio/instrumental, dejamos un espacio visual elegante
                    System.out.println("   ♪ ♪ ♪   ");
                } else {
                    // Formato de Karaoke limpio para la consola
                    System.out.println(" > " + texto);
                }

                // Liberamos los recursos del timer una vez usado para no saturar la memoria
                timer.cancel();
            }
        }
    }
}